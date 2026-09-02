# auto-poster

Đăng bài tự động lên forum từ file Markdown. MVP, **không cần `npm install`** (Node ≥ 18).

## Cài đặt

```bash
cd tools/auto-poster
cp .env.example .env
# sửa .env: API_URL, BOT_EMAIL, BOT_PASSWORD (tài khoản bot riêng, KHÔNG dùng admin)
```

## Viết bài

Thư mục `content/` để trống sẵn. Mỗi bài là 1 file `content/<slug>.md`:

```markdown
---
title: Tiêu đề bài viết          # bỏ trống -> lấy dòng "# ..." đầu tiên, hoặc tên file
visibility: public               # public | friends | private
tags: [thongbao, huongdan]       # tuỳ chọn -> tự chèn #tag xuống cuối body nếu thiếu
image: ./banner.png              # tuỳ chọn -> cùng thư mục file .md, chỉ .png/.jpg/.jpeg ≤10MB
publish: false                   # false = đăng BẢN NHÁP (mặc định) | true = xuất bản luôn
---
Nội dung ở đây, text thuần (không HTML). Xuống dòng thoải mái.
```

Xem `examples/` để lấy mẫu.

## Chạy

```bash
node post.mjs --dry-run     # xem sẽ đăng gì, KHÔNG gọi API
node post.mjs               # đăng (mặc định: bản nháp)
node post.mjs --publish     # đăng và xuất bản luôn (đè 'publish' trong mọi file)
node post.mjs --file content/bai-1.md
node post.mjs --force       # bỏ qua .state.json -> đăng lại tất cả thành bài mới
```

## Chống trùng

Sau mỗi bài đăng thành công, script ghi `.state.json` (`file -> {hash, postId, at}`).
Lần chạy sau: file đã có trong state → **bỏ qua**. Sửa nội dung file cũ cũng vẫn bỏ qua
(an toàn) — muốn đăng lại thì `--force` hoặc đổi tên file.

`.env` và `.state.json` đã được `.gitignore`.

## Chạy định kỳ

> **Lưu ý:** GitHub Actions gọi từ IP trung tâm dữ liệu → Cloudflare bắt "managed
> challenge" (HTTP 403 "Just a moment…"). Muốn dùng A phải thêm rule bỏ challenge
> (xem dưới). Không muốn đụng Cloudflare thì dùng **B** — chạy trên máy Ubuntu,
> gọi `localhost` thẳng, không qua Cloudflare.

### A. GitHub Actions (cần 1 rule Cloudflare)

Workflow `.github/workflows/auto-post.yml` chạy mỗi giờ. Cần **2 secret** ở
**Settings → Secrets and variables → Actions**, tab **Secrets** (KHÔNG phải "Variables"):

| Secret | Giá trị |
|---|---|
| `FORUM_BOT_EMAIL` | email tài khoản bot |
| `FORUM_BOT_PASSWORD` | mật khẩu tài khoản bot |
| `FORUM_BYPASS_TOKEN` | chuỗi ngẫu nhiên (vd `openssl rand -hex 16`) — phải khớp rule Cloudflare |

(`API_URL` đã ghi thẳng trong workflow — `https://api.hipe.id.vn`.)

**Rule Cloudflare** (bắt buộc cho A) — dashboard zone `hipe.id.vn`:

- *Cách nhanh:* **Security → Bots** → tắt **Bot Fight Mode** (giảm bảo vệ toàn zone).
- *Cách gọn hơn:* **Security → WAF → Custom rules → Create**:
  - Expression: `(http.host eq "api.hipe.id.vn" and http.request.headers["x-auto-poster"][0] eq "<FORUM_BYPASS_TOKEN>")`
  - Action: **Skip** → tích tất cả (Bot Fight Mode / Super Bot Fight Mode / Managed rules / rate limiting).
  - Poster tự gửi header `X-Auto-Poster: <BYPASS_TOKEN>` khi biến `BYPASS_TOKEN` được set.

Quy trình: commit file `.md` mới vào `content/` → workflow đăng → tự commit `.state.json`
trở lại repo để lần sau không đăng trùng. Bấm **Run workflow** để chạy tay.

### B. cron trên máy Ubuntu (backend gọi nội bộ, nhanh hơn)

`.env` trên máy đó nên đặt `API_URL=http://localhost:8081`. Nếu chưa có Node, chạy bằng Docker:

```cron
# crontab -e  — mỗi 30 phút
*/30 * * * * docker run --rm -v /home/hp/Deploy_Docker/tools/auto-poster:/app -w /app \
  --network deploy_docker_app-network -e API_URL=http://backend:8081 \
  node:20-alpine node post.mjs >> /home/hp/auto-poster.log 2>&1
```

(Có Node sẵn thì đơn giản hơn: `*/30 * * * * cd /home/hp/Deploy_Docker/tools/auto-poster && node post.mjs >> ~/auto-poster.log 2>&1`)

## Ghi chú kỹ thuật

- Xác thực: đăng nhập lại mỗi lần chạy (`/auth/signin`, giới hạn 20 lần/5 phút — thừa dùng).
  Gặp `401` giữa chừng → tự gọi `/auth/refresh` một lần rồi thử lại.
- `POST /post/new` là **multipart** (không phải JSON). Ảnh được server tự thu nhỏ về 1600px.
- `#hashtag` trong tiêu đề/nội dung được backend tự index — dùng để lọc/thống kê bài bot.
- `/post/**` không bị rate-limit; `POST_DELAY_MS` chỉ để rải cho tự nhiên.
