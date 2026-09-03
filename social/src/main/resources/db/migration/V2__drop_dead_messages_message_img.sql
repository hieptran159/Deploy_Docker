-- Bỏ cột ảnh tin nhắn trùng lặp.
--
-- Entity `Messages` map field `messageImg` với @Column(name = "messageImg"). Cột
-- `message_img` (snake_case) là do naming-strategy sinh ra ở phiên bản trước khi thêm
-- @Column, giờ không còn code nào đọc/ghi. Chuyển nốt dữ liệu còn sót của các tin nhắn
-- cũ sang cột đang dùng rồi bỏ cột chết.

UPDATE messages
   SET messageImg = message_img
 WHERE messageImg IS NULL
   AND message_img IS NOT NULL;

ALTER TABLE messages DROP COLUMN message_img;
