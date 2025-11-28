package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.adminanalytic.AnalyticsResponseDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manager Analytics Controller
 * Provides analytics filtered by manager's assigned branches
 */
@RestController
@RequestMapping("/api/v1/manager/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Manager Analytics", description = "Analytics APIs for Manager dashboard (filtered by assigned branches)")
@SecurityRequirement(name = "bearerAuth")
public class ManagerAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Get manager analytics",
            description = "Get analytics data filtered by manager's assigned branches"
    )
    public ResponseEntity<ResponseObject<AnalyticsResponseDTO>> getManagerAnalytics(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Manager {} requesting analytics for assigned branches", userPrincipal.getId());

        AnalyticsResponseDTO analytics = analyticsService.getManagerAnalytics(userPrincipal.getId());

        return ResponseEntity.ok(
                ResponseObject.<AnalyticsResponseDTO>builder()
                        .success(true)
                        .message("Manager analytics retrieved successfully")
                        .data(analytics)
                        .build()
        );
    }
}

