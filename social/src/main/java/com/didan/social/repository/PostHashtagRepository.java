package com.didan.social.repository;

import com.didan.social.entity.PostHashtags;
import com.didan.social.entity.keys.PostHashtagId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtags, PostHashtagId> {

    @Transactional
    void deleteByPostHashtagId_PostId(String postId);

    @Query("SELECT ph.postHashtagId.tag FROM post_hashtags ph WHERE ph.postHashtagId.postId = :pid")
    List<String> findTagsOfPost(@Param("pid") String postId);

    // Tag của nhiều bài trong 1 truy vấn -> [postId, tag]
    @Query("SELECT ph.postHashtagId.postId, ph.postHashtagId.tag FROM post_hashtags ph "
         + "WHERE ph.postHashtagId.postId IN :ids")
    List<Object[]> findTagsForPosts(@Param("ids") Collection<String> ids);

    // Hashtag phổ biến: đếm số bài ĐÃ ĐĂNG & công khai gắn mỗi tag, nhiều nhất trước.
    // (chỉ tính bài public để không lộ bài 'chỉ bạn bè' cho khách/không phải bạn)
    @Query("SELECT ph.postHashtagId.tag, COUNT(p) FROM post_hashtags ph, posts p "
         + "WHERE p.postId = ph.postHashtagId.postId "
         + "AND (p.status IS NULL OR p.status = 'published') "
         + "AND (p.visibility IS NULL OR p.visibility = 'public') "
         + "GROUP BY ph.postHashtagId.tag ORDER BY COUNT(p) DESC, ph.postHashtagId.tag ASC")
    List<Object[]> trending(Pageable pageable);

    // Bài theo tag (mới nhất trước), lọc quyền xem của người xem.
    // Công khai cho mọi người; bài của :me luôn thấy; 'friends' khi tác giả trong :vids; 'private' chỉ chính chủ.
    String VIS = "(p.visibility IS NULL OR p.visibility = 'public'"
            + " OR p.userPost.users.userId = :me"
            + " OR (p.visibility = 'friends' AND p.userPost.users.userId IN :vids))";

    @Query("SELECT p FROM posts p, post_hashtags ph "
         + "WHERE p.postId = ph.postHashtagId.postId AND ph.postHashtagId.tag = :tag "
         + "AND (p.status IS NULL OR p.status = 'published') AND " + VIS + " "
         + "ORDER BY p.postedAt DESC, p.postId ASC")
    List<com.didan.social.entity.Posts> findPostsByTag(@Param("tag") String tag,
                                                       @Param("vids") Collection<String> vids,
                                                       @Param("me") String me,
                                                       Pageable pageable);

    @Query("SELECT COUNT(p) FROM posts p, post_hashtags ph "
         + "WHERE p.postId = ph.postHashtagId.postId AND ph.postHashtagId.tag = :tag "
         + "AND (p.status IS NULL OR p.status = 'published') AND " + VIS)
    long countPostsByTag(@Param("tag") String tag, @Param("vids") Collection<String> vids, @Param("me") String me);
}
