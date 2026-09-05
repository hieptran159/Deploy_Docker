-- Sửa lệch kiểu cột `remember` của user_sessions.
--
-- V4 khai `tinyint` nhưng entity UserSessions để kiểu Integer, nên Hibernate
-- ddl-auto=validate từ chối khởi động:
--   Schema-validation: wrong column type encountered in column [remember]
--   in table [user_sessions]; found [tinyint], but expecting [integer]
--
-- Đổi cột sang `int` chứ không đổi kiểu field, để đồng bộ với các cờ sẵn có
-- trong lược đồ này (users.is_admin, users.twofa_enabled, users.deactivated
-- đều là `int`).
--
-- Không sửa thẳng V4 vì V4 đã chạy trên DB thật: sửa file đã áp dụng sẽ làm
-- Flyway báo sai checksum và chặn khởi động.
-- Câu lệnh này chạy lại nhiều lần cũng không sao (cột đã là int thì không đổi gì).

ALTER TABLE `user_sessions` MODIFY COLUMN `remember` int NOT NULL DEFAULT 0;
