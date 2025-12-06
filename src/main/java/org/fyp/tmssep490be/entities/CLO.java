package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "clo", uniqueConstraints = {
        @UniqueConstraint(name = "uq_clo_subject_code", columnNames = { "subject_id", "code" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class CLO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "clo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PLOCLOMapping> ploCloMappings = new HashSet<>();

    @OneToMany(mappedBy = "clo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SubjectSessionCLOMapping> subjectSessionCLOMappings = new HashSet<>();

    @OneToMany(mappedBy = "clo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SubjectAssessmentCLOMapping> subjectAssessmentCLOMappings = new HashSet<>();

    @org.springframework.data.annotation.CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
