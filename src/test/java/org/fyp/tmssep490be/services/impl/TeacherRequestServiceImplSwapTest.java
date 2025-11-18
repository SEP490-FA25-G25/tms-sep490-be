package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.SwapCandidateDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.DisplayName("TeacherRequestService Swap Unit Tests")
class TeacherRequestServiceImplSwapTest {

    @Autowired
    private TeacherRequestService service;

    @MockitoBean private TeacherRequestRepository teacherRequestRepository;
    @MockitoBean private TeacherRepository teacherRepository;
    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private ResourceRepository resourceRepository;
    @MockitoBean private SessionResourceRepository sessionResourceRepository;
    @MockitoBean private TeachingSlotRepository teachingSlotRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private StudentSessionRepository studentSessionRepository;
    @MockitoBean private TimeSlotTemplateRepository timeSlotTemplateRepository;
    @MockitoBean private TeacherSkillRepository teacherSkillRepository;

    private Teacher mockTeacher(Long id, Long userId, String name) {
        Teacher t = new Teacher();
        t.setId(id);
        UserAccount ua = new UserAccount();
        ua.setId(userId);
        ua.setFullName(name);
        ua.setEmail(name.toLowerCase().replace(" ", ".") + "@test.com");
        t.setUserAccount(ua);
        return t;
    }

    private Session mockSession(Long id, ClassEntity classEntity, TimeSlotTemplate timeSlot, LocalDate date) {
        Session s = new Session();
        s.setId(id);
        s.setClassEntity(classEntity);
        s.setTimeSlotTemplate(timeSlot);
        s.setDate(date);
        s.setStatus(SessionStatus.PLANNED);
        s.setCreatedAt(OffsetDateTime.now());
        s.setUpdatedAt(OffsetDateTime.now());
        return s;
    }

    private ClassEntity mockClass(Modality modality) {
        ClassEntity ce = new ClassEntity();
        ce.setId(111L);
        ce.setCode("C-001");
        ce.setModality(modality);
        return ce;
    }

