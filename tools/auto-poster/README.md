# auto-poster

Công cụ đăng bài tự động lên forum từ các file Markdown. MVP, **không cần `npm install`** (Node ≥ 18).

## Cài đặt

```bash
cd tools/auto-poster
cp .env.example .env
# sửa .env: API_URL, BOT_EMAIL, BOT_PASSWORD (tài khoản bot riêng, KHÔNG dùng admin)
```

## Viết bài

Mỗi bài là 1 file `content/<slug>.md` với frontmatter:

```markdown
---
title: Tiêu đề bài viết          # bỏ trống -> lấy dòng "# ..." đầu tiên, hoặc tên file
visibility: public               # public | friends | private
tags: [thongbao, huongdan]       # tuỳ chọn -> tự chèn #tag xuống cuối body nếu thiếu
image: ./banner.png              # tuỳ chọn -> chỉ .png/.jpg/.jpeg, ≤10MB
publish: false                   # false = đăng BẢN NHÁP (mặc định) | true = xuất bản luôn
---
Nội dung ở đây, text thuần (không HTML). Xuống dòng thoải mái.
```

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

**cron (máy Ubuntu):**
```cron
*/30 * * * * cd /home/hp/Deploy_Docker/tools/auto-poster && /usr/bin/node post.mjs >> poster.log 2>&1
```

**GitHub Actions** (nội dung commit vào repo):
```yaml
on:
  schedule: [{ cron: '0 * * * *' }]
  workflow_dispatch:
jobs:
  post:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 20 }
      - run: node post.mjs
        working-directory: tools/auto-poster
        env:
          API_URL: ${{ secrets.FORUM_API_URL }}
          BOT_EMAIL: ${{ secrets.FORUM_BOT_EMAIL }}
          BOT_PASSWORD: ${{ secrets.FORUM_BOT_PASSWORD }}
```

## Ghi chú kỹ thuật

- Xác thực: đăng nhập lại mỗi lần chạy (`/auth/signin`, giới hạn 20 lần/5 phút — thừa dùng).
  Gặp `401` giữa chừng → tự gọi `/auth/refresh` một lần rồi thử lại.
- `POST /post/new` là **multipart** (không phải JSON). Ảnh được server tự thu nhỏ về 1600px.
- `#hashtag` trong tiêu đề/nội dung được backend tự index — dùng để lọc/thống kê bài bot.
- `/post/**` không bị rate-limit; `POST_DELAY_MS` chỉ để rải cho tự nhiên.
