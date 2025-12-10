package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teacher.TeacherClassInfoDTO;
import org.fyp.tmssep490be.dtos.teacher.TeacherProfileDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// Service cho quản lý giáo viên
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeachingSlotRepository teachingSlotRepository;

    // Lấy thông tin profile của giáo viên hiện tại
    public TeacherProfileDTO getMyProfile(Long userAccountId) {
        log.debug("Getting teacher profile for user account {}", userAccountId);

        // Tìm giáo viên theo user account ID
        Teacher teacher = teacherRepository.findByUserAccountId(userAccountId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        // Chuyển đổi sang DTO
        return convertToTeacherProfileDTO(teacher);
    }

    // Chuyển đổi Teacher entity sang TeacherProfileDTO
    private TeacherProfileDTO convertToTeacherProfileDTO(Teacher teacher) {
        var user = teacher.getUserAccount();

        // Lấy thông tin chi nhánh
        String branchName = null;
        Long branchId = null;
        if (!user.getUserBranches().isEmpty()) {
            branchId = user.getUserBranches().iterator().next().getBranch().getId();
            branchName = user.getUserBranches().iterator().next().getBranch().getName();
        }

        // Lấy tất cả các lớp học riêng biệt mà giáo viên này dạy
        List<ClassEntity> allClasses = teachingSlotRepository.findDistinctClassesByTeacherId(teacher.getId());

        // Lọc lớp học theo trạng thái
        long totalClasses = allClasses.size();
        long activeClasses = allClasses.stream()
                .filter(c -> c.getStatus() == ClassStatus.ONGOING || c.getStatus() == ClassStatus.SCHEDULED)
                .count();
        long completedClasses = allClasses.stream()
                .filter(c -> c.getStatus() == ClassStatus.COMPLETED)
                .count();

        // Lấy ngày lớp học đầu tiên (ngày bắt đầu sớm nhất)
        LocalDate firstClassDate = allClasses.stream()
                .map(ClassEntity::getStartDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);

        // Chuyển đổi lớp học sang DTOs
        // Với mỗi lớp, tìm ngày assignedAt sớm nhất từ teaching slot
        List<TeacherClassInfoDTO> classDTOs = allClasses.stream()
                .sorted((c1, c2) -> {
                    // Sắp xếp theo trạng thái (ONGOING trước) rồi theo startDate giảm dần
                    int statusCompare = compareClassStatus(c1.getStatus(), c2.getStatus());
                    if (statusCompare != 0)
                        return statusCompare;
                    if (c1.getStartDate() == null && c2.getStartDate() == null)
                        return 0;
                    if (c1.getStartDate() == null)
                        return 1;
                    if (c2.getStartDate() == null)
                        return -1;
                    return c2.getStartDate().compareTo(c1.getStartDate()); // Giảm dần
                })
                .map(classEntity -> {
                    // Sử dụng created_at của class làm assignedAt (khi lớp được tạo/phân công)
                    var assignedAt = classEntity.getCreatedAt() != null
                            ? classEntity.getCreatedAt()
                            : java.time.OffsetDateTime.now();

                    return TeacherClassInfoDTO.builder()
                            .classId(classEntity.getId())
                            .classCode(classEntity.getCode())
                            .className(classEntity.getName())
                            .subjectName(classEntity.getSubject() != null ? classEntity.getSubject().getName() : null)
                            .branchName(classEntity.getBranch() != null ? classEntity.getBranch().getName() : null)
                            .startDate(classEntity.getStartDate())
                            .plannedEndDate(classEntity.getPlannedEndDate())
                            .status(classEntity.getStatus().name())
                            .assignedAt(assignedAt)
                            .build();
                })
                .collect(Collectors.toList());

        return TeacherProfileDTO.builder()
                .teacherId(teacher.getId())
                .teacherCode(teacher.getEmployeeCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .dateOfBirth(user.getDob())
                .facebookUrl(user.getFacebookUrl())
                .status(user.getStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .branchName(branchName)
                .branchId(branchId)
                .totalClasses(totalClasses)
                .activeClasses(activeClasses)
                .completedClasses(completedClasses)
                .firstClassDate(firstClassDate)
                .classes(classDTOs)
                .build();
    }

    // Helper method để so sánh trạng thái lớp học cho việc sắp xếp
    // ONGOING đứng đầu, sau đó SCHEDULED, rồi COMPLETED, cuối cùng là các trạng thái khác
    private int compareClassStatus(ClassStatus status1, ClassStatus status2) {
        int priority1 = getStatusPriority(status1);
        int priority2 = getStatusPriority(status2);
        return Integer.compare(priority1, priority2);
    }

    private int getStatusPriority(ClassStatus status) {
        if (status == ClassStatus.ONGOING)
            return 1;
        if (status == ClassStatus.SCHEDULED)
            return 2;
        if (status == ClassStatus.COMPLETED)
            return 3;
        return 4;
    }
}

