package com.didan.social.service;

public interface AuthorizePathService {
    String getUserIdAuthoried() throws Exception;

    /**
     * Access token gốc của request hiện tại (do JwtAuthenticationFilter đặt vào
     * credentials). Trả null nếu không có. Dùng để thu hồi đúng phiên của thiết
     * bị đang gọi khi đăng xuất.
     */
    String getAccessTokenAuthoried();
}
