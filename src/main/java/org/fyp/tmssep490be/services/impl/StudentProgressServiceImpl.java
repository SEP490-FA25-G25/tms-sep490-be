package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.course.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.StudentProgressService;
import org.springframework.stereotype.Service;

import java.util.Optional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentProgressServiceImpl implements StudentProgressService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final CourseMaterialRepository courseMaterialRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final ScoreRepository scoreRepository;
    private final CLORepository cloRepository;

    @Override
    public CourseProgressDTO calculateProgress(Long studentId, Long courseId) {
        log.debug("Calculating progress for student {} in course {}", studentId, courseId);

        // Get student enrollment
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ENROLLED);

        if (enrollment == null) {
            log.warn("Student {} not enrolled in course {}", studentId, courseId);
            return CourseProgressDTO.builder()
                    .courseId(courseId)
                    .studentId(studentId)
                    .totalSessions(0)
                    .completedSessions(0)
                    .totalMaterials(0)
                    .accessibleMaterials(0)
                    .progressPercentage(0.0)
                    .attendanceRate(0.0)
                    .build();
        }

        // Get total sessions for the course
        List<CourseSession> allSessions = courseSessionRepository.findByCourseIdOrderByPhaseIdAndSequenceNo(courseId);
        int totalSessions = allSessions.size();

        // Get completed sessions for the student
        List<StudentSession> studentSessions = studentSessionRepository.findByStudentIdAndClassId(
                enrollment.getStudentId(),
                enrollment.getClassId()
        );
        long completedSessions = studentSessions.stream()
                .filter(ss -> AttendanceStatus.PRESENT.equals(ss.getAttendanceStatus()))
                .count();

        // Calculate attendance rate
        double attendanceRate = totalSessions > 0 ?
                (double) completedSessions / totalSessions * 100 : 0.0;

        // Calculate progress percentage
        double progressPercentage = totalSessions > 0 ?
                (double) completedSessions / totalSessions * 100 : 0.0;

        // Get materials information
        List<CourseMaterial> allMaterials = courseMaterialRepository.findByCourseId(courseId);
        int totalMaterials = allMaterials.size();
        long accessibleMaterialsLong = calculateAccessibleMaterials(studentId, courseId);
        int accessibleMaterials = (int) accessibleMaterialsLong;

        // Calculate CLO progress
        List<CLOProgressDTO> cloProgress = calculateCLOProgress(enrollment, allSessions);

        // Calculate assessment progress
        List<AssessmentProgressDTO> assessmentProgress = calculateAssessmentProgress(enrollment);

        // Determine current phase and next session
        String currentPhase = determineCurrentPhase(enrollment, allSessions);
        String nextSession = determineNextSession(enrollment, allSessions);

        // Estimate completion date
        Long estimatedCompletionDate = estimateCompletionDate(enrollment, (int) completedSessions, totalSessions);

        return CourseProgressDTO.builder()
                .courseId(courseId)
                .studentId(studentId)
                .totalSessions(totalSessions)
                .completedSessions((int) completedSessions)
                .totalMaterials(totalMaterials)
                .accessibleMaterials(accessibleMaterials)
                .progressPercentage(round(progressPercentage, 1))
                .attendanceRate(round(attendanceRate, 1))
                .cloProgress(cloProgress)
                .assessmentProgress(assessmentProgress)
                .currentPhase(currentPhase)
                .nextSession(nextSession)
                .estimatedCompletionDate(estimatedCompletionDate)
                .build();
    }

    private int calculateAccessibleMaterials(Long studentId, Long courseId) {
        List<CourseMaterial> materials = courseMaterialRepository.findByCourseId(courseId);
        return (int) materials.stream()
                .filter(material -> isMaterialAccessible(studentId, material))
                .count();
    }

    private boolean isMaterialAccessible(Long studentId, CourseMaterial material) {
        // Course-level materials are always accessible
        if (material.getPhase() == null && material.getCourseSession() == null) {
            return true;
        }

        // Phase-level materials are accessible when phase starts
        if (material.getPhase() != null && material.getCourseSession() == null) {
            // Simplified logic - check if student has completed sessions before this phase
            return isPhaseAccessible(studentId, material.getPhase());
        }

        // Session-level materials are accessible after session completes
        if (material.getCourseSession() != null) {
            return isSessionAccessible(studentId, material.getCourseSession());
        }

        return false;
    }

    private boolean isPhaseAccessible(Long studentId, CoursePhase phase) {
        // Check if student has completed the first session of this phase
        List<CourseSession> sessions = courseSessionRepository
                .findByPhaseIdOrderBySequenceNo(phase.getId());

        if (sessions.isEmpty()) {
            return false;
        }

        return isSessionAccessible(studentId, sessions.get(0));
    }

    private boolean isSessionAccessible(Long studentId, CourseSession session) {
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseIdAndStatus(
                    studentId,
                    session.getPhase().getCourse().getId(),
                    EnrollmentStatus.ENROLLED
                );

        if (enrollment == null) {
            return false;
        }

        Optional<StudentSession> studentSession = studentSessionRepository
                .findByStudentIdAndCourseSessionId(enrollment.getStudentId(), session.getId());

        return studentSession.isPresent() &&
            AttendanceStatus.PRESENT.equals(studentSession.get().getAttendanceStatus());
    }

    private List<CLOProgressDTO> calculateCLOProgress(Enrollment enrollment, List<CourseSession> sessions) {
        List<CLO> clos = cloRepository.findByCourseId(enrollment.getClassEntity().getCourse().getId());

        return clos.stream()
                .map(clo -> {
                    List<CourseAssessment> assessments = courseAssessmentRepository.findByCourseId(enrollment.getClassEntity().getCourse().getId())
                            .stream()
                            .filter(assessment -> assessment.getCourseAssessmentCLOMappings().stream()
                                .map(mapping -> mapping.getClo())
                                .anyMatch(mappedClo -> mappedClo.getId().equals(clo.getId())))
                            .collect(Collectors.toList());

                    int totalAssessments = assessments.size();
                    int completedAssessments = 0;
                    double totalScore = 0.0;
                    double totalMaxScore = 0.0;

                    for (CourseAssessment assessment : assessments) {
                        Score score = scoreRepository.findByEnrollmentAndAssessment(enrollment.getStudentId(), assessment.getId())
                                .orElse(null);

                        if (score != null && score.getScore() != null) {
                            completedAssessments++;
                            totalScore += score.getScore().doubleValue();
                            totalMaxScore += assessment.getMaxScore().doubleValue();
                        }
                    }

                    double averageScore = totalMaxScore > 0 ?
                            (totalScore / totalMaxScore) * 100 : 0.0;

                    double achievementRate = totalAssessments > 0 ?
                            (double) completedAssessments / totalAssessments * 100 : 0.0;

                    boolean isAchieved = achievementRate >= 70.0 && averageScore >= 70.0;

                    return CLOProgressDTO.builder()
                            .cloId(clo.getId())
                            .cloCode(clo.getCode())
                            .description(clo.getDescription())
                            .achievementRate(round(achievementRate, 1))
                            .isAchieved(isAchieved)
                            .totalAssessments(totalAssessments)
                            .completedAssessments(completedAssessments)
                            .averageScore(round(averageScore, 1))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<AssessmentProgressDTO> calculateAssessmentProgress(Enrollment enrollment) {
        List<CourseAssessment> assessments = courseAssessmentRepository.findByCourseId(enrollment.getClassEntity().getCourse().getId());

        return assessments.stream()
                .map(assessment -> {
                    Score score = scoreRepository.findByEnrollmentAndAssessment(enrollment.getStudentId(), assessment.getId())
                            .orElse(null);

                    boolean isCompleted = score != null && score.getScore() != null;
                    BigDecimal achievedScore = isCompleted ? score.getScore() : BigDecimal.ZERO;
                    double percentageScore = assessment.getMaxScore().doubleValue() > 0 ?
                            achievedScore.doubleValue() / assessment.getMaxScore().doubleValue() * 100 : 0.0;

                    return AssessmentProgressDTO.builder()
                            .assessmentId(assessment.getId())
                            .name(assessment.getName())
                            .assessmentType(assessment.getKind().toString())
                            .weight(null) // Weight not available in CourseAssessment
                            .maxScore(assessment.getMaxScore())
                            .achievedScore(achievedScore)
                            .isCompleted(isCompleted)
                            .completedAt(score != null && score.getUpdatedAt() != null ?
                                    score.getUpdatedAt().toString() : null)
                            .percentageScore(round(percentageScore, 1))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String determineCurrentPhase(Enrollment enrollment, List<CourseSession> sessions) {
        List<StudentSession> completedSessions = studentSessionRepository.findByStudentIdAndClassId(
                enrollment.getStudentId(),
                enrollment.getClassId()
        )
                .stream()
                .filter(ss -> AttendanceStatus.PRESENT.equals(ss.getAttendanceStatus()))
                .collect(Collectors.toList());

        if (completedSessions.isEmpty()) {
            return "Not Started";
        }

        if (completedSessions.size() >= sessions.size()) {
            return "Completed";
        }

        // Calculate progress percentage to determine phase
        double progressPercentage = (double) completedSessions.size() / sessions.size() * 100;

        if (progressPercentage < 25) {
            return "Phase 1: Foundation";
        } else if (progressPercentage < 50) {
            return "Phase 2: Development";
        } else if (progressPercentage < 75) {
            return "Phase 3: Advanced";
        } else {
            return "Phase 4: Mastery";
        }
    }

    private String determineNextSession(Enrollment enrollment, List<CourseSession> sessions) {
        List<StudentSession> completedSessions = studentSessionRepository.findByStudentIdAndClassId(
                enrollment.getStudentId(),
                enrollment.getClassId()
        )
                .stream()
                .filter(ss -> AttendanceStatus.PRESENT.equals(ss.getAttendanceStatus()))
                .collect(Collectors.toList());

        if (completedSessions.size() >= sessions.size()) {
            return "All sessions completed";
        }

        // Find the next session to be completed
        int nextSessionIndex = completedSessions.size();
        if (nextSessionIndex < sessions.size()) {
            CourseSession nextSession = sessions.stream()
                    .sorted((a, b) -> a.getSequenceNo().compareTo(b.getSequenceNo()))
                    .collect(Collectors.toList())
                    .get(nextSessionIndex);

            return "Session " + nextSession.getSequenceNo() + ": " + nextSession.getTopic();
        }

        return "Next session to be scheduled";
    }

    private Long estimateCompletionDate(Enrollment enrollment, int completedSessions, int totalSessions) {
        if (completedSessions >= totalSessions) {
            return System.currentTimeMillis();
        }

        LocalDate startDate = enrollment.getClassEntity().getStartDate();
        if (startDate == null) {
            return null;
        }

        int sessionsPerWeek = enrollment.getClassEntity().getCourse().getSessionPerWeek();
        if (sessionsPerWeek <= 0) {
            return null;
        }

        int remainingSessions = totalSessions - completedSessions;
        int remainingWeeks = (int) Math.ceil((double) remainingSessions / sessionsPerWeek);

        // Calculate estimated completion based on current progress, not current date
        int weeksAlreadyElapsed = (int) Math.ceil((double) completedSessions / sessionsPerWeek);
        LocalDate currentDateInSchedule = startDate.plusWeeks(weeksAlreadyElapsed);

        LocalDate estimatedCompletionDate = currentDateInSchedule.plusWeeks(remainingWeeks);
        return estimatedCompletionDate.toEpochDay() * 24 * 60 * 60 * 1000; // Convert to milliseconds
    }

    private double round(double value, int places) {
        return BigDecimal.valueOf(value)
                .setScale(places, RoundingMode.HALF_UP)
                .doubleValue();
    }
}