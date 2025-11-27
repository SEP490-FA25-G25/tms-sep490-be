package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.adminanalytic.AnalyticsResponseDTO;

/**
 * Analytics service for Admin dashboard
 */
public interface AnalyticsService {
    /**
     * Get complete analytics data for Admin dashboard
     */
    AnalyticsResponseDTO getSystemAnalytics();
}

