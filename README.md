# HIPDN-EA Forum — Deploy_Docker

Diễn đàn mạng xã hội đóng gói sẵn để chạy bằng **Docker Compose** (frontend + backend + MySQL).

- **Frontend** (`forum_fe/`): Vue 3 + Vite, Pinia, vue-router, Tailwind, DevExtreme — https://github.com/hieptran159/forum_fe
- **Backend** (`social/`): Spring Boot 3.1.7, Java 17, JPA/Hibernate, MySQL 8, netty-socketio, SendGrid — https://github.com/dannguyenmessi1705/social

## Chạy nhanh

```bash
cp .env.example .env        # tuỳ chọn — mọi biến đều có mặc định
docker compose up -d --build
```

| Thành phần | Địa chỉ |
|---|---|
| Giao diện | http://localhost |
| REST API | http://localhost:8081 (Swagger: `/api-docs.html`) |
| Socket.IO (chat, thông báo, presence) | http://localhost:8082 |
| MySQL | localhost:3307 (db `socialapp`) |

Hướng dẫn chi tiết (biến môi trường, SendGrid, Cloudflare Tunnel, migrate DB…): xem [`DEPLOY.md`](DEPLOY.md).

> Deploy thật nên đặt: `APP_CORS_ALLOWED_ORIGINS` (giới hạn origin FE), `MESSAGE_CRYPTO_KEY`
> (bật mã hoá tin nhắn), `RATELIMIT_TRUST_FORWARDED=true` (chỉ khi có nginx/Cloudflare phía trước),
> và **rotate** `JWT_SECRET` / mật khẩu DB khỏi giá trị mặc định.

## Tính năng

### Bài viết & bảng tin
- Đăng bài kèm ảnh; sửa / xoá bài của mình.
- **Quyền xem bài**: *công khai* / *chỉ bạn bè* / *chỉ mình tôi* (bài hạn chế không lên feed / tìm kiếm của người ngoài, không chia sẻ được).
- **Bản nháp**: lưu bài chưa hoàn thiện (không lên feed), quản lý ở trang *Bản nháp*, đăng khi sẵn sàng.
- **Hashtag `#chủ_đề`**: gõ thẳng trong tiêu đề / nội dung — hiện chip dưới bài, bấm mở trang `/tag/<tag>`, có card *"Hashtag nổi bật"* ở trang chủ.
- **Chia sẻ (repost)** bài của người khác — feed hiển thị *"X đã chia sẻ"*, đếm lượt chia sẻ, mục *"Đã chia sẻ"* (phân trang) trên trang cá nhân.
- Bảng tin trộn chung bài gốc + lượt chia sẻ theo dòng thời gian, **phân trang** kèm nút *đi tới trang X*.
- Tab **"Tất cả" / "Bạn bè"** ở trang chủ — bảng tin riêng chỉ gồm bài & lượt chia sẻ của bạn bè.
- **Tìm kiếm** bài viết theo tiêu đề / nội dung (không phân biệt hoa thường), phân trang *"xem thêm"*.
- **Khách chưa đăng nhập** xem được trang chủ, chi tiết bài, bình luận, trang hashtag; muốn tương tác (thích, bình luận, lưu…) thì đăng nhập.

### Bình luận
- Bình luận kèm ảnh, emoji, **nhắc tên** `@Tên` (gợi ý khi gõ).
- Trả lời lồng nhau dạng cây, phân trang bình luận.
- **Thả cảm xúc đa dạng**: 👍 ❤️ 😆 😮 😢 😡 cho cả bài viết lẫn bình luận.

### Nhắn tin (realtime)
- Chat 1-1 và nhóm; đổi tên nhóm, thêm/xoá thành viên (chỉ trong danh sách bạn bè).
- **Ảnh đại diện nhóm** (đổi realtime); tin nhắn riêng hiển thị avatar của người kia.
- Sửa / thu hồi tin nhắn, emoji, nhắc tên `@`, ảnh đính kèm.
- **Thả cảm xúc cho từng tin nhắn** (👍 ❤️ 😆 😮 😢 😡), hiện chip đếm dưới bong bóng, realtime.
- Hiển thị *đang soạn*, *đã xem*, chấm **online** của bạn bè (heartbeat + sweep).
- Xem trước tin nhắn cuối + badge chưa đọc ở thanh bên.

### Kết bạn & quan hệ
- Luồng lời mời kết bạn: gửi / chấp nhận / từ chối / huỷ / huỷ kết bạn.
- **Chặn người dùng**: hai bên không thấy bài của nhau, không nhắn tin / kết bạn được; tab *"Đã chặn"* để bỏ chặn.
- Trang *Bạn bè* (`/follow`) và *Tìm người dùng* (`/users`) tải danh sách theo trang, nút *"Xem thêm"*.

### Thông báo
- Realtime qua socket + poll dự phòng; toast, badge số chưa đọc, hiện avatar người tạo.
- Bấm vào thông báo **nhảy đúng vị trí** (đúng bình luận được nhắc, đúng hội thoại…).
- **Trang thông báo đầy đủ** (`/notifications`) xem lại lịch sử, phân trang.
- Loại: kết bạn, chấp nhận kết bạn, bình luận, trả lời, nhắc tên, thích bài/bình luận, tin nhắn (kể cả nhóm), **chia sẻ bài**.

