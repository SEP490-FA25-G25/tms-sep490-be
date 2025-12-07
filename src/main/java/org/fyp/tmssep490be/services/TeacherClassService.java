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

    //Get all classes assigned to a specific teacher
    public List<TeacherClassListItemDTO> getTeacherClasses(Long teacherId) {
        // Query distinct classes where teacher has teaching slots with SCHEDULED status
        List<ClassEntity> classes = teachingSlotRepository.findDistinctClassesByTeacherId(teacherId);
        
        // Convert Entity to DTO for response
        return classes.stream()
                .map(this::mapToTeacherClassListItemDTO)
                .collect(Collectors.toList());
    }
    
    //Map ClassEntity to TeacherClassListItemDTO
    private TeacherClassListItemDTO mapToTeacherClassListItemDTO(ClassEntity classEntity) {
        return TeacherClassListItemDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                // Map subject information (subject is equivalent to course in this system)
                .courseName(classEntity.getSubject() != null ? classEntity.getSubject().getName() : null)
                .courseCode(classEntity.getSubject() != null ? classEntity.getSubject().getCode() : null)
                // Map branch information
                .branchName(classEntity.getBranch() != null ? classEntity.getBranch().getName() : null)
                .branchCode(classEntity.getBranch() != null ? classEntity.getBranch().getCode() : null)
                // Map schedule and status information
                .modality(classEntity.getModality())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .status(classEntity.getStatus())
                //Calculate total sessions from SessionRepository
                .totalSessions(null)
                .attendanceRate(null)
                .build();
    }

    //Map Enrollment entity to ClassStudentDTO
    private ClassStudentDTO convertToClassStudentDTO(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        UserAccount userAccount = student.getUserAccount();

        // Get student's branch name
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

    //Get paginated list of students enrolled in a class
    public Page<ClassStudentDTO> getClassStudents(Long classId, String search, Pageable pageable) {
        log.debug("Getting students for class {} with search: {}", classId, search);

        // Validate class existence
        classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND, "Class not found"));

        // Prepare search parameter (null if empty)
        String searchPattern = (search != null && !search.isBlank()) ? search : null;

        // Get enrolled students
        Page<Enrollment> enrollments = enrollmentRepository.findEnrolledStudentsByClass(
                classId, EnrollmentStatus.ENROLLED, searchPattern, pageable);

        return enrollments.map(this::convertToClassStudentDTO);
    }
}