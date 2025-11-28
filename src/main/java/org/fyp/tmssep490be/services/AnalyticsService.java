package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.adminanalytic.AnalyticsResponseDTO;

import java.util.List;

/**
 * Analytics service for Admin dashboard
 */
public interface AnalyticsService {
    /**
     * Get complete analytics data for Admin dashboard
     */
    AnalyticsResponseDTO getSystemAnalytics();

    /**
     * Get analytics data for Manager dashboard (filtered by manager's assigned branches)
     */
    AnalyticsResponseDTO getManagerAnalytics(Long managerUserId);
}

