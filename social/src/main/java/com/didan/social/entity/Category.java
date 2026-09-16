package com.didan.social.entity;

import jakarta.persistence.*;

/**
 * Chuyên mục khi đăng bài (Flyway V14). Đơn giản có chủ ý: chỉ tên + slug + vị trí
 * hiển thị, không lồng cây danh mục — 5 mục seed sẵn, admin thêm/sửa/xoá qua
 * `/admin/categories`. `posts.category_id` KHÔNG có khoá ngoại tới bảng này
 * (xem ghi chú trong V14__categories.sql).
 */
@Entity(name = "categories")
@Table(name = "categories")
public class Category {
    @Id
    @Column(name = "category_id")
    private String categoryId;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "slug", nullable = false, length = 60)
    private String slug;

    @Column(name = "position", nullable = false)
    private Integer position = 0;

    public Category() {}

    public Category(String categoryId, String name, String slug, Integer position) {
        this.categoryId = categoryId;
        this.name = name;
        this.slug = slug;
        this.position = position;
    }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
