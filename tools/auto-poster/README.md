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

## Nguồn nội dung tự động (RSS)

`fetch-rss.mjs` đọc feed RSS/Atom và tự sinh file `content/rss-*.md` (bản nháp),
sau đó `post.mjs` đăng.

```bash
cp feeds.txt.example feeds.txt      # điền URL feed, mỗi dòng 1 cái
node fetch-rss.mjs --dry-run        # xem sẽ tạo file nào
node fetch-rss.mjs --limit 3        # tạo tối đa 3 file mới
node post.mjs                       # đăng
```

- `DRAFT_LIMIT` (`.env`, mặc định 100): khi tài khoản bot có ≥ ngần này bản nháp,
  `post.mjs` **tạm dừng đăng** (thoát 0, không lỗi) cho tới khi bạn duyệt bớt ở `/drafts`
  ("Đăng tất cả" / "Xoá tất cả"). Đặt `0` để tắt.
- Nhiều feed → lấy **luân phiên** mỗi feed (round-robin) cho tới khi đủ `RSS_MAX_PER_RUN`
  (mặc định 10). Thứ tự feed được **xáo trộn mỗi lần chạy**, nên khi số feed nhiều hơn
  `RSS_MAX_PER_RUN` cũng không feed nào bị ưu tiên/bỏ rơi theo thứ tự trong `feeds.txt`.
- `.rss-seen.json` nhớ entry đã lấy (theo id/link) → không tạo trùng, kể cả sau khi xoá file.
- Chỉnh qua `.env`: `RSS_MAX_PER_RUN` (mặc định 5, `.env.example` đặt 10),
  `RSS_TAG` (mặc định `tin-tuc`), `RSS_VISIBILITY`,
  `RSS_PUBLISH` (`true` = xuất bản luôn / `false` = nháp; **code mặc định `false`**,
  `.env.example` đặt `true`). Bật `true` thì `DRAFT_LIMIT` gần như không bao giờ chạm.
- `feeds.txt` **nên commit** để máy chạy cron `git pull` về dùng. `.rss-seen.json` và
  `content/rss-*.md` đã `.gitignore` (CI tự commit `.rss-seen.json` trở lại).
- Tóm tắt lấy từ `<description>`/`<summary>`/`<content>`, cắt HTML, tối đa ~900 ký tự,
  kèm dòng `Nguồn: <link>`.

Muốn nguồn khác (AI sinh bài, CSV, Google Sheet…): viết script tương tự đổ file `.md`
vào `content/` là xong — `post.mjs` không quan tâm file từ đâu ra.

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

### B. cron trên máy Ubuntu (khuyên dùng — gọi `localhost`, không đụng Cloudflare)

`.env` trên máy đó đặt `API_URL=http://localhost:8081` + `BOT_EMAIL` + `BOT_PASSWORD`.
Máy không cần Node — chạy qua Docker. Dùng luôn `run.sh` trong repo (có `flock` chống
chạy chồng + `git reset --hard origin/main` để không kẹt vì sửa tay):

```bash
chmod +x /home/hp/Deploy_Docker/tools/auto-poster/run.sh

# cron mỗi 30 phút
( crontab -l 2>/dev/null; echo '*/30 * * * * /home/hp/Deploy_Docker/tools/auto-poster/run.sh >> /home/hp/auto-poster.log 2>&1' ) | crontab -

# xoay vòng file log cho khỏi phình
sudo cp /home/hp/Deploy_Docker/tools/auto-poster/auto-poster.logrotate /etc/logrotate.d/auto-poster
```

`run.sh` giả định repo ở `/home/hp/Deploy_Docker` và network Docker là
`deploy_docker_app-network` — sửa 2 biến đầu file nếu khác.

Dung lượng: log container đã giới hạn 10MB×3 trong `docker-compose.yml`; binary log
MySQL giữ 7 ngày (`--binlog-expire-logs-seconds` trong compose). DB tăng ~50MB/tháng
với nhịp 4 bài / 15 phút — không đáng kể.

## Ghi chú kỹ thuật

- Xác thực: đăng nhập lại mỗi lần chạy (`/auth/signin`, giới hạn 20 lần/5 phút — thừa dùng).
  Gặp `401` giữa chừng → thử `/auth/refresh`, hỏng thì **đăng nhập lại** (tối đa 3 lần/lượt).
  Backend blacklist token cũ mỗi lần login, nên **không chạy 2 tiến trình poster song song**
  (đã có `flock` trong `run.sh`).
- `POST /post/new` là **multipart** (không phải JSON). Ảnh được server tự thu nhỏ về 1600px.
- `#hashtag` trong tiêu đề/nội dung được backend tự index — dùng để lọc/thống kê bài bot.
- `/post/**` không bị rate-limit; `POST_DELAY_MS` chỉ để rải cho tự nhiên.
