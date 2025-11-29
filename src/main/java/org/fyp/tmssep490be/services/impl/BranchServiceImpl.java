package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.branch.BranchDTO;
import org.fyp.tmssep490be.dtos.branch.BranchRequestDTO;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.Center;
import org.fyp.tmssep490be.entities.enums.BranchStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.repositories.CenterRepository;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.StudentRequestRepository;
import org.fyp.tmssep490be.repositories.TeacherRequestRepository;
import org.fyp.tmssep490be.services.BranchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final CenterRepository centerRepository;
    private final ClassRepository classRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final TeacherRequestRepository teacherRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BranchDTO> getAllBranches() {
        log.info("Fetching all active branches");
        List<Branch> branches = branchRepository.findAll();

        return branches.stream()
                .filter(branch -> branch.getStatus() == BranchStatus.ACTIVE)
                .map(this::mapToBranchDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchDTO> getBranchesByCenterId(Long centerId) {
        log.info("Fetching branches for center ID: {}", centerId);
        
        // Verify center exists
        centerRepository.findById(centerId)
                .orElseThrow(() -> new IllegalArgumentException("Center not found with id: " + centerId));
        
        List<Branch> branches = branchRepository.findByCenterId(centerId);
        
        return branches.stream()
                .map(this::mapToBranchDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BranchDTO getBranchById(Long id) {
        log.info("Fetching branch with id: {}", id);
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + id));
        return mapToBranchDTO(branch);
    }

    @Override
    @Transactional
    public BranchDTO createBranch(BranchRequestDTO request) {
        log.info("Creating branch with code: {}", request.getCode());

        Center center = centerRepository.findById(request.getCenterId())
                .orElseThrow(() -> new IllegalArgumentException("Center not found with id: " + request.getCenterId()));

        BranchStatus status = request.getStatus() != null 
                ? BranchStatus.valueOf(request.getStatus())
                : BranchStatus.ACTIVE;

        Branch branch = Branch.builder()
                .center(center)
                .code(request.getCode())
                .name(request.getName())
                .address(request.getAddress())
                .district(request.getDistrict())
                .city(request.getCity())
                .phone(request.getPhone())
                .email(request.getEmail())
                .status(status)
                .openingDate(request.getOpeningDate())
                .build();

        Branch savedBranch = branchRepository.save(branch);
        log.info("Branch created successfully with id: {}", savedBranch.getId());

        return mapToBranchDTO(savedBranch);
    }

    @Override
    @Transactional
    public BranchDTO updateBranch(Long id, BranchRequestDTO request) {
        log.info("Updating branch with id: {}", id);

        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + id));

        Center center = centerRepository.findById(request.getCenterId())
                .orElseThrow(() -> new IllegalArgumentException("Center not found with id: " + request.getCenterId()));

        branch.setCenter(center);
        branch.setCode(request.getCode());
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setDistrict(request.getDistrict());
        branch.setCity(request.getCity());
        branch.setPhone(request.getPhone());
        branch.setEmail(request.getEmail());

        if (request.getStatus() != null) {
            BranchStatus newStatus = BranchStatus.valueOf(request.getStatus());
            BranchStatus oldStatus = branch.getStatus();

            // Validate before deactivating a branch
            if (oldStatus == BranchStatus.ACTIVE && newStatus == BranchStatus.INACTIVE) {
                Long branchId = branch.getId();

                // 1) Không có lớp SCHEDULED / ONGOING tại chi nhánh
                long activeClasses = classRepository.countByBranchIdAndStatusIn(
                        branchId,
                        Arrays.asList(ClassStatus.SCHEDULED, ClassStatus.ONGOING)
                );
                if (activeClasses > 0) {
                    throw new IllegalArgumentException(
                            "Không thể tạm ngưng chi nhánh vì vẫn còn " + activeClasses + " lớp đang hoạt động"
                    );
                }

                // 2) Không có request PENDING liên quan tới lớp/chi nhánh (student + teacher)
                long pendingStudentRequests = studentRequestRepository.countByStatusAndBranches(
                        RequestStatus.PENDING,
                        Collections.singletonList(branchId)
                );

                long pendingTeacherRequests = teacherRequestRepository.countByStatusAndBranchId(
                        RequestStatus.PENDING,
                        branchId
                );

                long totalPendingRequests = pendingStudentRequests + pendingTeacherRequests;
                if (totalPendingRequests > 0) {
                    throw new IllegalArgumentException(
                            "Không thể tạm ngưng chi nhánh vì vẫn còn " + totalPendingRequests + " yêu cầu đang chờ xử lý"
                    );
                }
            }

            branch.setStatus(newStatus);
        }
        
        branch.setOpeningDate(request.getOpeningDate());

        Branch updatedBranch = branchRepository.save(branch);
        log.info("Branch updated successfully with id: {}", updatedBranch.getId());

        return mapToBranchDTO(updatedBranch);
    }

    @Override
    @Transactional
    public void deleteBranch(Long id) {
        log.info("Deleting branch with id: {}", id);
        
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + id));
        
        // Check if branch has related data
        if (!branch.getClasses().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete branch: Branch has associated classes");
        }
        
        if (!branch.getResources().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete branch: Branch has associated resources");
        }
        
        branchRepository.delete(branch);
        log.info("Branch deleted successfully with id: {}", id);
    }

    private BranchDTO mapToBranchDTO(Branch branch) {
        return BranchDTO.builder()
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
                .build();
    }
}
