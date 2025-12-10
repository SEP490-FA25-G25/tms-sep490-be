package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.AARequestFilterDTO;
import org.fyp.tmssep490be.dtos.studentrequest.AARequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.OffsetDateTime;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StudentRequestService_GetPendingRequests_Test {

    @Mock private UserBranchesRepository userBranchesRepository;
    @Mock private StudentRequestRepository studentRequestRepository;

    @InjectMocks
    private StudentRequestService service;

    private AARequestFilterDTO filter;

    @BeforeEach
    void setup() {
        filter = new AARequestFilterDTO();
        filter.setPage(0);
        filter.setSize(10);
    }

    // ----------------------------------------------------------
    // TC1 — USER HAS NO BRANCHES → THROW EXCEPTION
    // ----------------------------------------------------------
    @Test
    void getPendingRequests_userHasNoBranches_throwsException() {

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.getPendingRequests(10L, filter));

        assertEquals(ErrorCode.CLASS_NO_BRANCH_ACCESS, ex.getErrorCode());
    }

    // ----------------------------------------------------------
    // TC2 — FILTER BRANCH NOT IN USER ACCESS → BRANCH_ACCESS_DENIED
    // ----------------------------------------------------------
    @Test
    void getPendingRequests_invalidBranchAccess_throwsAccessDenied() {

        filter.setBranchId(99L);

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L, 2L));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.getPendingRequests(10L, filter));

        assertEquals(ErrorCode.BRANCH_ACCESS_DENIED, ex.getErrorCode());
    }

    // ----------------------------------------------------------
    // TC3 — NO FILTER BRANCH → AUTO USE USER BRANCH LIST
    // ----------------------------------------------------------
    @Test
    void getPendingRequests_noBranchFilter_useUserBranches() {

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L, 2L));

        Page<StudentRequest> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(studentRequestRepository.findPendingRequestsByBranches(
                RequestStatus.PENDING,
                List.of(1L, 2L),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getPendingRequests(10L, filter);

        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------
    // TC4 — FILTER BRANCH VALID → USE EXACT BRANCHID
    // ----------------------------------------------------------
    @Test
    void getPendingRequests_branchFilterValid_useFilteredBranch() {

        filter.setBranchId(2L);

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L, 2L));

        Page<StudentRequest> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(studentRequestRepository.findPendingRequestsByBranches(
                RequestStatus.PENDING,
                List.of(2L),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getPendingRequests(10L, filter);

        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------
    // TC5 — SUCCESS: REPOSITORY RETURNS DATA, MAP TO DTO
    // ----------------------------------------------------------
    @Test
    void getPendingRequests_success_mappingWorks() {

        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .requestType(StudentRequestType.ABSENCE)
                .status(RequestStatus.PENDING)
                .submittedAt(OffsetDateTime.now())
                .build();

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L));

        Page<StudentRequest> page =
                new PageImpl<>(List.of(req), PageRequest.of(0, 10), 1);

        when(studentRequestRepository.findPendingRequestsByBranches(
                RequestStatus.PENDING,
                List.of(1L),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getPendingRequests(10L, filter);

        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).getId());
    }

    // ----------------------------------------------------------
    // TC6 — ADDITIONAL FILTER REMOVES SOME ITEMS
    // ----------------------------------------------------------
    @Test
    void getPendingRequests_additionalFilters_removedOneEntry() {

        StudentRequest req1 = StudentRequest.builder()
                .id(10L)
                .requestType(StudentRequestType.ABSENCE)
                .status(RequestStatus.PENDING)
                .build();

        StudentRequest req2 = StudentRequest.builder()
                .id(20L)
                .requestType(StudentRequestType.TRANSFER)
                .status(RequestStatus.PENDING)
                .build();

        filter.setRequestType("ABSENCE"); // Only req1 should match

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L));

        Page<StudentRequest> page =
                new PageImpl<>(List.of(req1, req2), PageRequest.of(0, 10), 2);

        when(studentRequestRepository.findPendingRequestsByBranches(
                RequestStatus.PENDING,
                List.of(1L),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getPendingRequests(10L, filter);

        assertEquals(1, result.getContent().size());
        assertEquals(10L, result.getContent().get(0).getId());
    }

    // ----------------------------------------------------------
    // TC7 — PAGINATION COUNT SHOULD KEEP ORIGINAL TOTAL
    // ----------------------------------------------------------
    @Test
    void getPendingRequests_keepOriginalTotalCount() {

        StudentRequest req = StudentRequest.builder()
                .id(10L)
                .requestType(StudentRequestType.ABSENCE)
                .status(RequestStatus.PENDING)
                .build();

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L));

        Page<StudentRequest> page =
                new PageImpl<>(List.of(req), PageRequest.of(0, 10), 999);

        when(studentRequestRepository.findPendingRequestsByBranches(
                RequestStatus.PENDING,
                List.of(1L),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getPendingRequests(10L, filter);

        assertEquals(999, result.getTotalElements());
    }

    // ----------------------------------------------------------
    // TC8 — FILTER REMOVES EVERYTHING → EMPTY LIST
    // ----------------------------------------------------------
    @Test
    void getPendingRequests_filteredEmptyList() {

        StudentRequest req = StudentRequest.builder()
                .id(10L)
                .requestType(StudentRequestType.TRANSFER)
                .status(RequestStatus.PENDING)
                .build();

        filter.setRequestType("ABSENCE");

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L));

        Page<StudentRequest> page =
                new PageImpl<>(List.of(req), PageRequest.of(0, 10), 1);

        when(studentRequestRepository.findPendingRequestsByBranches(
                RequestStatus.PENDING,
                List.of(1L),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getPendingRequests(10L, filter);

        assertTrue(result.getContent().isEmpty());
        assertEquals(1, result.getTotalElements());
    }
}
