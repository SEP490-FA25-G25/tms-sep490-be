package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.MappingStatus;

import java.io.Serializable;

@Entity
@Table(name = "subject_assessment_clo_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectAssessmentCLOMapping implements Serializable {

    @EmbeddedId
    private SubjectAssessmentCLOMappingId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("subjectAssessmentId")
    @JoinColumn(name = "subject_assessment_id")
    private SubjectAssessment subjectAssessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cloId")
    @JoinColumn(name = "clo_id")
    private CLO clo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MappingStatus status;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class SubjectAssessmentCLOMappingId implements Serializable {
        @Column(name = "subject_assessment_id")
        private Long subjectAssessmentId;

        @Column(name = "clo_id")
        private Long cloId;
    }
}
