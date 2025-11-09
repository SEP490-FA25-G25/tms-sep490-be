package org.fyp.tmssep490be.utils;

import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Test data builder utility for creating test entities.
 * Provides fluent API for building test data with sensible defaults.
 *
 * Usage:
 * <pre>
 * Center center = TestDataBuilder.buildCenter()
 *     .name("Test Center")
 *     .build();
 * </pre>
 */
public class TestDataBuilder {

    // Center Builder
    public static CenterBuilder buildCenter() {
        return new CenterBuilder();
    }

    public static class CenterBuilder {
        private final Center center = new Center();

        public CenterBuilder() {
            // Set defaults
            center.setCode("TC001");
            center.setName("Test Training Center");
            center.setDescription("A test training center");
            center.setAddress("123 Test Street");
            center.setPhone("0123456789");
            center.setEmail("test@center.com");
        }

        public CenterBuilder code(String code) {
            center.setCode(code);
            return this;
        }

        public CenterBuilder name(String name) {
            center.setName(name);
            return this;
        }

        public CenterBuilder id(Long id) {
            center.setId(id);
            return this;
        }

        public CenterBuilder description(String description) {
            center.setDescription(description);
            return this;
        }

        public CenterBuilder address(String address) {
            center.setAddress(address);
            return this;
        }

        public CenterBuilder phone(String phone) {
            center.setPhone(phone);
            return this;
        }

        public CenterBuilder email(String email) {
            center.setEmail(email);
            return this;
        }

        public Center build() {
            return center;
        }
    }

    // Branch Builder
    public static BranchBuilder buildBranch() {
        return new BranchBuilder();
    }

    public static class BranchBuilder {
        private final Branch branch = new Branch();

        public BranchBuilder() {
            // Set defaults
            branch.setCode("BR001");
            branch.setName("Test Branch");
            branch.setAddress("456 Branch Street");
            branch.setPhone("0987654321");
            branch.setEmail("test@branch.com");
            branch.setDistrict("Test District");
            branch.setCity("Test City");
        }

        public BranchBuilder id(Long id) {
            branch.setId(id);
            return this;
        }

        public BranchBuilder center(Center center) {
            branch.setCenter(center);
            return this;
        }

        public BranchBuilder code(String code) {
            branch.setCode(code);
            return this;
        }

        public BranchBuilder name(String name) {
            branch.setName(name);
            return this;
        }

        public BranchBuilder address(String address) {
            branch.setAddress(address);
            return this;
        }

        public Branch build() {
            return branch;
        }
    }

    // Subject Builder
    public static SubjectBuilder buildSubject() {
        return new SubjectBuilder();
    }

    public static class SubjectBuilder {
        private final Subject subject = new Subject();

        public SubjectBuilder() {
            // Set defaults
            subject.setCode("ENG");
            subject.setName("English");
            subject.setDescription("English language training");
        }

        public SubjectBuilder code(String code) {
            subject.setCode(code);
            return this;
        }

        public SubjectBuilder name(String name) {
            subject.setName(name);
            return this;
        }

        public SubjectBuilder description(String description) {
            subject.setDescription(description);
            return this;
        }

        public SubjectBuilder status(SubjectStatus status) {
            subject.setStatus(status);
            return this;
        }

        public Subject build() {
            return subject;
        }
    }

    // Level Builder
    public static LevelBuilder buildLevel() {
        return new LevelBuilder();
    }

    public static class LevelBuilder {
        private final Level level = new Level();

        public LevelBuilder() {
            // Set defaults
            level.setCode("A1");
            level.setName("Beginner");
            level.setDescription("Beginner level");
            level.setSortOrder(1);
            level.setExpectedDurationHours(40);
        }

        public LevelBuilder subject(Subject subject) {
            level.setSubject(subject);
            return this;
        }

        public LevelBuilder code(String code) {
            level.setCode(code);
            return this;
        }

        public LevelBuilder name(String name) {
            level.setName(name);
            return this;
        }

        public LevelBuilder description(String description) {
            level.setDescription(description);
            return this;
        }

        public LevelBuilder sortOrder(Integer order) {
            level.setSortOrder(order);
            return this;
        }

        public LevelBuilder expectedDurationHours(Integer hours) {
            level.setExpectedDurationHours(hours);
            return this;
        }

        public Level build() {
            return level;
        }
    }

    // Course Builder
    public static CourseBuilder buildCourse() {
        return new CourseBuilder();
    }

    public static class CourseBuilder {
        private final Course course = new Course();

