package com.didan.social.dto;

public class ReportDTO {
    private String reportId;
    private String reporterId;
    private String reporterName;
    private String targetType;
    private String targetId;
    private String targetPreview;   // tiêu đề bài / trích bình luận / tên user
    private String targetStatus;    // với POST: published | hidden | deleted
    private String reason;
    private String status;
    private String createdAt;
    private long sameTargetOpenCount;

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getTargetPreview() { return targetPreview; }
    public void setTargetPreview(String targetPreview) { this.targetPreview = targetPreview; }
    public String getTargetStatus() { return targetStatus; }
    public void setTargetStatus(String targetStatus) { this.targetStatus = targetStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public long getSameTargetOpenCount() { return sameTargetOpenCount; }
    public void setSameTargetOpenCount(long v) { this.sameTargetOpenCount = v; }
}
