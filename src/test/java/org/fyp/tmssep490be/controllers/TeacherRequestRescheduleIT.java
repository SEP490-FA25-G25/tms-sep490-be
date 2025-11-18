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
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.DisplayName("Teacher Request Reschedule Integration Tests")
class TeacherRequestRescheduleIT {

    @Autowired private TeacherRequestService teacherRequestService;
    @Autowired private TeacherRequestRepository teacherRequestRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private SessionResourceRepository sessionResourceRepository;
    @Autowired private ClassRepository classRepository;
    @Autowired private TimeSlotTemplateRepository timeSlotTemplateRepository;
    @Autowired private CenterRepository centerRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private TeachingSlotRepository teachingSlotRepository;

    private UserAccount staff;
    private Teacher teacher;
    private Session oldSession;
    private TimeSlotTemplate oldTimeSlot;
    private TimeSlotTemplate newTimeSlot;
    private ClassEntity classEntity;
    private Resource oldResource;
    private Resource newResource;
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

        UserAccount teacherAccount = new UserAccount();
        teacherAccount.setEmail("teacher+" + uniqueSuffix + "@test.com");
        teacherAccount.setFullName("Teacher User");
        teacherAccount.setGender(Gender.MALE);
        teacherAccount.setStatus(UserStatus.ACTIVE);
        teacherAccount.setPasswordHash("x");
        teacherAccount = userAccountRepository.save(teacherAccount);

        teacher = new Teacher();
        teacher.setUserAccount(teacherAccount);
        teacher = teacherRepository.save(teacher);

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

        // Time slots
        oldTimeSlot = new TimeSlotTemplate();
        oldTimeSlot.setBranch(branch);
        oldTimeSlot.setName("Morning");
        oldTimeSlot.setStartTime(java.time.LocalTime.of(8,0));
        oldTimeSlot.setEndTime(java.time.LocalTime.of(10,0));
        oldTimeSlot = timeSlotTemplateRepository.save(oldTimeSlot);

        newTimeSlot = new TimeSlotTemplate();
        newTimeSlot.setBranch(branch);
        newTimeSlot.setName("Afternoon");
        newTimeSlot.setStartTime(java.time.LocalTime.of(14,0));
        newTimeSlot.setEndTime(java.time.LocalTime.of(16,0));
        newTimeSlot = timeSlotTemplateRepository.save(newTimeSlot);

        // Resources
        oldResource = new Resource();
        oldResource.setBranch(branch);
        oldResource.setCode("ROOM-" + uniqueSuffix);
        oldResource.setName("Room-1");
        oldResource.setResourceType(ResourceType.ROOM);
        oldResource.setCapacity(50);
        oldResource = resourceRepository.save(oldResource);

        newResource = new Resource();
        newResource.setBranch(branch);
        newResource.setCode("ROOM2-" + uniqueSuffix);
        newResource.setName("Room-2");
        newResource.setResourceType(ResourceType.ROOM);
        newResource.setCapacity(50);
        newResource = resourceRepository.save(newResource);

        // Old session
        oldSession = new Session();
        oldSession.setClassEntity(classEntity);
        oldSession.setDate(LocalDate.now().plusDays(1));
        oldSession.setStatus(SessionStatus.PLANNED);
        oldSession.setTimeSlotTemplate(oldTimeSlot);
        oldSession.setCreatedAt(OffsetDateTime.now());
        oldSession.setUpdatedAt(OffsetDateTime.now());
        oldSession = sessionRepository.save(oldSession);

        // Teaching slot for old session
        TeachingSlot teachingSlot = TeachingSlot.builder()
                .id(new TeachingSlot.TeachingSlotId(oldSession.getId(), teacher.getId()))
                .session(oldSession)
                .teacher(teacher)
                .status(TeachingSlotStatus.SCHEDULED)
                .build();
        teachingSlotRepository.save(teachingSlot);

        // Session resource for old session
        SessionResource sr = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(oldSession.getId(), oldResource.getId()))
                .session(oldSession)
                .resource(oldResource)
                .build();
        sessionResourceRepository.save(sr);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Approve reschedule - creates new session and cancels old")
    void approve_reschedule_creates_new_session_and_cancels_old() {
        LocalDate newDate = LocalDate.now().plusDays(3);

        // Create pending teacher request
        TeacherRequest req = TeacherRequest.builder()
                .teacher(teacher)
                .session(oldSession)
                .requestType(TeacherRequestType.RESCHEDULE)
                .status(RequestStatus.PENDING)
                .newDate(newDate)
                .newTimeSlot(newTimeSlot)
                .newResource(newResource)
                .submittedAt(OffsetDateTime.now())
                .build();
        req = teacherRequestRepository.save(req);

        // Approve with new resource
        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .note("Approve reschedule")
                .build();

        teacherRequestService.approveRequest(req.getId(), approve, staff.getId());

        // Assert: old session cancelled
        Session updatedOldSession = sessionRepository.findById(oldSession.getId()).orElseThrow();
        assertThat(updatedOldSession.getStatus()).isEqualTo(SessionStatus.CANCELLED);

        // Assert: new session created
        TeacherRequest updatedReq = teacherRequestRepository.findByIdWithTeacherAndSession(req.getId()).orElseThrow();
        assertThat(updatedReq.getNewSession()).isNotNull();
        Long newSessionId = updatedReq.getNewSession().getId(); // Force load to get ID
        // Reload from database to avoid lazy loading issues
        Session newSession = sessionRepository.findById(newSessionId).orElseThrow();
        assertThat(newSession.getDate()).isEqualTo(newDate);
        assertThat(newSession.getTimeSlotTemplate().getId()).isEqualTo(newTimeSlot.getId());
        assertThat(newSession.getStatus()).isEqualTo(SessionStatus.PLANNED);

        // Assert: new session resource created
        boolean newResourceExists = sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(
                newResource.getId(), newDate, newTimeSlot.getId(), 
                Arrays.asList(SessionStatus.PLANNED, SessionStatus.DONE), null);
        assertThat(newResourceExists).isTrue();
    }
}

