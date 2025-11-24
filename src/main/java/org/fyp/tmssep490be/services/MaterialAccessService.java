package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.CourseMaterial;

public interface MaterialAccessService {
    boolean canAccessMaterial(Long studentId, Long materialId);
    boolean canAccessCourseLevelMaterial(Long studentId, Long courseId);
    boolean canAccessPhaseLevelMaterial(Long studentId, Long phaseId);
    boolean canAccessSessionLevelMaterial(Long studentId, Long sessionId);
}