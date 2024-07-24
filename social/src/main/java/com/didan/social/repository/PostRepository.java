package com.didan.social.repository;

import com.didan.social.entity.Posts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Posts, String> {
    @Query("SELECT p FROM posts p LEFT JOIN p.userComments uc " +
            "LEFT JOIN uc.comments c " +
            "GROUP BY p " +
            "ORDER BY MAX(COALESCE(c.commentAt, p.postedAt)) DESC")
    List<Posts> findAllPost();
    @Query("SELECT p FROM posts p LEFT JOIN p.userComments uc " +
            "LEFT JOIN uc.comments c " +
            "GROUP BY p " +
            "ORDER BY MAX(COALESCE(c.commentAt, p.postedAt)) DESC")
    Page<Posts> findAllPostByCommentAtOrPostAt(Pageable pageable);

    // Join all the tables to get the post, user, likes, comments and sub-comments
    @EntityGraph(attributePaths = {"userPost", "postLikes", "userComments", "userComments.comments"}, type = EntityGraph.EntityGraphType.FETCH)
    Posts findFirstByPostId(String postId);

    @EntityGraph(attributePaths = {"userPost", "postLikes", "userComments", "userComments.comments"})
    List<Posts> findByTitleOrBodyContainingOrderByPostedAtDesc(String title, String body);

}
