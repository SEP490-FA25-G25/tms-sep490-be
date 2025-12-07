package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/classes")
public class ClassController {

    private final ClassRepository classRepository;

    public ClassController(ClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    //Endpoint to get class details by ID (for TEACHER, STUDENT, ACADEMIC_AFFAIR, etc.)
    @GetMapping("/{classId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ACADEMIC_AFFAIR') or hasRole('CENTER_HEAD') or hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<Map<String, Object>>> getClassDetail(
            @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // Find class by ID
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND, "Class not found"));

        // Build response map
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
        
        // Add subject (course) information
        if (classEntity.getSubject() != null) {
            Map<String, Object> course = new HashMap<>();
            course.put("id", classEntity.getSubject().getId());
            course.put("code", classEntity.getSubject().getCode());
            course.put("name", classEntity.getSubject().getName());
            classDetail.put("course", course);
        }
        
        // Add branch information
        if (classEntity.getBranch() != null) {
            Map<String, Object> branch = new HashMap<>();
            branch.put("id", classEntity.getBranch().getId());
            branch.put("code", classEntity.getBranch().getCode());
            branch.put("name", classEntity.getBranch().getName());
            branch.put("address", classEntity.getBranch().getAddress());
            classDetail.put("branch", branch);
        }
        
        // TODO: Add teachers, enrollment summary, upcoming sessions, etc.
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
}

