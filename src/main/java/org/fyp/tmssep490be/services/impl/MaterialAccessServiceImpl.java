package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.MaterialAccessService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialAccessServiceImpl implements MaterialAccessService {

    private final CourseMaterialRepository courseMaterialRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final CoursePhaseRepository coursePhaseRepository;
    private final CourseSessionRepository courseSessionRepository;

    @Override
    public boolean canAccessMaterial(Long studentId, Long materialId) {
        log.debug("Checking access for student {} to material {}", studentId, materialId);

        CourseMaterial material = courseMaterialRepository.findById(materialId)
                .orElse(null);
        if (material == null) {
            log.warn("Material {} not found", materialId);
            return false;
        }

        // Check if student is enrolled in the course
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseIdAndStatus(studentId, material.getCourse().getId(), EnrollmentStatus.ENROLLED);
        if (enrollment == null) {
            log.debug("Student {} not enrolled in course {}", studentId, material.getCourse().getId());
            return false;
        }

        // Course-level materials are always available
        if (material.getPhase() == null && material.getCourseSession() == null) {
            return true;
        }

        // Phase-level materials are available when phase starts
        if (material.getPhase() != null && material.getCourseSession() == null) {
            return isPhaseStarted(enrollment, material.getPhase());
        }

        // Session-level materials are available after session completes
        if (material.getCourseSession() != null) {
            return isSessionCompleted(enrollment, material.getCourseSession());
        }

        return false;
    }

    @Override
    public boolean canAccessCourseLevelMaterial(Long studentId, Long courseId) {
        log.debug("Checking access for student {} to course-level materials for course {}", studentId, courseId);

        // Check if student is enrolled in the course
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ENROLLED);

        return enrollment != null;
    }

    @Override
    public boolean canAccessPhaseLevelMaterial(Long studentId, Long phaseId) {
        log.debug("Checking access for student {} to phase-level materials for phase {}", studentId, phaseId);

        CoursePhase phase = coursePhaseRepository.findById(phaseId).orElse(null);
        if (phase == null) {
            return false;
        }

        // Check if student is enrolled in the course
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseIdAndStatus(studentId, phase.getCourse().getId(), EnrollmentStatus.ENROLLED);
        if (enrollment == null) {
            return false;
        }

        return isPhaseStarted(enrollment, phase);
    }

    @Override
    public boolean canAccessSessionLevelMaterial(Long studentId, Long sessionId) {
        log.debug("Checking access for student {} to session-level materials for session {}", studentId, sessionId);

        CourseSession session = courseSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return false;
        }

        // Check if student is enrolled in the course
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseIdAndStatus(studentId, session.getPhase().getCourse().getId(), EnrollmentStatus.ENROLLED);
        if (enrollment == null) {
            return false;
        }

        return isSessionCompleted(enrollment, session);
    }

    private boolean isPhaseStarted(Enrollment enrollment, CoursePhase phase) {
        // A phase is considered started if the class has started and we've reached the phase
        LocalDate classStartDate = enrollment.getClassEntity().getStartDate();
        if (classStartDate == null) {
            return false;
        }

        // Calculate the start date of this phase based on session sequence
        List<CourseSession> sessionsBeforePhase = courseSessionRepository
                .findByPhaseIdOrderBySequenceNo(phase.getId())
                .stream()
                .filter(session -> session.getSequenceNo() <= 1) // First session of this phase
                .toList();

        if (sessionsBeforePhase.isEmpty()) {
            return false;
        }

        // Calculate the theoretical start date of this phase
        int sessionsPerWeek = enrollment.getClassEntity().getCourse().getSessionPerWeek();
        int weeksOffset = sessionsBeforePhase.get(0).getSequenceNo() / sessionsPerWeek;

        LocalDate phaseStartDate = classStartDate.plusWeeks(weeksOffset);
        LocalDate today = LocalDate.now();

        return today.isEqual(phaseStartDate) || today.isAfter(phaseStartDate);
    }

    private boolean isSessionCompleted(Enrollment enrollment, CourseSession session) {
        // Check if student has completed this session
        StudentSession studentSession = studentSessionRepository
                .findByStudentIdAndCourseSessionId(enrollment.getStudentId(), session.getId())
                .orElse(null);

        if (studentSession == null) {
            return false;
        }

        // Session is considered completed if student attended or it's marked as completed
        return AttendanceStatus.PRESENT.equals(studentSession.getAttendanceStatus()) ||
               (studentSession.getAttendanceStatus() != null &&
                studentSession.getUpdatedAt() != null);
    }
}