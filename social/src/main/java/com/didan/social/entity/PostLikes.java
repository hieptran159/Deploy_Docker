package com.didan.social.entity;

import com.didan.social.entity.keys.PostLikeId;
import jakarta.persistence.*;

@Entity(name = "post_likes")
public class PostLikes {
    @EmbeddedId
    PostLikeId postLikeId;

    @ManyToOne
    @JoinColumn(name = "post_id", updatable = false, insertable = false)
    private Posts posts;

    @ManyToOne
    @JoinColumn(name = "user_id", updatable = false, insertable = false)
    private Users users;

    // loại cảm xúc: LIKE | LOVE | HAHA | WOW | SAD | ANGRY (null = LIKE cho dữ liệu cũ)
    @Column(name = "type", length = 20)
    private String type;

    public PostLikes() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PostLikes(PostLikeId postLikeId, Posts posts, Users users) {
        this.postLikeId = postLikeId;
        this.posts = posts;
        this.users = users;
    }

    public PostLikeId getPostLikeId() {
        return postLikeId;
    }

    public void setPostLikeId(PostLikeId postLikeId) {
        this.postLikeId = postLikeId;
    }

    public Posts getPosts() {
        return posts;
    }

    public void setPosts(Posts posts) {
        this.posts = posts;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }
}
