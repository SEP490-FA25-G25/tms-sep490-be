package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.RegistrationStatus;

import java.time.OffsetDateTime;

// Đăng ký dạy lớp của giáo viên
@Entity
@Table(name = "teacher_class_registration",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_teacher_class_registration",
            columnNames = {"teacher_id", "class_id"}
        )
    },
    indexes = {
        @Index(name = "idx_tcr_teacher_id", columnList = "teacher_id"),
        @Index(name = "idx_tcr_class_id", columnList = "class_id"),
        @Index(name = "idx_tcr_status", columnList = "status"),
        @Index(name = "idx_tcr_registered_at", columnList = "registered_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherClassRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note; // Ghi chú của giáo viên

    @Column(name = "registered_at", nullable = false)
    private OffsetDateTime registeredAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserAccount reviewedBy; // Người duyệt

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason; // Lý do từ chối

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (registeredAt == null) {
            registeredAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
