package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test for createRequestForTeacherByStaff()
 */
@ExtendWith(MockitoExtension.class)
class TeacherRequestService_CreateRequest_Test {

    @InjectMocks
    private TeacherRequestService service;

    @Mock private TeacherRepository teacherRepository;
    @Mock private TeacherRequestRepository teacherRequestRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private UserBranchesRepository userBranchesRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private TimeSlotTemplateRepository timeSlotTemplateRepository;
    @Mock private SessionResourceRepository sessionResourceRepository;
    @Mock private TeachingSlotRepository teachingSlotRepository;

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private UserAccount mockUser(Long id) {
        UserAccount ua = new UserAccount();
        ua.setId(id);
        ua.setFullName("User " + id);
        ua.setEmail("u" + id + "@mail.com");
        return ua;
    }

    private Teacher mockTeacher(Long teacherId, Long userAccountId) {
        Teacher t = new Teacher();
        t.setId(teacherId);
        t.setUserAccount(mockUser(userAccountId));
        t.setEmployeeCode("T-" + teacherId);
        return t;
    }

    private ClassEntity mockClassEntity(Long branchId) {
        Branch b = new Branch();
        b.setId(branchId);

        ClassEntity c = new ClassEntity();
        c.setId(100L);
        c.setBranch(b);
        c.setCode("C100");
        c.setName("Class 100");
        c.setModality(Modality.OFFLINE);
        c.setMaxCapacity(30);
        return c;
    }

    private TimeSlotTemplate mockTimeSlot(Long id) {
        TimeSlotTemplate ts = new TimeSlotTemplate();
        ts.setId(id);
        ts.setName("Slot " + id);
        ts.setStartTime(LocalTime.of(8, 0));
        ts.setEndTime(LocalTime.of(10, 0));
        return ts;
    }

    private Session mockSession(Long sessionId, Teacher teacher, Long branchId) {
        Session s = new Session();
        s.setId(sessionId);
        s.setDate(LocalDate.now().plusDays(1));
        s.setStatus(SessionStatus.PLANNED);
        s.setTimeSlotTemplate(mockTimeSlot(1L));
        s.setClassEntity(mockClassEntity(branchId));

        // Assign teacher into Session teaching slots
        TeachingSlot slot = new TeachingSlot();
        TeachingSlot.TeachingSlotId slotId = new TeachingSlot.TeachingSlotId();
        slotId.setSessionId(sessionId);
        slotId.setTeacherId(teacher.getId());
        slot.setId(slotId);
        slot.setTeacher(teacher);
        slot.setSession(s);
        slot.setStatus(TeachingSlotStatus.SCHEDULED);

        Set<TeachingSlot> slots = new HashSet<>();
        slots.add(slot);
        s.setTeachingSlots(slots);

        return s;
    }

    private TeacherRequest mockSavedRequest(Long id, Teacher teacher, Session session, TeacherRequestType type) {
        TeacherRequest r = new TeacherRequest();
        r.setId(id);
        r.setTeacher(teacher);
        r.setSession(session);
        r.setRequestType(type);
        r.setStatus(RequestStatus.PENDING);
        r.setSubmittedAt(OffsetDateTime.now());
        return r;
    }

    private Resource mockResource(Long id, Long branchId) {
        Branch b = new Branch();
        b.setId(branchId);

        Resource r = new Resource();
        r.setId(id);
        r.setBranch(b);
        r.setResourceType(ResourceType.ROOM); // enum giả định có ROOM
        r.setCode("R" + id);
        r.setName("Resource " + id);
        r.setStatus(ResourceStatus.ACTIVE);
        return r;
    }

    private UserAccount mockUserAccount(Long id) {
        UserAccount ua = new UserAccount();
        ua.setId(id);
        ua.setFullName("Staff " + id);
        ua.setEmail("staff" + id + "@mail.com");
        return ua;
    }

    // Mock branch list for a user
    private void mockUserBranches(Long userId, Long... branchIds) {
        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(Arrays.asList(branchIds));
    }

