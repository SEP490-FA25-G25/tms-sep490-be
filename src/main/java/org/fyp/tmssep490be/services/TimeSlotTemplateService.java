package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.resource.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
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
public class TimeSlotTemplateService {

    private final TimeSlotTemplateRepository timeSlotTemplateRepository;
    private final BranchRepository branchRepository;
    private final UserAccountRepository userAccountRepository;
    private final SessionRepository sessionRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;

    @Transactional(readOnly = true)
    public List<TimeSlotResponseDTO> getAllTimeSlots(Long branchId, String search, Long userId, boolean isCenterHead, boolean isTeacher) {
        log.info("Getting all time slots - branchId: {}, search: {}, userId: {}", branchId, search, userId);

        List<TimeSlotTemplate> timeSlots = timeSlotTemplateRepository.findAll();

        if (isCenterHead) {
            Long userBranchId = getBranchIdForUser(userId);
            if (userBranchId != null) {
                branchId = userBranchId;
            }
        }

        if (isTeacher) {
            List<Long> userBranchIds = getBranchIdsForUser(userId);
            if (!userBranchIds.isEmpty()) {
                timeSlots = timeSlots.stream()
                        .filter(ts -> userBranchIds.contains(ts.getBranch().getId()))
                        .collect(Collectors.toList());
            }
        }

        if (branchId != null) {
            Long finalBranchId = branchId;
            timeSlots = timeSlots.stream()
                    .filter(ts -> ts.getBranch().getId().equals(finalBranchId))
                    .collect(Collectors.toList());
        }
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            timeSlots = timeSlots.stream()
                    .filter(ts -> ts.getName().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }

        return timeSlots.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TimeSlotResponseDTO getTimeSlotById(Long id) {
        log.info("Getting time slot by id: {}", id);
        TimeSlotTemplate timeSlot = timeSlotTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));
        return convertToDTO(timeSlot);
    }

    // Tạo khung giờ mới
    @Transactional
    public TimeSlotResponseDTO createTimeSlot(TimeSlotRequestDTO request, Long userId, Long forcedBranchId) {
        log.info("Creating time slot: {}", request);

        // 1. Xác định branchId
        Long branchId = forcedBranchId != null ? forcedBranchId : request.getBranchId();
        if (branchId == null) {
            throw new BusinessRuleException("Vui lòng chọn chi nhánh");
        }

        // 2. Lấy branch entity
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

        // 3. Validate tên
        String name = request.getName() != null ? request.getName().trim() : null;
        if (name == null || name.isEmpty()) {
            throw new BusinessRuleException("Vui lòng nhập tên khung giờ");
        }

        // 4. Validate thời gian
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessRuleException("Vui lòng nhập giờ bắt đầu và giờ kết thúc");
        }
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalTime endTime = LocalTime.parse(request.getEndTime());

        // 5. Check endTime > startTime
        if (!endTime.isAfter(startTime)) {
            throw new BusinessRuleException("Giờ kết thúc phải lớn hơn giờ bắt đầu");
        }

        // 6. Check tên không trùng
        if (timeSlotTemplateRepository.existsByBranchIdAndNameIgnoreCase(branchId, name, null)) {
            throw new BusinessRuleException("Tên khung giờ đã tồn tại trong chi nhánh này");
        }

        // 7. Check thời gian không trùng
        if (timeSlotTemplateRepository.existsByBranchIdAndStartTimeAndEndTime(branchId, startTime, endTime, null)) {
            throw new BusinessRuleException("Khung giờ này đã tồn tại trong chi nhánh");
        }

        // 8. Tạo entity và lưu
        TimeSlotTemplate timeSlot = new TimeSlotTemplate();
        timeSlot.setBranch(branch);
        timeSlot.setName(name);
        timeSlot.setStartTime(startTime);
        timeSlot.setEndTime(endTime);
        timeSlot.setStatus(ResourceStatus.ACTIVE);
        timeSlot.setCreatedAt(OffsetDateTime.now());
        timeSlot.setUpdatedAt(OffsetDateTime.now());

        TimeSlotTemplate saved = timeSlotTemplateRepository.save(timeSlot);
        log.info("Created time slot with ID: {}", saved.getId());
        return convertToDTO(saved);
    }

    // ==================== HELPER METHODS ====================

    private Long getBranchIdForUser(Long userId) {
        if (userId == null) return null;
        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        if (user != null && !user.getUserBranches().isEmpty()) {
            return user.getUserBranches().iterator().next().getBranch().getId();
        }
        return null;
    }

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

    private TimeSlotResponseDTO convertToDTO(TimeSlotTemplate ts) {
        TimeSlotResponseDTO.TimeSlotResponseDTOBuilder builder = TimeSlotResponseDTO.builder()
                .id(ts.getId())
                .branchId(ts.getBranch().getId())
                .branchName(ts.getBranch().getName())
                .name(ts.getName())
                .startTime(ts.getStartTime().toString())
                .endTime(ts.getEndTime().toString())
                .createdAt(ts.getCreatedAt() != null ? ts.getCreatedAt().toString() : null)
                .updatedAt(ts.getUpdatedAt() != null ? ts.getUpdatedAt().toString() : null)
                .status(ts.getStatus().name());

        try {
            Long activeClasses = sessionRepository.countDistinctClassesByTimeSlotId(ts.getId());
            Long totalSessions = sessionRepository.countSessionsByTimeSlotId(ts.getId());
            Long futureSessions = sessionRepository.countFutureSessionsByTimeSlotId(ts.getId(), LocalDate.now(), LocalTime.now());
            boolean hasTeacherAvailability = teacherAvailabilityRepository.existsById_TimeSlotTemplateId(ts.getId());

            builder.activeClassesCount(activeClasses)
                    .totalSessionsCount(totalSessions)
                    .hasAnySessions(totalSessions > 0)
                    .hasFutureSessions(futureSessions > 0)
                    .hasTeacherAvailability(hasTeacherAvailability);
        } catch (Exception e) {
            log.error("Error calculating statistics: {}", e.getMessage());
            builder.activeClassesCount(0L).totalSessionsCount(0L)
                    .hasAnySessions(false).hasFutureSessions(false).hasTeacherAvailability(false);
        }
        return builder.build();
    }
}