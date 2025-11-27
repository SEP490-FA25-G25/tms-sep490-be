package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.adminanalytic.AnalyticsResponseDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.services.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Analytics Controller
 * Provides system-wide analytics and statistics for Admin dashboard
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Analytics", description = "System analytics APIs for Admin dashboard")
@SecurityRequirement(name = "bearerAuth")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get system analytics",
            description = "Get comprehensive system analytics including overview, user statistics, class statistics, and branch statistics"
    )
    public ResponseEntity<ResponseObject<AnalyticsResponseDTO>> getSystemAnalytics() {
        log.info("Admin requesting system analytics");

        AnalyticsResponseDTO analytics = analyticsService.getSystemAnalytics();

        return ResponseEntity.ok(
                ResponseObject.<AnalyticsResponseDTO>builder()
                        .success(true)
                        .message("System analytics retrieved successfully")
                        .data(analytics)
                        .build()
        );
    }
}

