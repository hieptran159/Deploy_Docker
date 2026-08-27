package com.didan.social.controller;

import com.didan.social.payload.ResponseData;
import com.didan.social.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@RestController
@Tag(name = "Friend")
@RequestMapping("/friend")
public class FollowController {
    private final FollowService followService;

    @Autowired
    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    private ResponseEntity<?> run(Callable<Object> action, String okMsg) {
        ResponseData payload = new ResponseData();
        try {
            payload.setData(action.call());
            payload.setDescription(okMsg);
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Danh sách bạn bè của 1 người dùng", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/list/{userId}")
    public ResponseEntity<?> getFriends(@PathVariable String userId) {
        return run(() -> followService.getFriends(userId), "OK");
    }

    @Operation(summary = "Lời mời kết bạn đang nhận (chờ tôi chấp nhận)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/requests/incoming")
    public ResponseEntity<?> incoming() {
        return run(() -> followService.getIncomingRequests(), "OK");
    }

    @Operation(summary = "Lời mời kết bạn tôi đã gửi", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/requests/outgoing")
    public ResponseEntity<?> outgoing() {
        return run(() -> followService.getOutgoingRequests(), "OK");
    }

    @Operation(summary = "Quan hệ giữa tôi và userId", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/status/{userId}")
    public ResponseEntity<?> status(@PathVariable String userId) {
        return run(() -> {
            Map<String, String> m = new HashMap<>();
            m.put("status", followService.friendStatus(userId));
            return m;
        }, "OK");
    }

    @Operation(summary = "Gửi lời mời kết bạn", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/request/{userId}")
    public ResponseEntity<?> request(@PathVariable String userId) {
        return run(() -> followService.sendRequest(userId), "Đã gửi lời mời kết bạn");
    }

    @Operation(summary = "Chấp nhận lời mời kết bạn từ userId", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/accept/{userId}")
    public ResponseEntity<?> accept(@PathVariable String userId) {
        return run(() -> followService.acceptRequest(userId), "Đã chấp nhận");
    }

    @Operation(summary = "Từ chối lời mời kết bạn từ userId", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/decline/{userId}")
    public ResponseEntity<?> decline(@PathVariable String userId) {
        return run(() -> followService.declineRequest(userId), "Đã từ chối");
    }

    @Operation(summary = "Huỷ lời mời kết bạn mình đã gửi", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/cancel/{userId}")
    public ResponseEntity<?> cancel(@PathVariable String userId) {
        return run(() -> followService.cancelRequest(userId), "Đã huỷ lời mời");
    }

    @Operation(summary = "Huỷ kết bạn", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> unfriend(@PathVariable String userId) {
        return run(() -> followService.unfriend(userId), "Đã huỷ kết bạn");
    }
}
