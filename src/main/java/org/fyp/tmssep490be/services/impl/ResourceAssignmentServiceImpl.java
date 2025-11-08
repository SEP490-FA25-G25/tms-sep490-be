package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesResponse;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.SessionResource;
import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.ResourceRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.SessionResourceRepository;
import org.fyp.tmssep490be.services.ResourceAssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of ResourceAssignmentService with HYBRID approach
 * <p>
 * Phase 1: SQL bulk insert for fast assignment (~90% success rate)
 * Phase 2: Java conflict analysis for detailed error reporting
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceAssignmentServiceImpl implements ResourceAssignmentService {

    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;
    private final SessionResourceRepository sessionResourceRepository;
    private final ResourceRepository resourceRepository;

    @Override
    @Transactional
    public AssignResourcesResponse assignResources(Long classId, AssignResourcesRequest request) {
        log.info("Starting HYBRID resource assignment for class ID: {}", classId);
        long startTime = System.currentTimeMillis();

        // 1. Validate class exists
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        log.debug("Class found: {} (Branch ID: {}, Max Capacity: {})",
                classEntity.getCode(), classEntity.getBranch().getId(), classEntity.getMaxCapacity());

        // 2. Validate all resources exist and belong to same branch
        Map<Long, Resource> resourceMap = validateResources(request, classEntity);

        // 3. Get total session count for response
        long totalSessions = sessionRepository.countByClassEntityId(classId);

        // 4. PHASE 1: SQL Bulk Insert (Fast Path - ~90% sessions)
        int totalSuccessCount = 0;
        for (AssignResourcesRequest.ResourceAssignment assignment : request.getPattern()) {
            int assignedCount = sessionResourceRepository.bulkInsertResourcesForDayOfWeek(
                    classId,
                    assignment.getDayOfWeek().intValue(),
                    assignment.getResourceId()
            );
            totalSuccessCount += assignedCount;

            log.debug("Phase 1 - Day {}: Assigned {} sessions to Resource ID: {}",
                    assignment.getDayOfWeek(), assignedCount, assignment.getResourceId());
        }

        log.info("Phase 1 complete: {}/{} sessions assigned successfully", totalSuccessCount, totalSessions);

        // 5. PHASE 2: Java Conflict Analysis (Detailed Path - ~10% conflicts)
        List<AssignResourcesResponse.ResourceConflictDetail> conflicts = new ArrayList<>();

        for (AssignResourcesRequest.ResourceAssignment assignment : request.getPattern()) {
            List<Object[]> unassignedSessions = sessionRepository.findUnassignedSessionsByDayOfWeek(
                    classId,
                    assignment.getDayOfWeek().intValue()
            );

            log.debug("Phase 2 - Day {}: Found {} unassigned sessions (potential conflicts)",
                    assignment.getDayOfWeek(), unassignedSessions.size());

            // Analyze each unassigned session for conflict reason
            for (Object[] sessionData : unassignedSessions) {
                Long sessionId = ((Number) sessionData[0]).longValue();
                LocalDate sessionDate = (LocalDate) sessionData[1];
                Long timeSlotId = sessionData[2] != null ? ((Number) sessionData[2]).longValue() : null;

                AssignResourcesResponse.ResourceConflictDetail conflict = analyzeSessionConflict(
                        sessionId,
                        sessionDate,
                        timeSlotId,
                        assignment.getDayOfWeek(),
                        assignment.getResourceId(),
                        resourceMap.get(assignment.getResourceId()),
                        classEntity.getMaxCapacity()
                );

                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }

        log.info("Phase 2 complete: {} conflicts detected", conflicts.size());

        // 6. Calculate processing time
        long processingTime = System.currentTimeMillis() - startTime;
        log.info("HYBRID resource assignment completed in {}ms (Target: <200ms)", processingTime);

        // 7. Build response
        return AssignResourcesResponse.builder()
                .classId(classId)
                .totalSessions(Math.toIntExact(totalSessions))
                .successCount(totalSuccessCount)
                .conflictCount(conflicts.size())
                .conflicts(conflicts)
                .processingTimeMs(processingTime)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resource> queryAvailableResources(Long classId, Long sessionId, ResourceType resourceType) {
        log.info("Querying available resources for class ID: {}, session ID: {}, type: {}",
                classId, sessionId, resourceType);

        // Get class and session details
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        Session session = sessionRepository.findSessionWithResourcesAndTimeSlot(sessionId);
        if (session == null) {
            throw new CustomException(ErrorCode.SESSION_NOT_FOUND);
        }

        TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();
        if (timeSlot == null) {
            throw new CustomException(ErrorCode.TIME_SLOT_NOT_ASSIGNED);
        }

        // Query available resources
        List<Resource> availableResources = resourceRepository.findAvailableResourcesForSession(
                classEntity.getBranch().getId(),
                resourceType,
                classEntity.getMaxCapacity(),
                session.getDate(),
                timeSlot.getId()
        );

        log.info("Found {} available resources", availableResources.size());
        return availableResources;
    }

    @Override
    @Transactional
    public boolean assignResourceToSession(Long sessionId, Long resourceId) {
        log.info("Manually assigning Resource ID: {} to Session ID: {}", resourceId, sessionId);

        // Validate session exists
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        // Validate resource exists
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // Check capacity
        if (resource.getCapacity() < session.getClassEntity().getMaxCapacity()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_RESOURCE_CAPACITY);
        }

        // Check conflict
        boolean hasConflict = sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(
                resourceId,
                session.getDate(),
                session.getTimeSlotTemplate().getId(),
                List.of(org.fyp.tmssep490be.entities.enums.SessionStatus.PLANNED), // Only PLANNED sessions are active
                sessionId // Exclude current session
        );

        if (hasConflict) {
            throw new CustomException(ErrorCode.RESOURCE_CONFLICT);
        }

        // Create session resource
        SessionResource sessionResource = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(sessionId, resourceId))
                .session(session)
                .resource(resource)
                .build();

        sessionResourceRepository.save(sessionResource);
        log.info("Successfully assigned Resource ID: {} to Session ID: {}", resourceId, sessionId);
        return true;
    }

    @Override
    @Transactional
    public boolean removeResourceFromSession(Long sessionId, Long resourceId) {
        log.info("Removing Resource ID: {} from Session ID: {}", resourceId, sessionId);

        SessionResource.SessionResourceId id = new SessionResource.SessionResourceId(sessionId, resourceId);
        if (sessionResourceRepository.existsById(id)) {
            sessionResourceRepository.deleteById(id);
            log.info("Successfully removed Resource ID: {} from Session ID: {}", resourceId, sessionId);
            return true;
        }

        log.warn("Session resource not found: Session ID: {}, Resource ID: {}", sessionId, resourceId);
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public long getResourceAssignmentCount(Long classId) {
        return sessionRepository.countSessionsWithResources(classId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFullyAssigned(Long classId) {
        long totalSessions = sessionRepository.countByClassEntityId(classId);
        long assignedSessions = sessionRepository.countSessionsWithResources(classId);
        return totalSessions > 0 && totalSessions == assignedSessions;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Validate all resources exist and belong to class branch
     */
    private Map<Long, Resource> validateResources(AssignResourcesRequest request, ClassEntity classEntity) {
        List<Long> resourceIds = request.getPattern().stream()
                .map(AssignResourcesRequest.ResourceAssignment::getResourceId)
                .distinct()
                .toList();

        Map<Long, Resource> resourceMap = resourceRepository.findAllById(resourceIds).stream()
                .collect(Collectors.toMap(Resource::getId, r -> r));

        if (resourceMap.size() != resourceIds.size()) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // Validate branch match
        for (Resource resource : resourceMap.values()) {
            if (!resource.getBranch().getId().equals(classEntity.getBranch().getId())) {
                log.error("Resource ID: {} belongs to different branch (Resource Branch: {}, Class Branch: {})",
                        resource.getId(), resource.getBranch().getId(), classEntity.getBranch().getId());
                throw new CustomException(ErrorCode.RESOURCE_BRANCH_MISMATCH);
            }
        }

        return resourceMap;
    }

    /**
     * Analyze why a session couldn't be assigned a resource (Phase 2)
     */
    private AssignResourcesResponse.ResourceConflictDetail analyzeSessionConflict(
            Long sessionId,
            LocalDate sessionDate,
            Long timeSlotId,
            Short dayOfWeek,
            Long requestedResourceId,
            Resource requestedResource,
            Integer classMaxCapacity
    ) {
        log.debug("Analyzing conflict for Session ID: {}", sessionId);

        // Check if time slot is assigned
        if (timeSlotId == null) {
            log.debug("Session ID: {} has no time slot assigned", sessionId);
            return null; // Not a conflict, just not ready for resource assignment
        }

        // Check capacity
        if (requestedResource.getCapacity() < classMaxCapacity) {
            log.debug("Session ID: {} - Insufficient capacity (Resource: {}, Required: {})",
                    sessionId, requestedResource.getCapacity(), classMaxCapacity);

            return buildConflictDetail(
                    sessionId, sessionDate, dayOfWeek, timeSlotId, requestedResource,
                    AssignResourcesResponse.ConflictType.INSUFFICIENT_CAPACITY,
                    String.format("Resource '%s' capacity (%d) is less than class max capacity (%d)",
                            requestedResource.getName(), requestedResource.getCapacity(), classMaxCapacity),
                    null, null
            );
        }

        // Check class booking conflict
        Object[] conflictDetails = sessionResourceRepository.findConflictingSessionDetails(sessionId, requestedResourceId);
        if (conflictDetails != null && conflictDetails.length >= 3) {
            Long conflictingSessionId = ((Number) conflictDetails[0]).longValue();
            Long conflictingClassId = ((Number) conflictDetails[1]).longValue();
            String conflictingClassName = (String) conflictDetails[2];
            LocalTime timeStart = conflictDetails.length > 4 ? (LocalTime) conflictDetails[4] : null;
            LocalTime timeEnd = conflictDetails.length > 5 ? (LocalTime) conflictDetails[5] : null;

            log.debug("Session ID: {} - Class booking conflict (Conflicting Class: {})", sessionId, conflictingClassName);

            String conflictReason = String.format(
                    "Resource '%s' is already booked by class '%s' on %s at %s-%s",
                    requestedResource.getName(), conflictingClassName, sessionDate,
                    timeStart != null ? timeStart.toString() : "N/A",
                    timeEnd != null ? timeEnd.toString() : "N/A"
            );

            return buildConflictDetail(
                    sessionId, sessionDate, dayOfWeek, timeSlotId, requestedResource,
                    AssignResourcesResponse.ConflictType.CLASS_BOOKING,
                    conflictReason,
                    conflictingClassId, conflictingClassName
            );
        }

        // Unknown conflict reason
        log.warn("Session ID: {} - Unknown conflict reason", sessionId);
        return buildConflictDetail(
                sessionId, sessionDate, dayOfWeek, timeSlotId, requestedResource,
                AssignResourcesResponse.ConflictType.UNAVAILABLE,
                "Resource is unavailable for unknown reason",
                null, null
        );
    }

    /**
     * Build conflict detail object
     */
    private AssignResourcesResponse.ResourceConflictDetail buildConflictDetail(
            Long sessionId,
            LocalDate sessionDate,
            Short dayOfWeek,
            Long timeSlotId,
            Resource requestedResource,
            AssignResourcesResponse.ConflictType conflictType,
            String conflictReason,
            Long conflictingClassId,
            String conflictingClassName
    ) {
        // Get time slot details (if available)
        LocalTime timeStart = null;
        LocalTime timeEnd = null;

        // In real implementation, you might want to fetch TimeSlotTemplate details
        // For now, we'll leave them null or fetch if needed

        return AssignResourcesResponse.ResourceConflictDetail.builder()
                .sessionId(sessionId)
                .sessionNumber(null) // Could be calculated if needed
                .date(sessionDate)
                .dayOfWeek(dayOfWeek)
                .timeSlotStart(timeStart)
                .timeSlotEnd(timeEnd)
                .requestedResourceId(requestedResource.getId())
                .requestedResourceName(requestedResource.getName())
                .conflictType(conflictType)
                .conflictReason(conflictReason)
                .conflictingClassId(conflictingClassId)
                .conflictingClassName(conflictingClassName)
                .build();
    }
}
