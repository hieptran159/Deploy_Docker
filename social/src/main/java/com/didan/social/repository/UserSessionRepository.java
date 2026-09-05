package com.didan.social.repository;

import com.didan.social.entity.UserSessions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSessions, String> {

    UserSessions findFirstByRefreshHash(String refreshHash);

    /** Dùng khi đăng xuất: tìm đúng phiên của thiết bị đang gọi. */
    UserSessions findFirstByAccessToken(String accessToken);

    List<UserSessions> findByUserIdOrderByLastUsedAtDesc(String userId);

    /** Thu hồi toàn bộ phiên: bị chặn, tự vô hiệu hoá, xoá tài khoản. */
    @Transactional
    void deleteByUserId(String userId);

    /**
     * Dọn phiên quá hạn. Refresh token hết hiệu lực sau jwt.refresh-expiration-ms
     * nên hàng cũ hơn mốc đó chỉ còn là rác.
     */
    @Transactional
    void deleteByUserIdAndLastUsedAtBefore(String userId, Date cutoff);
}
