-- Màn hình "Thiết bị đang đăng nhập" cần nhãn để người dùng nhận ra máy nào là máy nào.
-- Không có hai cột này thì danh sách chỉ là mấy cái mốc thời gian, không ai phân biệt được
-- phiên của mình với phiên lạ.
-- Hàng cũ để NULL: phiên tạo trước bản này hiện là "Thiết bị không rõ", tự hết khi
-- refresh token của chúng hết hạn.
ALTER TABLE user_sessions
    ADD COLUMN user_agent varchar(255) DEFAULT NULL,
    ADD COLUMN ip varchar(45) DEFAULT NULL;
