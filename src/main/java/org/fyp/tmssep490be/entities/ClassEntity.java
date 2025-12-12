package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "class", uniqueConstraints = {
    @UniqueConstraint(name = "uq_class_branch_code", columnNames = {"branch_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Modality modality;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "schedule_days", columnDefinition = "SMALLINT[]")
    private Short[] scheduleDays;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ClassStatus status = ClassStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ApprovalStatus approvalStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private UserAccount decidedBy;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ========== TEACHER REGISTRATION ==========

    @Column(name = "registration_open_date")
    private OffsetDateTime registrationOpenDate; // Ngày mở đăng ký

    @Column(name = "registration_close_date")
    private OffsetDateTime registrationCloseDate; // Ngày đóng đăng ký

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_teacher_id")
    private Teacher assignedTeacher; // Giáo viên được gán

    @Column(name = "teacher_assigned_at")
    private OffsetDateTime teacherAssignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_assigned_by")
    private UserAccount teacherAssignedBy; // Người gán giáo viên

    @Column(name = "direct_assign_reason", columnDefinition = "TEXT")
    private String directAssignReason; // Lý do gán trực tiếp

    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TeacherClassRegistration> teacherRegistrations = new HashSet<>();

    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Session> sessions = new HashSet<>();

    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Enrollment> enrollments = new HashSet<>();

    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Assessment> assessments = new HashSet<>();

    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<StudentFeedback> studentFeedbacks = new HashSet<>();

    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<QAReport> qaReports = new HashSet<>();

    @OneToMany(mappedBy = "currentClass")
    @Builder.Default
    private Set<StudentRequest> currentStudentRequests = new HashSet<>();

    @OneToMany(mappedBy = "targetClass")
    @Builder.Default
    private Set<StudentRequest> targetStudentRequests = new HashSet<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