### Hồ sơ & tài khoản
- Trang cá nhân có **thẻ xem trước hồ sơ công khai** + lịch sử **"Bài viết"** và **"Đã chia sẻ"** (phân trang); phần chỉnh sửa tách riêng vào *Cài đặt tài khoản*.
- Avatar, **ảnh bìa**, đổi tên hiển thị, các trường mở rộng (nickname / SĐT / địa chỉ / sở thích / slogan) với cờ **công khai / riêng tư**.
- **Đổi avatar cập nhật realtime** cho mình và bạn bè đang online (không cần tải lại trang).
- **Xác thực email khi đăng ký** (mã OTP, email HTML có thương hiệu; không có SendGrid key thì mã in ra log).
- Đổi mật khẩu trong hồ sơ; hỏi lại xác nhận khi đăng xuất.
- **Vô hiệu hoá tài khoản tạm thời**: ẩn tài khoản + nội dung khỏi người khác, đăng nhập lại để kích hoạt.
- Xoá tài khoản vĩnh viễn.
- **Lưu bài viết** (bookmark) → trang *Đã lưu*.

### Quản trị & kiểm duyệt
- Cấp quyền admin, khoá / mở khoá người dùng.
- **Hàng đợi báo cáo** (bài / bình luận / người dùng): đánh dấu xử lý, bỏ qua, xoá nội dung.
- **Tự ẩn bài** khi đạt ngưỡng báo cáo (mặc định 3) — admin *khôi phục* được.
- **Bảng thống kê**: số người dùng / bài / bình luận / hội thoại / báo cáo… + biểu đồ bài đăng 14 ngày.
- **Nhật ký hành động admin** (audit log): ai làm gì, lúc nào.

### Bảo mật & hiệu năng
- JWT (custom filter, stateless); blacklist token khi đăng xuất / đổi mật khẩu / khoá.
- **Refresh token** (xoay vòng): access token hết hạn thì tự gia hạn ngầm, không đá người dùng ra trang đăng nhập. Lưu DB dạng **băm SHA‑256**, không phải bản gốc.
- **Xác thực 2 bước (2FA)** tuỳ chọn: bật trong hồ sơ → mỗi lần đăng nhập cần thêm mã 6 ký tự gửi qua email.
- **Mã hoá tin nhắn chat khi lưu DB** (AES‑256‑GCM, tuỳ chọn qua `MESSAGE_CRYPTO_KEY`): dump database không đọc được nội dung tin nhắn. Không phải E2EE.
- **Chống XSS**: nội dung do người dùng nhập luôn render dạng text (không `v-html`); tên hiển thị / tên nhóm lọc `<` `>` khi ghi.
- **Giới hạn tần suất** `/auth/**` chống brute-force mật khẩu và spam OTP — khoá theo IP TCP thật (không tin `X‑Forwarded‑For` trừ khi bật `RATELIMIT_TRUST_FORWARDED` sau proxy tin cậy).
- Đăng nhập **không tiết lộ email có tồn tại hay không** (thông báo lỗi + thời gian phản hồi đồng đều).
- Kiểm soát quyền: sửa/xoá bài & bình luận yêu cầu đúng tác giả (hoặc admin); đọc/ghi hội thoại yêu cầu là thành viên; endpoint `/admin/**` tự kiểm tra quyền admin.
- CORS mặc định mở (`*`); đặt `APP_CORS_ALLOWED_ORIGINS` khi deploy thật.
- Upload ảnh: chỉ png/jpg/jpeg (kiểm tra cả đuôi lẫn mime Tika), chặn quá kích thước, **tự thu nhỏ ≤1600px + nén** phía server; tên file do server sinh.
- Truy vấn DB tối ưu: index feed `(posted_at DESC, post_id ASC)`, bảng tin / bài-theo-tag nạp theo lô chống N+1, bỏ `COUNT` thừa khi phân trang, tìm kiếm không dựng cây bình luận thừa.
- Bộ **unit test** (`social/` — `./mvnw test`, 72 test) cho rate-limit (kể cả chống giả `X‑Forwarded‑For`), xử lý ảnh, quyền chặn/kết bạn, luật bài viết & repost (kể cả quyền xoá), quyền xem bài, tự ẩn bài, phân trang thông báo, hashtag, refresh token, 2FA; không cần DB/Docker.

## Cấu trúc repo

| Đường dẫn | Vai trò |
|---|---|
| `forum_fe/` | Frontend Vue 3 (bản build được nhúng vào image nginx) |
| `social/` | Backend Spring Boot |
| `docker-compose.yml` | Điểm khởi chạy duy nhất được hỗ trợ |
| `.env.example` | Mẫu biến môi trường |
| `scripts/db-export.sh`, `scripts/db-import.sh` | Xuất / nhập DB khi chuyển hạ tầng |
| `scripts/backup.sh` | Sao lưu định kỳ (DB + volume uploads), giữ 14 ngày — cắm vào cron |
| `scripts/backfill-hashtags.sh` | Quét hashtag cho bài viết cũ (chạy 1 lần khi bật tính năng) |
| `socialdata.sql`, `social/db.sql`, `social/db1.sql` | Dump / seed dữ liệu |
