package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.DisplayName("TeacherRequestService Reschedule Unit Tests")
class TeacherRequestServiceImplRescheduleTest {

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

    private Teacher mockTeacher(Long id, Long userId) {
        Teacher t = new Teacher();
        t.setId(id);
        UserAccount ua = new UserAccount();
        ua.setId(userId);
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

    private TimeSlotTemplate mockTimeSlot(Long id, String name) {
        TimeSlotTemplate t = new TimeSlotTemplate();
        t.setId(id);
        t.setName(name != null ? name : "TimeSlot-" + id);
        t.setStartTime(LocalTime.of(8, 0)); // Default start time 08:00
        t.setEndTime(LocalTime.of(10, 0)); // Default end time 10:00
        return t;
    }

    private Resource mockResource(Long id, ResourceType type) {
        Resource r = new Resource();
        r.setId(id);
        r.setName(type == ResourceType.VIRTUAL ? "Zoom-1" : "Room-101");
        r.setResourceType(type);
        r.setCapacity(50);
        return r;
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - reschedule - success")
    void createRequest_reschedule_success() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;
        Long newTimeSlotId = 5L;
        Long newResourceId = 40L;
        LocalDate newDate = LocalDate.now().plusDays(3);

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate oldTimeSlot = mockTimeSlot(3L, "OldSlot");
        TimeSlotTemplate newTimeSlot = mockTimeSlot(newTimeSlotId, "NewSlot");
        Session session = mockSession(sessionId, classEntity, oldTimeSlot, LocalDate.now().plusDays(2));
        Resource resource = mockResource(newResourceId, ResourceType.ROOM);

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(teacherRequestRepository.existsBySessionIdAndRequestTypeAndStatus(eq(sessionId), eq(TeacherRequestType.RESCHEDULE), eq(RequestStatus.PENDING)))
                .thenReturn(false);
        when(timeSlotTemplateRepository.findById(newTimeSlotId)).thenReturn(Optional.of(newTimeSlot));
        when(resourceRepository.findById(newResourceId)).thenReturn(Optional.of(resource));
        when(teachingSlotRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        when(sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(eq(newResourceId), eq(newDate), eq(newTimeSlotId), anyList(), isNull()))
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
                .requestType(TeacherRequestType.RESCHEDULE)
                .newDate(newDate)
                .newTimeSlotId(newTimeSlotId)
                .newResourceId(newResourceId)
                .reason("Need to reschedule")
                .build();

        TeacherRequestResponseDTO resp = service.createRequest(dto, userId);

        assertThat(resp.getId()).isEqualTo(999L);
        assertThat(resp.getRequestType()).isEqualTo(TeacherRequestType.RESCHEDULE);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        verify(teacherRequestRepository).save(any(TeacherRequest.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("approveRequest - reschedule - success")
    void approveRequest_reschedule_success() {
        Long staffId = 99L;
        Long requestId = 777L;
        Long sessionId = 30L;
        Long newTimeSlotId = 5L;
        Long newResourceId = 40L;
        LocalDate newDate = LocalDate.now().plusDays(3);

        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate oldTimeSlot = mockTimeSlot(3L, "OldSlot");
        TimeSlotTemplate newTimeSlot = mockTimeSlot(newTimeSlotId, "NewSlot");
        Session oldSession = mockSession(sessionId, classEntity, oldTimeSlot, LocalDate.now().plusDays(2));
        Resource resource = mockResource(newResourceId, ResourceType.ROOM);

        Teacher teacher = new Teacher();
        teacher.setId(20L);
        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(teacher)
                .session(oldSession)
                .requestType(TeacherRequestType.RESCHEDULE)
                .status(RequestStatus.PENDING)
                .newDate(newDate)
                .newTimeSlot(newTimeSlot)
                .newResource(resource)
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        UserAccount staff = new UserAccount();
        staff.setId(staffId);
        when(userAccountRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(oldSession));
        when(timeSlotTemplateRepository.findById(newTimeSlotId)).thenReturn(Optional.of(newTimeSlot));
        // Mock resourceRepository.findById with anyLong() to handle any resource ID lookup
        when(resourceRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            if (id.equals(newResourceId)) {
                return Optional.of(resource);
            }
            return Optional.empty();
        });
        when(teachingSlotRepository.existsById(any())).thenReturn(true);
        when(teachingSlotRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        when(sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(eq(newResourceId), eq(newDate), eq(newTimeSlotId), anyList(), isNull()))
                .thenReturn(false);
        when(studentSessionRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teachingSlotRepository.save(any(TeachingSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentSessionRepository.save(any(StudentSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionResourceRepository.save(any(SessionResource.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .note("Approve reschedule")
                .build();

        TeacherRequestResponseDTO resp = service.approveRequest(requestId, approve, staffId);

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.APPROVED);
        // Verify save called 2 times: once for newSession (PLANNED), once for oldSession (CANCELLED)
        verify(sessionRepository, times(2)).save(any(Session.class));
        verify(sessionRepository).save(oldSession); // Old session cancelled
    }
}

