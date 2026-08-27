package com.didan.social.entity;

import com.didan.social.entity.keys.CommentLikeId;
import jakarta.persistence.*;

@Entity(name = "comment_likes")
public class CommentLikes {
    @EmbeddedId
    private CommentLikeId commentLikeId;

    // Khoa ngoai comment_id, user_id
    @ManyToOne
    @JoinColumn(name = "comment_id", updatable = false, insertable = false)
    private Comments comments;

    @ManyToOne
    @JoinColumn(name = "user_id", updatable = false, insertable = false)
    private Users users;

    // loại cảm xúc: LIKE | LOVE | HAHA | WOW | SAD | ANGRY (null = LIKE cho dữ liệu cũ)
    @Column(name = "type", length = 20)
    private String type;

    public CommentLikes() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public CommentLikes(CommentLikeId commentLikeId, Comments comments, Users users) {
        this.commentLikeId = commentLikeId;
        this.comments = comments;
        this.users = users;
    }

    public CommentLikeId getCommentLikeId() {
        return commentLikeId;
    }

    public void setCommentLikeId(CommentLikeId commentLikeId) {
        this.commentLikeId = commentLikeId;
    }

    public Comments getComments() {
        return comments;
    }

    public void setComments(Comments comments) {
        this.comments = comments;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }
}
