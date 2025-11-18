package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.CourseSession;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TeacherRequestService staff view tests")
class TeacherRequestServiceImplStaffViewTest {

    @Autowired
    private TeacherRequestService teacherRequestService;

    @MockitoBean private TeacherRequestRepository teacherRequestRepository;
    @MockitoBean private TeacherRepository teacherRepository;
    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private ResourceRepository resourceRepository;
    @MockitoBean private SessionResourceRepository sessionResourceRepository;
    @MockitoBean private TeachingSlotRepository teachingSlotRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private TimeSlotTemplateRepository timeSlotTemplateRepository;
    @MockitoBean private StudentSessionRepository studentSessionRepository;

    @Test
    void getPendingRequestsForStaff_shouldReturnTeacherAndClassInfo() {
        TeacherRequest request = mockRequest(RequestStatus.PENDING);
        when(teacherRequestRepository.findByStatusOrderBySubmittedAtDesc(RequestStatus.PENDING))
                .thenReturn(List.of(request));

        List<TeacherRequestListDTO> result = teacherRequestService.getPendingRequestsForStaff();

        assertThat(result).hasSize(1);
        TeacherRequestListDTO dto = result.get(0);
        assertThat(dto.getTeacherName()).isEqualTo("Teacher One");
        assertThat(dto.getTeacherEmail()).isEqualTo("teacher1@tms.test");
        assertThat(dto.getClassCode()).isEqualTo("CLS-001");
        assertThat(dto.getSessionDate()).isEqualTo(request.getSession().getDate());
        assertThat(dto.getSessionStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(dto.getSessionEndTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(dto.getSessionTopic()).isEqualTo("Topic 01");
        assertThat(dto.getReplacementTeacherName()).isEqualTo("Teacher Two");
        assertThat(dto.getNewSessionDate()).isEqualTo(request.getNewDate());
        assertThat(dto.getNewSessionStartTime()).isEqualTo(LocalTime.of(13, 0));
        assertThat(dto.getNewSessionEndTime()).isEqualTo(LocalTime.of(15, 0));

        verify(teacherRequestRepository).findByStatusOrderBySubmittedAtDesc(RequestStatus.PENDING);
        verify(teacherRequestRepository, never()).findAllByOrderBySubmittedAtDesc();
    }

    @Test
    void getRequestsForStaff_withoutStatus_shouldFallbackToAll() {
        TeacherRequest request = mockRequest(RequestStatus.APPROVED);
        when(teacherRequestRepository.findAllByOrderBySubmittedAtDesc())
                .thenReturn(List.of(request));

        List<TeacherRequestListDTO> result = teacherRequestService.getRequestsForStaff(null);

        assertThat(result).hasSize(1);
        TeacherRequestListDTO dto = result.get(0);
        assertThat(dto.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(dto.getTeacherId()).isEqualTo(request.getTeacher().getId());

        verify(teacherRequestRepository).findAllByOrderBySubmittedAtDesc();
        verify(teacherRequestRepository, never()).findByStatusOrderBySubmittedAtDesc(any());
    }

    @Test
    void getRequestForStaff_withApprovedStatus_shouldReturnDecidedByInfo() {
        TeacherRequest request = mockRequestWithDecidedBy(RequestStatus.APPROVED);
        when(teacherRequestRepository.findByIdWithTeacherAndSession(99L))
                .thenReturn(Optional.of(request));

        TeacherRequestResponseDTO response = teacherRequestService.getRequestForStaff(99L);

        assertThat(response.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(response.getDecidedById()).isEqualTo(100L);
        assertThat(response.getDecidedByName()).isEqualTo("Staff Approver");
        assertThat(response.getDecidedByEmail()).isEqualTo("staff@tms.test");
        assertThat(response.getDecidedAt()).isNotNull();

        verify(teacherRequestRepository).findByIdWithTeacherAndSession(99L);
    }

    @Test
    void getRequestForStaff_shouldReturnDetailedInfo() {
        TeacherRequest request = mockRequest(RequestStatus.PENDING);
        when(teacherRequestRepository.findByIdWithTeacherAndSession(99L))
                .thenReturn(Optional.of(request));

        TeacherRequestResponseDTO response = teacherRequestService.getRequestForStaff(99L);

        assertThat(response.getId()).isEqualTo(request.getId());
        assertThat(response.getTeacherId()).isEqualTo(request.getTeacher().getId());
        assertThat(response.getTeacherName()).isEqualTo("Teacher One");
        assertThat(response.getClassCode()).isEqualTo("CLS-001");
        assertThat(response.getSessionDate()).isEqualTo(request.getSession().getDate());
        assertThat(response.getSessionStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.getSessionEndTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(response.getSessionTopic()).isEqualTo("Topic 01");

        verify(teacherRequestRepository).findByIdWithTeacherAndSession(99L);
    }

    private TeacherRequest mockRequest(RequestStatus status) {
        UserAccount teacherAccount = new UserAccount();
        teacherAccount.setId(10L);
        teacherAccount.setEmail("teacher1@tms.test");
        teacherAccount.setFullName("Teacher One");

        Teacher teacher = new Teacher();
        teacher.setId(20L);
        teacher.setUserAccount(teacherAccount);

        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(30L);
        classEntity.setCode("CLS-001");
        classEntity.setName("Class A");
        classEntity.setModality(Modality.OFFLINE);

        TimeSlotTemplate timeSlot = new TimeSlotTemplate();
        timeSlot.setId(40L);
        timeSlot.setName("Morning");
        timeSlot.setStartTime(LocalTime.of(8, 0));
        timeSlot.setEndTime(LocalTime.of(10, 0));

        CourseSession courseSession = new CourseSession();
        courseSession.setId(70L);
        courseSession.setTopic("Topic 01");

        Session session = new Session();
        session.setId(50L);
        session.setClassEntity(classEntity);
        session.setTimeSlotTemplate(timeSlot);
        session.setCourseSession(courseSession);
        session.setStatus(SessionStatus.PLANNED);
        session.setDate(LocalDate.now().plusDays(1));

        UserAccount replacementAccount = new UserAccount();
        replacementAccount.setId(11L);
        replacementAccount.setEmail("teacher2@tms.test");
        replacementAccount.setFullName("Teacher Two");

        Teacher replacementTeacher = new Teacher();
        replacementTeacher.setId(21L);
        replacementTeacher.setUserAccount(replacementAccount);

        TimeSlotTemplate newTimeSlot = new TimeSlotTemplate();
        newTimeSlot.setId(41L);
        newTimeSlot.setName("Afternoon");
        newTimeSlot.setStartTime(LocalTime.of(13, 0));
        newTimeSlot.setEndTime(LocalTime.of(15, 0));

        return TeacherRequest.builder()
                .id(60L)
                .teacher(teacher)
                .session(session)
                .requestType(TeacherRequestType.RESCHEDULE)
                .status(status)
                .submittedAt(OffsetDateTime.now().minusHours(1))
                .requestReason("Need to adjust schedule")
                .replacementTeacher(replacementTeacher)
                .newDate(LocalDate.now().plusDays(3))
                .newTimeSlot(newTimeSlot)
                .build();
    }

    private TeacherRequest mockRequestWithDecidedBy(RequestStatus status) {
        TeacherRequest request = mockRequest(status);
        
        UserAccount decidedByAccount = new UserAccount();
        decidedByAccount.setId(100L);
        decidedByAccount.setEmail("staff@tms.test");
        decidedByAccount.setFullName("Staff Approver");
        
        request.setDecidedBy(decidedByAccount);
        request.setDecidedAt(OffsetDateTime.now().minusMinutes(30));
        
        return request;
    }
}


