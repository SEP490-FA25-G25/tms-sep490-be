package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.SessionType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_session_id")
    private SubjectSession subjectSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_template_id")
    private TimeSlotTemplate timeSlotTemplate;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SessionType type = SessionType.CLASS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.PLANNED;

    @Column(name = "teacher_note", columnDefinition = "TEXT")
    private String teacherNote;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SessionResource> sessionResources = new HashSet<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TeachingSlot> teachingSlots = new HashSet<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<StudentSession> studentSessions = new HashSet<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<QAReport> qaReports = new HashSet<>();

    @OneToMany(mappedBy = "session")
    @Builder.Default
    private Set<TeacherRequest> teacherRequests = new HashSet<>();

    @OneToMany(mappedBy = "targetSession")
    @Builder.Default
    private Set<StudentRequest> targetStudentRequests = new HashSet<>();

    @OneToMany(mappedBy = "makeupSession")
    @Builder.Default
    private Set<StudentRequest> makeupStudentRequests = new HashSet<>();

    @OneToMany(mappedBy = "effectiveSession")
    @Builder.Default
    private Set<StudentRequest> effectiveStudentRequests = new HashSet<>();

    @OneToMany(mappedBy = "joinSession")
    @Builder.Default
    private Set<Enrollment> joinEnrollments = new HashSet<>();

    @OneToMany(mappedBy = "leftSession")
    @Builder.Default
    private Set<Enrollment> leftEnrollments = new HashSet<>();

    @OneToMany(mappedBy = "makeupSession")
    @Builder.Default
    private Set<StudentSession> makeupStudentSessions = new HashSet<>();

    @OneToMany(mappedBy = "originalSession")
    @Builder.Default
    private Set<StudentSession> originalStudentSessions = new HashSet<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
