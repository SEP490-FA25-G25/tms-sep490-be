package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.teacherrequest.ModalityResourceSuggestionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.DisplayName("TeacherRequestService Modality Unit Tests")
class TeacherRequestServiceImplModalityTest {

    @Autowired
    private TeacherRequestService service;

    @MockitoBean private TeacherRequestRepository teacherRequestRepository;
    @MockitoBean private TeacherRepository teacherRepository;
    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private ResourceRepository resourceRepository;
    @MockitoBean private SessionResourceRepository sessionResourceRepository;
    @MockitoBean private ClassRepository classRepository;
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
        return s;
    }

    private Branch mockBranch(Long id) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setCode("BR-" + id);
        branch.setName("Branch " + id);
        return branch;
    }

    private ClassEntity mockClass(Modality modality) {
        ClassEntity ce = new ClassEntity();
        ce.setId(111L);
        ce.setCode("C-001");
        ce.setModality(modality);
        ce.setBranch(mockBranch(1L));
        return ce;
    }

    private TimeSlotTemplate mockTimeSlot(Long id) {
        TimeSlotTemplate t = new TimeSlotTemplate();
        t.setId(id);
        t.setName("Morning");
        return t;
    }

    private Resource mockResource(Long id, ResourceType type) {
        Resource r = new Resource();
        r.setId(id);
        r.setName(type == ResourceType.VIRTUAL ? "Zoom-1" : "Room-101");
        r.setResourceType(type);
        r.setCapacity(50); // Set capacity for testing
        return r;
    }

    @Test
    @org.junit.jupiter.api.DisplayName("suggestModalityResources - returns compatible resources including current one")
    void suggestModalityResources_success() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.ONLINE); // needs ROOM resource
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        Branch branch = classEntity.getBranch();

        Resource currentResource = mockResource(41L, ResourceType.ROOM);
        currentResource.setBranch(branch);
        Resource availableResource = mockResource(42L, ResourceType.ROOM);
        availableResource.setBranch(branch);
        Resource wrongTypeResource = mockResource(43L, ResourceType.VIRTUAL);
        wrongTypeResource.setBranch(branch);
        Resource otherBranchResource = mockResource(44L, ResourceType.ROOM);
        otherBranchResource.setBranch(mockBranch(2L));

        SessionResource currentSessionResource = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(sessionId, currentResource.getId()))
                .session(session)
                .resource(currentResource)
                .build();

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(sessionResourceRepository.findBySessionId(sessionId)).thenReturn(java.util.List.of(currentSessionResource));
        when(resourceRepository.findAll()).thenReturn(java.util.List.of(currentResource, availableResource, wrongTypeResource, otherBranchResource));
        when(sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(anyLong(), any(), anyLong(), anyList(), eq(sessionId)))
                .thenReturn(false);
        when(studentSessionRepository.countBySessionId(sessionId)).thenReturn(10L);

        java.util.List<ModalityResourceSuggestionDTO> suggestions = service.suggestModalityResources(sessionId, userId);

        assertThat(suggestions)
                .extracting(ModalityResourceSuggestionDTO::getResourceId)
                .containsExactly(currentResource.getId(), availableResource.getId());
        assertThat(suggestions.get(0).isCurrentResource()).isTrue();
        assertThat(suggestions.get(1).isCurrentResource()).isFalse();
        assertThat(suggestions).allMatch(dto -> dto.getBranchId().equals(branch.getId()));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - modality change - success")
    void createRequest_modality_success() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;
        Long resourceId = 40L;

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.ONLINE); // ONLINE class
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));
        Resource resource = mockResource(resourceId, ResourceType.ROOM); // ONLINE class → ROOM resource (chuyển sang offline)

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(teacherRequestRepository.existsBySessionIdAndRequestTypeAndStatus(eq(sessionId), eq(TeacherRequestType.MODALITY_CHANGE), eq(RequestStatus.PENDING)))
                .thenReturn(false);
        UserAccount ua = new UserAccount(); ua.setId(userId);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(ua));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(eq(resourceId), any(), eq(timeSlot.getId()), anyList(), eq(sessionId)))
                .thenReturn(false);
        when(studentSessionRepository.countBySessionId(sessionId)).thenReturn(10L); // 10 students in session
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
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .newResourceId(resourceId)
                .reason("Need to switch to online")
                .build();

        TeacherRequestResponseDTO resp = service.createRequest(dto, userId);

        assertThat(resp.getId()).isEqualTo(999L);
        assertThat(resp.getRequestType()).isEqualTo(TeacherRequestType.MODALITY_CHANGE);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        verify(teacherRequestRepository).save(any(TeacherRequest.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - without newResourceId - success (teacher không chọn, staff sẽ chọn khi approve)")
    void createRequest_modality_withoutResource_success() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.ONLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(teacherRequestRepository.existsBySessionIdAndRequestTypeAndStatus(eq(sessionId), eq(TeacherRequestType.MODALITY_CHANGE), eq(RequestStatus.PENDING)))
                .thenReturn(false);
        UserAccount ua = new UserAccount(); ua.setId(userId);
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
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .reason("Need to switch")
                // newResourceId = null - teacher không chọn, staff sẽ chọn khi approve
                .build();

        TeacherRequestResponseDTO resp = service.createRequest(dto, userId);

        assertThat(resp.getId()).isEqualTo(999L);
        assertThat(resp.getRequestType()).isEqualTo(TeacherRequestType.MODALITY_CHANGE);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(resp.getNewResourceId()).isNull(); // Teacher không chọn resource
        verify(teacherRequestRepository).save(any(TeacherRequest.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("approveRequest - modality - teacher không chọn resource → throw error")
    void approveRequest_modality_teacherNotSelect_throwsInvalidInput() {
        Long staffId = 99L;
        Long requestId = 777L;
        Long sessionId = 30L;

        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        Teacher teacher = new Teacher(); teacher.setId(20L);
        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(teacher)
                .session(session)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .status(RequestStatus.PENDING)
                .newResource(null) // Teacher không chọn resource → bắt buộc phải chọn
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        UserAccount staff = new UserAccount(); staff.setId(staffId);
        when(userAccountRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .note("Staff approve")
                .build();

        assertThatThrownBy(() -> service.approveRequest(requestId, approve, staffId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("approveRequest - modality - teacher chọn, staff không override → dùng teacher chọn")
    void approveRequest_modality_teacherSelected_staffNotOverride_usesTeacherResource() {
        Long staffId = 99L;
        Long requestId = 777L;
        Long sessionId = 30L;
        Long teacherResourceId = 123L;

        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));
        Resource teacherResource = mockResource(teacherResourceId, ResourceType.VIRTUAL);

        Teacher teacher = new Teacher(); teacher.setId(20L);
        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(teacher)
                .session(session)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .status(RequestStatus.PENDING)
                .newResource(teacherResource) // Teacher đã chọn resource
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        UserAccount staff = new UserAccount(); staff.setId(staffId);
        when(userAccountRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        // Không cần mock resourceRepository.findById vì dùng trực tiếp request.getNewResource()
        when(sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(eq(teacherResourceId), any(), eq(timeSlot.getId()), anyList(), eq(sessionId)))
                .thenReturn(false);
        when(studentSessionRepository.countBySessionId(sessionId)).thenReturn(10L);
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .newResourceId(null) // Staff không override, dùng resource của teacher
                .note("ok")
                .build();

        TeacherRequestResponseDTO resp = service.approveRequest(requestId, approve, staffId);

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.APPROVED);
        verify(sessionResourceRepository).deleteBySessionId(sessionId);
        verify(sessionResourceRepository).save(any(SessionResource.class));
        verify(classRepository, never()).save(any(ClassEntity.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("approveRequest - modality - teacher chọn, staff override → dùng staff chọn")
    void approveRequest_modality_teacherSelected_staffOverride_usesStaffResource() {
        Long staffId = 99L;
        Long requestId = 777L;
        Long sessionId = 30L;
        Long teacherResourceId = 123L;
        Long staffResourceId = 456L;

        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));
        Resource teacherResource = mockResource(teacherResourceId, ResourceType.VIRTUAL);
        Resource staffResource = mockResource(staffResourceId, ResourceType.VIRTUAL);

        Teacher teacher = new Teacher(); teacher.setId(20L);
        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(teacher)
                .session(session)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .status(RequestStatus.PENDING)
                .newResource(teacherResource) // Teacher đã chọn resource
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        UserAccount staff = new UserAccount(); staff.setId(staffId);
        when(userAccountRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(resourceRepository.findById(staffResourceId)).thenReturn(Optional.of(staffResource));
        when(sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(eq(staffResourceId), any(), eq(timeSlot.getId()), anyList(), eq(sessionId)))
                .thenReturn(false);
        when(studentSessionRepository.countBySessionId(sessionId)).thenReturn(10L);
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .newResourceId(staffResourceId) // Staff override resource từ teacher
                .note("Resource của teacher bị conflict, chọn resource khác")
                .build();

        TeacherRequestResponseDTO resp = service.approveRequest(requestId, approve, staffId);

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.APPROVED);
        verify(resourceRepository).findById(staffResourceId); // Verify staff resource được dùng
        verify(sessionResourceRepository).deleteBySessionId(sessionId);
        verify(sessionResourceRepository).save(any(SessionResource.class));
        verify(classRepository, never()).save(any(ClassEntity.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("approveRequest - not pending - throws")
    void approveRequest_notPending_throws() {
        Long staffId = 99L;
        Long requestId = 888L;
        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(new Teacher())
                .status(RequestStatus.APPROVED)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .build();
        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));

        assertThatThrownBy(() -> service.approveRequest(requestId, TeacherRequestApproveDTO.builder().build(), staffId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("pending");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("rejectRequest - sets REJECTED and note")
    void rejectRequest_setsRejected() {
        Long staffId = 99L;
        Long requestId = 889L;
        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(new Teacher())
                .status(RequestStatus.PENDING)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .build();
        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        UserAccount staff = new UserAccount(); staff.setId(staffId);
        when(userAccountRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherRequestResponseDTO resp = service.rejectRequest(requestId, "not appropriate", staffId);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(resp.getNote()).contains("not appropriate");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - invalid resource type for modality - throws INVALID_RESOURCE_FOR_MODALITY")
    void createRequest_modality_invalidResourceType_throwsInvalidResourceForModality() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;
        Long resourceId = 40L;

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.OFFLINE); // OFFLINE class
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));
        Resource resource = mockResource(resourceId, ResourceType.ROOM); // ROOM resource - should be VIRTUAL for OFFLINE class

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(eq(resourceId), any(), eq(timeSlot.getId()), anyList(), eq(sessionId)))
                .thenReturn(false);

        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(sessionId)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .newResourceId(resourceId)
                .reason("Need to switch")
                .build();

        assertThatThrownBy(() -> service.createRequest(dto, userId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Invalid resource for modality change");
        verify(teacherRequestRepository, never()).save(any());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - insufficient resource capacity - throws RESOURCE_CAPACITY_INSUFFICIENT")
    void createRequest_modality_insufficientCapacity_throwsResourceCapacityInsufficient() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;
        Long resourceId = 40L;

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.ONLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));
        Resource resource = mockResource(resourceId, ResourceType.ROOM);
        resource.setCapacity(5); // Capacity = 5

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(eq(resourceId), any(), eq(timeSlot.getId()), anyList(), eq(sessionId)))
                .thenReturn(false);
        when(studentSessionRepository.countBySessionId(sessionId)).thenReturn(10L); // 10 students > capacity 5

        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(sessionId)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .newResourceId(resourceId)
                .reason("Need to switch")
                .build();

        assertThatThrownBy(() -> service.createRequest(dto, userId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Resource capacity is insufficient");
        verify(teacherRequestRepository, never()).save(any());
    }
}


