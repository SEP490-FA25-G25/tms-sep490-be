package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.MaterialType;

import java.time.OffsetDateTime;

@Entity
@Table(name = "course_material")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class CourseMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    private CoursePhase phase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_session_id")
    private CourseSession courseSession;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "material_type", nullable = false)
    private MaterialType materialType;

    @Column(nullable = false, length = 1000)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private UserAccount uploadedBy;

    @org.springframework.data.annotation.CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
