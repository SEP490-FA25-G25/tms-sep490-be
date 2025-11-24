package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.schedule.WeeklyScheduleResponseDTO;
import org.fyp.tmssep490be.dtos.studentrequest.*;
import org.fyp.tmssep490be.entities.StudentRequest;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface StudentRequestService {

    // Student operations (userId is user_account.id, will be mapped to student.id internally)
    Page<StudentRequestResponseDTO> getMyRequests(Long userId, RequestFilterDTO filter);
    StudentRequestDetailDTO getRequestById(Long requestId, Long userId);
    StudentRequestResponseDTO submitAbsenceRequest(Long userId, AbsenceRequestDTO dto);
    StudentRequestResponseDTO submitMakeupRequest(Long userId, MakeupRequestDTO dto);
    StudentRequestResponseDTO submitTransferRequest(Long userId, TransferRequestDTO dto);
    StudentRequestResponseDTO cancelRequest(Long requestId, Long userId);
    List<SessionAvailabilityDTO> getAvailableSessionsForDate(Long userId, LocalDate date, StudentRequestType requestType);
    MissedSessionsResponseDTO getMissedSessions(Long userId, Integer weeksBack, Boolean excludeRequested);
    MakeupOptionsResponseDTO getMakeupOptions(Long targetSessionId, Long userId);
    TransferEligibilityDTO getTransferEligibility(Long userId);
    List<TransferOptionDTO> getTransferOptions(Long userId, Long currentClassId);
    TransferOptionsResponseDTO getTransferOptionsFlexible(Long currentClassId, Long targetBranchId, String targetModality, Boolean scheduleOnly);
    List<StudentClassDTO> getMyClassesForStudent(Long studentId);

    // Academic Affairs operations (with branch-level security)
    Page<AARequestResponseDTO> getPendingRequests(Long currentUserId, AARequestFilterDTO filter);
    Page<AARequestResponseDTO> getAllRequests(Long currentUserId, AARequestFilterDTO filter);
    StudentRequestDetailDTO getRequestDetailsForAA(Long requestId);
    StudentRequestResponseDTO approveRequest(Long requestId, Long decidedById, ApprovalDTO dto);
    StudentRequestResponseDTO rejectRequest(Long requestId, Long decidedById, RejectionDTO dto);
    RequestSummaryDTO getRequestSummary(Long currentUserId, AARequestFilterDTO filter);

    // AA on-behalf operations
    MissedSessionsResponseDTO getMissedSessionsForStudent(Long studentId, Integer weeksBack);
    MakeupOptionsResponseDTO getMakeupOptionsForStudent(Long targetSessionId, Long studentId);
    StudentRequestResponseDTO submitAbsenceRequestOnBehalf(Long decidedById, AbsenceRequestDTO dto);
    StudentRequestResponseDTO submitMakeupRequestOnBehalf(Long decidedById, MakeupRequestDTO dto);
    StudentRequestResponseDTO submitTransferRequestOnBehalf(Long decidedById, TransferRequestDTO dto);

    // Support methods
    boolean hasDuplicateRequest(Long studentId, Long sessionId, StudentRequestType requestType);
    double calculateAbsenceRate(Long studentId, Long classId);
    boolean hasExceededTransferLimit(Long studentId, Long courseId);
    boolean isValidTransfer(Long currentClassId, Long targetClassId);
    void executeTransfer(StudentRequest request);

    // Student Schedule methods for AA
    WeeklyScheduleResponseDTO getWeeklySchedule(Long studentId, LocalDate weekStart);
    WeeklyScheduleResponseDTO getWeeklyScheduleByClass(Long studentId, Long classId, LocalDate weekStart);
    LocalDate getCurrentWeekStart();
}
