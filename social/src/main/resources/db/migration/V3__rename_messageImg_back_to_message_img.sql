-- Sửa V2. Nhận định trong V2 bị NGƯỢC: physical-naming-strategy của Spring Boot
-- (CamelCaseToUnderscoresNamingStrategy) áp cho CẢ tên trong @Column(name=...), nên
-- @Column(name = "messageImg") của entity Messages thực chất trỏ tới cột `message_img`.
-- Cột dư là `messageImg`. V2 đã gộp dữ liệu `message_img` -> `messageImg` (chỉ điền chỗ
-- NULL) rồi DROP `message_img`, nên bây giờ chỉ cần đổi tên `messageImg` -> `message_img`.
-- Dữ liệu giữ nguyên (là hợp của 2 cột cũ).

ALTER TABLE messages CHANGE COLUMN messageImg message_img varchar(255) NULL DEFAULT NULL;
