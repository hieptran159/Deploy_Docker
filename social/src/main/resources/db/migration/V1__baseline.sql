-- Baseline: ảnh chụp schema prod tại thời điểm áp dụng Flyway.
-- Sinh từ `mysqldump --no-data socialapp` (đã bỏ AUTO_INCREMENT, bỏ bảng
-- flyway_schema_history — Flyway tự tạo & quản bảng này).
--
-- Trên DB prod ĐANG CHẠY: file này KHÔNG được thực thi lại (baseline-on-migrate
-- đánh dấu schema đã ở V1). Nó chỉ chạy khi deploy vào một DB RỖNG.

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `admin_logs` (
  `id` varchar(36) NOT NULL,
  `action` varchar(40) DEFAULT NULL,
  `admin_id` varchar(50) DEFAULT NULL,
  `admin_name` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `detail` varchar(500) DEFAULT NULL,
  `target_id` varchar(50) DEFAULT NULL,
  `target_type` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_adminlog_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `blacklist_token` (
  `token` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `blacklist_user` (
  `user_id` varchar(255) NOT NULL,
  `blocked_at` datetime(6) DEFAULT NULL,
  `reported_quantity` int DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `blocks` (
  `blocked_id` varchar(50) NOT NULL,
  `blocker_id` varchar(50) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`blocked_id`,`blocker_id`),
  KEY `idx_blocks_blocked` (`blocked_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `bookmarks` (
  `post_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`post_id`,`user_id`),
  KEY `idx_bookmark_user_created` (`user_id`,`created_at`),
  CONSTRAINT `FK7nbb4ldgek7ux7y6lu0y4g826` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`),
  CONSTRAINT `FKdbsho2e05w5r13fkjqfjmge5f` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `comment_likes` (
  `comment_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `type` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`user_id`,`comment_id`),
  UNIQUE KEY `unique_like` (`comment_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `comment_likes_ibfk_1` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`comment_id`),
  CONSTRAINT `comment_likes_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `comments` (
  `comment_id` varchar(50) NOT NULL,
  `content` text NOT NULL,
  `comment_img` varchar(255) DEFAULT NULL,
  `comment_at` datetime NOT NULL,
  `parent_id` varchar(36) DEFAULT NULL,
  `edited_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`comment_id`),
  UNIQUE KEY `comment_id_UNIQUE` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `conversations` (
  `conversation_id` varchar(50) NOT NULL,
  `conversation_name` varchar(100) NOT NULL,
  `created_at` datetime NOT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `followers` (
  `follower_id` varchar(50) NOT NULL,
  `followed_id` varchar(50) NOT NULL,
  `status` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`followed_id`,`follower_id`),
  KEY `followers_users_user_id_fk` (`follower_id`),
  CONSTRAINT `followers_users_user_id_fk` FOREIGN KEY (`follower_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `followers_users_user_id_fk2` FOREIGN KEY (`followed_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `message_reactions` (
  `message_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `emoji` varchar(16) DEFAULT NULL,
  PRIMARY KEY (`message_id`,`user_id`),
  KEY `idx_msgreact_message` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `messages` (
  `message_id` varchar(50) NOT NULL,
  `conversation_id` varchar(50) NOT NULL,
  `sender_id` varchar(50) NOT NULL,
  `content` text NOT NULL,
  `messageImg` varchar(255) DEFAULT NULL,
  `sent_at` datetime NOT NULL,
  `message_img` varchar(255) DEFAULT NULL,
  `recalled` bit(1) DEFAULT NULL,
  PRIMARY KEY (`message_id`),
  KEY `conversation_id` (`conversation_id`),
  KEY `sender_id` (`sender_id`),
  KEY `idx_msg_conv_sent` (`conversation_id`,`sent_at`),
  CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`conversation_id`),
  CONSTRAINT `messages_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notifications` (
  `notification_id` varchar(255) NOT NULL,
  `actor_id` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `is_read` int NOT NULL,
  `message` varchar(255) DEFAULT NULL,
  `recipient_id` varchar(50) NOT NULL,
  `target_id` varchar(50) DEFAULT NULL,
  `type` varchar(30) NOT NULL,
  `ref_id` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `idx_notif_recipient_read` (`recipient_id`,`is_read`),
  KEY `idx_notif_recipient_created` (`recipient_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `participants` (
  `conversation_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `muted` int DEFAULT NULL,
  PRIMARY KEY (`conversation_id`,`user_id`),
  KEY `conversation_id` (`conversation_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `participants_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`conversation_id`),
  CONSTRAINT `participants_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `post_hashtags` (
  `post_id` varchar(50) NOT NULL,
  `tag` varchar(50) NOT NULL,
  PRIMARY KEY (`post_id`,`tag`),
  KEY `idx_post_hashtags_tag` (`tag`),
  KEY `idx_post_hashtags_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `post_likes` (
  `post_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `type` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`post_id`,`user_id`),
  UNIQUE KEY `unique_like` (`post_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `post_likes_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`),
  CONSTRAINT `post_likes_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `posts` (
  `post_id` varchar(50) NOT NULL,
  `title` varchar(255) NOT NULL,
  `post_img` varchar(255) DEFAULT NULL,
  `body` text NOT NULL,
  `posted_at` datetime NOT NULL,
  `status` varchar(20) DEFAULT NULL,
  `visibility` varchar(20) DEFAULT NULL,
  `edited_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`post_id`),
  UNIQUE KEY `post_id_UNIQUE` (`post_id`),
  KEY `idx_posts_feed` (`posted_at` DESC,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reports` (
  `report_id` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `handled_at` datetime(6) DEFAULT NULL,
  `handled_by` varchar(50) DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `reporter_id` varchar(50) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `target_id` varchar(50) DEFAULT NULL,
  `target_type` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`report_id`),
  KEY `idx_report_status_created` (`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reposts` (
  `post_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `note` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`post_id`,`user_id`),
  KEY `idx_reposts_post` (`post_id`),
  KEY `idx_reposts_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_comment` (
  `post_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `comment_id` varchar(50) NOT NULL,
  PRIMARY KEY (`post_id`,`user_id`,`comment_id`),
  UNIQUE KEY `comment_id_UNIQUE` (`comment_id`),
  UNIQUE KEY `post_id_UNIQUE` (`post_id`,`user_id`,`comment_id`),
  KEY `user_comment_users_user_id_fk` (`user_id`),
  KEY `idx_usercomment_comment` (`comment_id`),
  KEY `idx_usercomment_post` (`post_id`),
  CONSTRAINT `user_comment_comments_comment_id_fk` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`comment_id`),
  CONSTRAINT `user_comment_post_post_id_fk` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`),
  CONSTRAINT `user_comment_users_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_posts` (
  `post_id` varchar(50) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  PRIMARY KEY (`user_id`,`post_id`),
  KEY `user_posts_posts_post_id_fk` (`post_id`),
  CONSTRAINT `user_posts_posts_post_id_fk` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`),
  CONSTRAINT `user_posts_users_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `user_id` varchar(50) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `profile_avatar` varchar(255) NOT NULL,
  `date_of_birth` date NOT NULL,
  `reset_password_expires` datetime DEFAULT NULL,
  `reset_password_token` varchar(255) DEFAULT NULL,
  `access_token` varchar(255) DEFAULT NULL,
  `is_admin` int NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `address_public` int DEFAULT NULL,
  `hobbies` varchar(500) DEFAULT NULL,
  `hobbies_public` int DEFAULT NULL,
  `nickname` varchar(100) DEFAULT NULL,
  `nickname_public` int DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `phone_public` int DEFAULT NULL,
  `slogan` varchar(255) DEFAULT NULL,
  `slogan_public` int DEFAULT NULL,
  `email_verified` int DEFAULT NULL,
  `verify_code` varchar(12) DEFAULT NULL,
  `cover_url` varchar(255) DEFAULT NULL,
  `deactivated` int DEFAULT NULL,
  `refresh_token` varchar(512) DEFAULT NULL,
  `twofa_code` varchar(12) DEFAULT NULL,
  `twofa_enabled` int DEFAULT NULL,
  `twofa_expires` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `email_UNIQUE` (`email`),
  UNIQUE KEY `user_id_UNIQUE` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
