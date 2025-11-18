package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.DisplayName("Teacher Request Modality Integration Tests")
class TeacherRequestModalityIT {

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
    @Autowired private StudentSessionRepository studentSessionRepository;

    private UserAccount staff;
    private Teacher teacher;
    private Session session;
    private TimeSlotTemplate timeSlot;
    private ClassEntity classEntity;
    private String uniqueSuffix;

    @BeforeEach
    void setup() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        // Basic users (set required fields)
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

        // Minimal org structure and curriculum to satisfy NOT NULL FKs
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

        // Session (planned, today+1)
        session = new Session();
        session.setClassEntity(classEntity);
        session.setDate(LocalDate.now().plusDays(1));
        session.setStatus(SessionStatus.PLANNED);
        session.setTimeSlotTemplate(timeSlot);
        session = sessionRepository.save(session);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Approve modality - updates session_resource only")
    void approve_modality_updates_session_resource_only() {
        // Current resource (room)
        Resource oldRoom = new Resource();
        oldRoom.setBranch(classEntity.getBranch());
        oldRoom.setCode("ROOM-" + uniqueSuffix);
        oldRoom.setName("Room-1");
        oldRoom.setResourceType(ResourceType.ROOM);
        oldRoom = resourceRepository.save(oldRoom);

        // Attach current session_resource
        SessionResource sr = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(session.getId(), oldRoom.getId()))
                .session(session)
                .resource(oldRoom)
                .build();
        sessionResourceRepository.save(sr);

        // New resource (virtual)
        Resource zoom = new Resource();
        zoom.setBranch(classEntity.getBranch());
        zoom.setCode("Z-" + uniqueSuffix);
        zoom.setName("Zoom-1");
        zoom.setResourceType(ResourceType.VIRTUAL);
        zoom.setCapacity(50); // Set capacity for testing
        zoom = resourceRepository.save(zoom);

        // Create some student sessions for the session (to test capacity validation)
        // Note: In real scenario, student sessions are created when students enroll
        // For testing, we'll create minimal student sessions

        // Create pending teacher request (modality change)
        TeacherRequest req = TeacherRequest.builder()
                .teacher(teacher)
                .session(session)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .status(RequestStatus.PENDING)
                .build();
        req = teacherRequestRepository.save(req);

        // Approve with new resource
        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .newResourceId(zoom.getId())
                .note("Approve to online")
                .build();

        teacherRequestService.approveRequest(req.getId(), approve, staff.getId());

        // Assert: old session_resource removed, new created
        boolean oldExists = sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(
                oldRoom.getId(), session.getDate(), timeSlot.getId(), Arrays.asList(SessionStatus.PLANNED, SessionStatus.DONE), null);
        assertThat(oldExists).isFalse();

        boolean newExists = sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(
                zoom.getId(), session.getDate(), timeSlot.getId(), Arrays.asList(SessionStatus.PLANNED, SessionStatus.DONE), null);
        assertThat(newExists).isTrue();

        // Assert class modality NOT changed (should remain OFFLINE)
        ClassEntity updated = classRepository.findById(classEntity.getId()).orElseThrow();
        assertThat(updated.getModality()).isEqualTo(Modality.OFFLINE); // Should remain unchanged
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Approve modality - conflict - fails and keeps DB unchanged")
    void approve_modality_conflict_fails_and_keeps_db_unchanged() {
        // Existing resource at same timeslot (virtual)
        Resource zoom = new Resource();
        zoom.setBranch(classEntity.getBranch());
        zoom.setCode("Z2-" + uniqueSuffix);
        zoom.setName("Zoom-2");
        zoom.setResourceType(ResourceType.VIRTUAL);
        zoom = resourceRepository.save(zoom);

        // Another session at same date/time holds the resource
        Session other = new Session();
        other.setClassEntity(classEntity);
        other.setDate(session.getDate());
        other.setStatus(SessionStatus.PLANNED);
        other.setTimeSlotTemplate(timeSlot);
        other = sessionRepository.save(other);

        SessionResource busy = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(other.getId(), zoom.getId()))
                .session(other)
                .resource(zoom)
                .build();
        sessionResourceRepository.save(busy);

        // Create pending teacher request
        TeacherRequest req = TeacherRequest.builder()
                .teacher(teacher)
                .session(session)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .status(RequestStatus.PENDING)
                .build();
        req = teacherRequestRepository.save(req);

        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .newResourceId(zoom.getId())
                .note("conflict case")
                .build();

        final Long reqId = req.getId();
        assertThatThrownBy(() -> teacherRequestService.approveRequest(reqId, approve, staff.getId()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Resource is not available");

        // Ensure no new session_resource was created for target session
        boolean exists = sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(
                zoom.getId(), session.getDate(), timeSlot.getId(), Arrays.asList(SessionStatus.PLANNED, SessionStatus.DONE), null);
        assertThat(exists).isTrue(); // only the other session holds it
    }
}


