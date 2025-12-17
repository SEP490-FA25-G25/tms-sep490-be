package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceOverviewItemDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceOverviewResponseDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceReportResponseDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceReportSessionDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentAttendanceService {

    private final StudentSessionRepository studentSessionRepository;
    private final EnrollmentRepository enrollmentRepository;

    public StudentAttendanceOverviewResponseDTO getOverview(Long studentId) {
        log.info("Getting attendance overview for student: {}", studentId);
        
        // 1. Lấy TẤT CẢ enrollments của student (bất kể class status, bất kể có session hay không)
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdWithClassAndCourse(studentId);

        List<StudentSession> allSessions = studentSessionRepository.findAllByStudentId(studentId);

        // Group sessions by classId
        Map<Long, List<StudentSession>> sessionsByClass = allSessions.stream()
                .collect(Collectors.groupingBy(ss -> ss.getSession().getClassEntity().getId()));

        // 4. Sort and map enrollments to DTOs (ONGOING first, then SCHEDULED, then COMPLETED)
        List<StudentAttendanceOverviewItemDTO> items = enrollments.stream()
                .sorted((e1, e2) -> {
                    ClassStatus s1 = e1.getClassEntity().getStatus();
                    ClassStatus s2 = e2.getClassEntity().getStatus();
                    // Priority: ONGOING=1, SCHEDULED=2, COMPLETED=3, others=4
                    int p1 = s1 == ClassStatus.ONGOING ? 1 : s1 == ClassStatus.SCHEDULED ? 2 : s1 == ClassStatus.COMPLETED ? 3 : 4;
                    int p2 = s2 == ClassStatus.ONGOING ? 1 : s2 == ClassStatus.SCHEDULED ? 2 : s2 == ClassStatus.COMPLETED ? 3 : 4;
                    return Integer.compare(p1, p2);
                })
                .map(enrollment -> {
                    ClassEntity classEntity = enrollment.getClassEntity();
                    Long classId = classEntity.getId();

                    // Get sessions for this class (may be empty)
                    List<StudentSession> classSessions = sessionsByClass.getOrDefault(classId, List.of());

                    // Filter sessions by enrollment metadata and status
                    List<StudentSession> activeSessions = classSessions.stream()
                            .filter(ss -> {
                                Session session = ss.getSession();
                                if (session == null || session.getStatus() == SessionStatus.CANCELLED) {
                                    return false;
                                }
                                
                                Long sessionId = session.getId();
                                Long joinId = enrollment.getJoinSessionId();
                                Long leftId = enrollment.getLeftSessionId();
                                
                                // Filter by enrollment timeline
                                if (enrollment.getStatus() == EnrollmentStatus.TRANSFERRED) {
                                    // Only sessions <= leftSessionId
                                    return leftId == null || sessionId <= leftId;
                                } else if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
                                    // Only sessions >= joinSessionId
                                    return joinId == null || sessionId >= joinId;
                                } else if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
                                    // Sessions from join to end
                                    return joinId == null || sessionId >= joinId;
                                }
                                
                                return true;
                            })
                            .toList();

                    int attended = 0;
                    int absent = 0;
                    int excused = 0;
                    int upcoming = 0;
                    for (StudentSession ss : activeSessions) {
                        AttendanceStatus displayStatus = resolveDisplayStatusForStudent(ss);
                        if (displayStatus == null) {
                            continue;
                        }
                        switch (displayStatus) {
                            case PRESENT -> attended++;
                            case ABSENT -> absent++;
                            case EXCUSED -> excused++;
                            case PLANNED -> upcoming++;
                            default -> {
                                // Handle other statuses if needed
                            }
                        }
                    }

                    return StudentAttendanceOverviewItemDTO.builder()
                            .classId(classId)
                            .classCode(classEntity.getCode())
                            .className(classEntity.getName())
                            .courseId(classEntity.getSubject().getId())
                            .courseCode(classEntity.getSubject().getCode())
                            .courseName(classEntity.getSubject().getName())
                            .startDate(classEntity.getStartDate())
                            .actualEndDate(classEntity.getActualEndDate())
                            .totalSessions(activeSessions.size())
                            .attended(attended)
                            .absent(absent)
                            .excused(excused)
                            .upcoming(upcoming)
                            .status(classEntity.getStatus().name())
                            .enrollmentStatus(enrollment.getStatus() != null ? enrollment.getStatus().name() : null)
                            .lastUpdated(null) // Can be set if needed
                            .build();
                })
                .collect(Collectors.toList());

        return StudentAttendanceOverviewResponseDTO.builder()
                .classes(items)
                .build();
    }

    public StudentAttendanceReportResponseDTO getReport(Long studentId, Long classId) {
        log.info("Getting attendance report for student: {} in class: {}", studentId, classId);
        
        // Get enrollment to determine timeline boundaries
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassId(studentId, classId);
        if (enrollment == null) {
            log.warn("No enrollment found for student {} in class {}", studentId, classId);
            // Return empty report if no enrollment found
            return StudentAttendanceReportResponseDTO.builder()
                    .classId(classId)
                    .summary(StudentAttendanceReportResponseDTO.Summary.builder()
                            .totalSessions(0)
                            .attended(0)
                            .absent(0)
                            .excused(0)
                            .upcoming(0)
                            .attendanceRate(0.0)
                            .build())
                    .sessions(List.of())
                    .build();
        }
        
        List<StudentSession> studentSessions = studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId);
        
        // Filter sessions by enrollment metadata
        List<StudentSession> activeSessions = studentSessions.stream()
                .filter(ss -> {
                    Session session = ss.getSession();
                    if (session == null || session.getStatus() == SessionStatus.CANCELLED) {
                        return false;
                    }
                    
                    Long sessionId = session.getId();
                    Long joinId = enrollment.getJoinSessionId();
                    Long leftId = enrollment.getLeftSessionId();
                    
                    // Filter by enrollment timeline
                    if (enrollment.getStatus() == EnrollmentStatus.TRANSFERRED) {
                        // Only sessions <= leftSessionId
                        return leftId == null || sessionId <= leftId;
                    } else if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
                        // Only sessions >= joinSessionId
                        return joinId == null || sessionId >= joinId;
                    } else if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
                        // Sessions from join to end
                        return joinId == null || sessionId >= joinId;
                    }
                    
                    return true;
                })
                .toList();

        int attended = 0;
        int absent = 0;
        int excused = 0;
        int upcoming = 0;
        for (StudentSession ss : activeSessions) {
            AttendanceStatus displayStatus = resolveDisplayStatusForStudent(ss);
            if (displayStatus == null) {
                continue;
            }
            switch (displayStatus) {
                case PRESENT -> attended++;
                case ABSENT -> absent++;
                case EXCUSED -> excused++;
                case PLANNED -> upcoming++;
                default -> {
                }
            }
        }
        double rate = (attended + absent) == 0 ? 0d : (double) attended / (double) (attended + absent);

        // Map sessions DTO
        List<StudentAttendanceReportSessionDTO> sessionItems = activeSessions.stream()
                .sorted(Comparator.comparing(ss -> ss.getSession().getDate()))
                .map(ss -> {
                    Session s = ss.getSession();

                    // Thông tin thời gian
                    var timeSlot = s.getTimeSlotTemplate();
                    var startTime = timeSlot != null ? timeSlot.getStartTime() : null;
                    var endTime = timeSlot != null ? timeSlot.getEndTime() : null;

                    // Phòng / resource (lấy resource đầu tiên nếu có)
                    String classroomName = s.getSessionResources().stream()
                            .findFirst()
                            .map(sr -> sr.getResource() != null ? sr.getResource().getName() : null)
                            .orElse(null);

                    // Tên giáo viên (lấy slot đầu tiên nếu có)
                    String teacherName = s.getTeachingSlots().stream()
                            .findFirst()
                            .map(ts -> ts.getTeacher() != null && ts.getTeacher().getUserAccount() != null
                                    ? ts.getTeacher().getUserAccount().getFullName()
                                    : null)
                            .orElse(null);

                    StudentAttendanceReportSessionDTO.MakeupInfo makeupInfo = null;
                    if (Boolean.TRUE.equals(ss.getIsMakeup()) && ss.getMakeupSession() != null) {
                        Session ms = ss.getMakeupSession();
                        makeupInfo = StudentAttendanceReportSessionDTO.MakeupInfo.builder()
                                .sessionId(ms.getId())
                                .classId(ms.getClassEntity().getId())
                                .classCode(ms.getClassEntity().getCode())
                                .date(ms.getDate())
                                .attended(ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                                .build();
                    }

                    AttendanceStatus displayStatus = resolveDisplayStatusForStudent(ss);

                    return StudentAttendanceReportSessionDTO.builder()
                            .sessionId(s.getId())
                            .date(s.getDate())
                            .status(s.getStatus() != null ? s.getStatus().name() : null)
                            .startTime(startTime)
                            .endTime(endTime)
                            .classroomName(classroomName)
                            .teacherName(teacherName)
                            .attendanceStatus(displayStatus)
                            .homeworkStatus(ss.getHomeworkStatus())
                            .isMakeup(Boolean.TRUE.equals(ss.getIsMakeup()))
                            .note(ss.getNote())
                            .makeupSessionInfo(makeupInfo)
                            .build();
                })
                .collect(Collectors.toList());

        // Lấy thông tin lớp/khoá học từ một session bất kỳ (nếu có)
        Long courseId = null;
        String courseCode = null;
        String courseName = null;
        String classCode = null;
        String className = null;
        LocalDate startDate = null;
        LocalDate actualEndDate = null;
        String status = null;
        
        // Nếu activeSession đã bị lọc Cancelled hết thì lấy từ studentSessions ban đầu -> dùng studentSessions chưa lọc
        // Để đảm bảo luôn có ít nhất 1 session để lấy thông tin lớp/khoá học
        // Ví dụ lớp chỉ có 1 buổi duy nhất bị cancelled thì vẫn lấy được thông tin lớp/khoá học
        List<StudentSession> referenceSessions = activeSessions.isEmpty() ? studentSessions : activeSessions;
        if (!referenceSessions.isEmpty()) {
            Session any = referenceSessions.get(0).getSession();
            ClassEntity classEntity = any.getClassEntity();
            courseId = classEntity.getSubject().getId();
            courseCode = classEntity.getSubject().getCode();
            courseName = classEntity.getSubject().getName();
            classCode = classEntity.getCode();
            className = classEntity.getName();
            startDate = classEntity.getStartDate();
            actualEndDate = classEntity.getActualEndDate();
            status = classEntity.getStatus() != null ? classEntity.getStatus().name() : null;
        }

        StudentAttendanceReportResponseDTO.Summary summary = StudentAttendanceReportResponseDTO.Summary.builder()
                .totalSessions(activeSessions.size())
                .attended(attended)
                .absent(absent)
                .excused(excused)
                .upcoming(upcoming)
                .attendanceRate(rate)
                .build();

        return StudentAttendanceReportResponseDTO.builder()
                .classId(classId)
                .classCode(classCode)
                .className(className)
                .courseId(courseId)
                .courseCode(courseCode)
                .courseName(courseName)
                .startDate(startDate)
                .actualEndDate(actualEndDate)
                .status(status)
                .enrollmentStatus(enrollment.getStatus() != null ? enrollment.getStatus().name() : null)
                .summary(summary)
                .sessions(sessionItems)
                .build();
    }

    private AttendanceStatus resolveDisplayStatusForStudent(StudentSession studentSession) {
        Session session = studentSession.getSession();
        if (session == null) {
            return studentSession.getAttendanceStatus();
        }
        AttendanceStatus status = studentSession.getAttendanceStatus();
        LocalDate today = LocalDate.now();

        // Qua ngày mà vẫn chưa điểm danh (null hoặc PLANNED) thì coi là ABSENT
        if (session.getDate() != null
                && session.getDate().isBefore(today)
                && (status == null || status == AttendanceStatus.PLANNED)) {
            return AttendanceStatus.ABSENT;
        }

        return status;
    }
}