    // ===================================================================================
    //                                      TEST CASES
    // ===================================================================================

    /**
     * TC01 — Academic staff không có branch → FORBIDDEN
     */
    @Test
    void testCreateRequest_staffHasNoBranches_forbidden() {
        TeacherRequestCreateDTO dto = new TeacherRequestCreateDTO();
        dto.setTeacherId(1L);
        dto.setRequestType(TeacherRequestType.MODALITY_CHANGE);
        dto.setReason("Valid reason 1");

        mockUserBranches(100L); // empty

        CustomException ex = assertThrows(CustomException.class,
                () -> service.createRequestForTeacherByStaff(dto, 100L));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    /**
     * TC02 — Teacher không tồn tại → TEACHER_NOT_FOUND
     */
    @Test
    void testCreateRequest_teacherNotFound() {
        TeacherRequestCreateDTO dto = new TeacherRequestCreateDTO();
        dto.setTeacherId(1L);
        dto.setRequestType(TeacherRequestType.MODALITY_CHANGE);
        dto.setReason("Valid reason 2");

        mockUserBranches(200L, 1L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.createRequestForTeacherByStaff(dto, 200L));

        assertEquals(ErrorCode.TEACHER_NOT_FOUND, ex.getErrorCode());
    }

    /**
     * TC03 — Teacher tồn tại nhưng teacher.getUserAccount() == null → TEACHER_NOT_FOUND
     */
    @Test
    void testCreateRequest_teacherHasNoUserAccount() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setUserAccount(null);

        TeacherRequestCreateDTO dto = new TeacherRequestCreateDTO();
        dto.setTeacherId(1L);
        dto.setRequestType(TeacherRequestType.MODALITY_CHANGE);
        dto.setReason("Valid reason 3");

        mockUserBranches(200L, 1L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.createRequestForTeacherByStaff(dto, 200L));

        assertEquals(ErrorCode.TEACHER_NOT_FOUND, ex.getErrorCode());
    }

