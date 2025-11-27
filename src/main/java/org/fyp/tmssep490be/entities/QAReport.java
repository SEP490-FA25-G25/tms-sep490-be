package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.QAReportType;
import org.fyp.tmssep490be.entities.enums.QAReportStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "qa_report")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class QAReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    private CoursePhase phase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private UserAccount reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 100, nullable = false)
    private QAReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private QAReportStatus status;

    @Column(name = "findings", columnDefinition = "TEXT")
    private String findings;

    @OneToMany(mappedBy = "qaReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ActionItem> actionItems = new HashSet<>();

    // Keep backward compatibility with existing action_items text field
    @Column(name = "action_items", columnDefinition = "TEXT")
    private String actionItemsText;

    @CreatedDate
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Convenience methods for business logic
    public boolean isDraft() {
        return QAReportStatus.DRAFT.equals(status);
    }

    public boolean isSubmitted() {
        return QAReportStatus.SUBMITTED.equals(status);
    }

    public boolean isClassLevelReport() {
        return session == null && phase == null;
    }

    public boolean isSessionLevelReport() {
        return session != null;
    }

    public boolean isPhaseLevelReport() {
        return phase != null;
    }
}
