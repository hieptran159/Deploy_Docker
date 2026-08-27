package com.didan.social.repository;

import com.didan.social.entity.Followers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Followers, String> {
    Followers findFirstByUsers1_UserIdAndUsers2_UserId(String followerId, String followedId);

    // Lời mời đang chờ mà users2 nhận được
    List<Followers> findAllByUsers2_UserIdAndStatus(String userId, String status);
    // Lời mời đang chờ mà users1 đã gửi
    List<Followers> findAllByUsers1_UserIdAndStatus(String userId, String status);

    // Tất cả quan hệ bạn bè đã chấp nhận có liên quan tới uid (dữ liệu cũ status NULL cũng tính)
    @Query("SELECT f FROM followers f WHERE (f.users1.userId = :uid OR f.users2.userId = :uid) " +
            "AND (f.status IS NULL OR f.status = 'accepted')")
    List<Followers> findAcceptedOf(@Param("uid") String uid);
}
