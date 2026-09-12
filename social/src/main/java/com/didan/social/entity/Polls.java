package com.didan.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;

/**
 * Một cuộc bình chọn gắn vào một bài viết (post_id là UNIQUE — mỗi bài tối đa một).
 *
 * Câu hỏi chính là TIÊU ĐỀ bài viết, nên không có cột question.
 *
 * Cột phẳng, không quan hệ JPA: bình chọn được nạp theo lô cho cả feed
 * (xem PostServiceImpl.applyPolls), quan hệ lazy ở đây chỉ tổ đẻ ra N+1.
 */
@Entity(name = "polls")
@Table(name = "polls")
public class Polls {

    @Id
    @Column(name = "poll_id", length = 50)
    private String pollId;

    @Column(name = "post_id", length = 50, nullable = false)
    private String postId;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public Polls() {}

    public Polls(String pollId, String postId, Date createdAt) {
        this.pollId = pollId;
        this.postId = postId;
        this.createdAt = createdAt;
    }

    public String getPollId() { return pollId; }
    public void setPollId(String pollId) { this.pollId = pollId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
