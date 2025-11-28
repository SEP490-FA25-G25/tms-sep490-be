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

    SubjectResponseDTO getSubject(Long id);

    SubjectResponseDTO updateSubject(Long id, CreateSubjectDTO request);

    void deactivateSubject(Long id);

    void reactivateSubject(Long id);

    LevelResponseDTO getLevel(Long id);

    LevelResponseDTO updateLevel(Long id, CreateLevelDTO request);

    void deactivateLevel(Long id);

    void reactivateLevel(Long id);

    void updateLevelSortOrder(Long subjectId, java.util.List<Long> levelIds);
}