    private TimeSlotTemplate mockTimeSlot(Long id) {
        TimeSlotTemplate t = new TimeSlotTemplate();
        t.setId(id);
        t.setName("Morning");
        return t;
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - swap - success")
    void createRequest_swap_success() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;
        Long replacementTeacherId = 25L;

        Teacher teacher = mockTeacher(teacherId, userId, "Teacher One");
        Teacher replacementTeacher = mockTeacher(replacementTeacherId, 11L, "Teacher Two");
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(teacherRequestRepository.existsBySessionIdAndRequestTypeAndStatus(eq(sessionId), eq(TeacherRequestType.SWAP), eq(RequestStatus.PENDING)))
                .thenReturn(false);
        when(teacherRepository.findById(replacementTeacherId)).thenReturn(Optional.of(replacementTeacher));
        UserAccount ua = new UserAccount();
        ua.setId(userId);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(ua));
        java.util.concurrent.atomic.AtomicReference<TeacherRequest> savedRequest = new java.util.concurrent.atomic.AtomicReference<>();
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> {
            TeacherRequest tr = invocation.getArgument(0);
            tr.setId(999L);
            savedRequest.set(tr);
            return tr;
        });
        when(teacherRequestRepository.findByIdWithTeacherAndSession(999L)).thenAnswer(invocation -> {
            return Optional.ofNullable(savedRequest.get());
        });

        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(sessionId)
                .requestType(TeacherRequestType.SWAP)
                .replacementTeacherId(replacementTeacherId)
                .reason("Need to swap")
                .build();

        TeacherRequestResponseDTO resp = service.createRequest(dto, userId);

        assertThat(resp.getId()).isEqualTo(999L);
        assertThat(resp.getRequestType()).isEqualTo(TeacherRequestType.SWAP);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(resp.getReplacementTeacherId()).isEqualTo(replacementTeacherId);
        verify(teacherRequestRepository).save(any(TeacherRequest.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - swap - without replacementTeacherId - success")
    void createRequest_swap_withoutReplacementTeacher_success() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;

        Teacher teacher = mockTeacher(teacherId, userId, "Teacher One");
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(teacherRequestRepository.existsBySessionIdAndRequestTypeAndStatus(eq(sessionId), eq(TeacherRequestType.SWAP), eq(RequestStatus.PENDING)))
                .thenReturn(false);
        UserAccount ua = new UserAccount();
        ua.setId(userId);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(ua));
        java.util.concurrent.atomic.AtomicReference<TeacherRequest> savedRequest = new java.util.concurrent.atomic.AtomicReference<>();
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> {
            TeacherRequest tr = invocation.getArgument(0);
            tr.setId(999L);
            savedRequest.set(tr);
            return tr;
        });
        when(teacherRequestRepository.findByIdWithTeacherAndSession(999L)).thenAnswer(invocation -> {
            return Optional.ofNullable(savedRequest.get());
        });

        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(sessionId)
                .requestType(TeacherRequestType.SWAP)
                .reason("Need to swap")
                // replacementTeacherId = null - teacher không chọn, staff sẽ chọn khi approve
                .build();

        TeacherRequestResponseDTO resp = service.createRequest(dto, userId);

        assertThat(resp.getId()).isEqualTo(999L);
        assertThat(resp.getRequestType()).isEqualTo(TeacherRequestType.SWAP);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(resp.getReplacementTeacherId()).isNull(); // Teacher không chọn replacement teacher
        verify(teacherRequestRepository).save(any(TeacherRequest.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("approveRequest - swap - teacher chọn, staff không override → dùng teacher chọn")
    void approveRequest_swap_teacherSelected_staffNotOverride_usesTeacherChoice() {
        Long staffId = 99L;
        Long requestId = 777L;
        Long sessionId = 30L;
        Long replacementTeacherId = 25L;

        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));
        Teacher originalTeacher = mockTeacher(20L, 10L, "Original Teacher");
        Teacher replacementTeacher = mockTeacher(replacementTeacherId, 11L, "Replacement Teacher");

        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(originalTeacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.PENDING)
                .replacementTeacher(replacementTeacher) // Teacher đã chọn replacement teacher
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        UserAccount staff = new UserAccount();
        staff.setId(staffId);
        when(userAccountRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .replacementTeacherId(null) // Staff không override, dùng replacement teacher của teacher
                .note("ok")
                .build();

        TeacherRequestResponseDTO resp = service.approveRequest(requestId, approve, staffId);

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.WAITING_CONFIRM);
        assertThat(resp.getReplacementTeacherId()).isEqualTo(replacementTeacherId);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("approveRequest - swap - teacher không chọn, staff phải chọn → throw error nếu staff không chọn")
    void approveRequest_swap_teacherNotSelect_staffMustSelect_throwsIfStaffNotSelect() {
        Long staffId = 99L;
        Long requestId = 777L;
        Long sessionId = 30L;

        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));
        Teacher originalTeacher = mockTeacher(20L, 10L, "Original Teacher");

        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(originalTeacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.PENDING)
                .replacementTeacher(null) // Teacher không chọn replacement teacher
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        UserAccount staff = new UserAccount();
        staff.setId(staffId);
        when(userAccountRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .replacementTeacherId(null) // Staff cũng không chọn → throw error
                .note("ok")
                .build();

        assertThatThrownBy(() -> service.approveRequest(requestId, approve, staffId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("suggestSwapCandidates - success")
    void suggestSwapCandidates_success() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;

        Teacher currentTeacher = mockTeacher(teacherId, userId, "Current Teacher");
        Teacher candidate1 = mockTeacher(25L, 11L, "Candidate One");
        Teacher candidate2 = mockTeacher(26L, 12L, "Candidate Two");
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(currentTeacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(teacherRepository.findAll()).thenReturn(List.of(currentTeacher, candidate1, candidate2));
        when(teacherSkillRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        when(teachingSlotRepository.findAll()).thenReturn(java.util.Collections.emptyList());

        List<SwapCandidateDTO> candidates = service.suggestSwapCandidates(sessionId, userId);

        assertThat(candidates).hasSize(2); // Exclude current teacher
        assertThat(candidates).extracting("teacherId").containsExactlyInAnyOrder(25L, 26L);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("confirmSwap - success")
    void confirmSwap_success() {
        Long replacementUserId = 11L;
        Long requestId = 777L;
        Long sessionId = 30L;
        Long originalTeacherId = 20L;
        Long replacementTeacherId = 25L;

        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));
        Teacher originalTeacher = mockTeacher(originalTeacherId, 10L, "Original Teacher");
        Teacher replacementTeacher = mockTeacher(replacementTeacherId, replacementUserId, "Replacement Teacher");

        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(originalTeacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.WAITING_CONFIRM)
                .replacementTeacher(replacementTeacher)
                .build();

        TeachingSlot.TeachingSlotId originalSlotId = new TeachingSlot.TeachingSlotId(sessionId, originalTeacherId);
        TeachingSlot originalSlot = TeachingSlot.builder()
                .id(originalSlotId)
                .session(session)
                .teacher(originalTeacher)
                .status(TeachingSlotStatus.SCHEDULED)
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        when(teacherRepository.findByUserAccountId(replacementUserId)).thenReturn(Optional.of(replacementTeacher));
        when(teachingSlotRepository.findById(originalSlotId)).thenReturn(Optional.of(originalSlot));
        // Replacement slot doesn't exist yet, will be created
        TeachingSlot.TeachingSlotId replacementSlotId = new TeachingSlot.TeachingSlotId(sessionId, replacementTeacherId);
        when(teachingSlotRepository.findById(replacementSlotId)).thenReturn(Optional.empty());
        when(teachingSlotRepository.save(any(TeachingSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequestResponseDTO resp = service.confirmSwap(requestId, replacementUserId);

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.APPROVED);
        // Verify save called 2 times: once for originalSlot (ON_LEAVE), once for replacementSlot (SUBSTITUTED)
        verify(teachingSlotRepository, times(2)).save(any(TeachingSlot.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("declineSwap - success")
    void declineSwap_success() {
        Long replacementUserId = 11L;
        Long requestId = 777L;
        Long sessionId = 30L;
        Long replacementTeacherId = 25L;

        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));
        Teacher originalTeacher = mockTeacher(20L, 10L, "Original Teacher");
        Teacher replacementTeacher = mockTeacher(replacementTeacherId, replacementUserId, "Replacement Teacher");

        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(originalTeacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.WAITING_CONFIRM)
                .replacementTeacher(replacementTeacher)
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        when(teacherRepository.findByUserAccountId(replacementUserId)).thenReturn(Optional.of(replacementTeacher));
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequestResponseDTO resp = service.declineSwap(requestId, "Cannot take over", replacementUserId);

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(resp.getReplacementTeacherId()).isNull(); // Cleared after decline
        assertThat(resp.getNote()).contains("DECLINED_BY_TEACHER_ID_" + replacementTeacherId);
    }
}

