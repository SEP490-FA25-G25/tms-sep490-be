package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.RequestFilterDTO;
import org.fyp.tmssep490be.dtos.studentrequest.StudentRequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StudentRequestService_GetMyRequests_Test {

    @Mock private StudentRequestRepository studentRequestRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserBranchesRepository userBranchesRepository;
    @Mock private StudentSessionRepository studentSessionRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private ClassRepository classRepository;
    @Mock private UserAccountRepository userAccountRepository;

    @InjectMocks
    private StudentRequestService service;

    private Student student;

    @BeforeEach
    void setup() {
        student = Student.builder().id(1L).build();
    }

    // ----------------------------------------------------------
    // TC1 — USER NOT FOUND => THROW CustomException(USER_NOT_FOUND)
    // ----------------------------------------------------------
    @Test
    void getMyRequests_userNotFound() {
        RequestFilterDTO filter = new RequestFilterDTO();

        when(studentRepository.findByUserAccountId(10L))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(
                CustomException.class,
                () -> service.getMyRequests(10L, filter)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    // ----------------------------------------------------------
    // TC2 — INVALID REQUEST TYPE => IGNORE & CONTINUE
    // ----------------------------------------------------------
    @Test
    void getMyRequests_invalidRequestType_isIgnored() {
        RequestFilterDTO filter = new RequestFilterDTO();
        filter.setRequestType("WRONG_TYPE");
        filter.setPage(0);
        filter.setSize(10);

        when(studentRepository.findByUserAccountId(10L))
                .thenReturn(Optional.of(student));

        Page<StudentRequest> emptyPage =
                new PageImpl<>(List.of(), PageRequest.of(0,10), 0);

        when(studentRequestRepository.findStudentRequestsWithFilters(
                eq(1L), any(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(emptyPage);

        Page<StudentRequestResponseDTO> result =
                service.getMyRequests(10L, filter);

        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------
    // TC3 — INVALID STATUS => IGNORE & CONTINUE
    // ----------------------------------------------------------
    @Test
    void getMyRequests_invalidStatus_isIgnored() {
        RequestFilterDTO filter = new RequestFilterDTO();
        filter.setStatus("WRONG_STATUS");
        filter.setPage(0);
        filter.setSize(10);

        when(studentRepository.findByUserAccountId(10L))
                .thenReturn(Optional.of(student));

        Page<StudentRequest> emptyPage =
                new PageImpl<>(List.of(), PageRequest.of(0,10), 0);

        when(studentRequestRepository.findStudentRequestsWithFilters(
                eq(1L), any(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(emptyPage);

        Page<StudentRequestResponseDTO> result =
                service.getMyRequests(10L, filter);

        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------
    // TC4 — SEARCH TERM RỖNG => searchTerm = null
    // ----------------------------------------------------------
    @Test
    void getMyRequests_emptySearchTerm_becomesNull() {
        RequestFilterDTO filter = new RequestFilterDTO();
        filter.setSearch("   "); // SPACE
        filter.setPage(0);
        filter.setSize(5);

        when(studentRepository.findByUserAccountId(10L))
                .thenReturn(Optional.of(student));

        Page<StudentRequest> fake = new PageImpl<>(
                List.of(), PageRequest.of(0,5), 0
        );

        when(studentRequestRepository.findStudentRequestsWithFilters(
                eq(1L), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(fake);

        Page<StudentRequestResponseDTO> result =
                service.getMyRequests(10L, filter);

        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------
    // TC5 — FILTER REQUEST TYPE + STATUS hoạt động đúng
    // ----------------------------------------------------------
    @Test
    void getMyRequests_validFilters_arePassedToRepository() {
        RequestFilterDTO filter = new RequestFilterDTO();
        filter.setRequestType("ABSENCE");
        filter.setStatus("PENDING");
        filter.setPage(0);
        filter.setSize(10);

        when(studentRepository.findByUserAccountId(10L))
                .thenReturn(Optional.of(student));

        StudentRequest req = StudentRequest.builder()
                .id(123L)
                .requestType(StudentRequestType.ABSENCE)
                .status(RequestStatus.PENDING)
                .build();

        Page<StudentRequest> page = new PageImpl<>(
                List.of(req), PageRequest.of(0,10), 1
        );

        when(studentRequestRepository.findStudentRequestsWithFilters(
                eq(1L),
                isNull(),               // search term null
                eq(List.of(StudentRequestType.ABSENCE)),
                eq(List.of(RequestStatus.PENDING)),
                any(Pageable.class)
        )).thenReturn(page);

        Page<StudentRequestResponseDTO> result =
                service.getMyRequests(10L, filter);

        assertEquals(1, result.getTotalElements());
        assertEquals(123L, result.getContent().get(0).getId());
    }

    // ----------------------------------------------------------
    // TC6 — SUCCESS: Repository trả về page, mapping hoạt động
    // ----------------------------------------------------------
    @Test
    void getMyRequests_success_returnsMappedDTOs() {
        RequestFilterDTO filter = new RequestFilterDTO();
        filter.setPage(0);
        filter.setSize(10);

        when(studentRepository.findByUserAccountId(10L))
                .thenReturn(Optional.of(student));

        StudentRequest entity = StudentRequest.builder()
                .id(999L)
                .requestType(StudentRequestType.ABSENCE)
                .status(RequestStatus.APPROVED)
                .build();

        Page<StudentRequest> page =
                new PageImpl<>(List.of(entity), PageRequest.of(0,10), 1);

        when(studentRequestRepository.findStudentRequestsWithFilters(
                eq(1L), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(page);

        Page<StudentRequestResponseDTO> result =
                service.getMyRequests(10L, filter);

        assertEquals(1, result.getTotalElements());
        assertEquals(999L, result.getContent().get(0).getId());
        assertEquals("ABSENCE", result.getContent().get(0).getRequestType());
    }
}
