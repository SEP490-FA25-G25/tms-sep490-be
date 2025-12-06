package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "student_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_class_id")
    private ClassEntity currentClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private StudentRequestType requestType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_class_id")
    private ClassEntity targetClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_session_id")
    private Session targetSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "makeup_session_id")
    private Session makeupSession;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effective_session_id")
    private Session effectiveSession;

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
