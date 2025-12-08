package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentrequest.AbsenceRequestDTO;
import org.fyp.tmssep490be.dtos.studentrequest.StudentRequestResponseDTO;
import org.fyp.tmssep490be.dtos.studentrequest.StudentRequestSubmissionDTO;
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

        Long userId = currentUser.getId();
        StudentRequestResponseDTO response;

        switch (requestDTO.getRequestType()) {
            case "ABSENCE":
                AbsenceRequestDTO absenceRequest = AbsenceRequestDTO.builder()
                        .currentClassId(requestDTO.getCurrentClassId())
                        .targetSessionId(requestDTO.getTargetSessionId())
                        .requestReason(requestDTO.getRequestReason())
                        .note(requestDTO.getNote())
                        .build();

                response = studentRequestService.submitAbsenceRequest(userId, absenceRequest);
                break;

            case "MAKEUP":
                // TODO: Implement makeup request submission
                throw new UnsupportedOperationException("Makeup requests not yet implemented");

            case "TRANSFER":
                // TODO: Implement transfer request submission
                throw new UnsupportedOperationException("Transfer requests not yet implemented");

            default:
                throw new IllegalArgumentException("Invalid request type: " + requestDTO.getRequestType());
        }

        return ResponseEntity.ok(ResponseObject.success("Request submitted successfully", response));
    }
}
