package com.didan.social.controller;

import com.didan.social.payload.ResponseData;
import com.didan.social.service.BookmarkService;
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
@Tag(name = "Bookmark")
@RequestMapping("/bookmark")
public class BookmarkController {
    private final BookmarkService bookmarkService;

    @Autowired
    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    // Bật/tắt lưu bài viết
    @Operation(summary = "Toggle bookmark for a post",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{post_id}")
    public ResponseEntity<?> toggle(@PathVariable("post_id") String postId) {
        ResponseData payload = new ResponseData();
        try {
            boolean bookmarked = bookmarkService.toggle(postId);
            Map<String, Object> data = new HashMap<>();
            data.put("bookmarked", bookmarked);
            payload.setDescription(bookmarked ? "Đã lưu bài viết" : "Đã bỏ lưu bài viết");
            payload.setData(data);
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // Kiểm tra 1 bài đã được lưu chưa
    @Operation(summary = "Check whether a post is bookmarked",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/check/{post_id}")
    public ResponseEntity<?> check(@PathVariable("post_id") String postId) {
        ResponseData payload = new ResponseData();
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("bookmarked", bookmarkService.isBookmarked(postId));
            payload.setDescription("OK");
            payload.setData(data);
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // Danh sách bài đã lưu của tôi
    @Operation(summary = "List my bookmarked post ids",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/mine")
    public ResponseEntity<?> mine() {
        ResponseData payload = new ResponseData();
        try {
            payload.setData(bookmarkService.listMine());
            payload.setDescription("Load bookmarks successful");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
