package com.didan.social.entity;

import com.didan.social.entity.keys.BookmarkId;
import jakarta.persistence.*;

import java.util.Date;

@Entity(name = "bookmarks")
public class Bookmarks {
    @EmbeddedId
    private BookmarkId bookmarkId;

    @ManyToOne
    @JoinColumn(name = "post_id", updatable = false, insertable = false)
    private Posts posts;

    @ManyToOne
    @JoinColumn(name = "user_id", updatable = false, insertable = false)
    private Users users;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public Bookmarks() {
    }

    public Bookmarks(BookmarkId bookmarkId, Date createdAt) {
        this.bookmarkId = bookmarkId;
        this.createdAt = createdAt;
    }

    public BookmarkId getBookmarkId() {
        return bookmarkId;
    }

    public void setBookmarkId(BookmarkId bookmarkId) {
        this.bookmarkId = bookmarkId;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
