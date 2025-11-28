package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "course_phase", uniqueConstraints = {
        @UniqueConstraint(name = "uq_course_phase_course_number", columnNames = { "course_id", "phase_number" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class CoursePhase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "phase_number", nullable = false)
    private Integer phaseNumber;

    @Column(length = 255)
    private String name;

    @Column(name = "duration_weeks")
    private Integer durationWeeks;

    @Column(name = "learning_focus", columnDefinition = "TEXT")
    private String learningFocus;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseSession> courseSessions = new HashSet<>();

    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseMaterial> courseMaterials = new HashSet<>();

    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<StudentFeedback> studentFeedbacks = new HashSet<>();

    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<QAReport> qaReports = new HashSet<>();

    @org.springframework.data.annotation.CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
