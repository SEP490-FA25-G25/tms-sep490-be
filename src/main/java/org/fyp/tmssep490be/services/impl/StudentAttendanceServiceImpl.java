package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceOverviewItemDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceOverviewResponseDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceReportResponseDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceReportSessionDTO;
import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.services.StudentAttendanceService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentAttendanceServiceImpl implements StudentAttendanceService {

    private final StudentSessionRepository studentSessionRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    public StudentAttendanceOverviewResponseDTO getOverview(Long studentId) {
        // 1. Lấy TẤT CẢ enrollments của student (bất kể class status, bất kể có session hay không)
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdWithClassAndCourse(studentId);

        // 2. Lấy tất cả student sessions (để tính attendance)
        List<StudentSession> allSessions = studentSessionRepository.findAllByStudentId(studentId);

        // 3. Group sessions by classId
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
                    var classEntity = enrollment.getClassEntity();
                    Long classId = classEntity.getId();

                    // Get sessions for this class (may be empty)
                    List<StudentSession> classSessions = sessionsByClass.getOrDefault(classId, List.of());

                    // Filter out CANCELLED sessions
                    List<StudentSession> activeSessions = classSessions.stream()
                            .filter(ss -> {
                                Session session = ss.getSession();
                                return session != null && session.getStatus() != SessionStatus.CANCELLED;
                            })
                            .collect(Collectors.toList());

                    // Calculate stats
                    int attended = 0;
                    int absent = 0;
                    int upcoming = 0;
                    for (StudentSession ss : activeSessions) {
                        AttendanceStatus displayStatus = resolveDisplayStatusForStudent(ss);
                        if (displayStatus == null) {
                            continue;
                        }
                        switch (displayStatus) {
                            case PRESENT -> attended++;
                            case ABSENT -> absent++;
                            case PLANNED -> upcoming++;
                        }
                    }

                    return StudentAttendanceOverviewItemDTO.builder()
                            .classId(classId)
                            .classCode(classEntity.getCode())
                            .className(classEntity.getName())
                            .courseId(classEntity.getCourse().getId())
                            .courseCode(classEntity.getCourse().getCode())
                            .courseName(classEntity.getCourse().getName())
                            .startDate(classEntity.getStartDate())
                            .actualEndDate(classEntity.getActualEndDate())
                            .totalSessions(activeSessions.size())
                            .attended(attended)
                            .absent(absent)
                            .upcoming(upcoming)
                            .status(classEntity.getStatus().name())
                            .build();
                })
                .collect(Collectors.toList());

        return StudentAttendanceOverviewResponseDTO.builder()
                .classes(items)
                .build();
    }

    @Override
    public StudentAttendanceReportResponseDTO getReport(Long studentId, Long classId) {
        List<StudentSession> studentSessions = studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId);
        List<StudentSession> activeSessions = studentSessions.stream()
                .filter(ss -> {
                    Session session = ss.getSession();
                    return session != null && session.getStatus() != SessionStatus.CANCELLED;
                })
                .collect(Collectors.toList());

        // Tính summary
        int attended = 0;
        int absent = 0;
        int upcoming = 0;
        for (StudentSession ss : activeSessions) {
            AttendanceStatus displayStatus = resolveDisplayStatusForStudent(ss);
            if (displayStatus == null) {
                continue;
            }
            switch (displayStatus) {
                case PRESENT -> attended++;
                case ABSENT -> absent++;
                case PLANNED -> upcoming++;
            }
        }
        int completed = attended + absent;
        double rate = completed == 0 ? 0d : (double) attended / (double) completed;

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
                            .index(null)
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
        List<StudentSession> referenceSessions = activeSessions.isEmpty() ? studentSessions : activeSessions;
        if (!referenceSessions.isEmpty()) {
            Session any = referenceSessions.get(0).getSession();
            courseId = any.getClassEntity().getCourse().getId();
            courseCode = any.getClassEntity().getCourse().getCode();
            courseName = any.getClassEntity().getCourse().getName();
            classCode = any.getClassEntity().getCode();
            className = any.getClassEntity().getName();
        }

        StudentAttendanceReportResponseDTO.Summary summary = StudentAttendanceReportResponseDTO.Summary.builder()
                .totalSessions(activeSessions.size())
                .attended(attended)
                .absent(absent)
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
                .summary(summary)
                .sessions(sessionItems)
                .build();
    }

    /**
     * Hiển thị trạng thái cho học viên:
     * - Nếu ngày session < hôm nay và status là null hoặc PLANNED → ABSENT
     * - Ngược lại dùng status thực tế (PRESENT / ABSENT / PLANNED)
     */
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


