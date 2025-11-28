package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.AssessmentKind;
import org.fyp.tmssep490be.entities.enums.Skill;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "course_assessment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class CourseAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentKind kind;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(columnDefinition = "TEXT")
    private String description;

    @org.hibernate.annotations.Type(io.hypersistence.utils.hibernate.type.array.ListArrayType.class)
    @Column(name = "skills", columnDefinition = "varchar[]", nullable = false)
    @Builder.Default
    private java.util.List<String> skills = new java.util.ArrayList<>();

    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "courseAssessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseAssessmentCLOMapping> courseAssessmentCLOMappings = new HashSet<>();

    @OneToMany(mappedBy = "courseAssessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Assessment> assessments = new HashSet<>();

    @org.springframework.data.annotation.CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
