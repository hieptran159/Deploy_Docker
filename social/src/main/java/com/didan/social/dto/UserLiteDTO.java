package com.didan.social.dto;

/**
 * Một dòng trong danh sách người dùng — chỉ đúng những gì màn hình cần.
 *
 * UserDTO đầy đủ có 22 trường, trong đó có phone/address/dob/hobbies và bốn con số
 * đếm lấy từ quan hệ lazy. Dùng nó cho danh sách vừa nặng (4.430 byte/user) vừa lộ
 * dữ liệu riêng tư của người khác. DTO này chỉ mang thứ hiển thị được cho tất cả.
 */
public class UserLiteDTO {
    private String userId;
    private String fullName;
    private String avtUrl;
    private long followers;
    private long posts;

    public UserLiteDTO() {}

    public UserLiteDTO(String userId, String fullName, String avtUrl) {
        this.userId = userId;
        this.fullName = fullName;
        this.avtUrl = avtUrl;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAvtUrl() { return avtUrl; }
    public void setAvtUrl(String avtUrl) { this.avtUrl = avtUrl; }

    public long getFollowers() { return followers; }
    public void setFollowers(long followers) { this.followers = followers; }

    public long getPosts() { return posts; }
    public void setPosts(long posts) { this.posts = posts; }
}
