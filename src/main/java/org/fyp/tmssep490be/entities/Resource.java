package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "resource")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ResourceStatus status = ResourceStatus.ACTIVE;

    private Integer capacity;

    @Column(name = "capacity_override")
    private Integer capacityOverride;

    @Column(columnDefinition = "TEXT")
    private String equipment;

    @Column(name = "meeting_url", length = 500)
    private String meetingUrl;

    @Column(name = "meeting_id", length = 255)
    private String meetingId;

    @Column(name = "meeting_passcode", length = 255)
    private String meetingPasscode;

    @Column(name = "account_email", length = 255)
    private String accountEmail;

    @Column(name = "account_password", length = 255)
    private String accountPassword;

    @Column(name = "license_type", length = 100)
    private String licenseType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "renewal_date")
    private LocalDate renewalDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SessionResource> sessionResources = new HashSet<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
