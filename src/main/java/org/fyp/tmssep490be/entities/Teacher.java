package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "teacher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(name = "employee_code", unique = true, length = 50)
    private String employeeCode;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "contract_type", length = 100)
    private String contractType;

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TeacherSkill> teacherSkills = new HashSet<>();

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TeacherAvailability> teacherAvailabilities = new HashSet<>();

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TeachingSlot> teachingSlots = new HashSet<>();

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TeacherRequest> teacherRequests = new HashSet<>();

    @OneToMany(mappedBy = "replacementTeacher")
    @Builder.Default
    private Set<TeacherRequest> replacementRequests = new HashSet<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