        public CourseBuilder() {
            // Set defaults - need both subject and level
            course.setCode("ENG-A1-2024");
            course.setLogicalCourseCode("ENG-A1");
            course.setName("English A1 Course");
            course.setDescription("Beginner English course");
            course.setTotalHours(80);
            course.setDurationWeeks(10);
            course.setSessionPerWeek(2);
            course.setHoursPerSession(new BigDecimal("2.00"));
            course.setVersion(1);
            course.setStatus(CourseStatus.DRAFT);
            course.setApprovalStatus(ApprovalStatus.PENDING);
            course.setEffectiveDate(LocalDate.now().plusDays(30));
        }

        public CourseBuilder subject(Subject subject) {
            course.setSubject(subject);
            return this;
        }

        public CourseBuilder level(Level level) {
            course.setLevel(level);
            // Also set subject from level if not already set
            if (level != null && course.getSubject() == null) {
                course.setSubject(level.getSubject());
            }
            return this;
        }

        public CourseBuilder code(String code) {
            course.setCode(code);
            return this;
        }

        public CourseBuilder name(String name) {
            course.setName(name);
            return this;
        }

        public CourseBuilder status(CourseStatus status) {
            course.setStatus(status);
            return this;
        }

        public CourseBuilder approvalStatus(ApprovalStatus approvalStatus) {
            course.setApprovalStatus(approvalStatus);
            return this;
        }

        public Course build() {
            return course;
        }
    }

    // UserAccount Builder
    public static UserAccountBuilder buildUserAccount() {
        return new UserAccountBuilder();
    }

    public static class UserAccountBuilder {
        private final UserAccount userAccount = new UserAccount();

        public UserAccountBuilder() {
            // Set defaults
            userAccount.setEmail("test@example.com");
            userAccount.setFullName("Test User");
            userAccount.setGender(Gender.MALE);
            userAccount.setStatus(UserStatus.ACTIVE);
            userAccount.setPasswordHash("encodedPasswordHash");
        }

        public UserAccountBuilder id(Long id) {
            userAccount.setId(id);
            return this;
        }

        public UserAccountBuilder email(String email) {
            userAccount.setEmail(email);
            return this;
        }

        public UserAccountBuilder fullName(String fullName) {
            userAccount.setFullName(fullName);
            return this;
        }

        public UserAccountBuilder gender(Gender gender) {
            userAccount.setGender(gender);
            return this;
        }

        public UserAccountBuilder status(UserStatus status) {
            userAccount.setStatus(status);
            return this;
        }

        public UserAccountBuilder passwordHash(String passwordHash) {
            userAccount.setPasswordHash(passwordHash);
            return this;
        }

        public UserAccount build() {
            return userAccount;
        }
    }

    // Student Builder
    public static StudentBuilder buildStudent() {
        return new StudentBuilder();
    }

    public static class StudentBuilder {
        private final Student student = new Student();

        public StudentBuilder() {
            // Set defaults
            student.setStudentCode("ST001");
        }

        public StudentBuilder userAccount(UserAccount userAccount) {
            student.setUserAccount(userAccount);
            return this;
        }

        public StudentBuilder studentCode(String studentCode) {
            student.setStudentCode(studentCode);
            return this;
        }

        public Student build() {
            return student;
        }
    }

    // ClassEntity Builder
    public static ClassEntityBuilder buildClassEntity() {
        return new ClassEntityBuilder();
    }

    public static class ClassEntityBuilder {
        private final ClassEntity classEntity = new ClassEntity();

        public ClassEntityBuilder() {
            // Set defaults
            classEntity.setCode("CLASS001");
            classEntity.setName("Test Class");
            classEntity.setModality(Modality.ONLINE);
            classEntity.setStartDate(LocalDate.now().plusDays(7));
            classEntity.setPlannedEndDate(LocalDate.now().plusWeeks(12));
            classEntity.setMaxCapacity(20);
        }

        public ClassEntityBuilder id(Long id) {
            classEntity.setId(id);
            return this;
        }

        public ClassEntityBuilder branch(Branch branch) {
            classEntity.setBranch(branch);
            return this;
        }

        public ClassEntityBuilder course(Course course) {
            classEntity.setCourse(course);
            return this;
        }

        public ClassEntityBuilder code(String code) {
            classEntity.setCode(code);
            return this;
        }

        public ClassEntityBuilder name(String name) {
            classEntity.setName(name);
            return this;
        }

        public ClassEntityBuilder modality(Modality modality) {
            classEntity.setModality(modality);
            return this;
        }

        public ClassEntityBuilder startDate(LocalDate startDate) {
            classEntity.setStartDate(startDate);
            return this;
        }

