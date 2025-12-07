package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.resource.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.fyp.tmssep490be.exceptions.*;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final BranchRepository branchRepository;
    private final UserAccountRepository userAccountRepository;
    private final SessionResourceRepository sessionResourceRepository;

    // Lấy danh sách resources với filter
    @Transactional(readOnly = true)
    public List<ResourceDTO> getAllResources(Long branchId, String resourceType, String search, Long userId) {
        log.info("Getting resources - branchId: {}, type: {}, search: {}, userId: {}",
                branchId, resourceType, search, userId);

        // 1. Lấy branches user có quyền
        List<Long> userBranches = getBranchIdsForUser(userId);
        if (userBranches.isEmpty()) {
            log.warn("User {} has no branch access", userId);
            return List.of();
        }

        // 2. Validate branchId
        if (branchId != null && !userBranches.contains(branchId)) {
            throw new BusinessRuleException("ACCESS_DENIED", "Không có quyền truy cập chi nhánh này");
        }

        // 3. Query
        List<Resource> resources;
        if (branchId != null) {
            resources = resourceRepository.findByBranchIdOrderByNameAsc(branchId);
        } else {
            resources = resourceRepository.findAll().stream()
                    .filter(r -> userBranches.contains(r.getBranch().getId()))
                    .collect(Collectors.toList());
        }

        // 4. Filter theo type
        if (resourceType != null && !resourceType.isEmpty()) {
            ResourceType type = ResourceType.valueOf(resourceType);
            resources = resources.stream()
                    .filter(r -> r.getResourceType() == type)
                    .collect(Collectors.toList());
        }

        // 5. Filter theo search (tên hoặc mã)
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            resources = resources.stream()
                    .filter(r -> r.getName().toLowerCase().contains(searchLower) ||
                            r.getCode().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }

        return resources.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // Lấy resource theo ID
    @Transactional(readOnly = true)
    public ResourceDTO getResourceById(Long id) {
        log.info("Getting resource by id: {}", id);
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
        return convertToDTO(resource);
    }

    // Tạo resource mới
    @Transactional
    public ResourceDTO createResource(ResourceRequestDTO request, Long userId) {
        log.info("Creating resource: {}", request);

        // 1. Validate request cơ bản
        validateCreateRequest(request);

        // 2. Lấy branchId và validate quyền
        Long branchId = request.getBranchId();
        List<Long> userBranches = getBranchIdsForUser(userId);
        if (!userBranches.contains(branchId)) {
            throw new BusinessRuleException("ACCESS_DENIED", "Không có quyền truy cập chi nhánh này");
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

        // 3. Tạo full code với prefix branch
        String code = request.getCode().trim();
        String branchCode = branch.getCode();
        String fullCode = code.startsWith(branchCode + "-") ? code : branchCode + "-" + code;

        // 4. Kiểm tra trùng code
        if (resourceRepository.existsByBranchIdAndCodeIgnoreCase(branchId, fullCode)) {
            throw new BusinessRuleException("Mã tài nguyên '" + fullCode + "' đã tồn tại trong chi nhánh này");
        }

        // 5. Kiểm tra trùng tên
        if (resourceRepository.existsByBranchIdAndNameIgnoreCase(branchId, request.getName().trim())) {
            throw new BusinessRuleException("Tên tài nguyên '" + request.getName() + "' đã tồn tại trong chi nhánh này");
        }

        // 6. Validate type-specific fields
        validateResourceTypeFields(request);

        // 7. Tạo entity
        Resource resource = new Resource();
        resource.setBranch(branch);
        resource.setCode(fullCode);
        resource.setStatus(ResourceStatus.ACTIVE);
        resource.setCreatedAt(OffsetDateTime.now());
        resource.setUpdatedAt(OffsetDateTime.now());

        updateResourceFromRequest(resource, request, userId);

        Resource saved = resourceRepository.save(resource);
        log.info("Created resource with ID: {}", saved.getId());
        return convertToDTO(saved);
    }

    // ==================== VALIDATION METHODS ====================

    private void validateCreateRequest(ResourceRequestDTO request) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new BusinessRuleException("Mã tài nguyên là bắt buộc");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BusinessRuleException("Tên tài nguyên là bắt buộc");
        }
        if (request.getResourceType() == null || request.getResourceType().trim().isEmpty()) {
            throw new BusinessRuleException("Loại tài nguyên là bắt buộc");
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()
                && request.getDescription().trim().length() < 10) {
            throw new BusinessRuleException("Mô tả phải có ít nhất 10 ký tự hoặc để trống");
        }
        validateCapacity(request, "VIRTUAL".equals(request.getResourceType()));
    }

    private void validateCapacity(ResourceRequestDTO request, boolean isVirtual) {
        if (request.getCapacity() != null) {
            int maxCapacity = isVirtual ? 100 : 40;
            if (request.getCapacity() <= 0) {
                throw new BusinessRuleException("Sức chứa phải là số dương lớn hơn 0");
            }
            if (request.getCapacity() > maxCapacity) {
                if (isVirtual) {
                    throw new BusinessRuleException("Sức chứa của phòng ảo (Zoom) tối đa là 100 người");
                } else {
                    throw new BusinessRuleException("Sức chứa của phòng học tối đa là 40 người");
                }
            }
        }
    }

    private void validateResourceTypeFields(ResourceRequestDTO request) {
        if ("VIRTUAL".equals(request.getResourceType())) {
            boolean hasMeetingUrl = request.getMeetingUrl() != null && !request.getMeetingUrl().trim().isEmpty();
            boolean hasAccountEmail = request.getAccountEmail() != null && !request.getAccountEmail().trim().isEmpty();

            if (!hasMeetingUrl && !hasAccountEmail) {
                throw new BusinessRuleException("Tài nguyên ảo cần có Meeting URL hoặc Account Email");
            }

            if (hasMeetingUrl && !request.getMeetingUrl().matches("^https?://.*")) {
                throw new BusinessRuleException("Meeting URL phải bắt đầu bằng http:// hoặc https://");
            }

            if (hasAccountEmail && !request.getAccountEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                throw new BusinessRuleException("Account Email không đúng định dạng email");
            }

            validateExpiryDate(request.getExpiryDate());
        }
    }

    private void validateExpiryDate(String expiryDateStr) {
        if (expiryDateStr != null && !expiryDateStr.trim().isEmpty()) {
            try {
                LocalDate expiryDate = LocalDate.parse(expiryDateStr);
                if (expiryDate.isBefore(LocalDate.now())) {
                    throw new BusinessRuleException("Ngày hết hạn phải là ngày trong tương lai");
                }
            } catch (DateTimeParseException e) {
                throw new BusinessRuleException("Ngày hết hạn không đúng định dạng (YYYY-MM-DD)");
            }
        }
    }

    private void updateResourceFromRequest(Resource resource, ResourceRequestDTO request, Long userId) {
        if (request.getResourceType() != null) {
            resource.setResourceType(ResourceType.valueOf(request.getResourceType()));
        }
        if (request.getName() != null) {
            resource.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription().trim());
        }
        if (request.getCapacity() != null) {
            resource.setCapacity(request.getCapacity());
        }
        if (request.getCapacityOverride() != null) {
            resource.setCapacityOverride(request.getCapacityOverride());
        }
        if (request.getEquipment() != null) {
            resource.setEquipment(request.getEquipment());
        }
        if (request.getMeetingUrl() != null) {
            resource.setMeetingUrl(request.getMeetingUrl());
        }
        if (request.getMeetingId() != null) {
            resource.setMeetingId(request.getMeetingId());
        }
        if (request.getMeetingPasscode() != null) {
            resource.setMeetingPasscode(request.getMeetingPasscode());
        }
        if (request.getAccountEmail() != null) {
            resource.setAccountEmail(request.getAccountEmail());
        }
        if (request.getAccountPassword() != null) {
            resource.setAccountPassword(request.getAccountPassword());
        }
        if (request.getLicenseType() != null) {
            resource.setLicenseType(request.getLicenseType());
        }
        if (request.getStartDate() != null) {
            resource.setStartDate(request.getStartDate().isEmpty() ? null : LocalDate.parse(request.getStartDate()));
        }
        if (request.getExpiryDate() != null) {
            resource.setExpiryDate(request.getExpiryDate().isEmpty() ? null : LocalDate.parse(request.getExpiryDate()));
        }
        if (request.getRenewalDate() != null) {
            resource.setRenewalDate(request.getRenewalDate().isEmpty() ? null : LocalDate.parse(request.getRenewalDate()));
        }

        if (resource.getCreatedBy() == null && userId != null) {
            UserAccount user = userAccountRepository.findById(userId).orElse(null);
            resource.setCreatedBy(user);
        }
    }

    // ==================== HELPER METHODS ====================

    private List<Long> getBranchIdsForUser(Long userId) {
        if (userId == null) return List.of();
        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        if (user != null && !user.getUserBranches().isEmpty()) {
            return user.getUserBranches().stream()
                    .map(ub -> ub.getBranch().getId())
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private ResourceDTO convertToDTO(Resource resource) {
        ResourceDTO.ResourceDTOBuilder builder = ResourceDTO.builder()
                .id(resource.getId())
                .branchId(resource.getBranch().getId())
                .branchName(resource.getBranch().getName())
                .resourceType(resource.getResourceType().toString())
                .code(resource.getCode())
                .name(resource.getName())
                .description(resource.getDescription())
                .capacity(resource.getCapacity())
                .capacityOverride(resource.getCapacityOverride())
                .equipment(resource.getEquipment())
                .meetingUrl(resource.getMeetingUrl())
                .meetingId(resource.getMeetingId())
                .meetingPasscode(resource.getMeetingPasscode())
                .accountEmail(resource.getAccountEmail())
                .licenseType(resource.getLicenseType())
                .startDate(resource.getStartDate() != null ? resource.getStartDate().toString() : null)
                .expiryDate(resource.getExpiryDate() != null ? resource.getExpiryDate().toString() : null)
                .renewalDate(resource.getRenewalDate() != null ? resource.getRenewalDate().toString() : null)
                .createdAt(resource.getCreatedAt() != null ? resource.getCreatedAt().toString() : null)
                .updatedAt(resource.getUpdatedAt() != null ? resource.getUpdatedAt().toString() : null)
                .status(resource.getStatus().name());

        // Thêm statistics
        try {
            Long activeClasses = sessionResourceRepository.countDistinctClassesByResourceId(resource.getId());
            Long totalSessions = sessionResourceRepository.countSessionsByResourceId(resource.getId());
            Session nextSession = sessionResourceRepository.findNextSessionByResourceId(
                    resource.getId(), LocalDate.now(), LocalTime.now());

            builder.activeClassesCount(activeClasses)
                    .totalSessionsCount(totalSessions)
                    .hasAnySessions(totalSessions > 0)
                    .hasFutureSessions(nextSession != null);

            if (nextSession != null) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                String nextSessionInfo = String.format("%s lúc %s | %s - %s",
                        nextSession.getDate().format(dateFormatter),
                        nextSession.getTimeSlotTemplate().getStartTime().format(timeFormatter),
                        nextSession.getClassEntity().getCode(),
                        nextSession.getClassEntity().getName());
                builder.nextSessionInfo(nextSessionInfo);
            }
        } catch (Exception e) {
            log.error("Error calculating statistics: {}", e.getMessage());
            builder.activeClassesCount(0L).totalSessionsCount(0L)
                    .hasAnySessions(false).hasFutureSessions(false);
        }

        return builder.build();
    }

    private SessionInfoDTO convertSessionToDTO(Session session) {
        return SessionInfoDTO.builder()
                .id(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .className(session.getClassEntity().getName())
                .date(session.getDate().toString())
                .startTime(session.getTimeSlotTemplate().getStartTime().toString())
                .endTime(session.getTimeSlotTemplate().getEndTime().toString())
                .status(session.getStatus().toString())
                .type(session.getType().toString())
                .build();
    }
}