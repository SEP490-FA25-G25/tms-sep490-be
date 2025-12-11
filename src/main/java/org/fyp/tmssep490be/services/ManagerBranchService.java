package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.branch.CreateBranchRequest;
import org.fyp.tmssep490be.dtos.branch.ManagerBranchOverviewDTO;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.Center;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.UserBranches;
import org.fyp.tmssep490be.entities.enums.BranchStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.repositories.CenterRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerBranchService {

    private final BranchRepository branchRepository;
    private final CenterRepository centerRepository;
    private final NotificationService notificationService;
    private final UserAccountRepository userAccountRepository;

    // Lấy danh sách tất cả chi nhánh với thông tin tổng quan
    @Transactional(readOnly = true)
    public List<ManagerBranchOverviewDTO> getAllBranches() {
        log.info("Manager đang lấy danh sách tất cả chi nhánh");

        List<Branch> branches = branchRepository.findAll();
        return branches.stream()
                .map(this::mapToOverviewDTO)
                .collect(Collectors.toList());
    }

    // Lấy chi tiết một chi nhánh theo ID
    @Transactional(readOnly = true)
    public ManagerBranchOverviewDTO getBranchById(Long id) {
        log.info("Manager đang lấy chi tiết chi nhánh ID: {}", id);
        
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chi nhánh không tồn tại với ID: " + id));
        
        return mapToOverviewDTO(branch);
    }

    // Tạo chi nhánh mới
    @Transactional
    public ManagerBranchOverviewDTO createBranch(CreateBranchRequest request) {
        log.info("Manager đang tạo chi nhánh mới: {}", request.getName());

        // Lấy Center mặc định (center đầu tiên trong DB)
        Center center = centerRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Hệ thống chưa có Center nào"));

        // Create branch
        Branch branch = Branch.builder()
                .center(center)
                .code(request.getCode())
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .district(request.getDistrict())
                .phone(request.getPhone())
                .email(request.getEmail())
                .openingDate(request.getOpeningDate())
                .status(BranchStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Branch savedBranch = branchRepository.save(branch);
        log.info("Đã tạo chi nhánh mới với ID: {}", savedBranch.getId());

        return mapToOverviewDTO(savedBranch);
    }

    // Gửi thông báo cho Admin khi có chi nhánh mới (gọi từ Controller)
    public void sendNewBranchNotificationToAdmins(String branchName, String branchCode) {
        try {
            // Thử tìm với 'ADMIN' trước, nếu không có thì thử 'admin'
            List<UserAccount> admins = userAccountRepository.findUsersByRole("ADMIN");
            
            if (admins.isEmpty()) {
                admins = userAccountRepository.findUsersByRole("admin");
            }
            
            if (admins.isEmpty()) {
                return;
            }

            String title = "Chi nhánh mới cần thiết lập";
            String message = String.format(
                "Chi nhánh '%s' (Mã: %s) vừa được tạo. Vui lòng thêm tài khoản nhân viên cho chi nhánh này.",
                branchName,
                branchCode
            );

            for (UserAccount admin : admins) {
                notificationService.createNotification(
                    admin.getId(),
                    NotificationType.SYSTEM,
                    title,
                    message
                );
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo chi nhánh mới: {}", e.getMessage());
        }
    }

    // Check xem email đã tồn tại trong hệ thống chưa
    @Transactional(readOnly = true)
    public boolean checkEmailExists(String email, Long excludeBranchId) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        if (excludeBranchId != null) {
            return branchRepository.existsByEmailAndIdNot(email.trim(), excludeBranchId);
        }
        return branchRepository.existsByEmail(email.trim());
    }

    // Cập nhật thông tin chi nhánh
    @Transactional
    public ManagerBranchOverviewDTO updateBranch(Long id, org.fyp.tmssep490be.dtos.branch.UpdateBranchRequest request) {
        log.info("Manager đang cập nhật chi nhánh ID: {}", id);

        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chi nhánh không tồn tại với ID: " + id));

        // Update fields
        branch.setCode(request.getCode());
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setCity(request.getCity());
        branch.setDistrict(request.getDistrict());
        branch.setPhone(request.getPhone());
        branch.setEmail(request.getEmail());
        branch.setOpeningDate(request.getOpeningDate());

        // Update status if provided
        if (request.getStatus() != null) {
            try {
                branch.setStatus(BranchStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                log.warn("Trạng thái không hợp lệ: {}", request.getStatus());
            }
        }

        branch.setUpdatedAt(OffsetDateTime.now());

        Branch savedBranch = branchRepository.save(branch);
        log.info("Đã cập nhật chi nhánh ID: {}", savedBranch.getId());

        return mapToOverviewDTO(savedBranch);
    }

    // Ngưng hoạt động chi nhánh (soft delete)
    @Transactional
    public ManagerBranchOverviewDTO deactivateBranch(Long id) {
        log.info("Manager đang ngưng hoạt động chi nhánh ID: {}", id);

        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chi nhánh không tồn tại với ID: " + id));

        if (branch.getStatus() == BranchStatus.INACTIVE) {
            log.warn("Chi nhánh ID: {} đã ở trạng thái không hoạt động", id);
            return mapToOverviewDTO(branch);
        }

        // Kiểm tra xem có lớp học đang hoạt động không
        if (branch.getClasses() != null) {
            long activeClassCount = branch.getClasses().stream()
                    .filter(c -> c.getStatus() == org.fyp.tmssep490be.entities.enums.ClassStatus.ONGOING 
                              || c.getStatus() == org.fyp.tmssep490be.entities.enums.ClassStatus.SCHEDULED)
                    .count();
            
            if (activeClassCount > 0) {
                throw new IllegalStateException("Không thể ngưng hoạt động chi nhánh vì vẫn còn " 
                        + activeClassCount + " lớp học đang hoạt động hoặc đã lên lịch");
            }
        }

        branch.setStatus(BranchStatus.INACTIVE);
        branch.setUpdatedAt(OffsetDateTime.now());

        Branch savedBranch = branchRepository.save(branch);
        log.info("Đã ngưng hoạt động chi nhánh ID: {}", savedBranch.getId());

        return mapToOverviewDTO(savedBranch);
    }

    // Kích hoạt lại chi nhánh
    @Transactional
    public ManagerBranchOverviewDTO activateBranch(Long id) {
        log.info("Manager đang kích hoạt lại chi nhánh ID: {}", id);

        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chi nhánh không tồn tại với ID: " + id));

        if (branch.getStatus() == BranchStatus.ACTIVE) {
            log.warn("Chi nhánh ID: {} đã ở trạng thái hoạt động", id);
        }

        branch.setStatus(BranchStatus.ACTIVE);
        branch.setUpdatedAt(OffsetDateTime.now());

        Branch savedBranch = branchRepository.save(branch);
        log.info("Đã kích hoạt lại chi nhánh ID: {}", savedBranch.getId());

        return mapToOverviewDTO(savedBranch);
    }

    // Map Branch entity sang DTO
    private ManagerBranchOverviewDTO mapToOverviewDTO(Branch branch) {
        // Get Center Head từ userBranches với role CENTER_HEAD
        ManagerBranchOverviewDTO.CenterHeadInfo centerHeadInfo = null;

        if (branch.getUserBranches() != null) {
            for (UserBranches ub : branch.getUserBranches()) {
                UserAccount user = ub.getUserAccount();
                if (user != null && user.getUserRoles() != null) {
                    boolean isCenterHead = user.getUserRoles().stream()
                            .anyMatch(ur -> "CENTER_HEAD".equals(ur.getRole().getCode()));

                    if (isCenterHead) {
                        centerHeadInfo = ManagerBranchOverviewDTO.CenterHeadInfo.builder()
                                .userId(user.getId())
                                .fullName(user.getFullName())
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .avatarUrl(user.getAvatarUrl())
                                .build();
                        break;
                    }
                }
            }
        }

        // Count classes
        int totalClasses = branch.getClasses() != null ? branch.getClasses().size() : 0;
        ManagerBranchOverviewDTO.StatusCount classStatus = ManagerBranchOverviewDTO.StatusCount.builder()
                .total(totalClasses)
                .active(totalClasses)
                .inactive(0)
                .build();

        // Count teachers từ userBranches
        long totalTeachers = 0;
        if (branch.getUserBranches() != null) {
            totalTeachers = branch.getUserBranches().stream()
                    .filter(ub -> ub.getUserAccount() != null && ub.getUserAccount().getUserRoles() != null)
                    .filter(ub -> ub.getUserAccount().getUserRoles().stream()
                            .anyMatch(ur -> "TEACHER".equals(ur.getRole().getCode())))
                    .count();
        }
        ManagerBranchOverviewDTO.StatusCount teacherStatus = ManagerBranchOverviewDTO.StatusCount.builder()
                .total((int) totalTeachers)
                .active((int) totalTeachers)
                .inactive(0)
                .build();

        // Count resources từ userBranches
        int totalResources = branch.getResources() != null ? branch.getResources().size() : 0;
        ManagerBranchOverviewDTO.StatusCount resourceStatus = ManagerBranchOverviewDTO.StatusCount.builder()
                .total(totalResources)
                .active(totalResources)
                .inactive(0)
                .build();

        return ManagerBranchOverviewDTO.builder()
                .id(branch.getId())
                .centerId(branch.getCenter() != null ? branch.getCenter().getId() : null)
                .centerName(branch.getCenter() != null ? branch.getCenter().getName() : null)
                .code(branch.getCode())
                .name(branch.getName())
                .address(branch.getAddress())
                .city(branch.getCity())
                .district(branch.getDistrict())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .status(branch.getStatus() != null ? branch.getStatus().name() : null)
                .openingDate(branch.getOpeningDate())
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .centerHead(centerHeadInfo)
                .classStatus(classStatus)
                .teacherStatus(teacherStatus)
                .resourceStatus(resourceStatus)
                .build();
    }
}
