package com.didan.social.service;

import com.didan.social.dto.UserDTO;
import com.didan.social.payload.request.EditUserRequest;
import com.didan.social.payload.request.UpdateProfileRequest;

import java.util.List;

public interface UserService {
    // Lấy tất cả user
    List<UserDTO> getAllUser();
    // Lấy user từ id
    UserDTO getUserById(String userId);
    //Tìm kiếm

    // Sửa thông tin user (email/mật khẩu/avatar — cần mật khẩu hiện tại)
    boolean updateUser(EditUserRequest editUserRequest) throws Exception;

    // Cập nhật hồ sơ mở rộng (nickname/phone/address/hobbies/slogan + cờ công khai)
    boolean updateProfile(UpdateProfileRequest req) throws Exception;

    // Report user
    boolean reportUser(String userId) throws Exception;

    // Người dùng tự xoá tài khoản (cần mật khẩu hiện tại)
    boolean deleteMyAccount(String password) throws Exception;
    // Tự vô hiệu hoá tạm thời (đăng nhập lại để kích hoạt)
    boolean deactivateMyAccount(String password) throws Exception;

    // Bật/tắt xác thực 2 bước qua email (xác nhận bằng mật khẩu hiện tại)
    boolean setTwoFactor(boolean enable, String password) throws Exception;

    // Đổi ảnh bìa hồ sơ (không cần mật khẩu)
    boolean updateCover(org.springframework.web.multipart.MultipartFile cover) throws Exception;


    /** Đổi ảnh đại diện. KHÔNG cần mật khẩu — giống đổi ảnh bìa. */
    boolean updateAvatar(org.springframework.web.multipart.MultipartFile avatar) throws Exception;

    /**
     * Tìm người dùng, phân trang phía server, chỉ trả id/tên/ảnh + số theo dõi + số bài.
     * Thay cho getAllUser + lọc ở client.
     */
    java.util.Map<String, Object> searchUsersLite(String q, int page, int size);
}
