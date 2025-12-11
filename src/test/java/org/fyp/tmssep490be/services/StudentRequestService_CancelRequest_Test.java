package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.StudentRequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.*;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentRequestService_CancelRequest_Test {

    @Mock private StudentRepository studentRepository;
    @Mock private StudentRequestRepository studentRequestRepository;

    // mapToStudentRequestResponseDTO() là private → dùng Spy để gọi thực
    @Spy
    @InjectMocks
    private StudentRequestService service;

    private Student student;
    private StudentRequest request;

    @BeforeEach
    void setup() {

        student = Student.builder()
                .id(1L)
                .userAccount(UserAccount.builder().id(99L).build())
                .build();

        request = StudentRequest.builder()
                .id(100L)
                .student(student)
                .status(RequestStatus.PENDING)
                .build();
    }

    // ---------------------------------------------------------------
    // TC1 — student not found (userId không mapping student)
    // ---------------------------------------------------------------
    @Test
    void cancelRequest_studentNotFound() {
        when(studentRepository.findByUserAccountId(99L))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(
                CustomException.class,
                () -> service.cancelRequest(100L, 99L)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    // ---------------------------------------------------------------
    // TC2 — request not found
    // ---------------------------------------------------------------
    @Test
    void cancelRequest_requestNotFound() {

        when(studentRepository.findByUserAccountId(99L))
                .thenReturn(Optional.of(student));

        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.cancelRequest(100L, 99L));
    }

    // ---------------------------------------------------------------
    // TC3 — ACCESS_DENIED (student không sở hữu request)
    // ---------------------------------------------------------------
    @Test
    void cancelRequest_accessDenied() {

        Student other = Student.builder().id(2L).build();
        request.setStudent(other);

        when(studentRepository.findByUserAccountId(99L))
                .thenReturn(Optional.of(student));

        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.cancelRequest(100L, 99L)
        );

        assertEquals("ACCESS_DENIED", ex.getErrorCode());
    }

    // ---------------------------------------------------------------
    // TC4 — INVALID_STATUS (chỉ pending mới được cancel)
    // ---------------------------------------------------------------
    @Test
    void cancelRequest_invalidStatus() {

        request.setStatus(RequestStatus.APPROVED);

        when(studentRepository.findByUserAccountId(99L))
                .thenReturn(Optional.of(student));

        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.cancelRequest(100L, 99L)
        );

        assertEquals("INVALID_STATUS", ex.getErrorCode());
    }

    // ---------------------------------------------------------------
    // TC5 — SUCCESS → status = CANCELLED
    // ---------------------------------------------------------------
    @Test
    void cancelRequest_success_statusUpdated() {

        when(studentRepository.findByUserAccountId(99L))
                .thenReturn(Optional.of(student));

        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        when(studentRequestRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        StudentRequestResponseDTO result = service.cancelRequest(100L, 99L);

        assertEquals("CANCELLED", result.getStatus());
    }

    // ---------------------------------------------------------------
    // TC6 — SUCCESS → đúng DTO trả về
    // ---------------------------------------------------------------
    @Test
    void cancelRequest_success_returnDTO() {

        when(studentRepository.findByUserAccountId(99L))
                .thenReturn(Optional.of(student));

        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.of(request));

        when(studentRequestRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        StudentRequestResponseDTO result = service.cancelRequest(100L, 99L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("CANCELLED", result.getStatus());
    }
}
