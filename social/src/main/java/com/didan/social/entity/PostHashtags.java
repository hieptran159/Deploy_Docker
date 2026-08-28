package com.didan.social.entity;

import com.didan.social.entity.keys.PostHashtagId;
import jakarta.persistence.*;

/**
 * Thẻ hashtag (#chu_de) gắn với 1 bài viết. Khóa kép (post_id, tag) -> mỗi bài
 * không lặp tag. Tag được chuẩn hóa: bỏ dấu '#', viết thường. Bảng tự tạo bởi
 * ddl-auto=update.
 */
@Entity(name = "post_hashtags")
@Table(name = "post_hashtags", indexes = {
        @Index(name = "idx_post_hashtags_tag", columnList = "tag"),
        @Index(name = "idx_post_hashtags_post", columnList = "post_id")
})
public class PostHashtags {
    @EmbeddedId
    private PostHashtagId postHashtagId;

    public PostHashtags() {
    }

    public PostHashtags(PostHashtagId postHashtagId) {
        this.postHashtagId = postHashtagId;
    }

    public PostHashtagId getPostHashtagId() { return postHashtagId; }
    public void setPostHashtagId(PostHashtagId postHashtagId) { this.postHashtagId = postHashtagId; }
}
