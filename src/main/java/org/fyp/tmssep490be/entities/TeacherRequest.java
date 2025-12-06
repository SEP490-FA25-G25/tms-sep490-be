package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "teacher_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(name = "new_date")
    private LocalDate newDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_time_slot_id")
    private TimeSlotTemplate newTimeSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_resource_id")
    private Resource newResource;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private TeacherRequestType requestType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_teacher_id")
    private Teacher replacementTeacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_session_id")
    private Session newSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private UserAccount submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private UserAccount decidedBy;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "request_reason", columnDefinition = "TEXT")
    private String requestReason;

    @Column(columnDefinition = "TEXT")
    private String note;
}
