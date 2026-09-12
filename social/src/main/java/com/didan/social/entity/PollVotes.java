package com.didan.social.entity;

import com.didan.social.entity.keys.PollVoteId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;

/** Phiếu của một người trong một cuộc bình chọn. Khoá chính lo phần "một người một phiếu". */
@Entity(name = "poll_votes")
@Table(name = "poll_votes")
public class PollVotes {

    @EmbeddedId
    private PollVoteId pollVoteId;

    @Column(name = "option_id", length = 50, nullable = false)
    private String optionId;

    @Column(name = "voted_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date votedAt;

    public PollVotes() {}

    public PollVotes(PollVoteId pollVoteId, String optionId, Date votedAt) {
        this.pollVoteId = pollVoteId;
        this.optionId = optionId;
        this.votedAt = votedAt;
    }

    public PollVoteId getPollVoteId() { return pollVoteId; }
    public void setPollVoteId(PollVoteId pollVoteId) { this.pollVoteId = pollVoteId; }

    public String getOptionId() { return optionId; }
    public void setOptionId(String optionId) { this.optionId = optionId; }

    public Date getVotedAt() { return votedAt; }
    public void setVotedAt(Date votedAt) { this.votedAt = votedAt; }
}
