package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.curriculum.*;

import java.util.List;

/**
 * Service for curriculum-related operations
 * Provides subject and level information for student creation and management
 */
public interface CurriculumService {

    /**
     * Get all subjects with their levels
     * Used for dropdown/select components when creating student skill assessments
     *
     * @return List of subjects with their levels sorted by subject name and level
     *         order
     */
    List<SubjectWithLevelsDTO> getAllSubjectsWithLevels();

    SubjectResponseDTO createSubject(CreateSubjectDTO request);

    LevelResponseDTO createLevel(CreateLevelDTO request);

    List<LevelResponseDTO> getLevels(Long subjectId);

    java.math.BigDecimal getStandardTimeslotDuration();
}