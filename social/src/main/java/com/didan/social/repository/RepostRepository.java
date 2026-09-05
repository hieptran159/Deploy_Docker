package com.didan.social.repository;

import com.didan.social.entity.Reposts;
import com.didan.social.entity.keys.RepostId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface RepostRepository extends JpaRepository<Reposts, RepostId> {

    boolean existsByRepostId_UserIdAndRepostId_PostId(String userId, String postId);

    long countByRepostId_PostId(String postId);

    @Transactional
    void deleteByRepostId_UserIdAndRepostId_PostId(String userId, String postId);

    // Dọn lượt chia sẻ khi xoá bài. Bảng reposts do ddl-auto tạo nên không có khoá
    // ngoại, xoá bài mà bỏ sót thì còn lại hàng mồ côi — FEED_UNION vẫn gộp chúng
    // vào nên feed sinh ra mục trỏ tới bài không còn tồn tại.
    @Transactional
    void deleteByRepostId_PostId(String postId);

    List<Reposts> findByRepostId_UserIdOrderByCreatedAtDesc(String userId);

    Page<Reposts> findByRepostId_UserId(String userId, Pageable pageable);

    long countByRepostId_UserId(String userId);

    // Số lượt repost cho nhiều bài (1 truy vấn) -> [postId, count]
    @Query("SELECT r.repostId.postId, COUNT(r) FROM reposts r WHERE r.repostId.postId IN :ids GROUP BY r.repostId.postId")
    List<Object[]> countForPosts(@Param("ids") Collection<String> ids);

    // Các bài mà user hiện tại đã repost, trong tập ids
    @Query("SELECT r.repostId.postId FROM reposts r WHERE r.repostId.userId = :uid AND r.repostId.postId IN :ids")
    List<String> repostedByUserIn(@Param("uid") String uid, @Param("ids") Collection<String> ids);

    // Tất cả lượt repost của các bài trong ids, mới nhất trước (service lấy dòng đầu / mỗi post)
    @Query("SELECT r FROM reposts r WHERE r.repostId.postId IN :ids ORDER BY r.createdAt DESC")
    List<Reposts> findByPostIdsOrderByCreatedAtDesc(@Param("ids") Collection<String> ids);
}
