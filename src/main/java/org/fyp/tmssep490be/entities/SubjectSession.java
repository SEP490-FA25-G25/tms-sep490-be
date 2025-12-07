package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "subject_session", uniqueConstraints = {
        @UniqueConstraint(name = "uq_subject_session_phase_sequence", columnNames = { "phase_id", "sequence_no" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class SubjectSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id", nullable = false)
    private SubjectPhase phase;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(length = 500)
    private String topic;

    @Column(name = "student_task", columnDefinition = "TEXT")
    private String studentTask;

    @Column(name = "skill", nullable = false, columnDefinition = "TEXT")
    private String skills; // JSON string: ["READING", "VOCABULARY"]

    @OneToMany(mappedBy = "subjectSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SubjectMaterial> subjectMaterials = new HashSet<>();

    @OneToMany(mappedBy = "subjectSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SubjectSessionCLOMapping> subjectSessionCLOMappings = new HashSet<>();

    @OneToMany(mappedBy = "subjectSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Session> sessions = new HashSet<>();

    @org.springframework.data.annotation.CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
