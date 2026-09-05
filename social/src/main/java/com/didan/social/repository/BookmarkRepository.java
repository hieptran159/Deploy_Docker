package com.didan.social.repository;

import com.didan.social.entity.Bookmarks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmarks, com.didan.social.entity.keys.BookmarkId> {
    Bookmarks findFirstByBookmarkId_PostIdAndBookmarkId_UserId(String postId, String userId);

    // Dọn lượt lưu khi xoá bài. bookmarks.post_id có khoá ngoại trỏ tới posts nhưng
    // entity Posts KHÔNG cascade sang Bookmarks, nên thiếu bước này thì bất kỳ bài
    // nào đã có người lưu đều không xoá được (vi phạm ràng buộc khoá ngoại).
    @org.springframework.transaction.annotation.Transactional
    void deleteByBookmarkId_PostId(String postId);
    List<Bookmarks> findAllByBookmarkId_UserIdOrderByCreatedAtDesc(String userId);
}
