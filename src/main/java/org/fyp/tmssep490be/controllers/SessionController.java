package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.SessionDetailDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sessions", description = "Session Detail APIs - View session information with attendance data")
public class SessionController {

    private final SessionService sessionService;

    /**
     * Get Session Detail with Attendance Data
     */
    @GetMapping("/{sessionId}")
    @PreAuthorize("hasRole('QA') or hasRole('TEACHER') or hasRole('ACADEMIC_AFFAIR')")
    @Operation(
        summary = "Get Session Detail",
        description = "Retrieve detailed session information including: " +
                      "session info (date, time, topic, teacher), " +
                      "attendance statistics (present/absent counts and rates), " +
                      "student attendance list with homework status, " +
                      "and CLOs covered in this session. " +
                      "Accessible by QA for quality monitoring, Teacher for review, and Academic Affair for oversight."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session detail retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - user does not have access to this session")
    })
    public ResponseEntity<ResponseObject<SessionDetailDTO>> getSessionDetail(
        @PathVariable Long sessionId,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting session detail for sessionId={}", currentUser.getId(), sessionId);

        SessionDetailDTO sessionDetail = sessionService.getSessionDetail(sessionId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<SessionDetailDTO>builder()
            .success(true)
            .message("Session detail retrieved successfully")
            .data(sessionDetail)
            .build());
    }
}
