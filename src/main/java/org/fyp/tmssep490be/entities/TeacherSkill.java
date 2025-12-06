package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.Skill;

import java.io.Serializable;

@Entity
@Table(name = "teacher_skill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherSkill implements Serializable {

    @EmbeddedId
    private TeacherSkillId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teacherId")
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(length = 255)
    private String specialization;

    @Column(length = 255)
    private String language;

    private Short level;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TeacherSkillId implements Serializable {
        @Column(name = "teacher_id")
        private Long teacherId;

        @Enumerated(EnumType.STRING)
        @Column(name = "skill")
        private Skill skill;
    }
}
