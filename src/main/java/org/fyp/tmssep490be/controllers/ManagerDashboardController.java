package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.dashboard.ManagerDashboardDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.ManagerDashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * API Dashboard cho vai trò MANAGER.
 *
 * Frontend gọi: GET /api/v1/manager/dashboard?fromDate=2025-12-10&toDate=2025-12-17
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/manager/dashboard")
@RequiredArgsConstructor
public class ManagerDashboardController {

    private final ManagerDashboardService managerDashboardService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<ManagerDashboardDTO>> getManagerDashboard(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        log.info("Manager {} requesting dashboard overview from {} to {}", userId, fromDate, toDate);

        ManagerDashboardDTO dashboard = managerDashboardService.getDashboard(userId, fromDate, toDate);
        return ResponseEntity.ok(ResponseObject.success("Lấy dashboard manager thành công", dashboard));
    }
}


