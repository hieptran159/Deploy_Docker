-- Nới chỗ cho token trong blacklist.
--
-- JWT nay mang thêm claim `jti` ngẫu nhiên (xem JwtUtils) nên dài thêm khoảng
-- 28 ký tự: refresh token đo được 208 -> ~236. Cột cũ varchar(255) chỉ còn
-- ~19 ký tự dự phòng.
--
-- Tràn cột ở đây KHÔNG chỉ là lỗi hiển thị: bảng này là nơi chặn token khi đăng
-- xuất / chặn người dùng. Ghi hụt là token vẫn dùng được sau khi đã bị thu hồi.
-- Nới rộng hẳn thay vì để sát mép.

ALTER TABLE `blacklist_token` MODIFY COLUMN `token` varchar(512) DEFAULT NULL;
