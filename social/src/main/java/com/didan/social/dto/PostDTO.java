package com.didan.social.dto;

import java.util.List;
import java.util.Map;

public class PostDTO {
    private String postId;
    private String userCreatedPost;
    private String authorName;
    private String authorAvatar;
    private String title;
    private String postImg;
    private String body;
    private String postedAt;
    private String status;
    private int likesQuantity;
    private List<String> userLikedPost;
    private int commentsQuantity;
    private List<CommentDTO> comments;
    private Map<String, Long> reactionCounts;
    private String myReaction;

    public PostDTO() {
    }

    public PostDTO(String postId, String userCreatedPost, String title, String postImg, String body, String postedAt, int likesQuantity, List<String> userLikedPost, int commentsQuantity, List<CommentDTO> comments) {
        this.postId = postId;
        this.userCreatedPost = userCreatedPost;
        this.title = title;
        this.postImg = postImg;
        this.body = body;
        this.postedAt = postedAt;
        this.likesQuantity = likesQuantity;
        this.userLikedPost = userLikedPost;
        this.commentsQuantity = commentsQuantity;
        this.comments = comments;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUserCreatedPost() {
        return userCreatedPost;
    }

    public void setUserCreatedPost(String userCreatedPost) {
        this.userCreatedPost = userCreatedPost;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPostImg() {
        return postImg;
    }

    public void setPostImg(String postImg) {
        this.postImg = postImg;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(String postedAt) {
        this.postedAt = postedAt;
    }

    public int getLikesQuantity() {
        return likesQuantity;
    }

    public void setLikesQuantity(int likesQuantity) {
        this.likesQuantity = likesQuantity;
    }

    public List<String> getUserLikedPost() {
        return userLikedPost;
    }

    public void setUserLikedPost(List<String> userLikedPost) {
        this.userLikedPost = userLikedPost;
    }

    public int getCommentsQuantity() {
        return commentsQuantity;
    }

    public void setCommentsQuantity(int commentsQuantity) {
        this.commentsQuantity = commentsQuantity;
    }

    public List<CommentDTO> getComments() {
        return comments;
    }

    public void setComments(List<CommentDTO> comments) {
        this.comments = comments;
    }

    public Map<String, Long> getReactionCounts() {
        return reactionCounts;
    }

    public void setReactionCounts(Map<String, Long> reactionCounts) {
        this.reactionCounts = reactionCounts;
    }

    public String getMyReaction() {
        return myReaction;
    }

    public void setMyReaction(String myReaction) {
        this.myReaction = myReaction;
    }
}
