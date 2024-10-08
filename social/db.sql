-- MySQL Script generated by MySQL Workbench
-- Mon Mar 18 14:53:14 2024
-- Model: New Model    Version: 1.0
-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Table `blacklist_token`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `blacklist_token` (
  `token` VARCHAR(255) NULL DEFAULT NULL)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `users`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `user_id` VARCHAR(50) NOT NULL,
  `is_admin` INT NOT NULL,
  `full_name` VARCHAR(255) NOT NULL,
  `email` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `profile_avatar` VARCHAR(255) NOT NULL,
  `date_of_birth` DATE NOT NULL,
  `reset_password_expires` DATETIME NULL DEFAULT NULL,
  `reset_password_token` VARCHAR(255) NULL DEFAULT NULL,
  `access_token` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE INDEX `email_UNIQUE` (`email` ASC) VISIBLE,
  UNIQUE INDEX `user_id_UNIQUE` (`user_id` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `blacklist_user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `blacklist_user` (
  `user_id` VARCHAR(50) NOT NULL,
  `status` VARCHAR(50) NOT NULL DEFAULT 'pending',
  `reported_quantity` INT NOT NULL DEFAULT '0',
  `blocked_at` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `user_id_blocked_fk`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`user_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `comments`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `comments` (
  `comment_id` VARCHAR(50) NOT NULL,
  `content` TEXT NOT NULL,
  `comment_img` VARCHAR(255) NULL DEFAULT NULL,
  `comment_at` DATETIME NOT NULL,
  PRIMARY KEY (`comment_id`),
  UNIQUE INDEX `comment_id_UNIQUE` (`comment_id` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `comment_likes`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `comment_likes` (
  `comment_id` VARCHAR(50) NOT NULL,
  `user_id` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`user_id`, `comment_id`),
  UNIQUE INDEX `unique_like` (`comment_id` ASC, `user_id` ASC) VISIBLE,
  INDEX `user_id` (`user_id` ASC) VISIBLE,
  CONSTRAINT `comment_likes_ibfk_1`
    FOREIGN KEY (`comment_id`)
    REFERENCES `comments` (`comment_id`),
  CONSTRAINT `comment_likes_ibfk_2`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`user_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `conversations`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `conversations` (
  `conversation_id` VARCHAR(50) NOT NULL,
  `conversation_name` VARCHAR(100) NOT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`conversation_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `followers`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `followers` (
  `follower_id` VARCHAR(50) NOT NULL,
  `followed_id` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`followed_id`, `follower_id`),
  INDEX `followers_users_user_id_fk` (`follower_id` ASC) VISIBLE,
  CONSTRAINT `followers_users_user_id_fk`
    FOREIGN KEY (`follower_id`)
    REFERENCES `users` (`user_id`),
  CONSTRAINT `followers_users_user_id_fk2`
    FOREIGN KEY (`followed_id`)
    REFERENCES `users` (`user_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `messages`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `messages` (
  `message_id` VARCHAR(50) NOT NULL,
  `conversation_id` VARCHAR(50) NOT NULL,
  `sender_id` VARCHAR(50) NOT NULL,
  `content` TEXT NOT NULL,
  `messageImg` VARCHAR(255) NULL DEFAULT NULL,
  `sent_at` DATETIME NOT NULL,
  `message_img` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`message_id`),
  INDEX `conversation_id` (`conversation_id` ASC) VISIBLE,
  INDEX `sender_id` (`sender_id` ASC) VISIBLE,
  CONSTRAINT `messages_ibfk_1`
    FOREIGN KEY (`conversation_id`)
    REFERENCES `conversations` (`conversation_id`),
  CONSTRAINT `messages_ibfk_2`
    FOREIGN KEY (`sender_id`)
    REFERENCES `users` (`user_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `participants`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `participants` (
  `conversation_id` VARCHAR(50) NOT NULL,
  `user_id` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`conversation_id`, `user_id`),
  INDEX `conversation_id` (`conversation_id` ASC) VISIBLE,
  INDEX `user_id` (`user_id` ASC) VISIBLE,
  CONSTRAINT `participants_ibfk_1`
    FOREIGN KEY (`conversation_id`)
    REFERENCES `conversations` (`conversation_id`),
  CONSTRAINT `participants_ibfk_2`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`user_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `posts`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `posts` (
  `post_id` VARCHAR(50) NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `post_img` VARCHAR(255) NULL DEFAULT NULL,
  `body` TEXT NOT NULL,
  `posted_at` DATETIME NOT NULL,
  PRIMARY KEY (`post_id`),
  UNIQUE INDEX `post_id_UNIQUE` (`post_id` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `post_likes`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `post_likes` (
  `post_id` VARCHAR(50) NOT NULL,
  `user_id` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`post_id`, `user_id`),
  UNIQUE INDEX `unique_like` (`post_id` ASC, `user_id` ASC) VISIBLE,
  INDEX `user_id` (`user_id` ASC) VISIBLE,
  CONSTRAINT `post_likes_ibfk_1`
    FOREIGN KEY (`post_id`)
    REFERENCES `posts` (`post_id`),
  CONSTRAINT `post_likes_ibfk_2`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`user_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `user_comment`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_comment` (
  `post_id` VARCHAR(50) NOT NULL,
  `user_id` VARCHAR(50) NOT NULL,
  `comment_id` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`post_id`, `user_id`, `comment_id`),
  UNIQUE INDEX `comment_id_UNIQUE` (`comment_id` ASC) VISIBLE,
  UNIQUE INDEX `post_id_UNIQUE` (`post_id` ASC, `user_id` ASC, `comment_id` ASC) VISIBLE,
  INDEX `user_comment_users_user_id_fk` (`user_id` ASC) VISIBLE,
  CONSTRAINT `user_comment_comments_comment_id_fk`
    FOREIGN KEY (`comment_id`)
    REFERENCES `comments` (`comment_id`),
  CONSTRAINT `user_comment_post_post_id_fk`
    FOREIGN KEY (`post_id`)
    REFERENCES `posts` (`post_id`),
  CONSTRAINT `user_comment_users_user_id_fk`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`user_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `user_posts`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_posts` (
  `post_id` VARCHAR(50) NOT NULL,
  `user_id` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`user_id`, `post_id`),
  INDEX `user_posts_posts_post_id_fk` (`post_id` ASC) VISIBLE,
  CONSTRAINT `user_posts_posts_post_id_fk`
    FOREIGN KEY (`post_id`)
    REFERENCES `posts` (`post_id`),
  CONSTRAINT `user_posts_users_user_id_fk`
    FOREIGN KEY (`user_id`)
    REFERENCES `users` (`user_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;