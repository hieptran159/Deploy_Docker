package com.didan.social.repository;

import com.didan.social.entity.Posts;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Posts, String> {
    // Điều kiện "đã đăng": status NULL (dữ liệu cũ) hoặc 'published'. Bản nháp ('draft') bị loại.
    String PUBLISHED = "(p.status IS NULL OR p.status = 'published')";
    // Quyền xem: công khai (NULL/'public') cho mọi người; bài của chính người xem (:me) luôn thấy;
    // 'friends' chỉ khi tác giả nằm trong :vids (bạn bè của người xem + chính người xem);
    // 'private' chỉ chính chủ (đã được nhánh ':me' phủ).
    String VISIBLE = "(p.visibility IS NULL OR p.visibility = 'public'"
            + " OR p.userPost.users.userId = :me"
            + " OR (p.visibility = 'friends' AND p.userPost.users.userId IN :vids))";

    // Nạp bài theo id KÈM tác giả trong 1 truy vấn. Cần vì Posts.userPost là @OneToOne(mappedBy)
    // nghịch đảo -> Hibernate KHÔNG thể lazy/batch, mỗi bài = 1 SELECT phụ (N+1) khi dựng feed.
    @EntityGraph(attributePaths = {"userPost", "userPost.users"})
    List<Posts> findByPostIdIn(Collection<String> ids);

    @EntityGraph(attributePaths = {"userPost", "userPost.users"})
    @Query("SELECT p FROM posts p WHERE " + PUBLISHED + " AND " + VISIBLE + " ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> findAllPost(@Param("vids") Collection<String> vids, @Param("me") String me);

    @Query("SELECT COUNT(p) FROM posts p WHERE " + PUBLISHED)
    long countPublished();

    long countByStatus(String status);

    // Số bài đã đăng theo ngày, từ mốc :since (cho biểu đồ dashboard)
    @Query(value = "SELECT DATE(posted_at) d, COUNT(*) c FROM posts "
                 + "WHERE (status IS NULL OR status = 'published') AND posted_at >= :since "
                 + "GROUP BY DATE(posted_at) ORDER BY d", nativeQuery = true)
    List<Object[]> countPostsPerDaySince(@Param("since") java.sql.Timestamp since);

    // Bản nháp của một người dùng (mới nhất trước)
    @EntityGraph(attributePaths = {"userPost", "userPost.users"})
    @Query("SELECT p FROM posts p WHERE p.status = 'draft' AND p.userPost.users.userId = :uid "
         + "ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> findDraftsOfAuthor(@Param("uid") String uid);

    @EntityGraph(attributePaths = {"userPost", "userPost.users"})
    @Query("SELECT p FROM posts p WHERE p.status = 'draft' AND p.userPost.users.userId = :uid "
         + "ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> findDraftsOfAuthor(@Param("uid") String uid, Pageable pageable);

    @Query("SELECT COUNT(p) FROM posts p WHERE p.status = 'draft' AND p.userPost.users.userId = :uid")
    long countDraftsOfAuthor(@Param("uid") String uid);

    // Lịch sử bài đã đăng của 1 người dùng (mới nhất trước), phân trang; lọc theo quyền xem của người xem
    @EntityGraph(attributePaths = {"userPost", "userPost.users"})
    @Query("SELECT p FROM posts p WHERE " + PUBLISHED + " AND " + VISIBLE
         + " AND p.userPost.users.userId = :uid ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> findPublishedByAuthor(@Param("uid") String uid, @Param("vids") Collection<String> vids,
                                      @Param("me") String me, Pageable pageable);

    @Query("SELECT COUNT(p) FROM posts p WHERE " + PUBLISHED + " AND " + VISIBLE + " AND p.userPost.users.userId = :uid")
    long countPublishedByAuthor(@Param("uid") String uid, @Param("vids") Collection<String> vids, @Param("me") String me);

    // Feed hợp nhất: bài gốc (posted_at, is_repost=0) + lượt chia sẻ (created_at, is_repost=1).
    // Gộp theo post_id -> mỗi bài chỉ 1 dòng. Vì thời điểm repost luôn > posted_at,
    // MAX(t) = thời điểm repost mới nhất nếu có repost, ngược lại là posted_at.
    // :ex và :vids phải KHÁC RỖNG (native IN/NOT IN) -> service truyền sentinel "-".
    String FEED_UNION =
        "SELECT p.post_id AS pid, p.posted_at AS t, 0 AS is_repost " +
        "FROM posts p JOIN user_posts up ON up.post_id = p.post_id " +
        "WHERE (p.status IS NULL OR p.status = 'published') AND up.user_id NOT IN (:ex) " +
        "AND (p.visibility IS NULL OR p.visibility = 'public' OR up.user_id = :me " +
        "     OR (p.visibility = 'friends' AND up.user_id IN (:vids))) " +
        "UNION ALL " +
        "SELECT rp.post_id AS pid, rp.created_at AS t, 1 AS is_repost " +
        "FROM reposts rp JOIN posts p2 ON p2.post_id = rp.post_id " +
        "JOIN user_posts up2 ON up2.post_id = rp.post_id " +
        "WHERE (p2.status IS NULL OR p2.status = 'published') " +
        "AND up2.user_id NOT IN (:ex) AND rp.user_id NOT IN (:ex) " +
        "AND (p2.visibility IS NULL OR p2.visibility = 'public' OR up2.user_id = :me " +
        "     OR (p2.visibility = 'friends' AND up2.user_id IN (:vids)))";

    @Query(value = "SELECT u.pid, MAX(u.t) AS sort_t, MAX(u.is_repost) AS is_repost FROM (" + FEED_UNION
                 + ") u GROUP BY u.pid ORDER BY sort_t DESC, u.pid ASC LIMIT :lim OFFSET :off", nativeQuery = true)
    List<Object[]> feedPage(@Param("ex") Collection<String> ex, @Param("vids") Collection<String> vids,
                            @Param("me") String me, @Param("lim") int lim, @Param("off") int off);

    // COUNT(DISTINCT) trên UNION toàn bộ bài + lượt chia sẻ, chạy mỗi lần mở trang chủ
    // (chỉ để biết có bao nhiêu trang). Khoá cache theo :me là đủ — ex/vids đều suy ra
    // từ chính :me. Số trang trễ 60 giây thì không ai chết.
    @org.springframework.cache.annotation.Cacheable(cacheNames = com.didan.social.config.CacheConfig.FEED_COUNT,
            key = "#me")
    @Query(value = "SELECT COUNT(DISTINCT u.pid) FROM (" + FEED_UNION + ") u", nativeQuery = true)
    long feedCount(@Param("ex") Collection<String> ex, @Param("vids") Collection<String> vids, @Param("me") String me);

    // Bảng tin bạn bè: bài gốc của người trong :ids + lượt chia sẻ do người trong :ids thực hiện.
    // (Không cần lọc "chỉ bạn bè": mọi bài đều của bạn bè / của mình rồi.)
    String FRIEND_FEED_UNION =
        "SELECT p.post_id AS pid, p.posted_at AS t, 0 AS is_repost " +
        "FROM posts p JOIN user_posts up ON up.post_id = p.post_id " +
        "WHERE (p.status IS NULL OR p.status = 'published') AND up.user_id IN (:ids) " +
        "AND (p.visibility IS NULL OR p.visibility <> 'private') " +
        "UNION ALL " +
        "SELECT rp.post_id AS pid, rp.created_at AS t, 1 AS is_repost " +
        "FROM reposts rp JOIN posts p2 ON p2.post_id = rp.post_id " +
        "WHERE (p2.status IS NULL OR p2.status = 'published') AND rp.user_id IN (:ids) " +
        "AND (p2.visibility IS NULL OR p2.visibility <> 'private')";

    @Query(value = "SELECT u.pid, MAX(u.t) AS sort_t, MAX(u.is_repost) AS is_repost FROM (" + FRIEND_FEED_UNION
                 + ") u GROUP BY u.pid ORDER BY sort_t DESC, u.pid ASC LIMIT :lim OFFSET :off", nativeQuery = true)
    List<Object[]> friendFeedPage(@Param("ids") Collection<String> ids, @Param("lim") int lim, @Param("off") int off);

    @Query(value = "SELECT COUNT(DISTINCT u.pid) FROM (" + FRIEND_FEED_UNION + ") u", nativeQuery = true)
    long friendFeedCount(@Param("ids") Collection<String> ids);

    // Join all the tables to get the post, user, likes, comments and sub-comments
    @EntityGraph(attributePaths = {"userPost", "postLikes", "userComments", "userComments.comments"}, type = EntityGraph.EntityGraphType.FETCH)
    Posts findFirstByPostId(String postId);

    // Tìm kiếm không phân biệt hoa thường, phân trang. Chỉ fetch-join tác giả (to-one, an toàn
    // với Pageable); comments/likes để default_batch_fetch_size nạp theo lô, tránh tích Descartes.
    @EntityGraph(attributePaths = {"userPost", "userPost.users"})
    @Query("SELECT p FROM posts p WHERE " + PUBLISHED + " AND " + VISIBLE
         + " AND (lower(p.title) LIKE lower(concat('%', :q, '%')) "
         + "OR lower(p.body) LIKE lower(concat('%', :q, '%'))) ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> searchByKeyword(@Param("q") String q, @Param("vids") Collection<String> vids,
                                @Param("me") String me, Pageable pageable);

    @EntityGraph(attributePaths = {"userPost", "userPost.users"})
    @Query("SELECT p FROM posts p WHERE " + PUBLISHED + " AND " + VISIBLE
         + " AND (lower(p.title) LIKE lower(concat('%', :q, '%')) "
         + "OR lower(p.body) LIKE lower(concat('%', :q, '%'))) AND p.userPost.users.userId NOT IN :ex "
         + "ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> searchByKeywordExcludingAuthors(@Param("q") String q,
                                                @Param("ex") Collection<String> ex,
                                                @Param("vids") Collection<String> vids,
                                                @Param("me") String me,
                                                Pageable pageable);

    /**
     * Cộng lượt xem bằng một UPDATE nguyên tử thay vì đọc-sửa-ghi: hai người mở
     * cùng lúc mà đọc-sửa-ghi thì một lượt bị nuốt. Cũng tránh làm bẩn entity
     * đang được dùng để dựng DTO.
     */
    @Modifying
    @Transactional
    @Query("UPDATE posts p SET p.views = p.views + 1 WHERE p.postId = :id")
    void incrementViews(@Param("id") String id);

    // ---------- Tìm kiếm FULLTEXT ----------
    // Native vì JPQL không có MATCH ... AGAINST. Cùng bộ lọc hiển thị với FEED_UNION.
    // :ex và :vids phải KHÁC RỖNG (native IN/NOT IN) -> service truyền sentinel "-".
    // Xếp theo ĐIỂM KHỚP trước, rồi mới tới ngày — LIKE cũ chỉ xếp theo ngày nên bài
    // liên quan nhất chưa chắc lên đầu.
    String FT_WHERE =
        "FROM posts p JOIN user_posts up ON up.post_id = p.post_id "
      + "WHERE MATCH(p.title, p.body) AGAINST (:q IN BOOLEAN MODE) "
      + "AND (p.status IS NULL OR p.status = 'published') AND up.user_id NOT IN (:ex) "
      + "AND (p.visibility IS NULL OR p.visibility = 'public' OR up.user_id = :me "
      + "     OR (p.visibility = 'friends' AND up.user_id IN (:vids)))";

    // Xếp hạng hai tầng: khớp TIÊU ĐỀ trước (điểm 0 nếu không khớp -> mọi bài khớp
    // tiêu đề lên trên), rồi mới tới điểm khớp chung, rồi mới tới ngày.
    // Cần chỉ mục riêng ft_posts_title: MATCH(title, body) tính một điểm cho cả cụm
    // cột nên KHÔNG ưu tiên tiêu đề — đã đo trên MySQL thật.
    @Query(value = "SELECT p.post_id " + FT_WHERE
                 + " ORDER BY MATCH(p.title) AGAINST (:q IN BOOLEAN MODE) DESC,"
                 + " MATCH(p.title, p.body) AGAINST (:q IN BOOLEAN MODE) DESC,"
                 + " p.posted_at DESC, p.post_id ASC LIMIT :lim OFFSET :off", nativeQuery = true)
    List<String> searchFulltextIds(@Param("q") String q, @Param("ex") Collection<String> ex,
                                   @Param("vids") Collection<String> vids, @Param("me") String me,
                                   @Param("lim") int lim, @Param("off") int off);
}
