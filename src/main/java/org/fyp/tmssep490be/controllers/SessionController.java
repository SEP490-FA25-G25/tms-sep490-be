package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.qa.SessionDetailDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionService sessionService;

    @GetMapping("/qa/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('QA', 'TEACHER', 'ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER')")
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
