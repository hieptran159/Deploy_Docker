-- Theo dõi hashtag: biến hashtag từ cái nhãn thành một kênh có người đăng ký.
--
-- Chỉ có FEED, cố ý KHÔNG gửi thông báo. Auto-poster đăng tin liên tục dưới #tin
-- (đã hơn 4.100 bài) — bắn thông báo theo tag sẽ dội bom bất kỳ ai lỡ theo dõi nó.
-- Cần thông báo thì phải có cơ chế gộp/giới hạn trước, đó là việc khác.
--
-- tag varchar(50) khớp post_hashtags.tag; HashtagUtils cũng chặn ở 50 ký tự.
CREATE TABLE hashtag_follows (
    user_id    varchar(50) NOT NULL,
    tag        varchar(50) NOT NULL,
    created_at datetime    NOT NULL,
    PRIMARY KEY (user_id, tag),
    KEY idx_hashtag_follows_tag (tag),
    CONSTRAINT fk_hashtag_follows_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
