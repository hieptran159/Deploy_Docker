package com.didan.social.dto;

public class CategoryDTO {
    private String categoryId;
    private String name;
    private String slug;

    public CategoryDTO() {}

    public CategoryDTO(String categoryId, String name, String slug) {
        this.categoryId = categoryId;
        this.name = name;
        this.slug = slug;
    }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
}
