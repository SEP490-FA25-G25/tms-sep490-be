package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;

import java.io.Serializable;

@Entity
@Table(name = "teaching_slot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeachingSlot implements Serializable {

    @EmbeddedId
    private TeachingSlotId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sessionId")
    @JoinColumn(name = "session_id")
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teacherId")
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TeachingSlotStatus status = TeachingSlotStatus.SCHEDULED;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TeachingSlotId implements Serializable {
        @Column(name = "session_id")
        private Long sessionId;

        @Column(name = "teacher_id")
        private Long teacherId;
    }
}
