#!/bin/bash
# Wrapper cho cron auto-poster trên máy Ubuntu.
# Cài:
#   cp tools/auto-poster/run.sh /home/hp/Deploy_Docker/tools/auto-poster/run.sh
#   chmod +x /home/hp/Deploy_Docker/tools/auto-poster/run.sh
#   ( crontab -l 2>/dev/null; echo '*/30 * * * * /home/hp/Deploy_Docker/tools/auto-poster/run.sh >> /home/hp/auto-poster.log 2>&1' ) | crontab -
set -e

REPO=/home/hp/Deploy_Docker
NET=deploy_docker_app-network

# Chống chạy chồng: cron tick trùng lần chạy tay -> 2 tiến trình post.mjs cùng lúc,
# login của cái sau blacklist token của cái trước -> hàng loạt 401. flock đảm bảo 1 lượt/1 lúc.
exec 9>/tmp/auto-poster.lock
flock -n 9 || { echo "$(date '+%F %T') — đang chạy, bỏ lượt này"; exit 0; }

# Bám cứng origin/main (không kẹt khi có sửa tay trên box). Không đụng file .gitignore
# (.env / .state.json / .rss-seen.json / content/rss-*.md vẫn giữ nguyên).
cd "$REPO" && git fetch -q origin && git reset --hard -q origin/main

docker run --rm \
  -v "$REPO/tools/auto-poster:/app" -w /app \
  --network "$NET" -e API_URL=http://backend:8081 \
  node:20-alpine \
  sh -c '[ -f feeds.txt ] && node fetch-rss.mjs || true; node post.mjs'

# Dọn file RSS đã đăng cũ hơn 14 ngày
find "$REPO/tools/auto-poster/content" -name 'rss-*.md' -mtime +14 -delete
