package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.DisplayName("Teacher Request Swap Integration Tests")
class TeacherRequestSwapIT {

    @Autowired private TeacherRequestService teacherRequestService;
    @Autowired private TeacherRequestRepository teacherRequestRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private ClassRepository classRepository;
    @Autowired private TimeSlotTemplateRepository timeSlotTemplateRepository;
    @Autowired private CenterRepository centerRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private TeachingSlotRepository teachingSlotRepository;

    private UserAccount staff;
    private Teacher originalTeacher;
    private Teacher replacementTeacher;
    private Session session;
    private TimeSlotTemplate timeSlot;
    private ClassEntity classEntity;
    private String uniqueSuffix;

    @BeforeEach
    void setup() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        
        // Basic users
        staff = new UserAccount();
        staff.setEmail("staff+" + uniqueSuffix + "@test.com");
        staff.setFullName("Staff User");
        staff.setGender(Gender.MALE);
        staff.setStatus(UserStatus.ACTIVE);
        staff.setPasswordHash("x");
        staff = userAccountRepository.save(staff);

        UserAccount originalTeacherAccount = new UserAccount();
        originalTeacherAccount.setEmail("original+" + uniqueSuffix + "@test.com");
        originalTeacherAccount.setFullName("Original Teacher");
        originalTeacherAccount.setGender(Gender.MALE);
        originalTeacherAccount.setStatus(UserStatus.ACTIVE);
        originalTeacherAccount.setPasswordHash("x");
        originalTeacherAccount = userAccountRepository.save(originalTeacherAccount);

        originalTeacher = new Teacher();
        originalTeacher.setUserAccount(originalTeacherAccount);
        originalTeacher = teacherRepository.save(originalTeacher);

        UserAccount replacementTeacherAccount = new UserAccount();
        replacementTeacherAccount.setEmail("replacement+" + uniqueSuffix + "@test.com");
        replacementTeacherAccount.setFullName("Replacement Teacher");
        replacementTeacherAccount.setGender(Gender.MALE);
        replacementTeacherAccount.setStatus(UserStatus.ACTIVE);
        replacementTeacherAccount.setPasswordHash("x");
        replacementTeacherAccount = userAccountRepository.save(replacementTeacherAccount);

        replacementTeacher = new Teacher();
        replacementTeacher.setUserAccount(replacementTeacherAccount);
        replacementTeacher = teacherRepository.save(replacementTeacher);

        // Org structure
        Center center = new Center();
        center.setCode("CEN-" + uniqueSuffix);
        center.setName("Center 1");
        center = centerRepository.save(center);

        Branch branch = new Branch();
        branch.setCenter(center);
        branch.setCode("BR-" + uniqueSuffix);
        branch.setName("Branch 1");
        branch = branchRepository.save(branch);

        Subject subject = new Subject();
        subject.setCode("SUB-" + uniqueSuffix);
        subject.setName("Subject 1");
        subject = subjectRepository.save(subject);

        Course course = new Course();
        course.setSubject(subject);
        course.setCode("COURSE-" + uniqueSuffix);
        course.setName("Course 1");
        course = courseRepository.save(course);

        // Class
        classEntity = new ClassEntity();
        classEntity.setBranch(branch);
        classEntity.setCourse(course);
        classEntity.setCode("C-IT-" + uniqueSuffix);
        classEntity.setName("IT Class");
        classEntity.setModality(Modality.OFFLINE);
        classEntity.setStartDate(LocalDate.now());
        classEntity = classRepository.save(classEntity);

        // Time slot
        timeSlot = new TimeSlotTemplate();
        timeSlot.setBranch(branch);
        timeSlot.setName("Morning");
        timeSlot.setStartTime(java.time.LocalTime.of(8,0));
        timeSlot.setEndTime(java.time.LocalTime.of(10,0));
        timeSlot = timeSlotTemplateRepository.save(timeSlot);

