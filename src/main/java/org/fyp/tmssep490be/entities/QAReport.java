package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.QAReportType;
import org.fyp.tmssep490be.entities.enums.QAReportStatus;
import java.time.OffsetDateTime;

@Entity
@Table(name = "qa_report")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "report_type", length = 100, nullable = false)
    private String reportType; // Store as lowercase_with_underscores value

    @Column(name = "status", length = 50, nullable = false)
    private String status; // Store as lowercase_with_underscores value

    @Column(name = "findings", columnDefinition = "TEXT")
    private String findings;

    @Column(name = "action_items", columnDefinition = "TEXT")
    private String actionItems;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;

        // Validate and normalize enum values
        if (reportType != null) {
            QAReportType.fromString(reportType); // This will throw if invalid
            this.reportType = QAReportType.fromString(reportType).getValue();
        }
        if (status != null) {
            QAReportStatus.fromString(status); // This will throw if invalid
            this.status = QAReportStatus.fromString(status).getValue();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();

        // Validate and normalize enum values on update
        if (reportType != null) {
            QAReportType.fromString(reportType);
            this.reportType = QAReportType.fromString(reportType).getValue();
        }
        if (status != null) {
            QAReportStatus.fromString(status);
            this.status = QAReportStatus.fromString(status).getValue();
        }
    }

    // Helper methods for enum conversion
    public QAReportType getReportTypeEnum() {
        return reportType != null ? QAReportType.fromString(reportType) : null;
    }

    public void setReportTypeEnum(QAReportType reportType) {
        this.reportType = reportType != null ? reportType.getValue() : null;
    }

    public QAReportStatus getStatusEnum() {
        return status != null ? QAReportStatus.fromString(status) : null;
    }

    public void setStatusEnum(QAReportStatus status) {
        this.status = status != null ? status.getValue() : null;
    }

    // Convenience methods
    public boolean isDraft() {
        return "draft".equals(status);
    }

    public boolean isSubmitted() {
        return "submitted".equals(status);
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
