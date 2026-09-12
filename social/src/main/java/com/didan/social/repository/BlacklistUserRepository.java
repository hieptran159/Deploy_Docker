package com.didan.social.repository;

import com.didan.social.entity.BlacklistUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlacklistUserRepository extends JpaRepository<BlacklistUser, String> {
    BlacklistUser findByUserId(String userId);

    long countByStatus(String status);

    /** Chỉ đếm lệnh cấm CÒN hiệu lực: vĩnh viễn, hoặc chưa tới hạn. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COUNT(b) FROM blacklist_user b WHERE b.status = 'blocked' "
          + "AND (b.bannedUntil IS NULL OR b.bannedUntil > :now)")
    long countActiveBans(@org.springframework.data.repository.query.Param("now") java.util.Date now);
}
