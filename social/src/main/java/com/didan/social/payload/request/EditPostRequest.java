package com.didan.social.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.multipart.MultipartFile;

public class EditPostRequest {
    @JsonProperty(required = false)
    private String title;
    @JsonProperty(required = false)
    private MultipartFile postImg = null;
    // Nhiều ảnh. postImg (một ảnh) vẫn nhận để auto-poster và client cũ không gãy;
    // nếu có postImgs thì postImgs thắng.
    @JsonProperty(required = false)
    private java.util.List<MultipartFile> postImgs;

    public java.util.List<MultipartFile> getPostImgs() {
        return postImgs;
    }

    public void setPostImgs(java.util.List<MultipartFile> postImgs) {
        this.postImgs = postImgs;
    }
    @JsonProperty(required = false)
    private String body;
    @JsonProperty(required = false)
    private String visibility;

    public EditPostRequest() {
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public EditPostRequest(String title, MultipartFile postImg, String body) {
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
