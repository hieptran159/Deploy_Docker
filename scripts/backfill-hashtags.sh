#!/usr/bin/env bash
# Quét hashtag (#tag) trong TẤT CẢ bài viết đang có và nạp vào bảng `post_hashtags`.
#
# Dùng khi bật tính năng hashtag trên một CSDL đã có sẵn bài cũ: các bài tạo trước đó
# chưa có dòng chỉ mục nào (chỉ `PostServiceImpl.syncHashtags` lúc tạo/sửa mới sinh ra).
# Chạy 1 lần là đủ; bài mới về sau tự được đánh chỉ mục.
#
#   ./scripts/backfill-hashtags.sh
#
# Idempotent: chỉ THÊM dòng còn thiếu (INSERT IGNORE), không xoá gì. Chạy lại vô hại.
#
# Cần: MySQL client (`mysql`) trên PATH, hoặc đặt MYSQL_BIN=/đường/dẫn/mysql
# Kết nối lấy từ .env (hoặc mặc định giống application.properties):
#   MYSQL_HOST=localhost  MYSQL_PORT=3306  MYSQL_USER=root
#   MYSQL_PASSWORD=tranhiep12345  MYSQL_DATABASE=socialapp
#
# Luật trích phải KHỚP với social/.../utils/HashtagUtils.java:
#   #<chữ/số/_ , 1..50 ký tự, hỗ trợ Unicode>, viết thường, bỏ tag toàn chữ số, tối đa 20 tag/bài.
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f .env ] && { set -a; . ./.env; set +a; }
MYSQL_BIN="${MYSQL_BIN:-mysql}"
HOST="${MYSQL_HOST:-localhost}"
PORT="${MYSQL_PORT:-3306}"
USER="${MYSQL_USER:-root}"
PW="${MYSQL_PASSWORD:-tranhiep12345}"
DB="${MYSQL_DATABASE:-socialapp}"

command -v "$MYSQL_BIN" >/dev/null 2>&1 || { echo "Không thấy '$MYSQL_BIN'. Đặt MYSQL_BIN trỏ tới mysql client." >&2; exit 1; }

myq() { "$MYSQL_BIN" -h"$HOST" -P"$PORT" -u"$USER" -p"$PW" "$DB" --default-character-set=utf8mb4 "$@"; }

echo "Đang đọc bài viết từ $HOST:$PORT/$DB ..."
# Gộp title + body về 1 dòng (thay \r \n \t bằng khoảng trắng) để xử lý theo dòng.
sql_dump="SELECT CONCAT(post_id, 0x09, REPLACE(REPLACE(REPLACE(CONCAT(COALESCE(title,''),' ',COALESCE(body,'')), 0x0d, ' '), 0x0a, ' '), 0x09, ' ')) FROM posts"

pairs=0
posts_with_tags=0
values=""

while IFS=$'\t' read -r pid text; do
  [ -n "${pid:-}" ] || continue
  # Trích #tag -> bỏ '#', viết thường, bỏ tag toàn số, khử trùng giữ thứ tự, tối đa 20.
  tags=$(printf '%s' "$text" \
    | grep -oP '#[\p{L}\p{N}_]{1,50}' 2>/dev/null \
    | sed 's/^#//' \
    | tr '[:upper:]' '[:lower:]' \
    | grep -vxE '[0-9]+' \
    | awk '!seen[$0]++' \
    | head -n 20 || true)
  [ -n "$tags" ] || continue
  posts_with_tags=$((posts_with_tags + 1))
  while IFS= read -r tag; do
    [ -n "$tag" ] || continue
    values="${values}${values:+,}('${pid}','${tag}')"
    pairs=$((pairs + 1))
  done <<< "$tags"
done < <(myq --raw -N -e "$sql_dump" 2>/dev/null)

if [ "$pairs" -eq 0 ]; then
  echo "Không tìm thấy hashtag nào trong bài viết hiện có. Không thay đổi gì."
  exit 0
fi

echo "Tìm thấy $pairs cặp (bài, tag) trên $posts_with_tags bài. Đang nạp (INSERT IGNORE) ..."
myq -e "INSERT IGNORE INTO post_hashtags (post_id, tag) VALUES ${values};"

total=$(myq -N -e "SELECT COUNT(*) FROM post_hashtags;" 2>/dev/null | tr -d '[:space:]')
distinct=$(myq -N -e "SELECT COUNT(DISTINCT tag) FROM post_hashtags;" 2>/dev/null | tr -d '[:space:]')
echo "Xong. post_hashtags hiện có $total dòng / $distinct tag khác nhau."
echo "Nếu frontend đang mở, tải lại trang để thấy chip hashtag + card 'Hashtag nổi bật'."
