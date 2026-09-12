package com.didan.social.repository;

import com.didan.social.dto.UserDTO;
import com.didan.social.entity.Users;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, String> {
    // Ngoài các method mặc định có sẵn, ta có thể định nghĩa thêm method mới dựa theo nguyên tắc đặt tên của JPA
    // Get All User
    // Login
    Users findFirstByEmail(String email);

    // Find By Token Reset Password
    Users findFirstByResetToken(String token);

    // Find User By userid
    Users findFirstByUserId(String userId);

    // Search User By Fullname
    List<Users> findByFullNameContainingOrEmailLike(String name, String email);

    long countByIsAdmin(int isAdmin);

    // Cột deactivated không có index -> quét cả bảng users, mà lần dựng feed nào cũng gọi.
    // Không xoá cache tường minh: nó đổi ở ba nơi rời rạc (tự vô hiệu hoá, đăng nhập lại,
    // xác thực 2 bước); TTL 60 giây rẻ hơn và không có chỗ nào để quên.
    @org.springframework.cache.annotation.Cacheable(com.didan.social.config.CacheConfig.DEACTIVATED_IDS)
    @org.springframework.data.jpa.repository.Query("SELECT u.userId FROM users u WHERE u.deactivated = 1")
    java.util.List<String> findDeactivatedIds();

    // ---------- Tìm người dùng: truy vấn chiếu, KHÔNG nạp entity ----------
    //
    // getAllUser/searchUser cũ dựng UserDTO đầy đủ qua convertToDTO, mà hàm đó chạm
    // BỐN quan hệ lazy mỗi user (followeds, followers, userPosts, participants) ->
    // 43 user thành ~172 truy vấn, 190 KB, 1,5 giây. Danh sách chỉ cần id + tên + ảnh.
    //
    // Chỉ lấy đúng ba cột; số người theo dõi và số bài đếm riêng theo LÔ ở dưới.
    @Query("SELECT u.userId, u.fullName, u.avtUrl FROM users u "
         + "WHERE (u.deactivated IS NULL OR u.deactivated = 0) "
         + "AND (:q = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) "
         + "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))) "
         + "ORDER BY u.fullName ASC, u.userId ASC")
    List<Object[]> searchLite(@Param("q") String q, Pageable pageable);

    @Query("SELECT COUNT(u) FROM users u "
         + "WHERE (u.deactivated IS NULL OR u.deactivated = 0) "
         + "AND (:q = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) "
         + "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    long countSearchLite(@Param("q") String q);

    /** [userId, số người theo dõi] cho cả trang bằng MỘT truy vấn. */
    @Query("SELECT f.folId.followedId, COUNT(f) FROM followers f "
         + "WHERE f.folId.followedId IN :ids AND (f.status IS NULL OR f.status = 'accepted') "
         + "GROUP BY f.folId.followedId")
    List<Object[]> countFollowersOf(@Param("ids") Collection<String> ids);

    /** [userId, số bài] cho cả trang bằng MỘT truy vấn. */
    @Query("SELECT up.userPostId.userId, COUNT(up) FROM user_posts up "
         + "WHERE up.userPostId.userId IN :ids GROUP BY up.userPostId.userId")
    List<Object[]> countPostsOf(@Param("ids") Collection<String> ids);

    /** Nạp nhiều người trong MỘT truy vấn (hàng đợi báo cáo, và mọi chỗ cần map id -> user). */
    List<Users> findByUserIdIn(Collection<String> ids);
}
