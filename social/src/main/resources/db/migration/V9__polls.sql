-- Bình chọn gắn vào bài viết.
--
-- Phạm vi cố ý hẹp cho bản đầu: MỘT lựa chọn cho mỗi người, không hạn chót, không
-- ẩn danh, không sửa lại phương án sau khi tạo. Thêm sau dễ hơn gỡ ra.
--
-- Câu hỏi chính là TIÊU ĐỀ bài viết — không có cột question, khỏi phải bắt người
-- dùng gõ hai lần cùng một câu.
--
-- poll_votes lấy (poll_id, user_id) làm khoá chính: chính DB bảo đảm mỗi người
-- một phiếu, không phải tầng ứng dụng tự canh. Đổi phiếu = UPDATE option_id.

CREATE TABLE polls (
    poll_id    varchar(50) NOT NULL,
    post_id    varchar(50) NOT NULL,
    created_at datetime    NOT NULL,
    PRIMARY KEY (poll_id),
    UNIQUE KEY uk_polls_post (post_id),
    CONSTRAINT fk_polls_post FOREIGN KEY (post_id) REFERENCES posts (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE poll_options (
    option_id   varchar(50)  NOT NULL,
    poll_id     varchar(50)  NOT NULL,
    option_text varchar(200) NOT NULL,
    position    int          NOT NULL,
    PRIMARY KEY (option_id),
    KEY idx_poll_options_poll (poll_id, position),
    CONSTRAINT fk_poll_options_poll FOREIGN KEY (poll_id) REFERENCES polls (poll_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE poll_votes (
    poll_id   varchar(50) NOT NULL,
    user_id   varchar(50) NOT NULL,
    option_id varchar(50) NOT NULL,
    voted_at  datetime    NOT NULL,
    PRIMARY KEY (poll_id, user_id),
    KEY idx_poll_votes_option (option_id),
    CONSTRAINT fk_poll_votes_poll FOREIGN KEY (poll_id) REFERENCES polls (poll_id),
    CONSTRAINT fk_poll_votes_option FOREIGN KEY (option_id) REFERENCES poll_options (option_id),
    CONSTRAINT fk_poll_votes_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
