package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.Skill;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "replacement_skill_assessment", uniqueConstraints = {
    @UniqueConstraint(name = "uq_student_skill_assessment", columnNames = {"student_id", "skill", "assessment_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplacementSkillAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private Level level;

    /**
     * Điểm đánh giá dạng text linh hoạt (e.g., "35/40", "6.5", "650", "7/10", "85%")
     */
    @Column(name = "score", length = 50)
    private String score;

    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @Column(name = "assessment_type", length = 100)
    private String assessmentType;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessed_by")
    private UserAccount assessedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;
}
