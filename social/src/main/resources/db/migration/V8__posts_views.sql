-- Lượt xem bài viết. int (không phải tinyint/bigint) để khớp Integer bên entity —
-- ddl-auto=validate soi đúng KIỂU cột, xem ghi chú Flyway trong CLAUDE.md.
-- Bài cũ bắt đầu từ 0; không có cách nào dựng lại số xem trong quá khứ.
ALTER TABLE posts ADD COLUMN views int NOT NULL DEFAULT 0;
