package com.didan.social.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity(name = "reports")
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_status_created", columnList = "status, created_at")
})
public class Reports {
    @Id
    @Column(name = "report_id")
    private String reportId;

    @Column(name = "reporter_id", length = 50)
    private String reporterId;

    // USER | POST | COMMENT
    @Column(name = "target_type", length = 20)
    private String targetType;

    @Column(name = "target_id", length = 50)
    private String targetId;

    @Column(name = "reason", length = 500)
    private String reason;

    // OPEN | RESOLVED | DISMISSED
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "handled_by", length = 50)
    private String handledBy;

    @Column(name = "handled_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date handledAt;

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getHandledBy() { return handledBy; }
    public void setHandledBy(String handledBy) { this.handledBy = handledBy; }
    public Date getHandledAt() { return handledAt; }
    public void setHandledAt(Date handledAt) { this.handledAt = handledAt; }
}
