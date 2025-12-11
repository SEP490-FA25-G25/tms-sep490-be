package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.StudentRequestDetailDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.*;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Set;
import java.util.HashSet;
import java.util.HashSet;
import java.util.List;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StudentRequestService_GetRequestById_Test {

    @Mock private StudentRepository studentRepository;
    @Mock private StudentRequestRepository studentRequestRepository;
    @Mock private StudentSessionRepository studentSessionRepository;
    @Mock private UserBranchesRepository userBranchesRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private ClassRepository classRepository;
    @Mock private UserAccountRepository userAccountRepository;

    @InjectMocks
    private StudentRequestService service;

    private Student student;
    private UserAccount userAccount;

    @BeforeEach
    void setup() {
        userAccount = UserAccount.builder().id(10L).fullName("Student A").email("a@fpt.edu.vn").build();

        student = Student.builder()
                .id(1L)
                .userAccount(userAccount)
                .build();
    }

    // ----------------------------------------------------------
    // TC1 — USER NOT FOUND
    // ----------------------------------------------------------
    @Test
    void getRequestById_studentNotFound() {
        when(studentRepository.findByUserAccountId(10L))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.getRequestById(100L, 10L));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    // ----------------------------------------------------------
    // TC2 — REQUEST NOT FOUND
    // ----------------------------------------------------------
    @Test
    void getRequestById_requestNotFound() {
        when(studentRepository.findByUserAccountId(10L)).thenReturn(Optional.of(student));
        when(studentRequestRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getRequestById(100L, 10L));
    }

    // ----------------------------------------------------------
    // TC3 — ACCESS DENIED
    // ----------------------------------------------------------
    @Test
    void getRequestById_accessDenied() {
        Student another = Student.builder().id(2L).build();
        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .student(another)
                .build();

        when(studentRepository.findByUserAccountId(10L)).thenReturn(Optional.of(student));
        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(req));

        assertThrows(BusinessRuleException.class,
                () -> service.getRequestById(100L, 10L));
    }

    // ----------------------------------------------------------
    // TC4 — TRANSFER REQUEST → map effectiveSession
    // ----------------------------------------------------------
    @Test
    void getRequestById_transferRequest_mapsEffectiveSession() {
        Session eff = Session.builder().id(300L).date(LocalDate.now()).build();
        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .student(student)
                .requestType(StudentRequestType.TRANSFER)
                .effectiveSession(eff)
                .status(RequestStatus.APPROVED)
                .build();

        when(studentRepository.findByUserAccountId(10L)).thenReturn(Optional.of(student));
        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(req));

        StudentRequestDetailDTO dto = service.getRequestById(100L, 10L);

        assertEquals(300L, dto.getTargetSession().getId());
    }

    // ----------------------------------------------------------
    // TC5 — MAKEUP REQUEST → map makeupSession
    // ----------------------------------------------------------
    @Test
    void getRequestById_makeupRequest_mapsMakeupSession() {
        Session target = Session.builder().id(200L).date(LocalDate.now()).build();
        Session makeup = Session.builder().id(300L).build();

        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .student(student)
                .requestType(StudentRequestType.MAKEUP)
                .targetSession(target)
                .makeupSession(makeup)
                .build();

        when(studentRepository.findByUserAccountId(10L)).thenReturn(Optional.of(student));
        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(req));

        StudentRequestDetailDTO dto = service.getRequestById(100L, 10L);

        assertEquals(300L, dto.getMakeupSession().getId());
    }

    // ----------------------------------------------------------
    // TC6 — ABSENCE REQUEST → map targetSession
    // ----------------------------------------------------------
    @Test
    void getRequestById_absenceRequest_mapsTargetSession() {
        Session target = Session.builder().id(555L).date(LocalDate.now()).build();

        StudentRequest req = StudentRequest.builder()
                .id(200L)
                .student(student)
                .requestType(StudentRequestType.ABSENCE)
                .targetSession(target)
                .build();

        when(studentRepository.findByUserAccountId(10L)).thenReturn(Optional.of(student));
        when(studentRequestRepository.findById(200L)).thenReturn(Optional.of(req));

        StudentRequestDetailDTO dto = service.getRequestById(200L, 10L);

        assertEquals(555L, dto.getTargetSession().getId());
    }

    // ----------------------------------------------------------
    // TC7 — submittedBy = null không gây lỗi
    // ----------------------------------------------------------
    @Test
    void getRequestById_nullSubmittedBy_isHandled() {
        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .student(student)
                .submittedBy(null)
                .requestType(StudentRequestType.ABSENCE)
                .build();

        when(studentRepository.findByUserAccountId(10L)).thenReturn(Optional.of(student));
        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(req));

        StudentRequestDetailDTO dto = service.getRequestById(100L, 10L);

        assertNull(dto.getSubmittedBy());
    }

    // ----------------------------------------------------------
    // TC8 — decidedBy = null không gây lỗi
    // ----------------------------------------------------------
    @Test
    void getRequestById_nullDecidedBy_isHandled() {
        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .student(student)
                .decidedBy(null)
                .requestType(StudentRequestType.ABSENCE)
                .build();

        when(studentRepository.findByUserAccountId(10L)).thenReturn(Optional.of(student));
        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(req));

        StudentRequestDetailDTO dto = service.getRequestById(100L, 10L);

        assertNull(dto.getDecidedBy());
    }

    // ----------------------------------------------------------
    // TC9 — REQUEST HAS TEACHING SLOT → Teacher mapping correct
    // ----------------------------------------------------------
    @Test
    void getRequestById_mapsTeacherCorrectly() {
        Teacher teacher = Teacher.builder()
                .id(50L)
                .userAccount(UserAccount.builder().id(999L).fullName("Mr Smith").email("t@fpt.edu.vn").build())
                .build();

        TeachingSlot slot = TeachingSlot.builder().teacher(teacher).build();

        Session sess = Session.builder()
                .id(777L)
                .date(LocalDate.now())
                .teachingSlots(Set.of(slot))
                .build();

        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .student(student)
                .requestType(StudentRequestType.ABSENCE)
                .targetSession(sess)
                .build();

        when(studentRepository.findByUserAccountId(10L)).thenReturn(Optional.of(student));
        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(req));

        StudentRequestDetailDTO dto = service.getRequestById(100L, 10L);

        assertNotNull(dto.getTargetSession().getTeacher());
        assertEquals(50L, dto.getTargetSession().getTeacher().getId());
    }

    // ----------------------------------------------------------
    // TC10 — FULL SUCCESS PATH
    // ----------------------------------------------------------
    @Test
    void getRequestById_success_fullMapping() {
        Session sess = Session.builder()
                .id(444L)
                .date(LocalDate.now())
                .status(SessionStatus.PLANNED)
                .build();

        StudentRequest req = StudentRequest.builder()
                .id(100L)
                .student(student)
                .requestType(StudentRequestType.ABSENCE)
                .targetSession(sess)
                .status(RequestStatus.APPROVED)
                .requestReason("Sick")
                .note("Test note")
                .build();

        when(studentRepository.findByUserAccountId(10L)).thenReturn(Optional.of(student));
        when(studentRequestRepository.findById(100L)).thenReturn(Optional.of(req));

        StudentRequestDetailDTO dto = service.getRequestById(100L, 10L);

        assertEquals(100L, dto.getId());
        assertEquals("ABSENCE", dto.getRequestType());
        assertEquals("APPROVED", dto.getStatus());
        assertEquals(444L, dto.getTargetSession().getId());
        assertEquals("Sick", dto.getRequestReason());
        assertEquals("Test note", dto.getNote());
    }
}
