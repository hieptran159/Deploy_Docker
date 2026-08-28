package com.didan.social.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import org.springframework.web.multipart.MultipartFile;

public class CreatePostRequest {
    @JsonProperty(required = true)
    private String title;
    @JsonProperty(required = false)
    private MultipartFile postImg = null;
    @JsonProperty(required = true)
    private String body;
    // "true" -> lưu bản nháp (không lên feed)
    @JsonProperty(required = false)
    private String draft;
    // "friends" -> chỉ bạn bè xem được; còn lại = công khai
    @JsonProperty(required = false)
    private String visibility;

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public CreatePostRequest() {
    }

    public String getDraft() {
        return draft;
    }

    public void setDraft(String draft) {
        this.draft = draft;
    }

    public boolean isDraft() {
        return "true".equalsIgnoreCase(draft) || "1".equals(draft);
    }

    public CreatePostRequest(String title, MultipartFile postImg, String body) {
        this.title = title;
        this.postImg = postImg;
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public MultipartFile getPostImg() {
        return postImg;
    }

    public void setPostImg(MultipartFile postImg) {
        this.postImg = postImg;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
