package com.didan.social.repository;

import com.didan.social.entity.Posts;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Posts, String> {
    // Bài mới đăng lên trước; p.postId làm tiebreaker để phân trang không bị xáo trộn
    @Query("SELECT p FROM posts p ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> findAllPost();
    // Trả List (không phải Page) -> Spring Data chỉ chạy SELECT có LIMIT/OFFSET, bỏ COUNT thừa mỗi lần đổi trang
    @Query("SELECT p FROM posts p ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> findAllPostByCommentAtOrPostAt(Pageable pageable);

    // Join all the tables to get the post, user, likes, comments and sub-comments
    @EntityGraph(attributePaths = {"userPost", "postLikes", "userComments", "userComments.comments"}, type = EntityGraph.EntityGraphType.FETCH)
    Posts findFirstByPostId(String postId);

    // Tìm kiếm không phân biệt hoa thường, phân trang. Bỏ @EntityGraph nặng (fetch-join
    // comments/likes tạo tích Descartes) - toListDTO chỉ cần author + likes, đã có
    // default_batch_fetch_size lo phần nạp theo lô.
    @Query("SELECT p FROM posts p WHERE lower(p.title) LIKE lower(concat('%', :q, '%')) "
         + "OR lower(p.body) LIKE lower(concat('%', :q, '%')) ORDER BY p.postedAt DESC, p.postId ASC")
    List<Posts> searchByKeyword(@org.springframework.data.repository.query.Param("q") String q, Pageable pageable);

}
