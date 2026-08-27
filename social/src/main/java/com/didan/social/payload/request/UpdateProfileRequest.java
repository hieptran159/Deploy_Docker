package com.didan.social.payload.request;

// Cập nhật thông tin hồ sơ (không cần mật khẩu). Trường nào null thì giữ nguyên.
// *Public: 1 = công khai, 0 = riêng tư.
public class UpdateProfileRequest {
    private String nickname;
    private String phone;
    private String address;
    private String hobbies;
    private String slogan;
    private Integer nicknamePublic;
    private Integer phonePublic;
    private Integer addressPublic;
    private Integer hobbiesPublic;
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
}
