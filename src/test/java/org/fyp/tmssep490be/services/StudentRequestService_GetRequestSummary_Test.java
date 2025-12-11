package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.AARequestFilterDTO;
import org.fyp.tmssep490be.dtos.studentrequest.RequestSummaryDTO;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.StudentRequestRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentRequestService_GetRequestSummary_Test {

    @Mock
    private UserBranchesRepository userBranchesRepository;

    @Mock
    private StudentRequestRepository studentRequestRepository;

    @InjectMocks
    private StudentRequestService service;

    // ----------------------------------------------------------------------
    // TC1 — User has NO branches → throw CLASS_NO_BRANCH_ACCESS
    // ----------------------------------------------------------------------
    @Test
    void getRequestSummary_userHasNoBranches_throwsException() {
        Long userId = 10L;
        AARequestFilterDTO filter = new AARequestFilterDTO();

        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of()); // empty

        CustomException ex = assertThrows(
                CustomException.class,
                () -> service.getRequestSummary(userId, filter)
        );

        assertEquals(ErrorCode.CLASS_NO_BRANCH_ACCESS, ex.getErrorCode());
    }

    // ----------------------------------------------------------------------
    // TC2 — No filter.branchId → use user's branch list
    // ----------------------------------------------------------------------
    @Test
    void getRequestSummary_noBranchFilter_success() {
        Long userId = 10L;
        List<Long> branches = List.of(1L, 2L);

        AARequestFilterDTO filter = new AARequestFilterDTO();
        filter.setBranchId(null);  // No specific branch filter

        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(branches);

        when(studentRequestRepository.countByStatusAndBranches(RequestStatus.PENDING, branches))
                .thenReturn(5L);
        when(studentRequestRepository.countByRequestTypeAndStatusAndBranches(StudentRequestType.ABSENCE, RequestStatus.PENDING, branches))
                .thenReturn(2L);
        when(studentRequestRepository.countByRequestTypeAndStatusAndBranches(StudentRequestType.MAKEUP, RequestStatus.PENDING, branches))
                .thenReturn(1L);
        when(studentRequestRepository.countByRequestTypeAndStatusAndBranches(StudentRequestType.TRANSFER, RequestStatus.PENDING, branches))
                .thenReturn(2L);

        RequestSummaryDTO dto = service.getRequestSummary(userId, filter);

        assertEquals(5, dto.getTotalPending());
        assertEquals(2, dto.getAbsenceRequests());
        assertEquals(1, dto.getMakeupRequests());
        assertEquals(2, dto.getTransferRequests());
    }

    // ----------------------------------------------------------------------
    // TC3 — With filter.branchId → only that branch used
    // ----------------------------------------------------------------------
    @Test
    void getRequestSummary_specificBranchFilter_success() {
        Long userId = 10L;
        Long branchFilter = 99L;

        AARequestFilterDTO filter = new AARequestFilterDTO();
        filter.setBranchId(branchFilter);

        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L, 99L)); // user has access to branch 99

        List<Long> target = List.of(branchFilter);

        when(studentRequestRepository.countByStatusAndBranches(RequestStatus.PENDING, target))
                .thenReturn(3L);
        when(studentRequestRepository.countByRequestTypeAndStatusAndBranches(StudentRequestType.ABSENCE, RequestStatus.PENDING, target))
                .thenReturn(1L);
        when(studentRequestRepository.countByRequestTypeAndStatusAndBranches(StudentRequestType.MAKEUP, RequestStatus.PENDING, target))
                .thenReturn(1L);
        when(studentRequestRepository.countByRequestTypeAndStatusAndBranches(StudentRequestType.TRANSFER, RequestStatus.PENDING, target))
                .thenReturn(1L);

        RequestSummaryDTO dto = service.getRequestSummary(userId, filter);

        assertEquals(3, dto.getTotalPending());
        assertEquals(1, dto.getAbsenceRequests());
        assertEquals(1, dto.getMakeupRequests());
        assertEquals(1, dto.getTransferRequests());
    }

    // ----------------------------------------------------------------------
    // TC4 — REPO COUNTS = 0 → should return all 0 (not null)
    // ----------------------------------------------------------------------
    @Test
    void getRequestSummary_repoReturnsZero_success() {
        Long userId = 10L;

        AARequestFilterDTO filter = new AARequestFilterDTO();

        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(5L)); // user has 1 branch

        List<Long> target = List.of(5L);

        when(studentRequestRepository.countByStatusAndBranches(RequestStatus.PENDING, target))
                .thenReturn(0L);
        when(studentRequestRepository.countByRequestTypeAndStatusAndBranches(any(), any(), eq(target)))
                .thenReturn(0L);

        RequestSummaryDTO dto = service.getRequestSummary(userId, filter);

        assertEquals(0, dto.getTotalPending());
        assertEquals(0, dto.getAbsenceRequests());
        assertEquals(0, dto.getMakeupRequests());
        assertEquals(0, dto.getTransferRequests());
    }
}
