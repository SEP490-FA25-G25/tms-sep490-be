package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.user.UserImportData;
import org.fyp.tmssep490be.dtos.user.UserImportPreview;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.Role;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.UserBranches;
import org.fyp.tmssep490be.entities.UserRole;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.repositories.RoleRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.fyp.tmssep490be.repositories.UserRoleRepository;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.Student;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserImportService {

    private final ExcelParserService excelParserService;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    public UserImportPreview previewUserImport(MultipartFile file) {
        List<UserImportData> users = excelParserService.parseUserImport(file);
        int validCount = 0;
        int errorCount = 0;

        for (UserImportData user : users) {
            validateUserImportData(user);
            if (user.isValid()) {
                validCount++;
            } else {
                errorCount++;
            }
        }

        return UserImportPreview.builder()
                .users(users)
                .totalCount(users.size())
                .validCount(validCount)
                .errorCount(errorCount)
                .build();
    }

    private void validateUserImportData(UserImportData user) {
        // Bỏ qua nếu đã có lỗi từ lúc parse
        if ("ERROR".equals(user.getStatus())) {
            user.setValid(false);
            return;
        }

        StringBuilder errorMsg = new StringBuilder();

        // 1. Kiểm tra Email
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            errorMsg.append("Email là bắt buộc. ");
        } else if (userAccountRepository.existsByEmail(user.getEmail())) {
            errorMsg.append("Email đã tồn tại. ");
        }

        // 2. Kiểm tra Vai trò
        if (user.getRole() == null || user.getRole().isEmpty()) {
            errorMsg.append("Vai trò là bắt buộc. ");
        } else {
            Optional<Role> role = roleRepository.findByCode(user.getRole().toUpperCase());
            if (role.isEmpty()) {
                errorMsg.append("Vai trò không hợp lệ: ").append(user.getRole()).append(". ");
            }
        }

        // 3. Kiểm tra Chi nhánh (bắt buộc cho các role không phải ADMIN/MANAGER)
        String roleCode = user.getRole() != null ? user.getRole().toUpperCase() : "";
        boolean isGlobalRole = "ADMIN".equals(roleCode) || "MANAGER".equals(roleCode);
        
        if (user.getBranchCode() == null || user.getBranchCode().trim().isEmpty()) {
            if (!isGlobalRole) {
                errorMsg.append("Chi nhánh là bắt buộc. ");
            }
        } else {
            String[] branchCodes = user.getBranchCode().split(",");
            for (String code : branchCodes) {
                String trimmedCode = code.trim();
                if (!trimmedCode.isEmpty()) {
                    Optional<Branch> branch = branchRepository.findByCode(trimmedCode);
                    if (branch.isEmpty()) {
                        errorMsg.append("Mã chi nhánh không tồn tại: ").append(trimmedCode).append(". ");
                    }
                }
            }
        }

        // 4. Kiểm tra Số điện thoại (nếu có)
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            String phone = user.getPhone().trim();
            // Validate format: SĐT Việt Nam 10 số, bắt đầu bằng 0(3|5|7|8|9)
            if (!phone.matches("^(0[35789])[0-9]{8}$")) {
                errorMsg.append("Số điện thoại không hợp lệ. ");
            } else if (userAccountRepository.existsByPhone(phone)) {
                errorMsg.append("Số điện thoại đã tồn tại trong hệ thống. ");
            }
        }

        // 5. Kiểm tra Họ và tên
        if (user.getFullName() == null || user.getFullName().isEmpty()) {
            errorMsg.append("Họ tên là bắt buộc. ");
        }

        if (errorMsg.length() > 0) {
            user.setStatus("ERROR");
            user.setErrorMessage(errorMsg.toString().trim());
            user.setValid(false);
        } else {
            user.setStatus("CREATE");
            user.setErrorMessage(null);
            user.setValid(true);
        }
    }

    @Transactional
    public int executeUserImport(List<UserImportData> users) {
        int count = 0;
        String defaultPasswordHash = passwordEncoder.encode("12345678");

        for (UserImportData data : users) {
            // Chỉ xử lý các bản ghi hợp lệ
            if (!data.isValid() || !"CREATE".equals(data.getStatus())) {
                continue;
            }

            try {
                // Kiểm tra lại email đề phòng thay đổi đồng thời
                if (userAccountRepository.existsByEmail(data.getEmail())) {
                    log.warn("Bỏ qua user {}, email {} đã tồn tại khi thực thi", data.getFullName(), data.getEmail());
                    continue;
                }

                // Tạo UserAccount
                UserAccount user = new UserAccount();
                user.setEmail(data.getEmail());
                user.setFullName(data.getFullName());
                user.setPhone(data.getPhone());
                user.setPasswordHash(defaultPasswordHash);
                user.setStatus(UserStatus.ACTIVE);
                user.setGender(Gender.OTHER); // Mặc định
                user.setDob(parseDobString(data.getDob())); // Parse từ Excel, null nếu trống
                
                user = userAccountRepository.save(user);

                // Gán Role
                Optional<Role> roleOpt = roleRepository.findByCode(data.getRole().toUpperCase());
                if (roleOpt.isPresent()) {
                    UserRole userRole = new UserRole();
                    userRole.setId(new UserRole.UserRoleId(user.getId(), roleOpt.get().getId()));
                    userRole.setUserAccount(user);
                    userRole.setRole(roleOpt.get());
                    userRoleRepository.save(userRole);

                    // Tạo Teacher/Student profile nếu cần
                    String roleCode = roleOpt.get().getCode();
                    if ("TEACHER".equals(roleCode)) {
                        createTeacherProfile(user);
                    } else if ("STUDENT".equals(roleCode)) {
                        createStudentProfile(user);
                    }
                }

                // Gán Chi nhánh
                List<String> assignedBranchNames = new ArrayList<>();
                String roleCode = data.getRole().toUpperCase();
                boolean isGlobalRole = "ADMIN".equals(roleCode) || "MANAGER".equals(roleCode);
                
                if (isGlobalRole) {
                    // ADMIN/MANAGER: tự động gán vào TẤT CẢ chi nhánh
                    List<Branch> allBranches = branchRepository.findAll();
                    for (Branch branch : allBranches) {
                        UserBranches userBranch = new UserBranches();
                        userBranch.setUserAccount(user);
                        userBranch.setBranch(branch);
                        userBranchesRepository.save(userBranch);
                        assignedBranchNames.add(branch.getName());
                    }
                    log.info("ADMIN/MANAGER {} được gán vào {} chi nhánh", user.getFullName(), allBranches.size());
                } else if (data.getBranchCode() != null && !data.getBranchCode().isEmpty()) {
                    // Các role khác: gán theo branch code trong file Excel
                    String[] branchCodes = data.getBranchCode().split(",");
                    for (String code : branchCodes) {
                        String trimmedCode = code.trim();
                        if (!trimmedCode.isEmpty()) {
                            Optional<Branch> branchOpt = branchRepository.findByCode(trimmedCode);
                            if (branchOpt.isPresent()) {
                                UserBranches userBranch = new UserBranches();
                                userBranch.setUserAccount(user);
                                userBranch.setBranch(branchOpt.get());
                                userBranchesRepository.save(userBranch);
                                assignedBranchNames.add(branchOpt.get().getName());
                            }
                        }
                    }
                }
                
                // Gửi Email chào mừng
                try {
                    String branchNameStr = String.join(", ", assignedBranchNames);
                    
                    emailService.sendNewUserCredentialsAsync(
                        user.getEmail(),
                        user.getFullName(),
                        user.getEmail(),
                        "12345678",
                        branchNameStr
                    );

                } catch (Exception ex) {
                    log.error("Lỗi gửi email chào mừng tới {}", user.getEmail(), ex);
                }

                count++;
            } catch (Exception e) {
                log.error("Lỗi tạo user {} khi import: {}", data.getEmail(), e.getMessage());
                // Tiếp tục import các user khác
            }
        }
        return count;
    }

    /**
     * Parse date string from Excel to LocalDate
     * Supports formats: dd/MM/yyyy, yyyy-MM-dd, MM/dd/yyyy
     * Returns null if string is null, empty, or invalid format
     */
    private LocalDate parseDobString(String dobString) {
        if (dobString == null || dobString.trim().isEmpty()) {
            return null;
        }

        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dobString.trim(), formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        log.warn("Không thể parse ngày sinh: {}", dobString);
        return null;
    }

    /**
     * Tạo Teacher profile khi import user với role TEACHER
     */
    private void createTeacherProfile(UserAccount user) {
        long count = teacherRepository.count() + 1;
        String employeeCode = String.format("TCH-%03d", count);
        
        Teacher teacher = Teacher.builder()
            .userAccount(user)
            .employeeCode(employeeCode)
            .hireDate(java.time.LocalDate.now())
            .contractType("full-time")
            .createdAt(java.time.OffsetDateTime.now())
            .updatedAt(java.time.OffsetDateTime.now())
            .build();
        
        teacherRepository.save(teacher);
        log.info("Đã tạo Teacher profile cho user {} với mã nhân viên {}", 
            user.getFullName(), employeeCode);
    }

    /**
     * Tạo Student profile khi import user với role STUDENT
     */
    private void createStudentProfile(UserAccount user) {
        long count = studentRepository.count() + 1;
        String studentCode = String.format("STD-%04d", count);
        
        Student student = Student.builder()
            .userAccount(user)
            .studentCode(studentCode)
            .build();
        
        studentRepository.save(student);
        log.info("Đã tạo Student profile cho user {} với mã học viên {}", 
            user.getFullName(), studentCode);
    }
}
