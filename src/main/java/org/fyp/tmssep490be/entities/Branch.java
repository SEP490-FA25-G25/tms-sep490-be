package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.BranchStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "branch", uniqueConstraints = {
    @UniqueConstraint(name = "uq_branch_center_code", columnNames = {"center_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String district;

    @Column(length = 255)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BranchStatus status = BranchStatus.ACTIVE;

    @Column(name = "opening_date")
    private LocalDate openingDate;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TimeSlotTemplate> timeSlotTemplates = new HashSet<>();

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Resource> resources = new HashSet<>();

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserBranches> userBranches = new HashSet<>();

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ClassEntity> classes = new HashSet<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
