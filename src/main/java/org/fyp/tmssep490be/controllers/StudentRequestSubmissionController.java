package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentrequest.AbsenceRequestDTO;
import org.fyp.tmssep490be.dtos.studentrequest.MakeupRequestDTO;
import org.fyp.tmssep490be.dtos.studentrequest.StudentRequestResponseDTO;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student-requests-submission")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Student Request Submission", description = "APIs for submitting new student requests")
@SecurityRequirement(name = "Bearer Authentication")
public class StudentRequestSubmissionController {

    private final StudentRequestService studentRequestService;

    @PostMapping
    @Operation(summary = "Submit new student request", description = "Submit a new absence, makeup, or transfer request")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> submitRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody StudentRequestSubmissionDTO requestDTO) {

        Long studentId = currentUser.getId();
        StudentRequestResponseDTO response;

        switch (requestDTO.getRequestType()) {
            case "ABSENCE":
                AbsenceRequestDTO absenceRequest = AbsenceRequestDTO.builder()
                        .currentClassId(requestDTO.getCurrentClassId())
                        .targetSessionId(requestDTO.getTargetSessionId())
                        .requestReason(requestDTO.getRequestReason())
                        .note(requestDTO.getNote())
                        .build();

                response = studentRequestService.submitAbsenceRequest(studentId, absenceRequest);
                break;

            case "MAKEUP":
                MakeupRequestDTO makeupRequest = MakeupRequestDTO.builder()
                        .currentClassId(requestDTO.getCurrentClassId())
                        .targetSessionId(requestDTO.getTargetSessionId())
                        .makeupSessionId(requestDTO.getMakeupSessionId())
                        .requestReason(requestDTO.getRequestReason())
                        .note(requestDTO.getNote())
                        .build();

                response = studentRequestService.submitMakeupRequest(studentId, makeupRequest);
                break;

            case "TRANSFER":
                // TODO: Implement transfer request submission
                throw new UnsupportedOperationException("Transfer requests not yet implemented");

            default:
                throw new IllegalArgumentException("Invalid request type: " + requestDTO.getRequestType());
        }

        return ResponseEntity.ok(ResponseObject.success("Request submitted successfully", response));
    }

    // DTO for generic request submission
    public static class StudentRequestSubmissionDTO {
        @NotBlank(message = "Request type is required")
        @Pattern(regexp = "ABSENCE|MAKEUP|TRANSFER", message = "Request type must be ABSENCE, MAKEUP, or TRANSFER")
        private String requestType; // ABSENCE, MAKEUP, TRANSFER

        @NotNull(message = "Class ID is required")
        private Long currentClassId;

        @NotNull(message = "Target session ID is required")
        private Long targetSessionId;

        private Long makeupSessionId; // For MAKEUP requests

        @NotBlank(message = "Reason is required")
        @Size(min = 10, message = "Reason must be at least 10 characters")
        private String requestReason;

        private String note;

        // Validation helper: makeupSessionId must be present for MAKEUP requests
        @AssertTrue(message = "Makeup session ID is required for MAKEUP requests")
        public boolean isMakeupSessionValid() {
            if ("MAKEUP".equalsIgnoreCase(requestType)) {
                return makeupSessionId != null;
            }
            return true;
        }

        // Getters and setters
        public String getRequestType() { return requestType; }
        public void setRequestType(String requestType) { this.requestType = requestType; }
        public Long getCurrentClassId() { return currentClassId; }
        public void setCurrentClassId(Long currentClassId) { this.currentClassId = currentClassId; }
        public Long getTargetSessionId() { return targetSessionId; }
        public void setTargetSessionId(Long targetSessionId) { this.targetSessionId = targetSessionId; }
        public Long getMakeupSessionId() { return makeupSessionId; }
        public void setMakeupSessionId(Long makeupSessionId) { this.makeupSessionId = makeupSessionId; }
        public String getRequestReason() { return requestReason; }
        public void setRequestReason(String requestReason) { this.requestReason = requestReason; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}
