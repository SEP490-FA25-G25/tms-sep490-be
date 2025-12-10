package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.dashboard.AdminStatsDTO;
import org.fyp.tmssep490be.services.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<AdminStatsDTO>> getAdminStats() {
        log.info("API: Lấy thống kê Admin Dashboard");
        AdminStatsDTO stats = adminDashboardService.getAdminStats();
        return ResponseEntity.ok(new ResponseObject<>(true, "Lấy thống kê thành công", stats));
    }
}
