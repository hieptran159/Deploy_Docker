package com.didan.social.dto;

import java.util.List;

/**
 * Kết quả bình chọn kèm theo bài viết.
 *
 * `myOptionId` là phiếu của chính người đang xem (null nếu chưa bỏ phiếu) — FE cần
 * nó để tô đậm phương án đã chọn mà không phải gọi thêm một API nữa.
 */
public class PollDTO {
    private String pollId;
    private List<Option> options;
    private long totalVotes;
    private String myOptionId;

    public static class Option {
        private String optionId;
        private String text;
        private long votes;

        public Option() {}

        public Option(String optionId, String text, long votes) {
            this.optionId = optionId;
            this.text = text;
            this.votes = votes;
        }

        public String getOptionId() { return optionId; }
        public void setOptionId(String optionId) { this.optionId = optionId; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public long getVotes() { return votes; }
        public void setVotes(long votes) { this.votes = votes; }
    }

    public String getPollId() { return pollId; }
    public void setPollId(String pollId) { this.pollId = pollId; }

    public List<Option> getOptions() { return options; }
    public void setOptions(List<Option> options) { this.options = options; }

    public long getTotalVotes() { return totalVotes; }
    public void setTotalVotes(long totalVotes) { this.totalVotes = totalVotes; }

    public String getMyOptionId() { return myOptionId; }
    public void setMyOptionId(String myOptionId) { this.myOptionId = myOptionId; }
}
