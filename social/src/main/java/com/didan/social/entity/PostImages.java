package com.didan.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Một ảnh của bài viết. Thay cho cột đơn posts.post_img (chỉ chứa được một ảnh).
 *
 * Cột phẳng, không quan hệ JPA: ảnh được nạp theo lô cho cả feed
 * (PostServiceImpl.applyImages), quan hệ lazy ở đây chỉ tổ đẻ ra N+1.
 */
@Entity(name = "post_images")
@Table(name = "post_images")
public class PostImages {

    @Id
    @Column(name = "image_id", length = 50)
    private String imageId;

    @Column(name = "post_id", length = 50, nullable = false)
    private String postId;

    /** Đường dẫn tương đối dưới /images/, ví dụ "post/abc-0.jpg". */
    @Column(name = "url", length = 255, nullable = false)
    private String url;

    @Column(name = "position", nullable = false)
    private Integer position;

    public PostImages() {}

    public PostImages(String imageId, String postId, String url, int position) {
        this.imageId = imageId;
        this.postId = postId;
        this.url = url;
        this.position = position;
    }

    public String getImageId() { return imageId; }
    public void setImageId(String imageId) { this.imageId = imageId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
