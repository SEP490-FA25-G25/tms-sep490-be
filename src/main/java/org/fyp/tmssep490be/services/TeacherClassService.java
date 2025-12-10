package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.QASessionListResponse;
import org.fyp.tmssep490be.dtos.teacherclass.ClassStudentDTO;
import org.fyp.tmssep490be.dtos.teacherclass.TeacherClassListItemDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.HomeworkStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.dtos.subject.SubjectDetailDTO;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.services.SubjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherClassService {
    
    private final TeachingSlotRepository teachingSlotRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final SubjectService subjectService;

    // Lấy tất cả lớp học được phân công cho giáo viên theo teacherId
    public List<TeacherClassListItemDTO> getTeacherClasses(Long teacherId) {
        // Query các lớp học riêng biệt mà giáo viên có teaching slots với trạng thái SCHEDULED
        List<ClassEntity> classes = teachingSlotRepository.findDistinctClassesByTeacherId(teacherId);
        
        // Chuyển đổi Entity sang DTO để trả về
        return classes.stream()
                .map(this::mapToTeacherClassListItemDTO)
                .collect(Collectors.toList());
    }
    
    // Chuyển đổi ClassEntity sang TeacherClassListItemDTO
    private TeacherClassListItemDTO mapToTeacherClassListItemDTO(ClassEntity classEntity) {
        return TeacherClassListItemDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                // Chuyển đổi thông tin môn học
                .subjectName(classEntity.getSubject() != null ? classEntity.getSubject().getName() : null)
                .subjectCode(classEntity.getSubject() != null ? classEntity.getSubject().getCode() : null)
                // Chuyển đổi thông tin chi nhánh
                .branchName(classEntity.getBranch() != null ? classEntity.getBranch().getName() : null)
                .branchCode(classEntity.getBranch() != null ? classEntity.getBranch().getCode() : null)
                // Chuyển đổi thông tin lịch học và trạng thái
                .modality(classEntity.getModality())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .status(classEntity.getStatus())
                // Tính toán tổng số buổi học từ SessionRepository
                .totalSessions(null)
                .attendanceRate(null)
                .build();
    }

    // Chuyển đổi Enrollment entity sang ClassStudentDTO
    private ClassStudentDTO convertToClassStudentDTO(Enrollment enrollment) {
        // Lấy Student từ Enrollment
        Student student = enrollment.getStudent();
        // Lấy UserAccount từ Student
        UserAccount userAccount = student.getUserAccount();

        // Lấy tên chi nhánh của sinh viên
        String branchName = null;
        if (userAccount.getUserBranches() != null && !userAccount.getUserBranches().isEmpty()) {
            // Lấy UserBranch đầu tiên từ collection, sau đó lấy Branch và tên
            branchName = userAccount.getUserBranches().iterator().next().getBranch().getName();
        }

        return ClassStudentDTO.builder()
                .id(enrollment.getId())
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(userAccount.getFullName())
                .email(userAccount.getEmail())
                .phone(userAccount.getPhone())
                .avatarUrl(userAccount.getAvatarUrl())
                .branchName(branchName)
                .enrolledAt(enrollment.getEnrolledAt())
                .enrolledBy(enrollment.getEnrolledByUser() != null ? enrollment.getEnrolledByUser().getFullName() : "System")
                .enrolledById(enrollment.getEnrolledBy())
                .status(enrollment.getStatus())
                .joinSessionId(enrollment.getJoinSessionId())
                .joinSessionDate(enrollment.getJoinSession() != null ? enrollment.getJoinSession().getDate().toString() : null)
                .capacityOverride(enrollment.getCapacityOverride())
                .overrideReason(enrollment.getOverrideReason())
                .build();
    }

    // Lấy danh sách sinh viên đăng ký lớp học theo ID lớp học
    public Page<ClassStudentDTO> getClassStudents(Long classId, String search, Pageable pageable) {
        log.debug("Getting students for class {} with search: {}", classId, search);

        // Kiểm tra xem lớp học có tồn tại
        classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND, "Class not found"));

        // Chuẩn bị tham số tìm kiếm (null nếu rỗng)
        String searchPattern = (search != null && !search.isBlank()) ? search : null;

        // Lấy danh sách sinh viên đăng ký lớp học theo ID lớp học
        Page<Enrollment> enrollments = enrollmentRepository.findEnrolledStudentsByClass(
                classId, EnrollmentStatus.ENROLLED, searchPattern, pageable);

        return enrollments.map(this::convertToClassStudentDTO);
    }

    // Lấy danh sách buổi học với metrics điểm danh và bài tập về nhà cho lớp học của giáo viên
    @Transactional(readOnly = true)
    public QASessionListResponse getSessionsWithMetrics(Long classId, Long teacherId) {
        log.info("Getting sessions with metrics for class ID {} by teacher {}", classId, teacherId);

        // Lấy lớp học từ database, nếu không tìm thấy thì throw exception
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND, "Class not found"));

        // Kiểm tra giáo viên có được phân công vào lớp học này không thông qua TeachingSlot
        // Query kiểm tra xem có TeachingSlot nào với teacherId và classId không
        boolean isAssigned = teachingSlotRepository.existsByTeacherIdAndClassEntityId(teacherId, classId);
        if (!isAssigned) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Teacher is not assigned to this class");
        }

        // Lấy tất cả buổi học của lớp học
        List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);

        // Map từng Session sang QASessionItemDTO với metrics thực tế
        List<QASessionListResponse.QASessionItemDTO> sessionItems = sessions.stream()
                .map(s -> {
                    // Lấy tất cả StudentSession của buổi học này
                    List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(s.getId());

                    // Tính toán metrics điểm danh
                    // Đếm số học sinh có trạng thái PRESENT (có mặt)
                    long presentCount = studentSessions.stream()
                            .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                            .count();

                    // Đếm số học sinh có trạng thái ABSENT (vắng mặt)
                    long absentCount = studentSessions.stream()
                            .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                            .count();

                    // Tổng số học sinh trong buổi học = tổng số StudentSession
                    long totalStudents = studentSessions.size();
                    
                    // Tính tỷ lệ điểm danh: (số có mặt / tổng số) * 100
                    // Nếu không có học sinh nào thì tỷ lệ = 0
                    double attendanceRate = totalStudents > 0 ? (presentCount * 100.0 / totalStudents) : 0.0;

                    // Tính toán metrics hoàn thành bài tập về nhà
                    // Đếm số học sinh đã hoàn thành bài tập (COMPLETED)
                    long homeworkCompletedCount = studentSessions.stream()
                            .filter(ss -> ss.getHomeworkStatus() == HomeworkStatus.COMPLETED)
                            .count();

                    // Đếm tổng số học sinh có bài tập về nhà (loại trừ NO_HOMEWORK và null)
                    // Logic: chỉ tính những học sinh có bài tập (COMPLETED hoặc INCOMPLETE)
                    // Không tính những học sinh không có bài tập (NO_HOMEWORK)
                    long homeworkTotalCount = studentSessions.stream()
                            .filter(ss -> ss.getHomeworkStatus() != null
                                    && ss.getHomeworkStatus() != HomeworkStatus.NO_HOMEWORK)
                            .count();

                    // Tính tỷ lệ hoàn thành: (số hoàn thành / tổng số có bài tập) * 100
                    // Nếu không có học sinh nào có bài tập thì tỷ lệ = 0
                    double homeworkCompletionRate = homeworkTotalCount > 0
                            ? (homeworkCompletedCount * 100.0 / homeworkTotalCount)
                            : 0.0;

                    // Kiểm tra QA reports cho buổi học này
                    // Nếu Session có QA reports thì đếm số lượng, không thì = 0
                    long qaReportCount = 0L;
                    if (s.getQaReports() != null) {
                        qaReportCount = s.getQaReports().size();
                    }

                    // Lấy thông tin buổi học từ các entity liên quan
                    // Sequence number: số thứ tự buổi học trong giáo trình (từ SubjectSession)
                    Integer sequenceNumber = s.getSubjectSession() != null ? s.getSubjectSession().getSequenceNo() : null;
                    
                    // Time slot: khung giờ học (từ TimeSlotTemplate), nếu không có thì "TBA" (To Be Announced)
                    String timeSlot = s.getTimeSlotTemplate() != null ? s.getTimeSlotTemplate().getName() : "TBA";
                    
                    // Topic: chủ đề buổi học (từ SubjectSession), nếu không có thì "N/A"
                    String topic = s.getSubjectSession() != null ? s.getSubjectSession().getTopic() : "N/A";

                    // Lấy tên giáo viên từ teaching slot
                    String teacherName = s.getTeachingSlots() != null && !s.getTeachingSlots().isEmpty()
                            ? s.getTeachingSlots().stream()
                                    .findFirst()
                                    .map(ts -> ts.getTeacher() != null && ts.getTeacher().getUserAccount() != null
                                            ? ts.getTeacher().getUserAccount().getFullName()
                                            : "TBA")
                                    .orElse("TBA")
                            : "TBA";

                    // Lấy tên thứ trong tuần trong tiếng Việt
                    String dayOfWeek = s.getDate() != null ? getVietnameseDayName(s.getDate().getDayOfWeek().getValue()) : null;

                    // Log metrics để debug (chỉ log khi ở chế độ debug)
                    log.debug("Session {} - Students: {}, Present: {}, Absent: {}, Attendance: {}%, Homework: {}%",
                            s.getId(), totalStudents, presentCount, absentCount,
                            String.format("%.1f", attendanceRate), String.format("%.1f", homeworkCompletionRate));

                    // Build QASessionItemDTO với tất cả thông tin đã thu thập
                    // Chuyển đổi từ long sang int cho các trường count (vì DTO dùng Integer)
                    return QASessionListResponse.QASessionItemDTO.builder()
                            .sessionId(s.getId())
                            .sequenceNumber(sequenceNumber)
                            .date(s.getDate())
                            .dayOfWeek(dayOfWeek)
                            .timeSlot(timeSlot)
                            .topic(topic)
                            .status(s.getStatus() != null ? s.getStatus().name() : null)
                            .teacherName(teacherName)
                            .totalStudents((int) totalStudents)
                            .presentCount((int) presentCount)
                            .absentCount((int) absentCount)
                            .attendanceRate(attendanceRate)
                            .homeworkCompletedCount((int) homeworkCompletedCount)
                            .homeworkCompletionRate(homeworkCompletionRate)
                            .hasQAReport(qaReportCount > 0)
                            .qaReportCount((int) qaReportCount)
                            .build();
                })
                .collect(Collectors.toList());  // Thu thập tất cả DTO vào List

        // Build response DTO với thông tin lớp học và danh sách buổi học
        return QASessionListResponse.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode() != null ? classEntity.getCode() : "N/A")
                .totalSessions(sessions.size())
                .sessions(sessionItems)
                .build();
    }

    // Lấy tên thứ trong tuần bằng tiếng Việt từ số thứ trong tuần
    private String getVietnameseDayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Thứ Hai";      // Monday
            case 2 -> "Thứ Ba";       // Tuesday
            case 3 -> "Thứ Tư";       // Wednesday
            case 4 -> "Thứ Năm";      // Thursday
            case 5 -> "Thứ Sáu";      // Friday
            case 6 -> "Thứ Bảy";      // Saturday
            case 7 -> "Chủ Nhật";     // Sunday
            default -> "Unknown";      // Trường hợp không hợp lệ
        };
    }

    // Lấy giáo trình (curriculum/syllabus) của lớp học
    // Lấy subjectId từ classId rồi gọi SubjectService.getSubjectSyllabus
    public SubjectDetailDTO getClassCurriculum(Long classId, Long teacherId) {
        // Validate lớp học tồn tại và giáo viên có quyền truy cập
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Kiểm tra giáo viên có được phân công vào lớp này không
        boolean hasAccess = teachingSlotRepository.existsByTeacherIdAndClassEntityId(teacherId, classId);
        if (!hasAccess) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Teacher does not have access to this class");
        }

        // Lấy subjectId từ class
        if (classEntity.getSubject() == null || classEntity.getSubject().getId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Class does not have a subject assigned");
        }

        Long subjectId = classEntity.getSubject().getId();

        // Lấy giáo trình từ SubjectService
        return subjectService.getSubjectSyllabus(subjectId);
    }
}