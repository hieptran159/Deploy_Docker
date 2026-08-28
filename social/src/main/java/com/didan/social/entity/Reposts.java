package com.didan.social.entity;

import com.didan.social.entity.keys.RepostId;
import jakarta.persistence.*;

import java.util.Date;

/**
 * Một lượt chia sẻ (repost) bài viết. Khóa kép (user_id, post_id) -> mỗi người chỉ
 * chia sẻ 1 bài 1 lần. Bảng tự tạo bởi ddl-auto=update.
 */
@Entity(name = "reposts")
@Table(name = "reposts", indexes = {
        @Index(name = "idx_reposts_post", columnList = "post_id"),
        @Index(name = "idx_reposts_created", columnList = "created_at")
})
public class Reposts {
    @EmbeddedId
    private RepostId repostId;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public Reposts() {
    }

    public Reposts(RepostId repostId, String note, Date createdAt) {
        this.repostId = repostId;
        this.note = note;
        this.createdAt = createdAt;
    }

    public RepostId getRepostId() { return repostId; }
    public void setRepostId(RepostId repostId) { this.repostId = repostId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
