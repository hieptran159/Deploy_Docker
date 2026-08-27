package com.didan.social.entity;

import jakarta.persistence.*;

import java.util.Date;
import java.util.Set;

@Entity(name = "users")
public class Users {
    @Id
    @Column(name = "user_id", unique = true)
    private String userId;

    @Column(name = "is_admin", nullable = false)
    private int isAdmin;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "profile_avatar", nullable = false, length = 255)
    private String avtUrl;

    @Column(name = "date_of_birth", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dob;

    @Column(name = "reset_password_expires", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date resetExp;

    @Column(name = "reset_password_token", nullable = true, length = 255)
    private String resetToken;

    @Column(name = "access_token", nullable = true, length = 255)
    private String accessToken;

    // Xác thực email khi đăng ký. null = tài khoản cũ (coi như đã xác thực), 0 = chưa, 1 = đã.
    @Column(name = "email_verified")
    private Integer emailVerified;
    @Column(name = "verify_code", length = 12)
    private String verifyCode;

    // ---- Thông tin hồ sơ mở rộng (tự thêm qua ddl-auto=update) ----
    @Column(name = "nickname", length = 100)
    private String nickname;
    @Column(name = "phone", length = 30)
    private String phone;
    @Column(name = "address", length = 255)
    private String address;
    @Column(name = "hobbies", length = 500)
    private String hobbies;
    @Column(name = "slogan", length = 255)
    private String slogan;

    // 1 = công khai, 0 = riêng tư (chỉ chủ hồ sơ thấy). NULL -> coi như công khai.
    @Column(name = "nickname_public")
    private Integer nicknamePublic;
    @Column(name = "phone_public")
    private Integer phonePublic;
    @Column(name = "address_public")
    private Integer addressPublic;
    @Column(name = "hobbies_public")
    private Integer hobbiesPublic;
    @Column(name = "slogan_public")
    private Integer sloganPublic;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getHobbies() { return hobbies; }
    public void setHobbies(String hobbies) { this.hobbies = hobbies; }
    public String getSlogan() { return slogan; }
    public void setSlogan(String slogan) { this.slogan = slogan; }
    public Integer getNicknamePublic() { return nicknamePublic; }
    public void setNicknamePublic(Integer v) { this.nicknamePublic = v; }
    public Integer getPhonePublic() { return phonePublic; }
    public void setPhonePublic(Integer v) { this.phonePublic = v; }
    public Integer getAddressPublic() { return addressPublic; }
    public void setAddressPublic(Integer v) { this.addressPublic = v; }
    public Integer getHobbiesPublic() { return hobbiesPublic; }
    public void setHobbiesPublic(Integer v) { this.hobbiesPublic = v; }
    public Integer getSloganPublic() { return sloganPublic; }
    public void setSloganPublic(Integer v) { this.sloganPublic = v; }

    @OneToMany(mappedBy = "users1", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Followers> followers;

    @OneToMany(mappedBy = "users2", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Followers> followeds;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PostLikes> postLikes;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserPosts> userPosts;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Participants> participants;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Messages> messages;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserComment> userComments;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CommentLikes> commentLikes;

    @OneToOne(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    @PrimaryKeyJoinColumn
    private BlacklistUser blacklistUser;

    public Users() {}

    public Users(String userId, int isAdmin, String fullName, String email, String password, String avtUrl, Date dob, Date resetExp, String resetToken, String accessToken, Set<Followers> followers, Set<Followers> followeds, Set<PostLikes> postLikes, Set<UserPosts> userPosts, Set<Participants> participants, Set<Messages> messages, Set<UserComment> userComments, Set<CommentLikes> commentLikes, BlacklistUser blacklistUser){
        this.userId = userId;
        this.isAdmin = isAdmin;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.avtUrl = avtUrl;
        this.dob = dob;
        this.resetExp = resetExp;
        this.resetToken = resetToken;
        this.accessToken = accessToken;
        this.followers = followers;
        this.followeds = followeds;
        this.postLikes = postLikes;
        this.userPosts = userPosts;
        this.participants = participants;
        this.messages = messages;
        this.userComments = userComments;
        this.commentLikes = commentLikes;
        this.blacklistUser = blacklistUser;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(int isAdmin) {
        this.isAdmin = isAdmin;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAvtUrl() {
        return avtUrl;
    }

    public void setAvtUrl(String avtUrl) {
        this.avtUrl = avtUrl;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public Date getResetExp() {
        return resetExp;
    }

    public void setResetExp(Date resetExp) {
        this.resetExp = resetExp;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Integer getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Integer emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public Set<Followers> getFollowers() {
        return followers;
    }

    public void setFollowers(Set<Followers> followers) {
        this.followers = followers;
    }

    public Set<Followers> getFolloweds() {
        return followeds;
    }

    public void setFolloweds(Set<Followers> followeds) {
        this.followeds = followeds;
    }

    public Set<PostLikes> getPostLikes() {
        return postLikes;
    }

    public void setPostLikes(Set<PostLikes> postLikes) {
        this.postLikes = postLikes;
    }

    public Set<UserPosts> getUserPosts() {
        return userPosts;
    }

    public void setUserPosts(Set<UserPosts> userPosts) {
        this.userPosts = userPosts;
    }

    public Set<Participants> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<Participants> participants) {
        this.participants = participants;
    }

    public Set<Messages> getMessages() {
        return messages;
    }

    public void setMessages(Set<Messages> messages) {
        this.messages = messages;
    }

    public Set<UserComment> getUserComments() {
        return userComments;
    }

    public void setUserComments(Set<UserComment> userComments) {
        this.userComments = userComments;
    }

    public Set<CommentLikes> getCommentLikes() {
        return commentLikes;
    }

    public void setCommentLikes(Set<CommentLikes> commentLikes) {
        this.commentLikes = commentLikes;
    }

    public BlacklistUser getBlacklistUser() {
        return blacklistUser;
    }

    public void setBlacklistUser(BlacklistUser blacklistUser) {
        this.blacklistUser = blacklistUser;
    }

    /* @Overide
    public String toString(){
        return "complete";
    } */
    
}
