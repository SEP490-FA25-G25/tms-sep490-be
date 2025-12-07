package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.resource.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.fyp.tmssep490be.exceptions.*;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
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