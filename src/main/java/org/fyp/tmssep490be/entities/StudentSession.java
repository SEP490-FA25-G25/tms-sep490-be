package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.HomeworkStatus;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Table(name = "student_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSession implements Serializable {

    @EmbeddedId
    private StudentSessionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentId")
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sessionId")
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(name = "is_makeup")
    @Builder.Default
    private Boolean isMakeup = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "makeup_session_id")
    private Session makeupSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_session_id")
    private Session originalSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false)
    @Builder.Default
    private AttendanceStatus attendanceStatus = AttendanceStatus.PLANNED;

    @Enumerated(EnumType.STRING)
    @Column(name = "homework_status")
    private HomeworkStatus homeworkStatus;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "recorded_at")
    private OffsetDateTime recordedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class StudentSessionId implements Serializable {
        @Column(name = "student_id")
        private Long studentId;

        @Column(name = "session_id")
        private Long sessionId;
    }
}
