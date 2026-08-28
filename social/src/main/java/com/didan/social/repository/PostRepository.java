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
