package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesResponse;
import org.fyp.tmssep490be.dtos.createclass.AvailableResourceDTO;
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
import java.util.Optional;
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

    /**
     * Query available resources for Step 4 by time slot and day of week.
     * Returns resources with conflict count for availability calculation.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Resource> queryAvailableResourcesByTimeSlotAndDay(Long classId, Long timeSlotId, Short dayOfWeek) {
        log.info("Querying available resources for class ID: {}, timeSlot ID: {}, dayOfWeek: {}",
                classId, timeSlotId, dayOfWeek);

        // Get class details
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Determine resource type based on modality
        ResourceType resourceType = classEntity.getModality() == org.fyp.tmssep490be.entities.enums.Modality.ONLINE
                ? ResourceType.VIRTUAL
                : ResourceType.ROOM;

        // Get sessions for this day of week to check conflicts
        List<Session> sessionsForDay = sessionRepository.findByClassIdAndDayOfWeek(classId, dayOfWeek.intValue());
        
        if (sessionsForDay.isEmpty()) {
            log.warn("No sessions found for class {} on day of week {}", classId, dayOfWeek);
            return List.of();
        }

        // Query resources that match criteria (branch, type, capacity)
        List<Resource> candidateResources = resourceRepository.findByBranchAndTypeAndCapacity(
                classEntity.getBranch().getId(),
                resourceType,
                classEntity.getMaxCapacity()
        );

        log.info("Found {} candidate resources for branch {} with type {} and capacity >= {}",
                candidateResources.size(), classEntity.getBranch().getId(), resourceType, classEntity.getMaxCapacity());

        return candidateResources;
    }

    /**
     * Query available resources with conflict counts for Step 4.
     * Calculates how many sessions each resource conflicts with.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Resource, Integer> queryAvailableResourcesWithConflicts(Long classId, Long timeSlotId, Short dayOfWeek) {
        log.info("Querying available resources with conflicts for class ID: {}, timeSlot ID: {}, dayOfWeek: {}",
                classId, timeSlotId, dayOfWeek);

        // Get class details
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Determine resource type based on modality
        ResourceType resourceType = classEntity.getModality() == org.fyp.tmssep490be.entities.enums.Modality.ONLINE
                ? ResourceType.VIRTUAL
                : ResourceType.ROOM;

        // Get sessions for this day of week
        List<Session> sessionsForDay = sessionRepository.findByClassIdAndDayOfWeek(classId, dayOfWeek.intValue());
        
        if (sessionsForDay.isEmpty()) {
            log.warn("No sessions found for class {} on day of week {}", classId, dayOfWeek);
            return Map.of();
        }

        // Extract dates and time slot IDs from sessions for conflict checking
        List<java.time.LocalDate> dates = sessionsForDay.stream()
                .map(Session::getDate)
                .distinct()
                .toList();

        List<Long> timeSlotIds = sessionsForDay.stream()
                .map(s -> s.getTimeSlotTemplate() != null ? s.getTimeSlotTemplate().getId() : null)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (timeSlotIds.isEmpty()) {
            log.warn("No time slots assigned for sessions on day {} in class {}", dayOfWeek, classId);
            return Map.of();
        }

        // Query resources that match criteria (branch, type, capacity)
        List<Resource> candidateResources = resourceRepository.findByBranchAndTypeAndCapacity(
                classEntity.getBranch().getId(),
                resourceType,
                classEntity.getMaxCapacity()
        );

        if (candidateResources.isEmpty()) {
            log.warn("No candidate resources found for branch {} with type {} and capacity >= {}",
                    classEntity.getBranch().getId(), resourceType, classEntity.getMaxCapacity());
            return Map.of();
        }

        // OPTIMIZED: Batch query for all resources at once (1 query instead of N queries)
        List<Long> resourceIds = candidateResources.stream()
                .map(Resource::getId)
                .toList();

        List<Object[]> conflictResults = sessionResourceRepository
                .batchCountConflictsByResourcesAcrossAllClasses(
                        resourceIds,
                        dates,
                        timeSlotIds,
                        classId  // Exclude current class from conflict check
                );

        // Convert results to map: resourceId -> conflictCount
        Map<Long, Integer> conflictMap = new java.util.HashMap<>();
        for (Object[] row : conflictResults) {
            Long resourceId = ((Number) row[0]).longValue();
            Integer conflictCount = ((Number) row[1]).intValue();
            conflictMap.put(resourceId, conflictCount);
        }

        // Build final map: Resource -> conflictCount (default 0 if no conflicts)
        Map<Resource, Integer> resourceConflicts = new java.util.HashMap<>();
        for (Resource resource : candidateResources) {
            Integer conflictCount = conflictMap.getOrDefault(resource.getId(), 0);
            resourceConflicts.put(resource, conflictCount);
        }

        log.info("Found {} candidate resources with conflict data for class {} (checked against all classes in 1 query)", 
                candidateResources.size(), classId);

        return resourceConflicts;
    }

    /**
     * Get total number of sessions for a specific day of week
     */
    @Override
    @Transactional(readOnly = true)
    public int getTotalSessionsForDayOfWeek(Long classId, Short dayOfWeek) {
        List<Session> sessions = sessionRepository.findByClassIdAndDayOfWeek(classId, dayOfWeek.intValue());
        return sessions.size();
    }

    /**
     * Assigns resources to class sessions using HYBRID approach.
     * <p>
     * <b>Phase 1 (SQL Bulk Insert):</b> Fast assignment for non-conflicting sessions (~90% success rate).
     * Uses native SQL INSERT to assign resources to sessions matching day of week and having time slots.
     * </p>
     * <p>
     * <b>Phase 2 (Java Conflict Analysis):</b> Detailed analysis of unassigned sessions.
     * Identifies conflict reasons: CLASS_BOOKING, INSUFFICIENT_CAPACITY, MAINTENANCE, UNAVAILABLE.
     * </p>
     *
     * @param classId ID of the class to assign resources to
     * @param request Assignment request containing resource-day patterns (e.g., Room A on Mon/Wed/Fri)
     * @return AssignResourcesResponse with success count, detailed conflicts, and processing time
     * @throws CustomException with CLASS_NOT_FOUND if class doesn't exist
     * @throws CustomException with RESOURCE_NOT_FOUND if any resource doesn't exist
     * @throws CustomException with RESOURCE_BRANCH_MISMATCH if resource belongs to different branch
     * @see AssignResourcesRequest
     * @see AssignResourcesResponse
     */
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
                // Convert java.sql.Date from native query to LocalDate
                LocalDate sessionDate = sessionData[1] instanceof java.sql.Date
                    ? ((java.sql.Date) sessionData[1]).toLocalDate()
                    : (LocalDate) sessionData[1];
                Long timeSlotId = sessionData[2] != null ? ((Number) sessionData[2]).longValue() : null;

                Optional<AssignResourcesResponse.ResourceConflictDetail> conflict = analyzeSessionConflict(
                        sessionId,
                        assignment.getDayOfWeek(),
                        assignment.getResourceId(),
                        resourceMap.get(assignment.getResourceId()),
                        classEntity
                );

                conflict.ifPresent(conflicts::add);
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

    /**
     * Queries available resources for a specific session (manual resolution).
     * <p>
     * Finds resources that:
     * <ul>
     *   <li>Belong to the same branch as the class</li>
     *   <li>Match the requested resource type</li>
     *   <li>Have sufficient capacity for the class</li>
     *   <li>Are NOT booked by another class at the same date/time slot</li>
     * </ul>
     * Used by Academic Staff to manually resolve resource conflicts.
     * </p>
     *
     * @param classId ID of the class needing resource
     * @param sessionId ID of the specific session to assign resource to
     * @param resourceType Type of resource (ROOM, LAB, AUDITORIUM, etc.)
     * @return List of available resources matching criteria (empty if none available)
     * @throws CustomException with CLASS_NOT_FOUND if class doesn't exist
     * @throws CustomException with SESSION_NOT_FOUND if session doesn't exist
     * @throws CustomException with TIME_SLOT_NOT_ASSIGNED if session has no time slot
     * @see Resource
     * @see ResourceType
     */
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

    /**
     * Manually assigns a specific resource to a specific session (conflict resolution).
     * <p>
     * <b>Validations performed:</b>
     * <ul>
     *   <li>Session exists</li>
     *   <li>Resource exists</li>
     *   <li>Resource capacity >= class max capacity</li>
     *   <li>No booking conflict at same date/time slot</li>
     * </ul>
     * Used by Academic Staff to resolve resource conflicts after bulk assignment.
     * </p>
     *
     * @param sessionId ID of the session to assign resource to
     * @param resourceId ID of the resource to assign
     * @return true if assignment successful
     * @throws CustomException with SESSION_NOT_FOUND if session doesn't exist
     * @throws CustomException with RESOURCE_NOT_FOUND if resource doesn't exist
     * @throws CustomException with INSUFFICIENT_RESOURCE_CAPACITY if capacity too small
     * @throws CustomException with RESOURCE_CONFLICT if resource already booked at same time
     * @see SessionResource
     */
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

    /**
     * Removes a resource assignment from a session (undo operation).
     * <p>
     * Used when Academic Staff needs to:
     * <ul>
     *   <li>Correct an incorrect assignment</li>
     *   <li>Free up a resource for another class</li>
     *   <li>Re-assign a different resource to the session</li>
     * </ul>
     * </p>
     *
     * @param sessionId ID of the session to remove resource from
     * @param resourceId ID of the resource to remove
     * @return true if removal successful, false if assignment didn't exist
     * @see SessionResource
     */
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

    /**
     * Gets the count of sessions that have resources assigned (progress tracking).
     * <p>
     * Used to calculate assignment progress: assignedCount / totalSessionCount * 100%
     * </p>
     *
     * @param classId ID of the class
     * @return Number of sessions with at least one resource assigned
     * @see #isFullyAssigned(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public long getResourceAssignmentCount(Long classId) {
        return sessionRepository.countSessionsWithResources(classId);
    }

    /**
     * Checks if all sessions in the class have resources assigned (completion check).
     * <p>
     * Used in class validation (STEP 6) to determine if class is ready for teacher assignment.
     * </p>
     *
     * @param classId ID of the class
     * @return true if all sessions have resources, false otherwise (or if no sessions exist)
     * @see #getResourceAssignmentCount(Long)
     */
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

    private List<AvailableResourceDTO> buildResourceSuggestions(ClassEntity classEntity, Session session) {
        if (session.getTimeSlotTemplate() == null) {
            return List.of();
        }

        ResourceType resourceType = classEntity.getModality() == org.fyp.tmssep490be.entities.enums.Modality.ONLINE
                ? ResourceType.VIRTUAL
                : ResourceType.ROOM;

        List<Resource> available = resourceRepository.findAvailableResourcesForSession(
                classEntity.getBranch().getId(),
                resourceType,
                classEntity.getMaxCapacity(),
                session.getDate(),
                session.getTimeSlotTemplate().getId()
        );

        return available.stream()
                .limit(10)
                .map(AvailableResourceDTO::basic)
                .toList();
    }

    /**
     * Analyze why a session couldn't be assigned a resource (Phase 2)
     * 
     * @return Optional.empty() if session is not ready for resource assignment (e.g., no time slot),
     *         Optional with conflict details if a conflict prevents assignment
     */
    private Optional<AssignResourcesResponse.ResourceConflictDetail> analyzeSessionConflict(
            Long sessionId,
            Short dayOfWeek,
            Long requestedResourceId,
            Resource requestedResource,
            ClassEntity classEntity
    ) {
        log.debug("Analyzing conflict for Session ID: {}", sessionId);

        Session session = sessionRepository.findSessionWithResourcesAndTimeSlot(sessionId);
        if (session == null) {
            log.warn("Session ID: {} not found during conflict analysis", sessionId);
            return Optional.empty();
        }

        TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();
        if (timeSlot == null) {
            log.debug("Session ID: {} has no time slot assigned", sessionId);
            return Optional.empty();
        }

        int classMaxCapacity = classEntity.getMaxCapacity() != null ? classEntity.getMaxCapacity() : 0;

        // Check capacity
        if (requestedResource.getCapacity() < classMaxCapacity) {
            log.debug("Session ID: {} - Insufficient capacity (Resource: {}, Required: {})",
                    sessionId, requestedResource.getCapacity(), classMaxCapacity);

            return Optional.of(buildConflictDetail(
                    session,
                    dayOfWeek,
                    timeSlot,
                    requestedResource,
                    AssignResourcesResponse.ConflictType.INSUFFICIENT_CAPACITY,
                    String.format("Resource '%s' capacity (%d) is less than class max capacity (%d)",
                            requestedResource.getName(), requestedResource.getCapacity(), classMaxCapacity),
                    null,
                    null,
                    buildResourceSuggestions(classEntity, session)
            ));
        }

        // Check class booking conflict
        Object[] conflictDetails = sessionResourceRepository.findConflictingSessionDetails(sessionId, requestedResourceId);
        if (conflictDetails != null && conflictDetails.length >= 3) {
            Long conflictingClassId = ((Number) conflictDetails[1]).longValue();
            String conflictingClassName = (String) conflictDetails[2];
            LocalTime timeStart = conflictDetails.length > 4 ? (LocalTime) conflictDetails[4] : null;
            LocalTime timeEnd = conflictDetails.length > 5 ? (LocalTime) conflictDetails[5] : null;

            log.debug("Session ID: {} - Class booking conflict (Conflicting Class: {})", sessionId, conflictingClassName);

            String conflictReason = String.format(
                    "Resource '%s' is already booked by class '%s' on %s at %s-%s",
                    requestedResource.getName(), conflictingClassName, session.getDate(),
                    timeStart != null ? timeStart : "N/A",
                    timeEnd != null ? timeEnd : "N/A"
            );

            return Optional.of(buildConflictDetail(
                    session,
                    dayOfWeek,
                    timeSlot,
                    requestedResource,
                    AssignResourcesResponse.ConflictType.CLASS_BOOKING,
                    conflictReason,
                    conflictingClassId,
                    conflictingClassName,
                    buildResourceSuggestions(classEntity, session)
            ));
        }

        // Unknown conflict reason
        log.warn("Session ID: {} - Unknown conflict reason", sessionId);
        return Optional.of(buildConflictDetail(
                session,
                dayOfWeek,
                timeSlot,
                requestedResource,
                AssignResourcesResponse.ConflictType.UNAVAILABLE,
                "Resource is unavailable for unknown reason",
                null,
                null,
                buildResourceSuggestions(classEntity, session)
        ));
    }

    /**
     * Build conflict detail object with optional suggestions.
     */
    private AssignResourcesResponse.ResourceConflictDetail buildConflictDetail(
            Session session,
            Short dayOfWeek,
            TimeSlotTemplate timeSlot,
            Resource requestedResource,
            AssignResourcesResponse.ConflictType conflictType,
            String conflictReason,
            Long conflictingClassId,
            String conflictingClassName,
            List<AvailableResourceDTO> suggestions
    ) {
        LocalTime timeStart = timeSlot != null ? timeSlot.getStartTime() : null;
        LocalTime timeEnd = timeSlot != null ? timeSlot.getEndTime() : null;
        String timeSlotName = timeStart != null && timeEnd != null
                ? timeStart + " - " + timeEnd
                : null;

        return AssignResourcesResponse.ResourceConflictDetail.builder()
                .sessionId(session.getId())
                .sessionNumber(session.getCourseSession() != null ? session.getCourseSession().getSequenceNo() : null)
                .date(session.getDate())
                .dayOfWeek(dayOfWeek)
                .timeSlotTemplateId(timeSlot != null ? timeSlot.getId() : null)
                .timeSlotName(timeSlotName)
                .timeSlotStart(timeStart)
                .timeSlotEnd(timeEnd)
                .requestedResourceId(requestedResource.getId())
                .requestedResourceName(requestedResource.getName())
                .conflictType(conflictType)
                .conflictReason(conflictReason)
                .conflictingClassId(conflictingClassId)
                .conflictingClassName(conflictingClassName)
                .suggestions(suggestions)
                .build();
    }
}
