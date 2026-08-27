package com.didan.social.dto;


import java.util.List;

public class UserDTO {
    private String userId;
    private String fullName;
    private String email;
    private String avtUrl;
    private String dateOfBirth;
    private int followers;
    private int followings;
    private int posts;
    private List<String> postId;
    private int participantGroups;
    // Hồ sơ mở rộng — trường nào riêng tư và không phải chủ hồ sơ thì để null
    private String nickname;
    private String phone;
    private String address;
    private String hobbies;
    private String slogan;
    // cờ công khai (chỉ có ý nghĩa khi xem hồ sơ của chính mình)
    private Boolean nicknamePublic;
    private Boolean phonePublic;
    private Boolean addressPublic;
    private Boolean hobbiesPublic;
    private Boolean sloganPublic;

    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }
    public String getHobbies() { return hobbies; }
    public void setHobbies(String v) { this.hobbies = v; }
    public String getSlogan() { return slogan; }
    public void setSlogan(String v) { this.slogan = v; }
    public Boolean getNicknamePublic() { return nicknamePublic; }
    public void setNicknamePublic(Boolean v) { this.nicknamePublic = v; }
    public Boolean getPhonePublic() { return phonePublic; }
    public void setPhonePublic(Boolean v) { this.phonePublic = v; }
    public Boolean getAddressPublic() { return addressPublic; }
    public void setAddressPublic(Boolean v) { this.addressPublic = v; }
    public Boolean getHobbiesPublic() { return hobbiesPublic; }
    public void setHobbiesPublic(Boolean v) { this.hobbiesPublic = v; }
    public Boolean getSloganPublic() { return sloganPublic; }
    public void setSloganPublic(Boolean v) { this.sloganPublic = v; }

    public UserDTO() {
    }

    public UserDTO(String userId, String fullName, String email, String avtUrl, String dateOfBirth, int followers, int followings, int posts, List<String> postId, int participantGroups) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.avtUrl = avtUrl;
        this.dateOfBirth = dateOfBirth;
        this.followers = followers;
        this.followings = followings;
        this.posts = posts;
        this.postId = postId;
        this.participantGroups = participantGroups;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvtUrl() {
        return avtUrl;
    }

    public void setAvtUrl(String avtUrl) {
        this.avtUrl = avtUrl;
    }

    public String getDob() {
        return dateOfBirth;
    }

    public void setDob(String dob) {
        this.dateOfBirth = dob;
    }

    public int getFollowers() {
        return followers;
    }

    public void setFollowers(int followers) {
        this.followers = followers;
    }

    public int getFollowings() {
        return followings;
    }

    public void setFollowings(int followings) {
        this.followings = followings;
    }

    public int getPosts() {
        return posts;
    }

    public void setPosts(int posts) {
        this.posts = posts;
    }

    public List<String> getPostId() {
        return postId;
    }

    public void setPostId(List<String> postId) {
        this.postId = postId;
    }

    public int getParticipantGroups() {
        return participantGroups;
    }

    public void setParticipantGroups(int participantGroups) {
        this.participantGroups = participantGroups;
    }
}