    /**
     * TC04 — Academic staff branch không trùng teacher branch → FORBIDDEN
     */
    @Test
    void testCreateRequest_branchNotMatch_forbidden() {
        Teacher teacher = mockTeacher(1L, 2L);
        TeacherRequestCreateDTO dto = new TeacherRequestCreateDTO();
        dto.setTeacherId(1L);
        dto.setRequestType(TeacherRequestType.MODALITY_CHANGE);
        dto.setReason("Valid reason 4");

        mockUserBranches(200L, 10L); // staff branch = 10
        mockUserBranches(2L, 99L);   // teacher branch = 99

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.createRequestForTeacherByStaff(dto, 200L));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    /**
     * TC05 — Reason quá ngắn → INVALID_INPUT
     */
    @Test
    void testCreateRequest_reasonTooShort() {
        Teacher teacher = mockTeacher(1L, 2L);
        TeacherRequestCreateDTO dto = new TeacherRequestCreateDTO();
        dto.setTeacherId(1L);
        dto.setRequestType(TeacherRequestType.MODALITY_CHANGE);
        dto.setReason("short"); // < 10 chars

        mockUserBranches(200L, 1L);
        mockUserBranches(2L, 1L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.createRequestForTeacherByStaff(dto, 200L));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    /**
     * TC06 — Session không tồn tại → INVALID_INPUT
     */
    @Test
    void testCreateRequest_sessionNotFound() {
        Teacher teacher = mockTeacher(1L, 2L);
        TeacherRequestCreateDTO dto = new TeacherRequestCreateDTO();
        dto.setTeacherId(1L);
        dto.setSessionId(50L);
        dto.setRequestType(TeacherRequestType.MODALITY_CHANGE);
        dto.setReason("Valid reason 6");

        mockUserBranches(200L, 1L);
        mockUserBranches(2L, 1L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(50L)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.createRequestForTeacherByStaff(dto, 200L));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    /**
     * TC07 — Teacher không thuộc session → FORBIDDEN
     */
    @Test
    void testCreateRequest_teacherNotOwner() {
        Teacher teacher = mockTeacher(1L, 2L);
        // Session thuộc teacher khác
        Session session = mockSession(10L, mockTeacher(999L, 888L), 1L);

        TeacherRequestCreateDTO dto = new TeacherRequestCreateDTO();
        dto.setTeacherId(1L);
        dto.setSessionId(10L);
        dto.setRequestType(TeacherRequestType.MODALITY_CHANGE);
        dto.setReason("Valid reason 7");

        mockUserBranches(200L, 1L);
        mockUserBranches(2L, 1L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.createRequestForTeacherByStaff(dto, 200L));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    /**
     * TC08 — Session date trong quá khứ → INVALID_INPUT
     */
    @Test
    void testCreateRequest_sessionInPast() {
        Teacher teacher = mockTeacher(1L, 2L);

        Session session = mockSession(10L, teacher, 1L);
        session.setDate(LocalDate.now().minusDays(1));

        TeacherRequestCreateDTO dto = new TeacherRequestCreateDTO();
        dto.setTeacherId(1L);
        dto.setSessionId(10L);
        dto.setRequestType(TeacherRequestType.MODALITY_CHANGE);
        dto.setReason("Valid reason 8");

        mockUserBranches(200L, 1L);
        mockUserBranches(2L, 1L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.createRequestForTeacherByStaff(dto, 200L));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    /**
     * TC09 — Session status not PLANNED → INVALID_INPUT
     */
    @Test
    void testCreateRequest_sessionNotPlanned() {
        Teacher teacher = mockTeacher(1L, 2L);

        Session session = mockSession(10L, teacher, 1L);
        session.setStatus(SessionStatus.DONE);

        TeacherRequestCreateDTO dto = new TeacherRequestCreateDTO();
        dto.setTeacherId(1L);
        dto.setSessionId(10L);
        dto.setRequestType(TeacherRequestType.MODALITY_CHANGE);
        dto.setReason("Valid reason 9");

        mockUserBranches(200L, 1L);
        mockUserBranches(2L, 1L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.createRequestForTeacherByStaff(dto, 200L));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    /**
     * TC10 — MODALITY_CHANGE nhưng không có newResourceId → INVALID_INPUT
     */
    @Test
    void testCreateRequest_modalityNoResource_invalid() {
        Teacher teacher = mockTeacher(1L, 2L);
        Session session = mockSession(10L, teacher, 1L);

        TeacherRequestCreateDTO dto = new TeacherRequestCreateDTO();
        dto.setTeacherId(1L);
        dto.setSessionId(10L);
        dto.setRequestType(TeacherRequestType.MODALITY_CHANGE);
        dto.setReason("Valid reason 10");
        dto.setNewResourceId(null); // missing

        mockUserBranches(200L, 1L);
        mockUserBranches(2L, 1L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        lenient().when(userAccountRepository.findById(200L))
                .thenReturn(Optional.of(mockUser(200L)));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.createRequestForTeacherByStaff(dto, 200L));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    /**
     * TC11 — REPLACEMENT nhưng không có replacementTeacherId → INVALID_INPUT
     */
    @Test
    void testCreateRequest_replacementMissingTeacher() {
        Teacher teacher = mockTeacher(1L, 2L);
        Session session = mockSession(10L, teacher, 1L);

        TeacherRequestCreateDTO dto = new TeacherRequestCreateDTO();
        dto.setTeacherId(1L);
        dto.setSessionId(10L);
        dto.setRequestType(TeacherRequestType.REPLACEMENT);
        dto.setReason("Valid reason 11");
        dto.setReplacementTeacherId(null); // missing

        mockUserBranches(200L, 1L);
        mockUserBranches(2L, 1L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        lenient().when(userAccountRepository.findById(200L))
                .thenReturn(Optional.of(mockUser(200L)));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.createRequestForTeacherByStaff(dto, 200L));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }
}
