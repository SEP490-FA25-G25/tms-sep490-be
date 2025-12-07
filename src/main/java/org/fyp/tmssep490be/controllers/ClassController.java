package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classdetail.ClassDetailDTO;
import org.fyp.tmssep490be.dtos.classmanagement.ClassListItemDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.ClassService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Slf4j
public class ClassController {

    private final ClassService classService;

    //Endpoint để lấy thông tin lớp học theo ID(cho TEACHER, STUDENT, ACADEMIC_AFFAIR, etc.)
//   @GetMapping("/{classId}")
//   @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ACADEMIC_AFFAIR') or hasRole('CENTER_HEAD') or hasRole('MANAGER')")
//   public ResponseEntity<ResponseObject<ClassDetailDTO>> getClassDetail(
//           @PathVariable Long classId,
//           @AuthenticationPrincipal UserPrincipal userPrincipal) {
//
//       // Lấy thông tin chi tiết lớp học từ service
//       ClassDetailDTO classDetail = classService.getClassDetail(classId);
//
//        return ResponseEntity.ok(
//               ResponseObject.<ClassDetailDTO>builder()
//                       .success(true)
//                       .message("Class details retrieved successfully")
//                       .data(classDetail)
//                       .build());
//   }

    @GetMapping
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR') or hasRole('CENTER_HEAD') or hasRole('TEACHER') or hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<Page<ClassListItemDTO>>> getClasses(
           @RequestParam(required = false) List<Long> branchIds,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) ClassStatus status,
            @RequestParam(required = false) ApprovalStatus approvalStatus,
            @RequestParam(required = false) Modality modality,
           @RequestParam(required = false) String search,
           @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate") String sort,
            @RequestParam(defaultValue = "asc") String sortDir,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info(
                "User {} requesting classes list with filters: branchIds={}, courseId={}, status={}, approvalStatus={}, modality={}, search={}",
                currentUser.getId(), branchIds, courseId, status, approvalStatus, modality, search);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<ClassListItemDTO> classes = classService.getClasses(
                branchIds, courseId, status, approvalStatus, modality, search, pageable, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<Page<ClassListItemDTO>>builder()
                .success(true)
                .message("Classes retrieved successfully")
                .data(classes)
                .build());
    }
}
