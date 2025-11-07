package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.classmanagement.ValidateClassResponse;

/**
 * Service interface for class validation functionality.
 * Provides comprehensive validation of class completeness before submission.
 */
public interface ValidationService {

    /**
     * Validates if a class is complete and ready for submission.
     * Checks time slot assignments, resource assignments, teacher assignments,
     * and provides detailed validation results.
     *
     * @param classId the ID of the class to validate
     * @return ValidateClassResponse containing comprehensive validation results
     */
    ValidateClassResponse validateClassComplete(Long classId);
}