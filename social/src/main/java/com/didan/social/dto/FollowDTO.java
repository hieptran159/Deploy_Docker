package com.didan.social.dto;

import java.util.List;

public class FollowDTO {
    private int quantity;
    private List<String> userId;
    // Kèm sẵn tên + avatar để FE khỏi gọi /user/{id} cho từng người (N+1 qua mạng).
    private List<UserBrief> users;

    public FollowDTO() {
    }

    public FollowDTO(int quantity, List<String> userId) {
        this.quantity = quantity;
        this.userId = userId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public List<String> getUserId() {
        return userId;
    }

    public void setUserId(List<String> userId) {
        this.userId = userId;
    }

    public List<UserBrief> getUsers() {
        return users;
    }

    public void setUsers(List<UserBrief> users) {
        this.users = users;
    }

    public static class UserBrief {
        private String userId;
        private String fullName;
        private String avtUrl;

        public UserBrief() {
        }

        public UserBrief(String userId, String fullName, String avtUrl) {
            this.userId = userId;
            this.fullName = fullName;
            this.avtUrl = avtUrl;
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

        public String getAvtUrl() {
            return avtUrl;
        }

        public void setAvtUrl(String avtUrl) {
            this.avtUrl = avtUrl;
        }
    }
}
