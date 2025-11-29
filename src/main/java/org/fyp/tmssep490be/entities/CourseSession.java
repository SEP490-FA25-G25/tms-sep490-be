package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "course_session", uniqueConstraints = {
        @UniqueConstraint(name = "uq_course_session_phase_sequence", columnNames = { "phase_id", "sequence_no" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class CourseSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id", nullable = false)
    private CoursePhase phase;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(length = 500)
    private String topic;

    @Column(name = "student_task", columnDefinition = "TEXT")
    private String studentTask;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill", nullable = false, length = 20)
    private org.fyp.tmssep490be.entities.enums.Skill skill;

    @OneToMany(mappedBy = "courseSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseMaterial> courseMaterials = new HashSet<>();

    @OneToMany(mappedBy = "courseSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseSessionCLOMapping> courseSessionCLOMappings = new HashSet<>();

    @OneToMany(mappedBy = "courseSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Session> sessions = new HashSet<>();

    @org.springframework.data.annotation.CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
