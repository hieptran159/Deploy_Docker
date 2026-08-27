package com.didan.social.entity;

import com.didan.social.entity.keys.FollowerId;
import jakarta.persistence.*;


@Entity(name = "followers")
public class Followers {
    @EmbeddedId
    FollowerId folId;

    @ManyToOne
    @JoinColumn(name = "follower_id", insertable = false, updatable = false)
    private Users users1;

    @ManyToOne
    @JoinColumn(name = "followed_id", insertable = false, updatable = false)
    private Users users2;

    // "pending" = users1 đã gửi lời mời kết bạn cho users2, chờ chấp nhận
    // "accepted" = đã là bạn bè. NULL (dữ liệu cũ) coi như đã là bạn.
    @Column(name = "status", length = 20)
    private String status;

    public Followers(){}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Followers(FollowerId folId, Users users1, Users users2) {
        this.folId = folId;
        this.users1 = users1;
        this.users2 = users2;
    }

    public FollowerId getFolId() {
        return folId;
    }

    public void setFolId(FollowerId folId) {
        this.folId = folId;
    }

    public Users getUsers1() {
        return users1;
    }

    public void setUsers1(Users users1) {
        this.users1 = users1;
    }

    public Users getUsers2() {
        return users2;
    }

    public void setUsers2(Users users2) {
        this.users2 = users2;
    }

    // others fields, getters and setters
}
