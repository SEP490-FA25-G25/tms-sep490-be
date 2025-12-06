package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "policy_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private SystemPolicy policy;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value", nullable = false)
    private String newValue;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", insertable = false, updatable = false)
    private OffsetDateTime changedAt;

    @Column(name = "reason")
    private String reason;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "change_type", length = 20)
    private String changeType;
}


