package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "system_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_key", length = 100, nullable = false)
    private String policyKey;

    @Column(name = "policy_category", length = 50, nullable = false)
    private String policyCategory;

    @Column(name = "policy_name", length = 200, nullable = false)
    private String policyName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "value_type", length = 20, nullable = false)
    private String valueType;

    @Column(name = "default_value", nullable = false)
    private String defaultValue;

    @Column(name = "current_value", nullable = false)
    private String currentValue;

    @Column(name = "min_value")
    private String minValue;

    @Column(name = "max_value")
    private String maxValue;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;
}


