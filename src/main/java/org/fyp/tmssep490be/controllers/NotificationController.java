package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.notification.NotificationDTO;
import org.fyp.tmssep490be.dtos.notification.NotificationFilterDTO;
import org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO;
import org.fyp.tmssep490be.dtos.notification.NotificationStatsDTO;
import org.fyp.tmssep490be.entities.enums.NotificationStatus;
import org.fyp.tmssep490be.services.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.fyp.tmssep490be.security.UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        throw new RuntimeException("Không xác định được người dùng hiện tại");
    }

    @GetMapping
    public ResponseEntity<ResponseObject<Page<NotificationDTO>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {

        try {
            Long userId = getCurrentUserId();

            NotificationFilterDTO filter = NotificationFilterDTO.builder()
                    .status(status)
                    .search(search)
                    .build();

            if (type != null) {
                try {
                    filter.setType(org.fyp.tmssep490be.entities.enums.NotificationType.valueOf(type.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid notification type: {}", type);
                }
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<NotificationDTO> notifications = notificationService.getUserNotifications(userId, filter, pageable);

            return ResponseEntity.ok(ResponseObject.<Page<NotificationDTO>>builder()
                    .success(true)
                    .message("Lấy danh sách thông báo thành công")
                    .data(notifications)
                    .build());

        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách thông báo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.<Page<NotificationDTO>>builder()
                            .success(false)
                            .message("Lỗi khi lấy danh sách thông báo: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseObject<NotificationDTO>> getNotification(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            NotificationDTO notification = notificationService.getNotificationById(id, userId);

            return ResponseEntity.ok(ResponseObject.<NotificationDTO>builder()
                    .success(true)
                    .message("Lấy thông tin thông báo thành công")
                    .data(notification)
                    .build());

        } catch (RuntimeException e) {
            log.error("Lỗi khi lấy thông tin thông báo: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseObject.<NotificationDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin thông báo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.<NotificationDTO>builder()
                            .success(false)
                            .message("Lỗi khi lấy thông tin thông báo: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD', 'ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<NotificationDTO>> createNotification(
            @Valid @RequestBody NotificationRequestDTO request) {
        try {
            var notification = notificationService.createNotificationFromRequest(request);
            NotificationDTO notificationDTO = NotificationDTO.fromEntity(notification);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseObject.<NotificationDTO>builder()
                            .success(true)
                            .message("Tạo thông báo thành công")
                            .data(notificationDTO)
                            .build());

        } catch (Exception e) {
            log.error("Lỗi khi tạo thông báo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.<NotificationDTO>builder()
                            .success(false)
                            .message("Lỗi khi tạo thông báo: " + e.getMessage())
                            .build());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD', 'ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<NotificationDTO>> updateNotification(
            @PathVariable Long id,
            @Valid @RequestBody NotificationRequestDTO request) {
        try {
            NotificationDTO notification = notificationService.updateNotification(id, request);

            return ResponseEntity.ok(ResponseObject.<NotificationDTO>builder()
                    .success(true)
                    .message("Cập nhật thông báo thành công")
                    .data(notification)
                    .build());

        } catch (RuntimeException e) {
            log.error("Lỗi khi cập nhật thông báo: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseObject.<NotificationDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật thông báo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.<NotificationDTO>builder()
                            .success(false)
                            .message("Lỗi khi cập nhật thông báo: " + e.getMessage())
                            .build());
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ResponseObject<Void>> markAsRead(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            notificationService.markAsRead(id, userId);

            return ResponseEntity.ok(ResponseObject.<Void>builder()
                    .success(true)
                    .message("Đánh dấu đã đọc thành công")
                    .build());

        } catch (RuntimeException e) {
            log.error("Lỗi khi đánh dấu đã đọc: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseObject.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Lỗi khi đánh dấu đã đọc", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.<Void>builder()
                            .success(false)
                            .message("Lỗi khi đánh dấu đã đọc: " + e.getMessage())
                            .build());
        }
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<ResponseObject<Integer>> markAllAsRead() {
        try {
            Long userId = getCurrentUserId();
            int updatedCount = notificationService.markAllAsRead(userId);

            return ResponseEntity.ok(ResponseObject.<Integer>builder()
                    .success(true)
                    .message("Đánh dấu đã đọc tất cả thành công")
                    .data(updatedCount)
                    .build());

        } catch (Exception e) {
            log.error("Lỗi khi đánh dấu đã đọc tất cả", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.<Integer>builder()
                            .success(false)
                            .message("Lỗi khi đánh dấu đã đọc tất cả: " + e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseObject<Void>> deleteNotification(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            notificationService.deleteNotification(id, userId);

            return ResponseEntity.ok(ResponseObject.<Void>builder()
                    .success(true)
                    .message("Xóa thông báo thành công")
                    .build());

        } catch (RuntimeException e) {
            log.error("Lỗi khi xóa thông báo: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseObject.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Lỗi khi xóa thông báo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.<Void>builder()
                            .success(false)
                            .message("Lỗi khi xóa thông báo: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/archive")
    public ResponseEntity<ResponseObject<Integer>> archiveNotifications(
            @RequestBody List<Long> notificationIds) {
        try {
            Long userId = getCurrentUserId();
            int archivedCount = notificationService.archiveNotifications(userId, notificationIds);

            return ResponseEntity.ok(ResponseObject.<Integer>builder()
                    .success(true)
                    .message("Lưu trữ thông báo thành công")
                    .data(archivedCount)
                    .build());

        } catch (Exception e) {
            log.error("Lỗi khi lưu trữ thông báo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.<Integer>builder()
                            .success(false)
                            .message("Lỗi khi lưu trữ thông báo: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ResponseObject<Long>> getUnreadCount() {
        try {
            Long userId = getCurrentUserId();
            Long unreadCount = notificationService.getUnreadCount(userId);

            return ResponseEntity.ok(ResponseObject.<Long>builder()
                    .success(true)
                    .message("Lấy số lượng thông báo chưa đọc thành công")
                    .data(unreadCount)
                    .build());

        } catch (Exception e) {
            log.error("Lỗi khi lấy số lượng thông báo chưa đọc", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.<Long>builder()
                            .success(false)
                            .message("Lỗi khi lấy số lượng thông báo chưa đọc: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ResponseObject<NotificationStatsDTO>> getUserStats() {
        try {
            Long userId = getCurrentUserId();
            NotificationStatsDTO stats = notificationService.getUserNotificationStats(userId);

            return ResponseEntity.ok(ResponseObject.<NotificationStatsDTO>builder()
                    .success(true)
                    .message("Lấy thống kê thông báo thành công")
                    .data(stats)
                    .build());

        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê thông báo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.<NotificationStatsDTO>builder()
                            .success(false)
                            .message("Lỗi khi lấy thống kê thông báo: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ResponseObject<NotificationStatsDTO>> getSystemStats() {
        try {
            NotificationStatsDTO stats = notificationService.getSystemNotificationStats();

            return ResponseEntity.ok(ResponseObject.<NotificationStatsDTO>builder()
                    .success(true)
                    .message("Lấy thống kê hệ thống thông báo thành công")
                    .data(stats)
                    .build());

        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê hệ thống thông báo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.<NotificationStatsDTO>builder()
                            .success(false)
                            .message("Lỗi khi lấy thống kê hệ thống thông báo: " + e.getMessage())
                            .build());
        }
    }

}
