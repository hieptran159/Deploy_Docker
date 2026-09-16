-- Chuyên mục khi đăng bài. category_id trên posts để NULL được: bài cũ (và bài không
-- chọn chuyên mục) vẫn hợp lệ, không cần backfill.
CREATE TABLE categories (
  category_id VARCHAR(36) NOT NULL,
  name        VARCHAR(60) NOT NULL,
  slug        VARCHAR(60) NOT NULL,
  position    INT NOT NULL DEFAULT 0,
  PRIMARY KEY (category_id),
  UNIQUE KEY uk_categories_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO categories (category_id, name, slug, position) VALUES
  (UUID(), 'Thảo luận chung', 'thao-luan-chung', 0),
  (UUID(), 'Hỏi đáp',         'hoi-dap',         1),
  (UUID(), 'Chia sẻ',         'chia-se',         2),
  (UUID(), 'Thông báo',       'thong-bao',       3),
  (UUID(), 'Khác',            'khac',            4);

ALTER TABLE posts ADD COLUMN category_id VARCHAR(36) NULL;
CREATE INDEX idx_posts_category ON posts(category_id);
-- Không thêm FOREIGN KEY: admin xoá một chuyên mục không được kéo theo lỗi
-- ràng buộc trên mọi bài đã đăng trong đó — bài chỉ đơn giản mất nhãn chuyên mục
-- (categoryId trỏ tới id không còn tồn tại, applyCategories bỏ qua khi không tìm thấy).
