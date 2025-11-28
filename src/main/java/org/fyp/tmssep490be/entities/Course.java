package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.CourseStatus;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "course")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private Level level;

    @Column(name = "logical_course_code", length = 100)
    private String logicalCourseCode;

    private Integer version;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "score_scale", length = 100)
    private String scoreScale;

    @Column(name = "total_hours")
    private Integer totalHours;

    @Column(name = "number_of_sessions")
    private Integer numberOfSessions;

    @Column(name = "hours_per_session", precision = 5, scale = 2)
    private BigDecimal hoursPerSession;

    @Column(columnDefinition = "TEXT")
    private String prerequisites;

    @Column(name = "target_audience", columnDefinition = "TEXT")
    private String targetAudience;

    @Column(name = "teaching_methods", columnDefinition = "TEXT")
    private String teachingMethods;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_manager")
    private UserAccount decidedByManager;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "hash_checksum", length = 255)
    private String hashChecksum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CoursePhase> coursePhases = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseMaterial> courseMaterials = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CLO> clos = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseAssessment> courseAssessments = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ClassEntity> classes = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
