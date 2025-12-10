package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.ApprovalDTO;
import org.fyp.tmssep490be.dtos.studentrequest.StudentRequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentRequestService_ApproveRequest_Test {

    @Mock private StudentRequestRepository studentRequestRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private ClassRepository classRepository;
    @Mock private StudentSessionRepository studentSessionRepository;

    @Spy
    @InjectMocks
    private StudentRequestService service;

    private StudentRequest request;
    private UserAccount approver;
    private ApprovalDTO dto;

    @BeforeEach
    void setup() {

        approver = UserAccount.builder()
                .id(200L)
                .fullName("Approver")
                .build();

        request = StudentRequest.builder()
                .id(100L)
                .status(RequestStatus.PENDING)
                .requestType(StudentRequestType.ABSENCE)
                .student(Student.builder().id(1L).build())
                .targetSession(Session.builder().id(999L).date(LocalDate.now()).build())
                .build();

        dto = new ApprovalDTO();
        dto.setNote("Approved OK");

        lenient().when(studentSessionRepository.findById(any()))
                .thenReturn(Optional.of(new StudentSession()));

        lenient().when(studentSessionRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));
    }

    // ----------------------------------------------------------------
    @Test
    void approveRequest_notFound() {
        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.approveRequest(100L, 200L, dto));
    }

    // ----------------------------------------------------------------
    @Test
    void approveRequest_invalidStatus() {
        request.setStatus(RequestStatus.APPROVED);

        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.approveRequest(100L, 200L, dto)
        );

        assertEquals("INVALID_STATUS", ex.getErrorCode());
    }

    // ----------------------------------------------------------------
    @Test
    void approveRequest_decidingUserNotFound() {
        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userAccountRepository.findById(200L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.approveRequest(100L, 200L, dto));
    }

    // ----------------------------------------------------------------
    @Test
    void approveRequest_absence_updatesStatus() {
        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userAccountRepository.findById(200L)).thenReturn(Optional.of(approver));
        when(studentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StudentRequestResponseDTO result = service.approveRequest(100L, 200L, dto);

        assertEquals("APPROVED", result.getStatus());
    }

    // ----------------------------------------------------------------
    @Test
    void approveRequest_absence_noTargetSession() {
        request.setTargetSession(null);

        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userAccountRepository.findById(200L)).thenReturn(Optional.of(approver));
        when(studentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StudentRequestResponseDTO result = service.approveRequest(100L, 200L, dto);

        assertEquals("APPROVED", result.getStatus());
        assertNull(result.getTargetSession());
    }

    // ----------------------------------------------------------------
    // MAKEUP request — indirect verification
    // ----------------------------------------------------------------
    @Test
    void approveRequest_makeup_indirectVerification() {

        request.setRequestType(StudentRequestType.MAKEUP);

        ClassEntity c = ClassEntity.builder()
                .id(777L)
                .maxCapacity(50)
                .build();

        Session makeupSession = Session.builder()
                .id(55L)
                .date(LocalDate.now())
                .classEntity(c)
                .build();

        request.setMakeupSession(makeupSession);

        // MUST mock enrolled count to avoid capacity check failure
        when(studentSessionRepository.countBySessionId(55L))
                .thenReturn(10L);

        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userAccountRepository.findById(200L)).thenReturn(Optional.of(approver));
        when(studentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StudentRequestResponseDTO result = service.approveRequest(100L, 200L, dto);

        assertEquals("APPROVED", result.getStatus());
    }


    // ----------------------------------------------------------------
    @Test
    void approveRequest_saveCalledOnce() {

        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userAccountRepository.findById(200L)).thenReturn(Optional.of(approver));
        when(studentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.approveRequest(100L, 200L, dto);

        verify(studentRequestRepository, times(1)).save(any());
    }

    // ----------------------------------------------------------------
    @Test
    void approveRequest_noteIsSet() {

        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(userAccountRepository.findById(200L)).thenReturn(Optional.of(approver));
        when(studentRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.approveRequest(100L, 200L, dto);

        assertEquals("Approved OK", request.getNote());
    }

    // ----------------------------------------------------------------
    @Test
    void approveRequest_fullSuccess() {

        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        when(userAccountRepository.findById(200L))
                .thenReturn(Optional.of(approver));

        when(studentRequestRepository.save(any()))
                .thenAnswer(i -> {
                    StudentRequest r = i.getArgument(0);
                    r.setId(888L);
                    return r;
                });

        StudentRequestResponseDTO result =
                service.approveRequest(100L, 200L, dto);

        assertEquals(888L, result.getId());
        assertEquals("APPROVED", result.getStatus());
    }
}
