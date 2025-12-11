package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.academicteacher.*;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.TeacherSkill;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.UserBranches;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.repositories.TeacherSkillRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

// Service cho Academic Affairs để quản lý giáo viên và skills của họ
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AcademicTeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherSkillRepository teacherSkillRepository;
    private final UserBranchesRepository userBranchesRepository;

    // Lấy danh sách giáo viên trong các branch mà user có quyền truy cập
    public List<AcademicTeacherListItemDTO> getTeachers(String search, Boolean hasSkills, Long branchId, Long userId) {
        log.info("Getting teachers for user {} with search='{}', hasSkills={}, branchId={}", userId, search, hasSkills, branchId);

        // Lấy các branch mà user có quyền truy cập
        List<Long> accessibleBranchIds = getUserAccessibleBranches(userId);
        if (accessibleBranchIds.isEmpty()) {
            log.warn("User {} has no accessible branches", userId);
            return Collections.emptyList();
        }

        // Nếu có branchId được chỉ định, kiểm tra user có quyền truy cập branch đó không
        List<Long> targetBranchIds;
        if (branchId != null) {
            if (!accessibleBranchIds.contains(branchId)) {
                log.warn("User {} does not have access to branch {}", userId, branchId);
                return Collections.emptyList();
            }
            targetBranchIds = Collections.singletonList(branchId);
        } else {
            // Nếu không có branchId, lấy tất cả branches mà user có quyền
            targetBranchIds = accessibleBranchIds;
        }

        // Lấy giáo viên trong các branch đã lọc
        List<Teacher> teachers = teacherRepository.findByBranchIds(targetBranchIds);
        log.debug("Found {} teachers in branches {}", teachers.size(), accessibleBranchIds);

        // Lấy thông tin skill cho tất cả giáo viên
        List<Long> teacherIds = teachers.stream().map(Teacher::getId).collect(Collectors.toList());
        Map<Long, List<String>> teacherSpecializations = getTeacherSpecializationsMap(teacherIds);
        Map<Long, Integer> teacherSkillCounts = getTeacherSkillCountsMap(teacherIds);

        // Lọc và chuyển đổi
        return teachers.stream()
                .filter(t -> filterBySearch(t, search))
                .filter(t -> filterBySkillStatus(t.getId(), hasSkills, teacherSkillCounts))
                .map(t -> convertToListItemDTO(t, teacherSpecializations, teacherSkillCounts))
                .sorted(Comparator.comparing(AcademicTeacherListItemDTO::getFullName, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
    }

    // Lấy thông tin chi tiết giáo viên bao gồm tất cả skills
    public AcademicTeacherDetailDTO getTeacherDetail(Long teacherId, Long userId) {
        log.info("Getting teacher detail for teacherId={} by user {}", teacherId, userId);

        Teacher teacher = getTeacherWithAccessCheck(teacherId, userId);
        List<TeacherSkill> skills = teacherSkillRepository.findByTeacherId(teacherId);

        return convertToDetailDTO(teacher, skills);
    }

    // Lấy danh sách skills của giáo viên
    public List<TeacherSkillDTO> getTeacherSkills(Long teacherId, Long userId) {
        log.info("Getting skills for teacherId={} by user {}", teacherId, userId);

        getTeacherWithAccessCheck(teacherId, userId); // Kiểm tra quyền truy cập
        List<TeacherSkill> skills = teacherSkillRepository.findByTeacherId(teacherId);

        return skills.stream()
                .map(this::convertToSkillDTO)
                .sorted(Comparator.comparing(TeacherSkillDTO::getSpecialization, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(s -> s.getSkill().name()))
                .collect(Collectors.toList());
    }

    // Cập nhật skills của giáo viên (thay thế toàn bộ)
    @Transactional
    public List<TeacherSkillDTO> updateTeacherSkills(Long teacherId, UpdateTeacherSkillsRequest request, Long userId) {
        log.info("Updating skills for teacherId={} by user {}, {} skills provided", 
                teacherId, userId, request.getSkills().size());

        Teacher teacher = getTeacherWithAccessCheck(teacherId, userId);

        // Xóa tất cả skills hiện tại
        List<TeacherSkill> existingSkills = teacherSkillRepository.findByTeacherId(teacherId);
        teacherSkillRepository.deleteAll(existingSkills);
        log.debug("Deleted {} existing skills for teacher {}", existingSkills.size(), teacherId);

        // Tạo skills mới
        List<TeacherSkill> newSkills = new ArrayList<>();
        for (TeacherSkillDTO dto : request.getSkills()) {
            TeacherSkill skill = createTeacherSkill(teacher, dto);
            newSkills.add(skill);
        }

        teacherSkillRepository.saveAll(newSkills);
        log.info("Created {} new skills for teacher {}", newSkills.size(), teacherId);

        return newSkills.stream()
                .map(this::convertToSkillDTO)
                .sorted(Comparator.comparing(TeacherSkillDTO::getSpecialization, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(s -> s.getSkill().name()))
                .collect(Collectors.toList());
    }

    // Xóa tất cả skills theo specialization cụ thể
    public void deleteTeacherSkillsBySpecialization(Long teacherId, String specialization, Long userId) {
        log.info("Deleting skills for teacherId={}, specialization='{}' by user {}", 
                teacherId, specialization, userId);

        getTeacherWithAccessCheck(teacherId, userId); // Kiểm tra quyền truy cập

        List<TeacherSkill> skillsToDelete = teacherSkillRepository.findByTeacherId(teacherId).stream()
                .filter(s -> specialization.equalsIgnoreCase(s.getSpecialization()))
                .collect(Collectors.toList());

        if (skillsToDelete.isEmpty()) {
            log.warn("No skills found with specialization '{}' for teacher {}", specialization, teacherId);
            return;
        }

        teacherSkillRepository.deleteAll(skillsToDelete);
        log.info("Deleted {} skills with specialization '{}' for teacher {}", 
                skillsToDelete.size(), specialization, teacherId);
    }

    private List<Long> getUserAccessibleBranches(Long userId) {
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    private Teacher getTeacherWithAccessCheck(Long teacherId, Long userId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        // Kiểm tra user có quyền truy cập branch của giáo viên không
        List<Long> userBranches = getUserAccessibleBranches(userId);
        Set<Long> teacherBranches = teacher.getUserAccount().getUserBranches().stream()
                .map(ub -> ub.getBranch().getId())
                .collect(Collectors.toSet());

        boolean hasAccess = teacherBranches.stream().anyMatch(userBranches::contains);
        if (!hasAccess) {
            log.warn("User {} denied access to teacher {} - no common branches", userId, teacherId);
            throw new CustomException(ErrorCode.TEACHER_ACCESS_DENIED);
        }

        return teacher;
    }

    private boolean filterBySearch(Teacher teacher, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String searchLower = search.toLowerCase().trim();
        UserAccount user = teacher.getUserAccount();
        
        return (user.getFullName() != null && user.getFullName().toLowerCase().contains(searchLower))
                || (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchLower))
                || (teacher.getEmployeeCode() != null && teacher.getEmployeeCode().toLowerCase().contains(searchLower))
                || (user.getPhone() != null && user.getPhone().contains(searchLower));
    }

    private boolean filterBySkillStatus(Long teacherId, Boolean hasSkills, Map<Long, Integer> skillCounts) {
        if (hasSkills == null) {
            return true;
        }
        int count = skillCounts.getOrDefault(teacherId, 0);
        return hasSkills ? count > 0 : count == 0;
    }

    private Map<Long, List<String>> getTeacherSpecializationsMap(List<Long> teacherIds) {
        if (teacherIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> results = teacherSkillRepository.findSpecializationsByTeacherIds(teacherIds);
        Map<Long, List<String>> map = new HashMap<>();
        for (Object[] row : results) {
            Long teacherId = (Long) row[0];
            String specialization = (String) row[1];
            map.computeIfAbsent(teacherId, k -> new ArrayList<>());
            if (specialization != null && !map.get(teacherId).contains(specialization)) {
                map.get(teacherId).add(specialization);
            }
        }
        return map;
    }

    private Map<Long, Integer> getTeacherSkillCountsMap(List<Long> teacherIds) {
        if (teacherIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> map = new HashMap<>();
        for (Long teacherId : teacherIds) {
            List<TeacherSkill> skills = teacherSkillRepository.findByTeacherId(teacherId);
            map.put(teacherId, skills.size());
        }
        return map;
    }

    private AcademicTeacherListItemDTO convertToListItemDTO(
            Teacher teacher, 
            Map<Long, List<String>> specializations, 
            Map<Long, Integer> skillCounts) {
        
        UserAccount user = teacher.getUserAccount();
        List<String> specs = specializations.getOrDefault(teacher.getId(), Collections.emptyList());
        int skillCount = skillCounts.getOrDefault(teacher.getId(), 0);

        return AcademicTeacherListItemDTO.builder()
                .teacherId(teacher.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .employeeCode(teacher.getEmployeeCode())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus() != null ? user.getStatus().name() : "UNKNOWN")
                .hasSkills(skillCount > 0)
                .totalSkills(skillCount)
                .specializations(specs)
                .build();
    }

    private AcademicTeacherDetailDTO convertToDetailDTO(Teacher teacher, List<TeacherSkill> skills) {
        UserAccount user = teacher.getUserAccount();
        
        // Lấy thông tin branch
        Long branchId = null;
        String branchName = null;
        if (!user.getUserBranches().isEmpty()) {
            UserBranches ub = user.getUserBranches().iterator().next();
            branchId = ub.getBranch().getId();
            branchName = ub.getBranch().getName();
        }

        List<TeacherSkillDTO> skillDTOs = skills.stream()
                .map(this::convertToSkillDTO)
                .sorted(Comparator.comparing(TeacherSkillDTO::getSpecialization, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(s -> s.getSkill().name()))
                .collect(Collectors.toList());

        return AcademicTeacherDetailDTO.builder()
                .teacherId(teacher.getId())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .employeeCode(teacher.getEmployeeCode())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus() != null ? user.getStatus().name() : "UNKNOWN")
                .address(user.getAddress())
                .facebookUrl(user.getFacebookUrl())
                .dob(user.getDob())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .hireDate(teacher.getHireDate())
                .contractType(teacher.getContractType())
                .note(teacher.getNote())
                .branchId(branchId)
                .branchName(branchName)
                .skills(skillDTOs)
                .build();
    }

    private TeacherSkillDTO convertToSkillDTO(TeacherSkill skill) {
        return TeacherSkillDTO.builder()
                .skill(skill.getId().getSkill())
                .specialization(skill.getSpecialization())
                .language(skill.getLanguage())
                .level(skill.getLevel())
                .build();
    }

    private TeacherSkill createTeacherSkill(Teacher teacher, TeacherSkillDTO dto) {
        TeacherSkill.TeacherSkillId id = new TeacherSkill.TeacherSkillId();
        id.setTeacherId(teacher.getId());
        id.setSkill(dto.getSkill());

        TeacherSkill skill = new TeacherSkill();
        skill.setId(id);
        skill.setTeacher(teacher);
        skill.setSpecialization(dto.getSpecialization());
        skill.setLanguage(dto.getLanguage());
        skill.setLevel(dto.getLevel());

        return skill;
    }
}
