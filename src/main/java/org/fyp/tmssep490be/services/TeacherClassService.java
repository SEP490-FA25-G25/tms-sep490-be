package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teacherclass.ClassStudentDTO;
import org.fyp.tmssep490be.dtos.teacherclass.TeacherClassListItemDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherClassService {
    
    private final TeachingSlotRepository teachingSlotRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;

    //Lấy tất cả lớp học được phân công cho giáo viên theo teacherId
    public List<TeacherClassListItemDTO> getTeacherClasses(Long teacherId) {
        // Query distinct classes where teacher has teaching slots with SCHEDULED status
        List<ClassEntity> classes = teachingSlotRepository.findDistinctClassesByTeacherId(teacherId);
        
        // Chuyển đổi Entity sang DTO để trả về
        return classes.stream()
                .map(this::mapToTeacherClassListItemDTO)
                .collect(Collectors.toList());
    }
    
    //Chuyển đổi ClassEntity sang TeacherClassListItemDTO
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
                //Tính toán tổng số buổi học từ SessionRepository
                .totalSessions(null)
                .attendanceRate(null)
                .build();
    }

    //Chuyển đổi Enrollment entity sang ClassStudentDTO
    private ClassStudentDTO convertToClassStudentDTO(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        UserAccount userAccount = student.getUserAccount();

        // Lấy tên chi nhánh của sinh viên
        String branchName = null;
        if (userAccount.getUserBranches() != null && !userAccount.getUserBranches().isEmpty()) {
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

    //Lấy danh sách sinh viên đăng ký lớp học theo ID lớp học
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
}