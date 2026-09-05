-- Đăng nhập nhiều thiết bị.
--
-- Trước đây mỗi tài khoản chỉ có MỘT ô access_token và MỘT ô refresh_token ngay
-- trên hàng `users`, nên đăng nhập ở thiết bị thứ hai sẽ ghi đè lên phiên của
-- thiết bị thứ nhất và đẩy nó ra. Từ nay mỗi thiết bị là một hàng riêng ở đây.

CREATE TABLE `user_sessions` (
  `session_id`   varchar(36)  NOT NULL,
  `user_id`      varchar(50)  NOT NULL,
  -- Chỉ lưu SHA-256 của refresh token, không lưu bản gốc.
  `refresh_hash` varchar(64)  NOT NULL,
  -- Access token của chính phiên này, giữ lại để còn thu hồi được khi chặn
  -- người dùng / vô hiệu hoá tài khoản (access token là JWT không trạng thái,
  -- không lưu thì không chặn giữa chừng được).
  `access_token` varchar(512) DEFAULT NULL,
  `remember`     tinyint      NOT NULL DEFAULT 0,
  `created_at`   datetime     DEFAULT NULL,
  `last_used_at` datetime     DEFAULT NULL,
  PRIMARY KEY (`session_id`),
  UNIQUE KEY `uk_user_sessions_refresh` (`refresh_hash`),
  KEY `idx_user_sessions_user` (`user_id`),
  CONSTRAINT `fk_user_sessions_user` FOREIGN KEY (`user_id`)
      REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Chuyển các phiên đang sống sang bảng mới để không ai bị đăng xuất khi deploy.
-- Lọc CHAR_LENGTH = 64 để chỉ lấy hash SHA-256, bỏ qua vài hàng còn lưu refresh
-- token dạng bản rõ từ trước đợt băm.
INSERT INTO `user_sessions`
    (`session_id`, `user_id`, `refresh_hash`, `access_token`, `remember`, `created_at`, `last_used_at`)
SELECT UUID(), `user_id`, `refresh_token`, `access_token`, 1, NOW(), NOW()
FROM `users`
WHERE `refresh_token` IS NOT NULL AND CHAR_LENGTH(`refresh_token`) = 64;

-- Hai cột cũ trên `users` từ nay không còn được đọc/ghi (entity Users đánh dấu
-- @Transient). Cố ý CHƯA xoá cột: nếu cần quay lại bản cũ thì dữ liệu vẫn còn.
-- Xoá hẳn ở một migration sau, khi bản này đã chạy ổn định.
