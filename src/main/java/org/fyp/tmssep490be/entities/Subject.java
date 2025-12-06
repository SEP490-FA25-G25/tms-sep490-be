package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "subject")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id", nullable = false)
    private Curriculum curriculum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private Level level;

    @Column(name = "logical_subject_code", length = 100)
    private String logicalSubjectCode;

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

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubjectStatus status = SubjectStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = true)
    private ApprovalStatus approvalStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_manager")
    private UserAccount decidedByManager;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "hash_checksum", length = 255)
    private String hashChecksum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SubjectPhase> subjectPhases = new HashSet<>();

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SubjectMaterial> subjectMaterials = new HashSet<>();

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CLO> clos = new HashSet<>();

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SubjectAssessment> subjectAssessments = new HashSet<>();

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ClassEntity> classes = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // Only set when re-submitting after rejection (not auto-updated)
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
