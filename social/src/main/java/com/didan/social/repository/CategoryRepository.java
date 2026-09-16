package com.didan.social.repository;

import com.didan.social.config.CacheConfig;
import com.didan.social.entity.Category;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    // Đọc ở MỌI trang liệt kê bài (applyCategories) + mỗi lần mở form đăng bài ->
    // cache. Admin sửa/thêm/xoá phải @CacheEvict cache này (xem CategoryServiceImpl).
    @Cacheable(CacheConfig.CATEGORIES)
    List<Category> findAllByOrderByPositionAscNameAsc();

    // applyCategories nạp theo lô cho cả trang feed -> 1 câu IN, không phải 1 câu/bài.
    @Query("SELECT c FROM categories c WHERE c.categoryId IN :ids")
    List<Category> findByIdIn(@Param("ids") List<String> ids);
}
