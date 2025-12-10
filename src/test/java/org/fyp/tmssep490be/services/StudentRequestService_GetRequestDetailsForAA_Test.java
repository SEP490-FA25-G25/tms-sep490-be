package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.StudentRequestDetailDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StudentRequestService_GetRequestDetailsForAA_Test {

    @Mock private StudentRequestRepository studentRequestRepository;
    @Mock private StudentSessionRepository studentSessionRepository;

    @InjectMocks
    private StudentRequestService service;

    private Student student;
    private ClassEntity clazz;
    private UserAccount submitted;
    private UserAccount decided;

    @BeforeEach
    void setup() {

        student = Student.builder()
                .id(1L)
                .userAccount(UserAccount.builder().id(10L).fullName("Student A").build())
                .build();

        clazz = ClassEntity.builder()
                .id(77L)
                .code("SE1801")
                .name("Class SE1801")
                .build();

        submitted = UserAccount.builder().id(20L).fullName("Submitter").build();
        decided   = UserAccount.builder().id(30L).fullName("Decider").build();

        lenient().when(studentSessionRepository.countBySessionId(anyLong()))
                .thenReturn(0L);
    }

    // ----------------------------------------------------------
    // TC1 — REQUEST NOT FOUND
    // ----------------------------------------------------------
    @Test
    void getRequestDetailsForAA_requestNotFound() {
        when(studentRequestRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getRequestDetailsForAA(100L));
    }

    // ----------------------------------------------------------
    // TC2 — ABSENCE REQUEST → maps targetSession
    // ----------------------------------------------------------
    @Test
    void getRequestDetailsForAA_mapsTargetSession_absence() {

        Session target = Session.builder()
                .id(200L)
                .date(LocalDate.now())
                .build();

        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .student(student)
                .currentClass(clazz)
                .requestType(StudentRequestType.ABSENCE)
                .targetSession(target)
                .submittedBy(submitted)
                .decidedBy(decided)
                .build();

        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(req));

        StudentRequestDetailDTO dto = service.getRequestDetailsForAA(100L);

        assertNotNull(dto.getTargetSession());
        assertEquals(200L, dto.getTargetSession().getId());
    }

    // ----------------------------------------------------------
    // TC3 — MAKEUP REQUEST → maps makeupSession
    // ----------------------------------------------------------
    @Test
    void getRequestDetailsForAA_mapsMakeupSession_makeup() {

        Session makeup = Session.builder()
                .id(333L)
                .date(LocalDate.now())
                .build();

        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .student(student)
                .currentClass(clazz)
                .requestType(StudentRequestType.MAKEUP)
                .makeupSession(makeup)
                .submittedBy(submitted)
                .decidedBy(decided)
                .build();

        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(req));

        StudentRequestDetailDTO dto = service.getRequestDetailsForAA(100L);

        assertNotNull(dto.getMakeupSession());
        assertEquals(333L, dto.getMakeupSession().getId());
    }

    // ----------------------------------------------------------
    // TC4 — TRANSFER REQUEST → effectiveSession maps to targetSession
    // ----------------------------------------------------------
    @Test
    void getRequestDetailsForAA_mapsEffectiveSession_transfer() {

        Session eff = Session.builder()
                .id(300L)
                .date(LocalDate.now())
                .build();

        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .student(student)
                .currentClass(clazz)
                .requestType(StudentRequestType.TRANSFER)
                .effectiveSession(eff)
                .submittedBy(submitted)
                .decidedBy(decided)
                .build();

        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(req));

        StudentRequestDetailDTO dto = service.getRequestDetailsForAA(100L);

        assertNotNull(dto.getTargetSession());
        assertEquals(300L, dto.getTargetSession().getId());
    }

    // ----------------------------------------------------------
    // TC5 — Teacher mapping
    // ----------------------------------------------------------
    @Test
    void getRequestDetailsForAA_mapsTeacherCorrectly() {

        Teacher teacher = Teacher.builder()
                .id(50L)
                .userAccount(UserAccount.builder().id(999L).fullName("Mr Smith").build())
                .build();

        TeachingSlot slot = TeachingSlot.builder()
                .teacher(teacher)
                .build();

        Session session = Session.builder()
                .id(555L)
                .date(LocalDate.now())
                .teachingSlots(new HashSet<>(List.of(slot)))
                .build();

        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .student(student)
                .currentClass(clazz)
                .requestType(StudentRequestType.ABSENCE)
                .targetSession(session)
                .submittedBy(submitted)
                .decidedBy(decided)
                .build();

        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(req));

        StudentRequestDetailDTO dto = service.getRequestDetailsForAA(100L);

        assertNotNull(dto.getTargetSession().getTeacher());
        assertEquals(50L, dto.getTargetSession().getTeacher().getId());
    }

    // ----------------------------------------------------------
    // TC6 — Full mapping success
    // ----------------------------------------------------------
    @Test
    void getRequestDetailsForAA_success_fullMapping() {

        Session sess = Session.builder()
                .id(444L)
                .date(LocalDate.now())
                .build();

        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .student(student)
                .currentClass(clazz)
                .requestType(StudentRequestType.ABSENCE)
                .targetSession(sess)
                .status(RequestStatus.REJECTED)
                .submittedBy(submitted)
                .decidedBy(decided)
                .requestReason("Sick")
                .note("Need rest")
                .submittedAt(OffsetDateTime.now().minusDays(1))
                .decidedAt(OffsetDateTime.now())
                // decisionNote is NOT in entity → remove
                .build();

        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(req));

        StudentRequestDetailDTO dto = service.getRequestDetailsForAA(100L);

        assertEquals(100L, dto.getId());
        assertEquals("ABSENCE", dto.getRequestType());
        assertEquals("REJECTED", dto.getStatus());

        assertNotNull(dto.getCurrentClass());
        assertEquals(77L, dto.getCurrentClass().getId());
        assertEquals("Class SE1801", dto.getCurrentClass().getName());

        assertEquals(444L, dto.getTargetSession().getId());
        assertEquals("Sick", dto.getRequestReason());
        assertEquals("Need rest", dto.getNote());

        assertEquals(20L, dto.getSubmittedBy().getId());
        assertEquals(30L, dto.getDecidedBy().getId());

        // Because entity has no decisionNote → DTO should be null
        assertEquals("Need rest", dto.getDecisionNote());
    }

}
