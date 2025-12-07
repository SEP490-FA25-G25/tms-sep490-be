package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classmanagement.ClassListItemDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ClassRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Slf4j
public class ClassController {

    private final ClassService classService;
    private final ClassRepository classRepository;

    //Endpoint để lấy thông tin lớp học theo ID(cho TEACHER, STUDENT, ACADEMIC_AFFAIR, etc.)
    @GetMapping("/{classId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ACADEMIC_AFFAIR') or hasRole('CENTER_HEAD') or hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<Map<String, Object>>> getClassDetail(
            @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // Tìm lớp học theo ID
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND, "Class not found"));

        // Xây dựng map response
        Map<String, Object> classDetail = new HashMap<>();
        classDetail.put("id", classEntity.getId());
        classDetail.put("code", classEntity.getCode());
        classDetail.put("name", classEntity.getName());
        classDetail.put("modality", classEntity.getModality());
        classDetail.put("startDate", classEntity.getStartDate());
        classDetail.put("plannedEndDate", classEntity.getPlannedEndDate());
        classDetail.put("actualEndDate", classEntity.getActualEndDate());
        classDetail.put("status", classEntity.getStatus());
        classDetail.put("maxCapacity", classEntity.getMaxCapacity());
        classDetail.put("scheduleDays", classEntity.getScheduleDays() != null ? 
            java.util.Arrays.asList(classEntity.getScheduleDays()) : java.util.Collections.emptyList());
        
        // Thêm thông tin môn học
        if (classEntity.getSubject() != null) {
            Map<String, Object> course = new HashMap<>();
            course.put("id", classEntity.getSubject().getId());
            course.put("code", classEntity.getSubject().getCode());
            course.put("name", classEntity.getSubject().getName());
            classDetail.put("course", course);
        }
        
        // Thêm thông tin chi nhánh
        if (classEntity.getBranch() != null) {
            Map<String, Object> branch = new HashMap<>();
            branch.put("id", classEntity.getBranch().getId());
            branch.put("code", classEntity.getBranch().getCode());
            branch.put("name", classEntity.getBranch().getName());
            branch.put("address", classEntity.getBranch().getAddress());
            classDetail.put("branch", branch);
        }
        
        // TODO: Thêm thông tin giáo viên, tóm tắt đăng ký, các buổi học sắp tới, etc.
        classDetail.put("teachers", java.util.Collections.emptyList());
        Map<String, Object> enrollmentSummary = new HashMap<>();
        enrollmentSummary.put("currentEnrolled", 0);
        Integer maxCap = classEntity.getMaxCapacity() != null ? classEntity.getMaxCapacity() : 0;
        enrollmentSummary.put("maxCapacity", maxCap);
        enrollmentSummary.put("availableSlots", maxCap);
        enrollmentSummary.put("utilizationRate", 0.0);
        enrollmentSummary.put("canEnrollStudents", true);
        classDetail.put("enrollmentSummary", enrollmentSummary);
        classDetail.put("upcomingSessions", java.util.Collections.emptyList());
        classDetail.put("scheduleSummary", "");

        return ResponseEntity.ok(
                ResponseObject.<Map<String, Object>>builder()
                        .success(true)
                        .message("Class details retrieved successfully")
                        .data(classDetail)
                        .build());
    }

    @GetMapping
    @Operation(summary = "Get classes list", description = "Retrieve paginated list of classes accessible to the user with filtering options. "
            +
            "By default, returns all classes regardless of status. " +
            "Academic Affairs can create and manage classes. Center Head can review and approve classes.")
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