        public ClassEntityBuilder plannedEndDate(LocalDate plannedEndDate) {
            classEntity.setPlannedEndDate(plannedEndDate);
            return this;
        }

        public ClassEntityBuilder maxCapacity(Integer maxCapacity) {
            classEntity.setMaxCapacity(maxCapacity);
            return this;
        }

        public ClassEntityBuilder status(ClassStatus status) {
            classEntity.setStatus(status);
            return this;
        }

        public ClassEntity build() {
            return classEntity;
        }
    }

    // Enrollment Builder
    public static EnrollmentBuilder buildEnrollment() {
        return new EnrollmentBuilder();
    }

    public static class EnrollmentBuilder {
        private final Enrollment enrollment = new Enrollment();

        public EnrollmentBuilder() {
            // Set defaults
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollment.setEnrolledAt(OffsetDateTime.now());
            enrollment.setEnrolledBy(1L);
            enrollment.setCapacityOverride(false);
        }

        public EnrollmentBuilder classId(Long classId) {
            enrollment.setClassId(classId);
            return this;
        }

        public EnrollmentBuilder studentId(Long studentId) {
            enrollment.setStudentId(studentId);
            return this;
        }

        public EnrollmentBuilder status(EnrollmentStatus status) {
            enrollment.setStatus(status);
            return this;
        }

        public EnrollmentBuilder enrolledAt(OffsetDateTime enrolledAt) {
            enrollment.setEnrolledAt(enrolledAt);
            return this;
        }

        public EnrollmentBuilder enrolledBy(Long enrolledBy) {
            enrollment.setEnrolledBy(enrolledBy);
            return this;
        }

        public EnrollmentBuilder capacityOverride(Boolean capacityOverride) {
            enrollment.setCapacityOverride(capacityOverride);
            return this;
        }

        public EnrollmentBuilder overrideReason(String overrideReason) {
            enrollment.setOverrideReason(overrideReason);
            return this;
        }

        public Enrollment build() {
            return enrollment;
        }
    }

    // Session Builder
    public static SessionBuilder buildSession() {
        return new SessionBuilder();
    }

    public static class SessionBuilder {
        private final Session session = new Session();

        public SessionBuilder() {
            // Set defaults
            session.setDate(LocalDate.now());
            session.setStatus(SessionStatus.PLANNED);
        }

        public SessionBuilder id(Long id) {
            session.setId(id);
            return this;
        }

        public SessionBuilder classEntity(ClassEntity classEntity) {
            session.setClassEntity(classEntity);
            return this;
        }

        public SessionBuilder date(LocalDate date) {
            session.setDate(date);
            return this;
        }

        public SessionBuilder status(SessionStatus status) {
            session.setStatus(status);
            return this;
        }

        public SessionBuilder teacherNote(String teacherNote) {
            session.setTeacherNote(teacherNote);
            return this;
        }

        public Session build() {
            return session;
        }
    }

    // ClassEntity shorthand (alias for buildClassEntity)
    public static ClassEntityBuilder buildClass() {
        return buildClassEntity();
    }

    // TeacherSkill Builder
    public static TeacherSkillBuilder buildTeacherSkill() {
        return new TeacherSkillBuilder();
    }

    public static class TeacherSkillBuilder {
        private final TeacherSkill teacherSkill = new TeacherSkill();
        private Long teacherId;
        private Skill skill = Skill.GENERAL;

        public TeacherSkillBuilder() {
            // Set defaults - composite ID will be built in build()
        }

        public TeacherSkillBuilder teacher(Teacher teacher) {
            teacherSkill.setTeacher(teacher);
            if (teacher != null && teacher.getId() != null) {
                this.teacherId = teacher.getId();
            }
            return this;
        }

        public TeacherSkillBuilder skill(Skill skill) {
            this.skill = skill;
            return this;
        }

        public TeacherSkill build() {
            // Build composite ID
            if (teacherId != null) {
                TeacherSkill.TeacherSkillId id = new TeacherSkill.TeacherSkillId();
                id.setTeacherId(teacherId);
                id.setSkill(skill);
                teacherSkill.setId(id);
            }
            return teacherSkill;
        }
    }

    // ReplacementSkillAssessment Builder
    public static ReplacementSkillAssessmentBuilder buildReplacementSkillAssessment() {
        return new ReplacementSkillAssessmentBuilder();
    }

    public static class ReplacementSkillAssessmentBuilder {
        private final ReplacementSkillAssessment assessment = new ReplacementSkillAssessment();

