package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.AARequestFilterDTO;
import org.fyp.tmssep490be.dtos.studentrequest.AARequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.*;
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
class StudentRequestService_GetAllRequests_Test {

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
    // TC1 — USER HAS NO BRANCHES → EXCEPTION
    // ----------------------------------------------------------
    @Test
    void getAllRequests_userHasNoBranches_throws() {

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.getAllRequests(10L, filter));

        assertEquals(ErrorCode.CLASS_NO_BRANCH_ACCESS, ex.getErrorCode());
    }

    // ----------------------------------------------------------
    // TC2 — INVALID BRANCH ACCESS → EXCEPTION
    // ----------------------------------------------------------
    @Test
    void getAllRequests_invalidBranchAccess_throws() {

        filter.setBranchId(99L);

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L, 2L));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.getAllRequests(10L, filter));

        assertEquals(ErrorCode.BRANCH_ACCESS_DENIED, ex.getErrorCode());
    }

    // ----------------------------------------------------------
    // TC3 — NO DECIDEDBY FILTER → CALL findAllRequestsByBranches
    // ----------------------------------------------------------
    @Test
    void getAllRequests_noDecidedBy_callsFindAllRequests() {

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L));

        Page<StudentRequest> page =
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(studentRequestRepository.findAllRequestsByBranches(
                List.of(1L),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getAllRequests(10L, filter);

        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------
    // TC4 — DECIDEDBY PRESENT → CALL findAllRequestsByBranchesAndDecidedBy
    // ----------------------------------------------------------
    @Test
    void getAllRequests_withDecidedBy_callsCorrectRepository() {

        filter.setDecidedBy(88L);

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L));

        Page<StudentRequest> page =
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(studentRequestRepository.findAllRequestsByBranchesAndDecidedBy(
                List.of(1L),
                88L,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getAllRequests(10L, filter);

        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------
    // TC5 — NO BRANCH FILTER → USE USER BRANCHES
    // ----------------------------------------------------------
    @Test
    void getAllRequests_noBranchFilter_usesAllUserBranches() {

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L, 2L));

        Page<StudentRequest> page =
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(studentRequestRepository.findAllRequestsByBranches(
                List.of(1L, 2L),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getAllRequests(10L, filter);

        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------
    // TC6 — VALID BRANCH FILTER → USE ONLY THAT BRANCH
    // ----------------------------------------------------------
    @Test
    void getAllRequests_branchFilterValid() {

        filter.setBranchId(2L);

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(2L, 3L));

        Page<StudentRequest> page =
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(studentRequestRepository.findAllRequestsByBranches(
                List.of(2L),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getAllRequests(10L, filter);

        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------
    // TC7 — SUCCESS → DTO MAPPING WORKS
    // ----------------------------------------------------------
    @Test
    void getAllRequests_success_mappingWorks() {

        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .requestType(StudentRequestType.ABSENCE)
                .status(RequestStatus.APPROVED)
                .submittedAt(OffsetDateTime.now())
                .build();

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L));

        Page<StudentRequest> page =
                new PageImpl<>(List.of(req), PageRequest.of(0, 10), 1);

        when(studentRequestRepository.findAllRequestsByBranches(
                List.of(1L),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getAllRequests(10L, filter);

        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).getId());
    }

    // ----------------------------------------------------------
    // TC8 — ADDITIONAL FILTER REMOVES ONE ENTRY
    // ----------------------------------------------------------
    @Test
    void getAllRequests_additionalFilters_removeOne() {

        StudentRequest r1 = StudentRequest.builder()
                .id(10L)
                .requestType(StudentRequestType.ABSENCE)
                .status(RequestStatus.APPROVED)
                .build();

        StudentRequest r2 = StudentRequest.builder()
                .id(20L)
                .requestType(StudentRequestType.TRANSFER)
                .status(RequestStatus.APPROVED)
                .build();

        filter.setRequestType("ABSENCE");

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L));

        Page<StudentRequest> page =
                new PageImpl<>(List.of(r1, r2), PageRequest.of(0, 10), 2);

        when(studentRequestRepository.findAllRequestsByBranches(
                List.of(1L),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getAllRequests(10L, filter);

        assertEquals(1, result.getContent().size());
        assertEquals(10L, result.getContent().get(0).getId());
    }

    // ----------------------------------------------------------
    // TC9 — ALL FILTERED OUT → EMPTY CONTENT
    // ----------------------------------------------------------
    @Test
    void getAllRequests_allFiltered_out() {

        StudentRequest req = StudentRequest.builder()
                .id(10L)
                .requestType(StudentRequestType.TRANSFER)
                .status(RequestStatus.APPROVED)
                .build();

        filter.setRequestType("ABSENCE");

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L));

        Page<StudentRequest> page =
                new PageImpl<>(List.of(req), PageRequest.of(0, 10), 1);

        when(studentRequestRepository.findAllRequestsByBranches(
                List.of(1L),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getAllRequests(10L, filter);

        assertTrue(result.getContent().isEmpty());
        assertEquals(1, result.getTotalElements());
    }

    // ----------------------------------------------------------
    // TC10 — TOTAL COUNT MUST REMAIN SAME
    // ----------------------------------------------------------
    @Test
    void getAllRequests_totalCountRemainsSame() {

        StudentRequest req = StudentRequest.builder()
                .id(10L)
                .requestType(StudentRequestType.ABSENCE)
                .status(RequestStatus.APPROVED)
                .build();

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(1L));

        Page<StudentRequest> page =
                new PageImpl<>(List.of(req), PageRequest.of(0, 10), 777);

        when(studentRequestRepository.findAllRequestsByBranches(
                List.of(1L),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "submittedAt"))
        )).thenReturn(page);

        Page<AARequestResponseDTO> result =
                service.getAllRequests(10L, filter);

        assertEquals(777, result.getTotalElements());
    }
}
