package com.didan.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "blacklist_token")
public class BlacklistToken {
    @Id
    // 512 chứ không phải 255: JWT mang thêm claim jti nên dài ~236 ký tự, để sát
    // mép thì chỉ cần thêm một claim nữa là ghi hụt -> token bị thu hồi vẫn dùng được.
    @Column(name = "token", nullable = true, length = 512)
    private String token;

    public BlacklistToken() {
    }

    public BlacklistToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
