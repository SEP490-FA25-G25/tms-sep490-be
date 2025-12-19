package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.dashboard.CenterHeadDashboardDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.CenterHeadDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * API Dashboard cho vai trò CENTER_HEAD.
 *
 * Frontend gọi: GET /api/v1/center-head/dashboard
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/center-head/dashboard")
@RequiredArgsConstructor
public class CenterHeadDashboardController {

    private final CenterHeadDashboardService centerHeadDashboardService;

    @GetMapping
    @PreAuthorize("hasRole('CENTER_HEAD')")
    public ResponseEntity<ResponseObject<CenterHeadDashboardDTO>> getCenterHeadDashboard(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        Long userId = currentUser != null ? currentUser.getId() : null;
        log.info("Center Head {} requesting dashboard overview from {} to {}", userId, fromDate, toDate);

        CenterHeadDashboardDTO dashboard = centerHeadDashboardService.getDashboard(userId, fromDate, toDate);
        return ResponseEntity.ok(ResponseObject.success("Lấy dashboard Center Head thành công", dashboard));
    }
}


