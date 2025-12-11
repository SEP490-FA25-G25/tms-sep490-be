package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.RejectionDTO;
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

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentRequestService_RejectRequest_Test {

    @Mock private StudentRequestRepository studentRequestRepository;
    @Mock private UserAccountRepository userAccountRepository;

    @Spy
    @InjectMocks
    private StudentRequestService service;

    private StudentRequest request;
    private UserAccount approver;
    private RejectionDTO dto;

    @BeforeEach
    void setup() {

        approver = UserAccount.builder()
                .id(200L)
                .fullName("Approver")
                .build();

        request = StudentRequest.builder()
                .id(100L)
                .requestType(StudentRequestType.ABSENCE)
                .status(RequestStatus.PENDING)
                .student(Student.builder().id(1L).build())
                .build();

        dto = new RejectionDTO();
        dto.setNote("Reason XYZ");
    }

    // ---------------------------------------------------------------
    // TC1 — Request not found
    // ---------------------------------------------------------------
    @Test
    void rejectRequest_notFound() {

        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.rejectRequest(100L, 200L, dto));
    }

    // ---------------------------------------------------------------
    // TC2 — INVALID_STATUS if not PENDING
    // ---------------------------------------------------------------
    @Test
    void rejectRequest_invalidStatus() {

        request.setStatus(RequestStatus.APPROVED);

        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.rejectRequest(100L, 200L, dto)
        );

        assertEquals("INVALID_STATUS", ex.getErrorCode());
    }

    // ---------------------------------------------------------------
    // TC3 — deciding user not found
    // ---------------------------------------------------------------
    @Test
    void rejectRequest_decidingUserNotFound() {

        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        when(userAccountRepository.findById(200L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.rejectRequest(100L, 200L, dto));
    }

    // ---------------------------------------------------------------
    // TC4 — Full rejection success
    // ---------------------------------------------------------------
    @Test
    void rejectRequest_fullSuccess() {

        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        when(userAccountRepository.findById(200L))
                .thenReturn(Optional.of(approver));

        when(studentRequestRepository.save(any()))
                .thenAnswer(i -> {
                    StudentRequest r = i.getArgument(0);
                    r.setId(999L);
                    return r;
                });

        StudentRequestResponseDTO result =
                service.rejectRequest(100L, 200L, dto);

        assertEquals("REJECTED", result.getStatus());
        assertEquals(999L, result.getId());
    }

    // ---------------------------------------------------------------
    // TC5 — Note is set correctly
    // ---------------------------------------------------------------
    @Test
    void rejectRequest_noteIsSet() {

        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        when(userAccountRepository.findById(200L))
                .thenReturn(Optional.of(approver));

        when(studentRequestRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        service.rejectRequest(100L, 200L, dto);

        assertEquals("Reason XYZ", request.getNote());
    }
}
