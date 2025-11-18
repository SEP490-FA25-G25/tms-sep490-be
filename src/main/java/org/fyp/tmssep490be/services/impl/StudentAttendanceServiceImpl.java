package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceOverviewItemDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceOverviewResponseDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceReportResponseDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceReportSessionDTO;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
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

    @Override
    public StudentAttendanceOverviewResponseDTO getOverview(Long studentId) {
        List<StudentSession> all = studentSessionRepository.findAllByStudentId(studentId);
        Map<Long, List<StudentSession>> byClass = all.stream()
                .collect(Collectors.groupingBy(ss -> ss.getSession().getClassEntity().getId(), LinkedHashMap::new, Collectors.toList()));

        List<StudentAttendanceOverviewItemDTO> items = byClass.entrySet().stream()
                .map(entry -> {
                    Long clsId = entry.getKey();
                    List<StudentSession> list = entry.getValue();
                    List<StudentSession> activeSessions = list.stream()
                            .filter(ss -> {
                                Session session = ss.getSession();
                                return session != null && session.getStatus() != SessionStatus.CANCELLED;
                            })
                            .collect(Collectors.toList());
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
                    StudentSession reference = !activeSessions.isEmpty() ? activeSessions.get(0) : list.get(0);
                    Session anySession = reference.getSession();
                    var classEntity = anySession.getClassEntity();
                    return StudentAttendanceOverviewItemDTO.builder()
                            .classId(clsId)
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