        public ReplacementSkillAssessmentBuilder() {
            // Set defaults
            assessment.setSkill(Skill.READING);
            assessment.setAssessmentDate(LocalDate.now());
            assessment.setRawScore(BigDecimal.valueOf(75));
            assessment.setScaledScore(BigDecimal.valueOf(75));
            assessment.setScoreScale("0-100");
            assessment.setAssessmentCategory("PLACEMENT");
        }

        public ReplacementSkillAssessmentBuilder student(Student student) {
            assessment.setStudent(student);
            return this;
        }

        public ReplacementSkillAssessmentBuilder skill(Skill skill) {
            assessment.setSkill(skill);
            return this;
        }

        public ReplacementSkillAssessmentBuilder level(Level level) {
            assessment.setLevel(level);
            return this;
        }

        public ReplacementSkillAssessmentBuilder assessmentDate(LocalDate assessmentDate) {
            assessment.setAssessmentDate(assessmentDate);
            return this;
        }

        public ReplacementSkillAssessmentBuilder rawScore(BigDecimal rawScore) {
            assessment.setRawScore(rawScore);
            return this;
        }

        public ReplacementSkillAssessmentBuilder scaledScore(BigDecimal scaledScore) {
            assessment.setScaledScore(scaledScore);
            return this;
        }

        public ReplacementSkillAssessmentBuilder scoreScale(String scoreScale) {
            assessment.setScoreScale(scoreScale);
            return this;
        }

        public ReplacementSkillAssessmentBuilder assessmentCategory(String assessmentCategory) {
            assessment.setAssessmentCategory(assessmentCategory);
            return this;
        }

        public ReplacementSkillAssessment build() {
            return assessment;
        }
    }

    // Resource Builder
    public static ResourceBuilder buildResource() {
        return new ResourceBuilder();
    }

    public static class ResourceBuilder {
        private final Resource resource = new Resource();

        public ResourceBuilder() {
            // Set defaults
            resource.setCode("RES001");
            resource.setName("Test Resource");
            resource.setResourceType(ResourceType.ROOM);
            resource.setCapacity(30);
            resource.setDescription("Test resource for testing");
        }

        public ResourceBuilder id(Long id) {
            resource.setId(id);
            return this;
        }

        public ResourceBuilder branch(Branch branch) {
            resource.setBranch(branch);
            return this;
        }

        public ResourceBuilder resourceType(ResourceType resourceType) {
            resource.setResourceType(resourceType);
            return this;
        }

        public ResourceBuilder code(String code) {
            resource.setCode(code);
            return this;
        }

        public ResourceBuilder name(String name) {
            resource.setName(name);
            return this;
        }

        public ResourceBuilder description(String description) {
            resource.setDescription(description);
            return this;
        }

        public ResourceBuilder capacity(Integer capacity) {
            resource.setCapacity(capacity);
            return this;
        }

        public ResourceBuilder equipment(String equipment) {
            resource.setEquipment(equipment);
            return this;
        }

        public ResourceBuilder meetingUrl(String meetingUrl) {
            resource.setMeetingUrl(meetingUrl);
            return this;
        }

        public Resource build() {
            return resource;
        }
    }

    // Teacher Builder
    public static TeacherBuilder buildTeacher() {
        return new TeacherBuilder();
    }

    public static class TeacherBuilder {
        private final Teacher teacher = new Teacher();

        public TeacherBuilder() {
            // Set defaults with UserAccount
            UserAccount userAccount = buildUserAccount()
                    .email("teacher@test.com")
                    .fullName("Test Teacher")
                    .build();
            teacher.setUserAccount(userAccount);
            teacher.setEmployeeCode("TCH001");
            teacher.setHireDate(LocalDate.now().minusYears(2));
            teacher.setContractType("FULL_TIME");
        }

        public TeacherBuilder id(Long id) {
            teacher.setId(id);
            return this;
        }

        public TeacherBuilder userAccount(UserAccount userAccount) {
            teacher.setUserAccount(userAccount);
            return this;
        }

        public TeacherBuilder employeeCode(String employeeCode) {
            teacher.setEmployeeCode(employeeCode);
            return this;
        }

        public TeacherBuilder hireDate(LocalDate hireDate) {
            teacher.setHireDate(hireDate);
            return this;
        }

        public TeacherBuilder contractType(String contractType) {
            teacher.setContractType(contractType);
            return this;
        }

        public TeacherBuilder note(String note) {
            teacher.setNote(note);
            return this;
        }

        public Teacher build() {
            return teacher;
        }
    }
}
