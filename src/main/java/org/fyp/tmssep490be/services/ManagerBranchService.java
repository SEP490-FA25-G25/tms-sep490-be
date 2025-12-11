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
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.repositories.CenterRepository;
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

    // Lấy danh sách tất cả chi nhánh với thông tin tổng quan
    @Transactional(readOnly = true)
    public List<ManagerBranchOverviewDTO> getAllBranches() {
        log.info("Manager đang lấy danh sách tất cả chi nhánh");

        List<Branch> branches = branchRepository.findAll();
        return branches.stream()
                .map(this::mapToOverviewDTO)
                .collect(Collectors.toList());
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
