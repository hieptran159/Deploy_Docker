package com.didan.social.controller;

import com.didan.social.payload.ResponseData;
import com.didan.social.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Report")
@RequestMapping("/report")
public class ReportController {
    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "Gửi báo cáo", description = "targetType: USER | POST | COMMENT",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<?> create(@RequestParam String targetType,
                                    @RequestParam String targetId,
                                    @RequestParam(required = false) String reason) {
        ResponseData payload = new ResponseData();
        try {
            reportService.create(targetType, targetId, reason);
            payload.setDescription("Đã gửi báo cáo, cảm ơn bạn");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Admin: danh sách báo cáo", description = "status: OPEN (mặc định) | RESOLVED | DISMISSED | ALL",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin")
    public ResponseEntity<?> list(@RequestParam(required = false) String status) {
        ResponseData payload = new ResponseData();
        try {
            payload.setData(reportService.listForAdmin(status));
            payload.setDescription("OK");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Admin: xoá nội dung bị báo cáo (POST/COMMENT) + đóng báo cáo liên quan",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/admin/{report_id}/remove-target")
    public ResponseEntity<?> removeTarget(@PathVariable("report_id") String reportId) {
        ResponseData payload = new ResponseData();
        try {
            reportService.removeReportedTarget(reportId);
            payload.setDescription("Đã xoá nội dung và đóng báo cáo");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Admin: khôi phục bài viết bị ẩn tự động + đóng báo cáo liên quan",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/admin/{report_id}/restore-target")
    public ResponseEntity<?> restoreTarget(@PathVariable("report_id") String reportId) {
        ResponseData payload = new ResponseData();
        try {
            reportService.restoreReportedTarget(reportId);
            payload.setDescription("Đã khôi phục bài viết và đóng báo cáo");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Admin: xử lý báo cáo", description = "status: RESOLVED | DISMISSED",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/admin/{report_id}")
    public ResponseEntity<?> handle(@PathVariable("report_id") String reportId,
                                    @RequestParam String status) {
        ResponseData payload = new ResponseData();
        try {
            reportService.handle(reportId, status);
            payload.setDescription("Đã cập nhật báo cáo");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
