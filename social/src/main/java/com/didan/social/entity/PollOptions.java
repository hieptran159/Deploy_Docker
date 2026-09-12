package com.didan.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Một phương án của cuộc bình chọn. `position` giữ đúng thứ tự tác giả đã nhập. */
@Entity(name = "poll_options")
@Table(name = "poll_options")
public class PollOptions {

    @Id
    @Column(name = "option_id", length = 50)
    private String optionId;

    @Column(name = "poll_id", length = 50, nullable = false)
    private String pollId;

    // "text" dễ nhầm với kiểu TEXT của MySQL -> đặt option_text cho khỏi phải quote
    @Column(name = "option_text", length = 200, nullable = false)
    private String optionText;

    @Column(name = "position", nullable = false)
    private Integer position;

    public PollOptions() {}

    public PollOptions(String optionId, String pollId, String optionText, int position) {
        this.optionId = optionId;
        this.pollId = pollId;
        this.optionText = optionText;
        this.position = position;
    }

    public String getOptionId() { return optionId; }
    public void setOptionId(String optionId) { this.optionId = optionId; }

    public String getPollId() { return pollId; }
    public void setPollId(String pollId) { this.pollId = pollId; }

    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
