package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teachergrade.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.TeacherGradeService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TeacherGradeServiceImpl implements TeacherGradeService {


    private final AssessmentRepository assessmentRepository;
    private final ScoreRepository scoreRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;

    /**
     * Verify that teacher teaches the class
     */
    private void assertTeacherOwnsClass(Long teacherId, Long classId) {
        boolean ownsClass = teachingSlotRepository.findDistinctClassesByTeacherId(teacherId)
                .stream()
                .anyMatch(c -> c.getId().equals(classId));
        
        if (!ownsClass) {
            throw new AccessDeniedException("Teacher does not teach this class");
        }
    }

    /**
     * Verify that teacher teaches the class of the assessment
     */
    private void assertTeacherOwnsAssessment(Long teacherId, Long assessmentId) {
        Optional<Assessment> assessmentOpt = Optional.ofNullable(assessmentRepository.findByIdWithClass(assessmentId));
        Assessment assessment = assessmentOpt.orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        
        assertTeacherOwnsClass(teacherId, assessment.getClassEntity().getId());
    }

    @Override
    public List<TeacherAssessmentDTO> getClassAssessments(Long teacherId, Long classId, String filter) {
        assertTeacherOwnsClass(teacherId, classId);
        
        List<Assessment> assessments = assessmentRepository.findByClassEntityId(classId);
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);
        int totalStudents = enrollments.size();
        
        // Get all scores for these assessments
        Map<Long, Long> assessmentGradedCountMap = new HashMap<>();
        for (Assessment assessment : assessments) {
            // Count distinct students with scores for this assessment
            Set<Score> scores = assessment.getScores();
            long gradedCount = 0;
            if (scores != null && !scores.isEmpty()) {
                gradedCount = scores.stream()
                        .filter(s -> s.getGradedAt() != null)
                        .map(s -> s.getStudent().getId())
                        .distinct()
                        .count();
            }
            assessmentGradedCountMap.put(assessment.getId(), gradedCount);
        }
        
        OffsetDateTime now = OffsetDateTime.now();
        final int finalTotalStudents = totalStudents;
        final Map<Long, Long> finalGradedCountMap = assessmentGradedCountMap;
        
        return assessments.stream()
                .map(assessment -> {
                    CourseAssessment courseAssessment = assessment.getCourseAssessment();
                    Long gradedCount = finalGradedCountMap.getOrDefault(assessment.getId(), 0L);
                    
                    // Determine status based on filter
                    boolean include = true;
                    if (filter != null && !filter.equals("all")) {
                        boolean hasGradedScores = gradedCount > 0;
                        boolean isUpcoming = assessment.getScheduledDate().isAfter(now);
                        boolean isOverdue = assessment.getScheduledDate().isBefore(now) && !hasGradedScores;
                        
                        switch (filter.toLowerCase()) {
                            case "upcoming":
                                include = isUpcoming && !hasGradedScores;
                                break;
                            case "graded":
                                include = hasGradedScores;
                                break;
                            case "overdue":
                                include = isOverdue;
                                break;
                        }
                    }
                    
                    if (!include) {
                        return null;
                    }
                    
                    return TeacherAssessmentDTO.builder()
                            .id(assessment.getId())
                            .classId(assessment.getClassEntity().getId())
                            .courseAssessmentId(courseAssessment != null ? courseAssessment.getId() : null)
                            .name(courseAssessment != null ? courseAssessment.getName() : "Assessment " + assessment.getId())
                            .description(courseAssessment != null ? courseAssessment.getDescription() : null)
                            .kind(courseAssessment != null ? courseAssessment.getKind().name() : null)
                            .maxScore(courseAssessment != null ? courseAssessment.getMaxScore() : null)
                            .durationMinutes(courseAssessment != null ? courseAssessment.getDurationMinutes() : null)
                            .scheduledDate(assessment.getScheduledDate())
                            .actualDate(assessment.getActualDate())
                            .gradedCount(gradedCount.intValue())
                            .totalStudents(finalTotalStudents)
                            .allGraded(gradedCount == finalTotalStudents)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TeacherAssessmentDTO::getScheduledDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<TeacherStudentScoreDTO> getAssessmentScores(Long teacherId, Long assessmentId) {
        assertTeacherOwnsAssessment(teacherId, assessmentId);
        
        Optional<Assessment> assessmentOpt = Optional.ofNullable(assessmentRepository.findByIdWithClass(assessmentId));
        Assessment assessment = assessmentOpt.orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        
        Long classId = assessment.getClassEntity().getId();
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);
        
        CourseAssessment courseAssessment = assessment.getCourseAssessment();
        BigDecimal maxScore = courseAssessment != null ? courseAssessment.getMaxScore() : BigDecimal.valueOf(100);
        
        // Get all existing scores for this assessment
        Map<Long, Score> scoreMap = assessment.getScores().stream()
                .collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s));
        
        return enrollments.stream()
                .map(enrollment -> {
                    Student student = enrollment.getStudent();
                    Score score = scoreMap.get(student.getId());
                    
                    BigDecimal scoreValue = score != null ? score.getScore() : null;
                    BigDecimal scorePercentage = null;
                    if (scoreValue != null && maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
                        scorePercentage = scoreValue.divide(maxScore, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                    }
                    
                    String gradedByName = null;
                    if (score != null && score.getGradedBy() != null) {
                        gradedByName = score.getGradedBy().getUserAccount().getFullName();
                    }
                    
                    return TeacherStudentScoreDTO.builder()
                            .scoreId(score != null ? score.getId() : null)
                            .studentId(student.getId())
                            .studentCode(student.getStudentCode())
                            .studentName(student.getUserAccount().getFullName())
                            .score(scoreValue)
                            .feedback(score != null ? score.getFeedback() : null)
                            .gradedBy(gradedByName)
                            .gradedAt(score != null ? score.getGradedAt() : null)
                            .maxScore(maxScore)
                            .scorePercentage(scorePercentage)
                            .isGraded(score != null && score.getGradedAt() != null)
                            .build();
                })
                .sorted(Comparator.comparing(TeacherStudentScoreDTO::getStudentCode))
                .collect(Collectors.toList());
    }

    @Override
    public TeacherStudentScoreDTO getStudentScore(Long teacherId, Long assessmentId, Long studentId) {
        assertTeacherOwnsAssessment(teacherId, assessmentId);
        
        Optional<Assessment> assessmentOpt = Optional.ofNullable(assessmentRepository.findByIdWithClass(assessmentId));
        Assessment assessment = assessmentOpt.orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        
        // Verify student is enrolled in the class
        Long classId = assessment.getClassEntity().getId();
        boolean isEnrolled = enrollmentRepository.existsByClassIdAndStudentIdAndStatus(
                classId, studentId, EnrollmentStatus.ENROLLED);
        
        if (!isEnrolled) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        
        Optional<Score> scoreOpt = scoreRepository.findByStudentIdAndAssessmentId(studentId, assessmentId);
        CourseAssessment courseAssessment = assessment.getCourseAssessment();
        BigDecimal maxScore = courseAssessment != null ? courseAssessment.getMaxScore() : BigDecimal.valueOf(100);
        
        if (scoreOpt.isEmpty()) {
            return TeacherStudentScoreDTO.builder()
                    .studentId(student.getId())
                    .studentCode(student.getStudentCode())
                    .studentName(student.getUserAccount().getFullName())
                    .maxScore(maxScore)
                    .isGraded(false)
                    .build();
        }
        
        Score score = scoreOpt.get();
        BigDecimal scoreValue = score.getScore();
        BigDecimal scorePercentage = null;
        if (scoreValue != null && maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
            scorePercentage = scoreValue.divide(maxScore, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        String gradedByName = null;
        if (score.getGradedBy() != null) {
            gradedByName = score.getGradedBy().getUserAccount().getFullName();
        }
        
        return TeacherStudentScoreDTO.builder()
                .scoreId(score.getId())
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .studentName(student.getUserAccount().getFullName())
                .score(scoreValue)
                .feedback(score.getFeedback())
                .gradedBy(gradedByName)
                .gradedAt(score.getGradedAt())
                .maxScore(maxScore)
                .scorePercentage(scorePercentage)
                .isGraded(score.getGradedAt() != null)
                .build();
    }

    @Override
    @Transactional
    public TeacherStudentScoreDTO saveOrUpdateScore(Long teacherId, Long assessmentId, ScoreInputDTO scoreInput) {
        assertTeacherOwnsAssessment(teacherId, assessmentId);
        
        Optional<Assessment> assessmentOpt = Optional.ofNullable(assessmentRepository.findByIdWithClass(assessmentId));
        Assessment assessment = assessmentOpt.orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        
        Student student = studentRepository.findById(scoreInput.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        
        // Verify student is enrolled
        Long classId = assessment.getClassEntity().getId();
        boolean isEnrolled = enrollmentRepository.existsByClassIdAndStudentIdAndStatus(
                classId, scoreInput.getStudentId(), EnrollmentStatus.ENROLLED);
        
        if (!isEnrolled) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        
        // Validate score against max score
        CourseAssessment courseAssessment = assessment.getCourseAssessment();
        BigDecimal maxScore = courseAssessment != null ? courseAssessment.getMaxScore() : BigDecimal.valueOf(100);
        
        if (scoreInput.getScore().compareTo(maxScore) > 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        
        if (scoreInput.getScore().compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        
        Optional<Score> existingScoreOpt = scoreRepository.findByStudentIdAndAssessmentId(
                scoreInput.getStudentId(), assessmentId);
        
        Score score;
        if (existingScoreOpt.isPresent()) {
            score = existingScoreOpt.get();
            score.setScore(scoreInput.getScore());
            score.setFeedback(scoreInput.getFeedback());
            score.setGradedBy(teacher);
            score.setGradedAt(OffsetDateTime.now());
            score.setUpdatedAt(OffsetDateTime.now());
        } else {
            score = Score.builder()
                    .assessment(assessment)
                    .student(student)
                    .score(scoreInput.getScore())
                    .feedback(scoreInput.getFeedback())
                    .gradedBy(teacher)
                    .gradedAt(OffsetDateTime.now())
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
        }
        
        score = scoreRepository.save(score);
        
        BigDecimal scorePercentage = null;
        if (score.getScore() != null && maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
            scorePercentage = score.getScore().divide(maxScore, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        return TeacherStudentScoreDTO.builder()
                .scoreId(score.getId())
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .studentName(student.getUserAccount().getFullName())
                .score(score.getScore())
                .feedback(score.getFeedback())
                .gradedBy(teacher.getUserAccount().getFullName())
                .gradedAt(score.getGradedAt())
                .maxScore(maxScore)
                .scorePercentage(scorePercentage)
                .isGraded(true)
                .build();
    }

    @Override
    @Transactional
    public List<TeacherStudentScoreDTO> batchSaveOrUpdateScores(Long teacherId, Long assessmentId, BatchScoreInputDTO batchInput) {
        assertTeacherOwnsAssessment(teacherId, assessmentId);
        
        if (batchInput.getScores() == null || batchInput.getScores().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        
        Optional<Assessment> assessmentOpt = Optional.ofNullable(assessmentRepository.findByIdWithClass(assessmentId));
        Assessment assessment = assessmentOpt.orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        
        CourseAssessment courseAssessment = assessment.getCourseAssessment();
        BigDecimal maxScore = courseAssessment != null ? courseAssessment.getMaxScore() : BigDecimal.valueOf(100);
        
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        
        Long classId = assessment.getClassEntity().getId();
        List<TeacherStudentScoreDTO> results = new ArrayList<>();
        
        for (ScoreInputDTO scoreInput : batchInput.getScores()) {
            // Validate student enrollment
            boolean isEnrolled = enrollmentRepository.existsByClassIdAndStudentIdAndStatus(
                    classId, scoreInput.getStudentId(), EnrollmentStatus.ENROLLED);
            
            if (!isEnrolled) {
                log.warn("Student {} is not enrolled in class {}, skipping", scoreInput.getStudentId(), classId);
                continue;
            }
            
            // Validate score
            if (scoreInput.getScore().compareTo(maxScore) > 0) {
                log.warn("Score {} exceeds max score {} for student {}, skipping", 
                        scoreInput.getScore(), maxScore, scoreInput.getStudentId());
                continue;
            }
            
            if (scoreInput.getScore().compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Score {} is negative for student {}, skipping", 
                        scoreInput.getScore(), scoreInput.getStudentId());
                continue;
            }
            
            Student student = studentRepository.findById(scoreInput.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + scoreInput.getStudentId()));
            
            Optional<Score> existingScoreOpt = scoreRepository.findByStudentIdAndAssessmentId(
                    scoreInput.getStudentId(), assessmentId);
            
            Score score;
            if (existingScoreOpt.isPresent()) {
                score = existingScoreOpt.get();
                score.setScore(scoreInput.getScore());
                score.setFeedback(scoreInput.getFeedback());
                score.setGradedBy(teacher);
                score.setGradedAt(OffsetDateTime.now());
                score.setUpdatedAt(OffsetDateTime.now());
            } else {
                score = Score.builder()
                        .assessment(assessment)
                        .student(student)
                        .score(scoreInput.getScore())
                        .feedback(scoreInput.getFeedback())
                        .gradedBy(teacher)
                        .gradedAt(OffsetDateTime.now())
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build();
            }
            
            score = scoreRepository.save(score);
            
            BigDecimal scorePercentage = null;
            if (score.getScore() != null && maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
                scorePercentage = score.getScore().divide(maxScore, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            
            results.add(TeacherStudentScoreDTO.builder()
                    .scoreId(score.getId())
                    .studentId(student.getId())
                    .studentCode(student.getStudentCode())
                    .studentName(student.getUserAccount().getFullName())
                    .score(score.getScore())
                    .feedback(score.getFeedback())
                    .gradedBy(teacher.getUserAccount().getFullName())
                    .gradedAt(score.getGradedAt())
                    .maxScore(maxScore)
                    .scorePercentage(scorePercentage)
                    .isGraded(true)
                    .build());
        }
        
        return results;
    }

    @Override
    public ClassGradesSummaryDTO getClassGradesSummary(Long teacherId, Long classId) {
        assertTeacherOwnsClass(teacherId, classId);
        
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        
        List<Assessment> assessments = assessmentRepository.findByClassEntityId(classId);
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);
        
        if (assessments.isEmpty() || enrollments.isEmpty()) {
            return ClassGradesSummaryDTO.builder()
                    .classId(classId)
                    .className(classEntity.getName())
                    .totalAssessments(assessments.size())
                    .totalStudents(enrollments.size())
                    .build();
        }
        
        // Calculate statistics for each student
        Map<Long, List<BigDecimal>> studentScoresMap = new HashMap<>();
        Map<Long, Integer> studentGradedCountMap = new HashMap<>();
        
        for (Enrollment enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();
            studentScoresMap.put(studentId, new ArrayList<>());
            studentGradedCountMap.put(studentId, 0);
        }
        
        for (Assessment assessment : assessments) {
            CourseAssessment courseAssessment = assessment.getCourseAssessment();
            if (courseAssessment == null) continue;
            
            BigDecimal maxScore = courseAssessment.getMaxScore();
            if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) continue;
            
            for (Score score : new ArrayList<>(assessment.getScores())) {
                if (score.getGradedAt() == null) continue;
                
                Long studentId = score.getStudent().getId();
                if (!studentScoresMap.containsKey(studentId)) continue;
                
                // Normalize score to percentage
                BigDecimal normalizedScore = score.getScore()
                        .divide(maxScore, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                
                studentScoresMap.get(studentId).add(normalizedScore);
                studentGradedCountMap.put(studentId, studentGradedCountMap.get(studentId) + 1);
            }
        }
        
        // Calculate averages and find top/bottom students
        List<ClassGradesSummaryDTO.StudentGradeSummaryDTO> studentSummaries = new ArrayList<>();
        
        for (Enrollment enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();
            Student student = enrollment.getStudent();
            List<BigDecimal> scores = studentScoresMap.get(studentId);
            
            BigDecimal averageScore = null;
            if (!scores.isEmpty()) {
                averageScore = scores.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
            }
            
            studentSummaries.add(ClassGradesSummaryDTO.StudentGradeSummaryDTO.builder()
                    .studentId(studentId)
                    .studentCode(student.getStudentCode())
                    .studentName(student.getUserAccount().getFullName())
                    .averageScore(averageScore)
                    .gradedCount(studentGradedCountMap.get(studentId))
                    .build());
        }
        
        // Calculate class statistics
        List<BigDecimal> allScores = studentSummaries.stream()
                .filter(s -> s.getAverageScore() != null)
                .map(ClassGradesSummaryDTO.StudentGradeSummaryDTO::getAverageScore)
                .collect(Collectors.toList());
        
        BigDecimal classAverage = null;
        BigDecimal highestScore = null;
        BigDecimal lowestScore = null;
        
        if (!allScores.isEmpty()) {
            classAverage = allScores.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(allScores.size()), 2, RoundingMode.HALF_UP);
            
            highestScore = allScores.stream().max(BigDecimal::compareTo).orElse(null);
            lowestScore = allScores.stream().min(BigDecimal::compareTo).orElse(null);
        }
        
        // Score distribution
        Map<String, Integer> scoreDistribution = new HashMap<>();
        for (BigDecimal score : allScores) {
            String range;
            double scoreValue = score.doubleValue();
            if (scoreValue >= 90) {
                range = "90-100";
            } else if (scoreValue >= 80) {
                range = "80-89";
            } else if (scoreValue >= 70) {
                range = "70-79";
            } else if (scoreValue >= 60) {
                range = "60-69";
            } else {
                range = "0-59";
            }
            scoreDistribution.put(range, scoreDistribution.getOrDefault(range, 0) + 1);
        }
        
        // Top and bottom students
        List<ClassGradesSummaryDTO.StudentGradeSummaryDTO> topStudents = studentSummaries.stream()
                .filter(s -> s.getAverageScore() != null)
                .sorted((a, b) -> b.getAverageScore().compareTo(a.getAverageScore()))
                .limit(5)
                .collect(Collectors.toList());
        
        List<ClassGradesSummaryDTO.StudentGradeSummaryDTO> bottomStudents = studentSummaries.stream()
                .filter(s -> s.getAverageScore() != null)
                .sorted(Comparator.comparing(ClassGradesSummaryDTO.StudentGradeSummaryDTO::getAverageScore))
                .limit(5)
                .collect(Collectors.toList());
        
        return ClassGradesSummaryDTO.builder()
                .classId(classId)
                .className(classEntity.getName())
                .totalAssessments(assessments.size())
                .totalStudents(enrollments.size())
                .averageScore(classAverage)
                .highestScore(highestScore)
                .lowestScore(lowestScore)
                .scoreDistribution(scoreDistribution)
                .topStudents(topStudents)
                .bottomStudents(bottomStudents)
                .build();
    }

    @Override
    public GradebookDTO getClassGradebook(Long teacherId, Long classId) {
        assertTeacherOwnsClass(teacherId, classId);
        
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        
        // Get all assessments for the class
        List<Assessment> assessments = assessmentRepository.findByClassEntityId(classId);
        
        // Get all enrolled students
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(
                classId, EnrollmentStatus.ENROLLED);
        
        // Build assessments list
        List<GradebookDTO.GradebookAssessmentDTO> assessmentDTOs = assessments.stream()
                .map(assessment -> {
                    CourseAssessment courseAssessment = assessment.getCourseAssessment();
                    return GradebookDTO.GradebookAssessmentDTO.builder()
                            .assessmentId(assessment.getId())
                            .assessmentName(courseAssessment != null && courseAssessment.getName() != null 
                                    ? courseAssessment.getName() : "Assessment " + assessment.getId())
                            .kind(courseAssessment != null && courseAssessment.getKind() != null 
                                    ? courseAssessment.getKind().name() : null)
                            .maxScore(courseAssessment != null ? courseAssessment.getMaxScore() : null)
                            .scheduledDate(assessment.getScheduledDate() != null 
                                    ? assessment.getScheduledDate().toString() : null)
                            .build();
                })
                .sorted(Comparator.comparing(GradebookDTO.GradebookAssessmentDTO::getScheduledDate, 
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        
        // Get all scores for all assessments
        Map<Long, Map<Long, Score>> scoreMap = new HashMap<>(); // studentId -> assessmentId -> Score
        for (Assessment assessment : assessments) {
            for (Score score : assessment.getScores()) {
                Long studentId = score.getStudent().getId();
                scoreMap.computeIfAbsent(studentId, k -> new HashMap<>())
                        .put(assessment.getId(), score);
            }
        }
        
        // Build students list with scores
        List<GradebookDTO.GradebookStudentDTO> studentDTOs = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Student student = enrollment.getStudent();
            Long studentId = student.getId();
            
            Map<Long, GradebookDTO.GradebookScoreDTO> scoresMap = new HashMap<>();
            List<BigDecimal> gradedScores = new ArrayList<>();
            
            for (Assessment assessment : assessments) {
                Long assessmentId = assessment.getId();
                Score score = scoreMap.getOrDefault(studentId, Collections.emptyMap())
                        .get(assessmentId);
                
                CourseAssessment courseAssessment = assessment.getCourseAssessment();
                BigDecimal maxScore = courseAssessment != null ? courseAssessment.getMaxScore() : null;
                
                GradebookDTO.GradebookScoreDTO scoreDTO;
                if (score != null && score.getGradedAt() != null) {
                    // Score exists and is graded
                    scoreDTO = GradebookDTO.GradebookScoreDTO.builder()
                            .score(score.getScore())
                            .maxScore(maxScore)
                            .feedback(score.getFeedback())
                            .isGraded(true)
                            .gradedBy(score.getGradedBy() != null 
                                    ? score.getGradedBy().getUserAccount().getFullName() : null)
                            .gradedAt(score.getGradedAt() != null 
                                    ? score.getGradedAt().toString() : null)
                            .build();
                    
                    // Add to graded scores for average calculation
                    if (maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal normalizedScore = score.getScore()
                                .divide(maxScore, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                        gradedScores.add(normalizedScore);
                    }
                } else {
                    // No score or not graded yet
                    scoreDTO = GradebookDTO.GradebookScoreDTO.builder()
                            .score(null)
                            .maxScore(maxScore)
                            .feedback(null)
                            .isGraded(false)
                            .gradedBy(null)
                            .gradedAt(null)
                            .build();
                }
                
                scoresMap.put(assessmentId, scoreDTO);
            }
            
            // Calculate average score
            BigDecimal averageScore = null;
            if (!gradedScores.isEmpty()) {
                averageScore = gradedScores.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(gradedScores.size()), 2, RoundingMode.HALF_UP);
            }
            
            int gradedCount = (int) scoresMap.values().stream()
                    .filter(GradebookDTO.GradebookScoreDTO::getIsGraded)
                    .count();
            
            studentDTOs.add(GradebookDTO.GradebookStudentDTO.builder()
                    .studentId(studentId)
                    .studentCode(student.getStudentCode())
                    .studentName(student.getUserAccount().getFullName())
                    .scores(scoresMap)
                    .averageScore(averageScore)
                    .gradedCount(gradedCount)
                    .totalAssessments(assessments.size())
                    .build());
        }
        
        // Sort students by student code
        studentDTOs.sort(Comparator.comparing(GradebookDTO.GradebookStudentDTO::getStudentCode));
        
        return GradebookDTO.builder()
                .classId(classId)
                .className(classEntity.getName())
                .classCode(classEntity.getCode())
                .assessments(assessmentDTOs)
                .students(studentDTOs)
                .build();
    }
}

