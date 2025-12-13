package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.managerteacher.ManagerTeacherListItemDTO;
import org.fyp.tmssep490be.dtos.schedule.WeeklyScheduleResponseDTO;
import org.fyp.tmssep490be.dtos.teacher.TeacherClassInfoDTO;
import org.fyp.tmssep490be.dtos.teacher.TeacherProfileDTO;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.UserBranches;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerTeacherService {

    private final UserBranchesRepository userBranchesRepository;
    private final TeacherRepository teacherRepository;
    private final BranchRepository branchRepository;
    private final TeacherScheduleService teacherScheduleService;

    // Lấy danh sách giáo viên trong phạm vi quản lý của manager
    @Transactional(readOnly = true)
    public List<ManagerTeacherListItemDTO> getManagedTeachers(Long managerUserId) {
        List<Long> branchIds = userBranchesRepository.findBranchIdsByUserId(managerUserId);
        if (branchIds == null || branchIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> branchScope = new HashSet<>(branchIds);

        // Lấy tất cả giáo viên đang được gán vào các chi nhánh trong phạm vi quản lý
        List<Teacher> teachers = teacherRepository.findByBranchIds(new ArrayList<>(branchScope));

        if (teachers.isEmpty()) {
            return Collections.emptyList();
        }

        return teachers.stream()
                .map(teacher -> mapToDto(teacher, branchScope))
                .toList();
    }

    // Map Teacher entity sang DTO
    private ManagerTeacherListItemDTO mapToDto(Teacher teacher, Set<Long> branchScope) {
        UserAccount ua = teacher.getUserAccount();

        List<String> branchNames = ua.getUserBranches().stream()
                .filter(ub -> ub.getBranch() != null && branchScope.contains(ub.getBranch().getId()))
                .map(ub -> ub.getBranch().getName())
                .distinct()
                .collect(Collectors.toList());

        return ManagerTeacherListItemDTO.builder()
                .teacherId(teacher.getId())
                .userId(ua.getId())
                .fullName(ua.getFullName())
                .email(ua.getEmail())
                .phone(ua.getPhone())
                .avatarUrl(ua.getAvatarUrl())
                .employeeCode(teacher.getEmployeeCode())
                .status(ua.getStatus())
                .branchNames(branchNames)
                .build();
    }

    // Gán giáo viên vào chi nhánh trong phạm vi quản lý
    @Transactional
    public void assignTeacherToBranch(Long managerUserId, Long teacherId, Long branchId) {
        List<Long> branchIds = userBranchesRepository.findBranchIdsByUserId(managerUserId);
        if (branchIds == null || branchIds.isEmpty() || !branchIds.contains(branchId)) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        Long userId = teacher.getUserAccount().getId();

        boolean exists = userBranchesRepository.existsByUserAccountIdAndBranchId(userId, branchId);
        if (exists) {
            log.info("Teacher {} (user {}) is already assigned to branch {}", teacherId, userId, branchId);
            return;
        }

        UserAccount managerAccount = new UserAccount();
        managerAccount.setId(managerUserId);

        UserBranches.UserBranchesId id = new UserBranches.UserBranchesId(userId, branchId);
        UserBranches userBranches = UserBranches.builder()
                .id(id)
                .userAccount(teacher.getUserAccount())
                .branch(branch)
                .assignedBy(managerAccount)
                .build();

        userBranchesRepository.save(userBranches);
    }

    // Cập nhật danh sách chi nhánh của giáo viên trong phạm vi quản lý
    @Transactional
    public void updateTeacherBranches(Long managerUserId, Long teacherId, List<Long> branchIds) {
        List<Long> managerBranchIds = userBranchesRepository.findBranchIdsByUserId(managerUserId);
        if (managerBranchIds == null || managerBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Long userId = teacher.getUserAccount().getId();

        // Chỉ cho phép thay đổi các chi nhánh trong phạm vi quản lý của manager
        Set<Long> scope = new HashSet<>(managerBranchIds);
        Set<Long> targetBranchIds = (branchIds == null ? Collections.<Long>emptyList() : branchIds)
                .stream()
                .filter(scope::contains)
                .collect(Collectors.toSet());

        // Lấy các assignment hiện tại của giáo viên
        List<UserBranches> existingAssignments = userBranchesRepository.findAll().stream()
                .filter(ub -> ub.getId().getUserId().equals(userId))
                .collect(Collectors.toList());

        // Lọc các assignment trong phạm vi quản lý
        List<UserBranches> scopedAssignments = existingAssignments.stream()
                .filter(ub -> ub.getBranch() != null && scope.contains(ub.getBranch().getId()))
                .toList();

        Set<Long> existingBranchIdsInScope = scopedAssignments.stream()
                .map(ub -> ub.getBranch().getId())
                .collect(Collectors.toSet());

        // Xác định các assignment cần xóa
        List<UserBranches> toDelete = scopedAssignments.stream()
                .filter(ub -> !targetBranchIds.contains(ub.getBranch().getId()))
                .toList();

        if (!toDelete.isEmpty()) {
            userBranchesRepository.deleteAll(toDelete);
        }

        // Xác định các assignment mới cần thêm
        Set<Long> toAddIds = new HashSet<>(targetBranchIds);
        toAddIds.removeAll(existingBranchIdsInScope);

        if (!toAddIds.isEmpty()) {
            UserAccount managerAccount = new UserAccount();
            managerAccount.setId(managerUserId);

            for (Long branchId : toAddIds) {
                Branch branch = branchRepository.findById(branchId)
                        .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

                UserBranches.UserBranchesId id = new UserBranches.UserBranchesId(userId, branchId);
                UserBranches userBranches = UserBranches.builder()
                        .id(id)
                        .userAccount(teacher.getUserAccount())
                        .branch(branch)
                        .assignedBy(managerAccount)
                        .build();

                userBranchesRepository.save(userBranches);
            }
        }
    }

    // Lấy chi tiết hồ sơ giáo viên trong phạm vi quản lý
    @Transactional(readOnly = true)
    public TeacherProfileDTO getTeacherProfile(Long managerUserId, Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        ensureTeacherInManagerScope(managerUserId, teacher);

        return buildTeacherProfileDTO(teacher);
    }

    // Lấy lịch dạy theo tuần của giáo viên trong phạm vi quản lý
    @Transactional(readOnly = true)
    public WeeklyScheduleResponseDTO getTeacherWeeklySchedule(Long managerUserId, Long teacherId, LocalDate weekStart) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        ensureTeacherInManagerScope(managerUserId, teacher);

        return teacherScheduleService.getWeeklySchedule(teacher.getId(), weekStart, null);
    }

    // Đảm bảo giáo viên thuộc ít nhất một chi nhánh trong phạm vi quản lý của manager
    private void ensureTeacherInManagerScope(Long managerUserId, Teacher teacher) {
        List<Long> managerBranchIds = userBranchesRepository.findBranchIdsByUserId(managerUserId);
        if (managerBranchIds == null || managerBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        Long teacherUserId = teacher.getUserAccount().getId();
        List<Long> teacherBranchIds = userBranchesRepository.findBranchIdsByUserId(teacherUserId);

        if (teacherBranchIds == null || teacherBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        boolean hasOverlap = teacherBranchIds.stream().anyMatch(managerBranchIds::contains);
        if (!hasOverlap) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }
    }

    // Build TeacherProfileDTO
    private TeacherProfileDTO buildTeacherProfileDTO(Teacher teacher) {
        UserAccount user = teacher.getUserAccount();

        String branchName = null;
        Long branchId = null;
        if (!user.getUserBranches().isEmpty()) {
            Branch firstBranch = user.getUserBranches().iterator().next().getBranch();
            if (firstBranch != null) {
                branchId = firstBranch.getId();
                branchName = firstBranch.getName();
            }
        }

        // Lấy tất cả các lớp học mà giáo viên đã dạy
        List<ClassEntity> allClasses = teacher.getTeachingSlots() == null
                ? Collections.emptyList()
                : teacher.getTeachingSlots().stream()
                .map(ts -> ts.getSession().getClassEntity())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        long totalClasses = allClasses.size();
        long activeClasses = allClasses.stream()
                .filter(c -> c.getStatus() == ClassStatus.ONGOING || c.getStatus() == ClassStatus.SCHEDULED)
                .count();
        long completedClasses = allClasses.stream()
                .filter(c -> c.getStatus() == ClassStatus.COMPLETED)
                .count();

        LocalDate firstClassDate = allClasses.stream()
                .map(ClassEntity::getStartDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);

        List<TeacherClassInfoDTO> classDTOs = allClasses.stream()
                .sorted((c1, c2) -> {
                    int statusCompare = compareClassStatus(c1.getStatus(), c2.getStatus());
                    if (statusCompare != 0) return statusCompare;
                    if (c1.getStartDate() == null && c2.getStartDate() == null) return 0;
                    if (c1.getStartDate() == null) return 1;
                    if (c2.getStartDate() == null) return -1;
                    return c2.getStartDate().compareTo(c1.getStartDate());
                })
                .map((ClassEntity classEntity) -> {
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

    private int compareClassStatus(ClassStatus status1, ClassStatus status2) {
        int priority1 = getStatusPriority(status1);
        int priority2 = getStatusPriority(status2);
        return Integer.compare(priority1, priority2);
    }

    private int getStatusPriority(ClassStatus status) {
        if (status == ClassStatus.ONGOING) return 1;
        if (status == ClassStatus.SCHEDULED) return 2;
        if (status == ClassStatus.COMPLETED) return 3;
        return 4;
    }
}

