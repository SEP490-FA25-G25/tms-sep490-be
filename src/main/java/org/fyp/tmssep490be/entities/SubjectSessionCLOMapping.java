package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.MappingStatus;

import java.io.Serializable;

@Entity
@Table(name = "subject_session_clo_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectSessionCLOMapping implements Serializable {

    @EmbeddedId
    private SubjectSessionCLOMappingId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("subjectSessionId")
    @JoinColumn(name = "subject_session_id")
    private SubjectSession subjectSession;

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
    public static class SubjectSessionCLOMappingId implements Serializable {
        @Column(name = "subject_session_id")
        private Long subjectSessionId;

        @Column(name = "clo_id")
        private Long cloId;
    }
}
