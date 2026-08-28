package com.didan.social.repository;

import com.didan.social.entity.Posts;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Posts, String> {
    // Điều kiện "đã đăng": status NULL (dữ liệu cũ) hoặc 'published'. Bản nháp ('draft') bị loại.
    String PUBLISHED = "(p.status IS NULL OR p.status = 'published')";

    // Bài mới đăng lên trước; p.postId làm tiebreaker để phân trang không bị xáo trộn
    @Query("SELECT p FROM posts p WHERE " + PUBLISHED + " ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> findAllPost();
    // Trả List (không phải Page) -> Spring Data chỉ chạy SELECT có LIMIT/OFFSET, bỏ COUNT thừa mỗi lần đổi trang
    @Query("SELECT p FROM posts p WHERE " + PUBLISHED + " ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> findAllPostByCommentAtOrPostAt(Pageable pageable);

    @Query("SELECT COUNT(p) FROM posts p WHERE " + PUBLISHED)
    long countPublished();

    long countByStatus(String status);

    // Số bài đã đăng theo ngày, từ mốc :since (cho biểu đồ dashboard)
    @Query(value = "SELECT DATE(posted_at) d, COUNT(*) c FROM posts "
                 + "WHERE (status IS NULL OR status = 'published') AND posted_at >= :since "
                 + "GROUP BY DATE(posted_at) ORDER BY d", nativeQuery = true)
    List<Object[]> countPostsPerDaySince(@Param("since") java.sql.Timestamp since);

    // Feed loại trừ bài của các tác giả bị chặn (2 chiều). Gọi khi tập loại trừ khác rỗng.
    @Query("SELECT p FROM posts p WHERE " + PUBLISHED + " AND p.userPost.users.userId NOT IN :ex "
         + "ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> findFeedExcludingAuthors(@Param("ex") Collection<String> ex, Pageable pageable);

    @Query("SELECT COUNT(p) FROM posts p WHERE " + PUBLISHED + " AND p.userPost.users.userId NOT IN :ex")
    long countFeedExcludingAuthors(@Param("ex") Collection<String> ex);

    // Bản nháp của một người dùng (mới nhất trước)
    @Query("SELECT p FROM posts p WHERE p.status = 'draft' AND p.userPost.users.userId = :uid "
         + "ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> findDraftsOfAuthor(@Param("uid") String uid);

    // Feed hợp nhất: bài gốc (theo posted_at) + lượt chia sẻ (theo created_at của repost).
    // :ex phải KHÁC RỖNG (native NOT IN) -> service truyền sentinel khi không loại trừ ai.
    String FEED_UNION =
        "SELECT p.post_id AS pid, p.posted_at AS t, NULL AS reposter " +
        "FROM posts p JOIN user_posts up ON up.post_id = p.post_id " +
        "WHERE (p.status IS NULL OR p.status = 'published') AND up.user_id NOT IN (:ex) " +
        "UNION ALL " +
        "SELECT rp.post_id AS pid, rp.created_at AS t, rp.user_id AS reposter " +
        "FROM reposts rp JOIN posts p2 ON p2.post_id = rp.post_id " +
        "JOIN user_posts up2 ON up2.post_id = rp.post_id " +
        "WHERE (p2.status IS NULL OR p2.status = 'published') " +
        "AND up2.user_id NOT IN (:ex) AND rp.user_id NOT IN (:ex)";

    @Query(value = "SELECT src.pid, src.t, src.reposter FROM (" + FEED_UNION
                 + ") src ORDER BY src.t DESC, src.pid ASC LIMIT :lim OFFSET :off", nativeQuery = true)
    List<Object[]> feedPage(@Param("ex") Collection<String> ex, @Param("lim") int lim, @Param("off") int off);

    @Query(value = "SELECT COUNT(*) FROM (" + FEED_UNION + ") src", nativeQuery = true)
    long feedCount(@Param("ex") Collection<String> ex);

    // Join all the tables to get the post, user, likes, comments and sub-comments
    @EntityGraph(attributePaths = {"userPost", "postLikes", "userComments", "userComments.comments"}, type = EntityGraph.EntityGraphType.FETCH)
    Posts findFirstByPostId(String postId);

    // Tìm kiếm không phân biệt hoa thường, phân trang. Bỏ @EntityGraph nặng (fetch-join
    // comments/likes tạo tích Descartes) - toListDTO chỉ cần author + likes, đã có
    // default_batch_fetch_size lo phần nạp theo lô.
    @Query("SELECT p FROM posts p WHERE " + PUBLISHED + " AND (lower(p.title) LIKE lower(concat('%', :q, '%')) "
         + "OR lower(p.body) LIKE lower(concat('%', :q, '%'))) ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> searchByKeyword(@Param("q") String q, Pageable pageable);

    @Query("SELECT p FROM posts p WHERE " + PUBLISHED + " AND (lower(p.title) LIKE lower(concat('%', :q, '%')) "
         + "OR lower(p.body) LIKE lower(concat('%', :q, '%'))) AND p.userPost.users.userId NOT IN :ex "
         + "ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> searchByKeywordExcludingAuthors(@Param("q") String q,
                                                @Param("ex") Collection<String> ex,
                                                Pageable pageable);
}
