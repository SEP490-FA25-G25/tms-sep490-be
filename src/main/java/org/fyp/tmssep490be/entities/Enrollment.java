package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;

import java.time.OffsetDateTime;

@Entity
@Table(name = "enrollment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", insertable = false, updatable = false)
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

    @Column(name = "enrolled_at")
    private OffsetDateTime enrolledAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    @Column(name = "join_session_id")
    private Long joinSessionId;

    @Column(name = "left_session_id")
    private Long leftSessionId;

    @Column(name = "enrolled_by")
    private Long enrolledBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "join_session_id", insertable = false, updatable = false)
    private Session joinSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "left_session_id", insertable = false, updatable = false)
    private Session leftSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrolled_by", insertable = false, updatable = false)
    private UserAccount enrolledByUser;

    /**
     * Indicates if this enrollment exceeded class capacity
     * True when enrollment was approved despite exceeding max_capacity
     */
    @Column(name = "capacity_override", nullable = false)
    @Builder.Default
    private Boolean capacityOverride = false;

    /**
     * Reason for capacity override (required when capacityOverride = true)
     * Min 20 characters, explains why capacity limit was exceeded
     */
    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