        // Session
        session = new Session();
        session.setClassEntity(classEntity);
        session.setDate(LocalDate.now().plusDays(1));
        session.setStatus(SessionStatus.PLANNED);
        session.setTimeSlotTemplate(timeSlot);
        session.setCreatedAt(OffsetDateTime.now());
        session.setUpdatedAt(OffsetDateTime.now());
        session = sessionRepository.save(session);

        // Teaching slot for original teacher
        TeachingSlot teachingSlot = TeachingSlot.builder()
                .id(new TeachingSlot.TeachingSlotId(session.getId(), originalTeacher.getId()))
                .session(session)
                .teacher(originalTeacher)
                .status(TeachingSlotStatus.SCHEDULED)
                .build();
        teachingSlotRepository.save(teachingSlot);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Approve swap - sets WAITING_CONFIRM status")
    void approve_swap_sets_waiting_confirm_status() {
        // Create pending teacher request
        TeacherRequest req = TeacherRequest.builder()
                .teacher(originalTeacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.PENDING)
                .replacementTeacher(replacementTeacher)
                .submittedAt(OffsetDateTime.now())
                .build();
        req = teacherRequestRepository.save(req);

        // Approve
        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .note("Approve swap")
                .build();

        var resp = teacherRequestService.approveRequest(req.getId(), approve, staff.getId());

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.WAITING_CONFIRM);
        assertThat(resp.getReplacementTeacherId()).isEqualTo(replacementTeacher.getId());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Confirm swap - updates teaching slots")
    void confirm_swap_updates_teaching_slots() {
        // Create WAITING_CONFIRM request
        TeacherRequest req = TeacherRequest.builder()
                .teacher(originalTeacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.WAITING_CONFIRM)
                .replacementTeacher(replacementTeacher)
                .submittedAt(OffsetDateTime.now())
                .build();
        req = teacherRequestRepository.save(req);

        // Confirm by replacement teacher
        teacherRequestService.confirmSwap(req.getId(), replacementTeacher.getUserAccount().getId());

        // Assert: original teacher slot → ON_LEAVE
        TeachingSlot.TeachingSlotId originalSlotId = new TeachingSlot.TeachingSlotId(session.getId(), originalTeacher.getId());
        TeachingSlot originalSlot = teachingSlotRepository.findById(originalSlotId).orElseThrow();
        assertThat(originalSlot.getStatus()).isEqualTo(TeachingSlotStatus.ON_LEAVE);

        // Assert: replacement teacher slot → SUBSTITUTED
        TeachingSlot.TeachingSlotId replacementSlotId = new TeachingSlot.TeachingSlotId(session.getId(), replacementTeacher.getId());
        TeachingSlot replacementSlot = teachingSlotRepository.findById(replacementSlotId).orElseThrow();
        assertThat(replacementSlot.getStatus()).isEqualTo(TeachingSlotStatus.SUBSTITUTED);

        // Assert: request status → APPROVED
        TeacherRequest updatedReq = teacherRequestRepository.findById(req.getId()).orElseThrow();
        assertThat(updatedReq.getStatus()).isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Decline swap - resets to PENDING and clears replacement teacher")
    void decline_swap_resets_to_pending_and_clears_replacement_teacher() {
        // Create WAITING_CONFIRM request
        TeacherRequest req = TeacherRequest.builder()
                .teacher(originalTeacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.WAITING_CONFIRM)
                .replacementTeacher(replacementTeacher)
                .submittedAt(OffsetDateTime.now())
                .build();
        req = teacherRequestRepository.save(req);

        // Decline by replacement teacher
        teacherRequestService.declineSwap(req.getId(), "Cannot take over", replacementTeacher.getUserAccount().getId());

        // Assert: request status → PENDING
        TeacherRequest updatedReq = teacherRequestRepository.findById(req.getId()).orElseThrow();
        assertThat(updatedReq.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(updatedReq.getReplacementTeacher()).isNull(); // Cleared
        assertThat(updatedReq.getNote()).contains("DECLINED_BY_TEACHER_ID_" + replacementTeacher.getId());
    }
}

