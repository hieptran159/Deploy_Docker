package com.didan.social.service;

import com.didan.social.entity.Users;
import com.didan.social.payload.request.SignupRequest;

public interface AuthService {

    // Login
    Users login(String email, String password) throws Exception;

    // Signup
    Users signup(SignupRequest signupRequest) throws Exception;

    // Logout
    void logout() throws Exception;

    // Cấp access token mới từ refresh token (xoay vòng refresh token cũ)
    Users refreshAccess(String refreshToken) throws Exception;

    // Bước 2 của đăng nhập khi bật 2FA: xác minh mã đã gửi qua email, cấp token
    Users verifyTwoFactor(String email, String code) throws Exception;

    // Request Forgot Password
    String requestForgot(String email) throws Exception;

    // Verify Token Reset Password
    String verifyToken(String token) throws Exception;

    // Update Password
    boolean updatePassword(String token, String newPassword) throws Exception;

    // Xác thực email bằng mã đã gửi (đăng nhập luôn sau khi xác thực)
    Users verifyEmail(String email, String code) throws Exception;

    // Gửi lại mã xác thực email
    void resendVerify(String email) throws Exception;
}