-- Cấm CÓ THỜI HẠN.
--
-- Trước đây chặn người dùng chỉ có một nấc: vĩnh viễn, tới khi admin tự gỡ. Kiểm
-- duyệt thực tế cần nấc thang — treo 7 ngày khác hẳn xoá sổ.
--
-- NULL = cấm vĩnh viễn (giữ nguyên hành vi cũ cho mọi hàng đang có).
ALTER TABLE blacklist_user ADD COLUMN banned_until datetime DEFAULT NULL;
