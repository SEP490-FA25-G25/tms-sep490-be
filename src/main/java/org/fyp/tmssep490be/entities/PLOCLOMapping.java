package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.MappingStatus;

import java.io.Serializable;

@Entity
@Table(name = "plo_clo_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PLOCLOMapping implements Serializable {

    @EmbeddedId
    private PLOCLOMappingId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ploId")
    @JoinColumn(name = "plo_id")
    private PLO plo;

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
    public static class PLOCLOMappingId implements Serializable {
        @Column(name = "plo_id")
        private Long ploId;

        @Column(name = "clo_id")
        private Long cloId;
    }
}
