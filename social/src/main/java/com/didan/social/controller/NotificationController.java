package com.didan.social.controller;

import com.didan.social.payload.ResponseData;
import com.didan.social.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Notification")
@RequestMapping("/notification")
public class NotificationController {
    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Get my notifications (newest first, max 50)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<?> getMine() {
        ResponseData payload = new ResponseData();
        try {
            payload.setData(notificationService.listMine());
            payload.setDescription("Load notifications successful");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Count my unread notifications",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount() {
        ResponseData payload = new ResponseData();
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("count", notificationService.unreadCountMine());
            payload.setData(data);
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Mark all my notifications as read",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/read-all")
    public ResponseEntity<?> readAll() {
        ResponseData payload = new ResponseData();
        try {
            notificationService.markAllRead();
            payload.setDescription("Marked all as read");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Mark one notification as read",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{notification_id}/read")
    public ResponseEntity<?> readOne(@PathVariable("notification_id") String notificationId) {
        ResponseData payload = new ResponseData();
        try {
            notificationService.markRead(notificationId);
            payload.setDescription("Marked as read");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
