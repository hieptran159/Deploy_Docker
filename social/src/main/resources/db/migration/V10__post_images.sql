-- Nhiều ảnh cho một bài viết.
--
-- posts.post_img chỉ có MỘT ô. Bảng này trở thành nguồn sự thật; cột cũ vẫn được
-- ghi song song như một cầu nối tương thích (ảnh đầu tiên) để bản FE cũ và
-- auto-poster không gãy trong lúc triển khai. Xoá cột đó ở một migration sau,
-- khi không còn ai đọc.
CREATE TABLE post_images (
    image_id varchar(50)  NOT NULL,
    post_id  varchar(50)  NOT NULL,
    url      varchar(255) NOT NULL,
    position int          NOT NULL,
    PRIMARY KEY (image_id),
    KEY idx_post_images_post (post_id, position),
    CONSTRAINT fk_post_images_post FOREIGN KEY (post_id) REFERENCES posts (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Chuyển ảnh của các bài đã có sang bảng mới, giữ nguyên đường dẫn file trên đĩa.
INSERT INTO post_images (image_id, post_id, url, position)
SELECT UUID(), post_id, post_img, 0
FROM posts
WHERE post_img IS NOT NULL AND post_img <> '';
