package com.didan.social.entity.keys;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * (poll_id, user_id) — chính DB bảo đảm mỗi người một phiếu cho mỗi cuộc bình chọn,
 * không để tầng ứng dụng tự canh. Đổi phiếu là UPDATE option_id trên cùng hàng.
 */
@Embeddable
public class PollVoteId implements Serializable {

    @Column(name = "poll_id", nullable = false, length = 50)
    private String pollId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    public PollVoteId() {}

    public PollVoteId(String pollId, String userId) {
        this.pollId = pollId;
        this.userId = userId;
    }

    public String getPollId() { return pollId; }
    public void setPollId(String pollId) { this.pollId = pollId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PollVoteId that)) return false;
        return Objects.equals(pollId, that.pollId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pollId, userId);
    }
}
