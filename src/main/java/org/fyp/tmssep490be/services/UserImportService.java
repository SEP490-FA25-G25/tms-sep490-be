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

        // 3. Kiểm tra Chi nhánh (hỗ trợ nhiều mã cách nhau bằng dấu phẩy)
        if (user.getBranchCode() != null && !user.getBranchCode().isEmpty()) {
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

        // 4. Kiểm tra Họ và tên
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
                }

                // Gán Chi nhánh (xử lý nhiều chi nhánh)
                List<String> assignedBranchNames = new ArrayList<>();
                if (data.getBranchCode() != null && !data.getBranchCode().isEmpty()) {
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
}
