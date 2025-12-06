package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "session_resource")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResource implements Serializable {

    @EmbeddedId
    private SessionResourceId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sessionId")
    @JoinColumn(name = "session_id")
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("resourceId")
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class SessionResourceId implements Serializable {
        @Column(name = "session_id")
        private Long sessionId;

        @Column(name = "resource_id")
        private Long resourceId;
    }
}
