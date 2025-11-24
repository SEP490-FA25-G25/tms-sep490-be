package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.teachergrade.*;

import java.util.List;

/**
 * Service for teacher grade management operations
 */
public interface TeacherGradeService {
    
    /**
     * Get list of assessments for a class with optional filter
     * @param teacherId Teacher ID
     * @param classId Class ID
     * @param filter Filter type: "all", "upcoming", "graded", "overdue"
     * @return List of assessments
     */
    List<TeacherAssessmentDTO> getClassAssessments(Long teacherId, Long classId, String filter);
    
    /**
     * Get all student scores for an assessment
     * @param teacherId Teacher ID
     * @param assessmentId Assessment ID
     * @return List of student scores
     */
    List<TeacherStudentScoreDTO> getAssessmentScores(Long teacherId, Long assessmentId);
    
    /**
     * Get a specific student's score for an assessment
     * @param teacherId Teacher ID
     * @param assessmentId Assessment ID
     * @param studentId Student ID
     * @return Student score or null if not found
     */
    TeacherStudentScoreDTO getStudentScore(Long teacherId, Long assessmentId, Long studentId);
    
    /**
     * Save or update a single student score
     * @param teacherId Teacher ID
     * @param assessmentId Assessment ID
     * @param scoreInput Score input DTO
     * @return Updated student score DTO
     */
    TeacherStudentScoreDTO saveOrUpdateScore(Long teacherId, Long assessmentId, ScoreInputDTO scoreInput);
    
    /**
     * Batch save or update multiple student scores
     * @param teacherId Teacher ID
     * @param assessmentId Assessment ID
     * @param batchInput Batch score input DTO
     * @return List of updated student score DTOs
     */
    List<TeacherStudentScoreDTO> batchSaveOrUpdateScores(Long teacherId, Long assessmentId, BatchScoreInputDTO batchInput);
    
    /**
     * Get class grades summary statistics
     * @param teacherId Teacher ID
     * @param classId Class ID
     * @return Class grades summary
     */
    ClassGradesSummaryDTO getClassGradesSummary(Long teacherId, Long classId);
    
    /**
     * Get gradebook (matrix view) for a class
     * @param teacherId Teacher ID
     * @param classId Class ID
     * @return Gradebook DTO with all students and assessments
     */
    GradebookDTO getClassGradebook(Long teacherId, Long classId);
}

