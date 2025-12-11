package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.resource.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import org.fyp.tmssep490be.exceptions.*;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeSlotTemplateService {

    private final TimeSlotTemplateRepository timeSlotTemplateRepository;
    private final BranchRepository branchRepository;
    private final UserAccountRepository userAccountRepository;
    private final SessionRepository sessionRepository;

    // Lấy danh sách khung giờ
    @Transactional(readOnly = true)
    public List<TimeSlotResponseDTO> getAllTimeSlots(Long branchId, String search, Long userId) {
        log.info("Getting all time slots - branchId: {}, search: {}, userId: {}", branchId, search, userId);

        // 1. Lấy tất cả branches user có quyền
        List<Long> userBranches = getBranchIdsForUser(userId);
        if (userBranches.isEmpty()) {
            log.warn("User {} has no branch access", userId);
            return List.of();
        }

        // 2. Nếu client gửi branchId → validate quyền truy cập
        if (branchId != null && !userBranches.contains(branchId)) {
            throw new BusinessRuleException("ACCESS_DENIED", "Không có quyền truy cập chi nhánh này");
        }

        // 3. Query theo branchId (hoặc tất cả branches của user)
        List<TimeSlotTemplate> timeSlots;
        if (branchId != null) {
            timeSlots = timeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc(branchId);
        } else {
            // Nếu không có branchId → lấy từ tất cả branches user có quyền
            timeSlots = timeSlotTemplateRepository.findAll().stream()
                    .filter(ts -> userBranches.contains(ts.getBranch().getId()))
                    .collect(Collectors.toList());
        }

        // 4. Filter by search
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            timeSlots = timeSlots.stream()
                    .filter(ts -> ts.getName().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }

        return timeSlots.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TimeSlotResponseDTO getTimeSlotById(Long id) {
        log.info("Getting time slot by id: {}", id);
        TimeSlotTemplate timeSlot = timeSlotTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));
        return convertToDTO(timeSlot);
    }

    // Tạo khung giờ mới
    @Transactional
    public TimeSlotResponseDTO createTimeSlot(TimeSlotRequestDTO request, Long userId, Long forcedBranchId) {
        log.info("Creating time slot: {}", request);

        // 1. Xác định branchId
        Long branchId = forcedBranchId != null ? forcedBranchId : request.getBranchId();

        // 2. Validate quyền truy cập
        List<Long> userBranches = getBranchIdsForUser(userId);
        if (!userBranches.contains(branchId)) {
            throw new BusinessRuleException("ACCESS_DENIED", "Bạn không có quyền truy cập chi nhánh này");
        }

        // 3. Lấy branch entity
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

        // 4. Validate tên
        String name = request.getName() != null ? request.getName().trim() : null;
        if (name == null || name.isEmpty()) {
            throw new BusinessRuleException("Vui lòng nhập tên khung giờ");
        }

        // 5. Validate thời gian
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessRuleException("Vui lòng nhập giờ bắt đầu và giờ kết thúc");
        }
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalTime endTime = LocalTime.parse(request.getEndTime());

        // 6. Check endTime > startTime
        if (!endTime.isAfter(startTime)) {
            throw new BusinessRuleException("Giờ kết thúc phải lớn hơn giờ bắt đầu");
        }

        // 7. Check tên không trùng
        if (timeSlotTemplateRepository.existsByBranchIdAndNameIgnoreCase(branchId, name, null)) {
            throw new BusinessRuleException("Tên khung giờ đã tồn tại trong chi nhánh này");
        }

        // 8. Check thời gian không trùng
        if (timeSlotTemplateRepository.existsByBranchIdAndStartTimeAndEndTime(branchId, startTime, endTime, null)) {
            throw new BusinessRuleException("Khung giờ này đã tồn tại trong chi nhánh");
        }

        // 9. Tạo entity và lưu
        TimeSlotTemplate timeSlot = new TimeSlotTemplate();
        timeSlot.setBranch(branch);
        timeSlot.setName(name);
        timeSlot.setStartTime(startTime);
        timeSlot.setEndTime(endTime);
        timeSlot.setStatus(ResourceStatus.ACTIVE);
        timeSlot.setCreatedAt(OffsetDateTime.now());
        timeSlot.setUpdatedAt(OffsetDateTime.now());

        TimeSlotTemplate saved = timeSlotTemplateRepository.save(timeSlot);
        log.info("Created time slot with ID: {}", saved.getId());
        return convertToDTO(saved);
    }

    // Cập nhật khung giờ
    @Transactional
    public TimeSlotResponseDTO updateTimeSlot(Long id, TimeSlotRequestDTO request, Long userId) {
        log.info("Updating time slot {}: {}", id, request);

        TimeSlotTemplate timeSlot = timeSlotTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));

        Long branchId = timeSlot.getBranch().getId();

        if (request.getName() != null) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(timeSlot.getName())) {
                if (timeSlotTemplateRepository.existsByBranchIdAndNameIgnoreCase(branchId, newName, id)) {
                    throw new BusinessRuleException("Tên khung giờ đã tồn tại");
                }
            }
            timeSlot.setName(newName);
        }

        if (request.getStartTime() != null || request.getEndTime() != null) {
            LocalTime newStartTime = request.getStartTime() != null
                    ? LocalTime.parse(request.getStartTime())
                    : timeSlot.getStartTime();
            LocalTime newEndTime = request.getEndTime() != null
                    ? LocalTime.parse(request.getEndTime())
                    : timeSlot.getEndTime();

            if (!newEndTime.isAfter(newStartTime)) {
                throw new BusinessRuleException("Giờ kết thúc phải lớn hơn giờ bắt đầu");
            }

            boolean isTimeChanged = !newStartTime.equals(timeSlot.getStartTime())
                    || !newEndTime.equals(timeSlot.getEndTime());
            if (isTimeChanged) {
                if (sessionRepository.existsByTimeSlotTemplateId(id)) {
                    throw new BusinessRuleException("Không thể thay đổi thời gian vì đang được sử dụng");
                }
                if (timeSlotTemplateRepository.existsByBranchIdAndStartTimeAndEndTime(branchId, newStartTime,
                        newEndTime, id)) {
                    throw new BusinessRuleException("Khung giờ này đã tồn tại");
                }
                timeSlot.setStartTime(newStartTime);
                timeSlot.setEndTime(newEndTime);
            }
        }

        timeSlot.setUpdatedAt(OffsetDateTime.now());
        TimeSlotTemplate saved = timeSlotTemplateRepository.save(timeSlot);
        return convertToDTO(saved);
    }

    // Xóa khung giờ
    @Transactional
    public void deleteTimeSlot(Long id) {
        log.info("Deleting time slot {}", id);

        TimeSlotTemplate timeSlot = timeSlotTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));

        // Phải ngưng hoạt động trước khi xóa
        if (timeSlot.getStatus() != ResourceStatus.INACTIVE) {
            throw new BusinessRuleException("Vui lòng ngưng hoạt động trước khi xóa");
        }

        // Không thể xóa nếu có session đang dùng
        if (sessionRepository.existsByTimeSlotTemplateId(id)) {
            throw new BusinessRuleException("Không thể xóa vì đang được sử dụng");
        }

        timeSlotTemplateRepository.deleteById(id);
    }

    // Đổi trạng thái hoạt động/ngưng hoạt động
    @Transactional
    public TimeSlotResponseDTO updateTimeSlotStatus(Long id, ResourceStatus status) {
        log.info("Updating status for time slot {}: {}", id, status);

        TimeSlotTemplate timeSlot = timeSlotTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));

        // Nếu ngưng hoạt động → check không có session tương lai
        if (status == ResourceStatus.INACTIVE) {
            Long futureSessions = sessionRepository.countFutureSessionsByTimeSlotId(id, LocalDate.now(),
                    LocalTime.now());
            if (futureSessions > 0) {
                throw new BusinessRuleException(
                        "Không thể ngưng hoạt động vì có " + futureSessions + " lớp học sắp diễn ra");
            }
        }

        timeSlot.setStatus(status);
        timeSlot.setUpdatedAt(OffsetDateTime.now());
        TimeSlotTemplate saved = timeSlotTemplateRepository.save(timeSlot);
        return convertToDTO(saved);
    }

    // Lấy danh sách sessions đang dùng khung giờ
    @Transactional(readOnly = true)
    public List<SessionInfoDTO> getSessionsByTimeSlotId(Long id) {
        if (!timeSlotTemplateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Time slot not found with id: " + id);
        }
        List<Session> sessions = sessionRepository.findByTimeSlotTemplateId(id);
        return sessions.stream().map(this::convertSessionToDTO).collect(Collectors.toList());
    }

    // Helper: chuyển Session → DTO
    private SessionInfoDTO convertSessionToDTO(Session session) {
        return SessionInfoDTO.builder()
                .id(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .className(session.getClassEntity().getName())
                .date(session.getDate().toString())
                .startTime(session.getTimeSlotTemplate().getStartTime().toString())
                .endTime(session.getTimeSlotTemplate().getEndTime().toString())
                .status(session.getStatus().toString())
                .type(session.getType().toString())
                .build();
    }

    // Lấy danh sách khung giờ cho dropdown
    @Transactional(readOnly = true)
    public List<TimeSlotTemplateDTO> getBranchTimeSlotTemplates(Long branchId) {
        List<TimeSlotTemplate> timeSlots = timeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc(branchId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return timeSlots.stream()
                .map(ts -> TimeSlotTemplateDTO.builder()
                        .id(ts.getId())
                        .name(ts.getName())
                        .startTime(ts.getStartTime().toString())
                        .endTime(ts.getEndTime().toString())
                        .displayName(ts.getStartTime().format(formatter) + " - " + ts.getEndTime().format(formatter))
                        .build())
                .collect(Collectors.toList());
    }

    // Lấy danh sách branchId của user
    private List<Long> getBranchIdsForUser(Long userId) {
        if (userId == null)
            return List.of();
        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        if (user != null && !user.getUserBranches().isEmpty()) {
            return user.getUserBranches().stream()
                    .map(ub -> ub.getBranch().getId())
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private TimeSlotResponseDTO convertToDTO(TimeSlotTemplate ts) {
        TimeSlotResponseDTO.TimeSlotResponseDTOBuilder builder = TimeSlotResponseDTO.builder()
                .id(ts.getId())
                .branchId(ts.getBranch().getId())
                .branchName(ts.getBranch().getName())
                .name(ts.getName())
                .startTime(ts.getStartTime().toString())
                .endTime(ts.getEndTime().toString())
                .createdAt(ts.getCreatedAt() != null ? ts.getCreatedAt().toString() : null)
                .updatedAt(ts.getUpdatedAt() != null ? ts.getUpdatedAt().toString() : null)
                .status(ts.getStatus().name());

        try {
            Long activeClasses = sessionRepository.countDistinctClassesByTimeSlotId(ts.getId());
            Long totalSessions = sessionRepository.countSessionsByTimeSlotId(ts.getId());
            Long futureSessions = sessionRepository.countFutureSessionsByTimeSlotId(ts.getId(), LocalDate.now(),
                    LocalTime.now());
            builder.activeClassesCount(activeClasses)
                    .totalSessionsCount(totalSessions)
                    .hasAnySessions(totalSessions > 0)
                    .hasFutureSessions(futureSessions > 0);
        } catch (Exception e) {
            log.error("Error calculating statistics: {}", e.getMessage());
            builder.activeClassesCount(0L).totalSessionsCount(0L)
                    .hasAnySessions(false).hasFutureSessions(false);
        }
        return builder.build();
    }
}