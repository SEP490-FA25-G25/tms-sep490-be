package org.fyp.tmssep490be.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Simple policy service that returns default values.
 * For capstone project, hardcoded defaults are sufficient.
 * Future improvement: Could read from application.properties if needed.
 */
@Service
@Slf4j
public class PolicyService {

    /**
     * Get a string policy value. Returns the default value directly.
     */
    public String getGlobalString(String policyKey, String defaultValue) {
        log.debug("Getting policy '{}', returning default: {}", policyKey, defaultValue);
        return defaultValue;
    }

    /**
     * Get an integer policy value. Returns the default value directly.
     */
    public int getGlobalInt(String policyKey, int defaultValue) {
        log.debug("Getting policy '{}', returning default: {}", policyKey, defaultValue);
        return defaultValue;
    }

    /**
     * Get a boolean policy value. Returns the default value directly.
     */
    public boolean getGlobalBoolean(String policyKey, boolean defaultValue) {
        log.debug("Getting policy '{}', returning default: {}", policyKey, defaultValue);
        return defaultValue;
    }

    /**
     * Get a double policy value. Returns the default value directly.
     */
    public double getGlobalDouble(String policyKey, double defaultValue) {
        log.debug("Getting policy '{}', returning default: {}", policyKey, defaultValue);
        return defaultValue;
    }
}
