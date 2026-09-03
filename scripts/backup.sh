#!/bin/bash
# Sao lưu định kỳ: dump database + nén volume ảnh upload, giữ lại N ngày gần nhất.
# Tự chứa — KHÔNG source .env (đọc biến ngay trong container db).
#
# Cài trên máy Ubuntu (1 lần):
#   chmod +x /home/hp/Deploy_Docker/scripts/backup.sh
#   ( crontab -l 2>/dev/null; \
#     echo '30 3 * * * /home/hp/Deploy_Docker/scripts/backup.sh >> /home/hp/backup.log 2>&1' \
#   ) | crontab -
#   sudo cp /home/hp/Deploy_Docker/scripts/backup.logrotate /etc/logrotate.d/forum-backup
#
# Tuỳ chỉnh qua biến môi trường:
#   BACKUP_KEEP_DAYS    số ngày giữ lại               (mặc định 14)
#   UPLOADS_VOLUME      tên docker volume ảnh upload   (mặc định tự dò *_uploads)
#   BACKUP_RSYNC_DEST   nếu đặt -> rsync backup/ sang đích này (offsite qua SSH)
#   BACKUP_RCLONE_DEST  nếu đặt -> `rclone sync backup/` lên đây (vd remote R2/B2:bucket/path)
#   RCLONE_BIN          đường dẫn rclone (mặc định "rclone" trong PATH)
#   BACKUP_GIT_DIR      nếu đặt -> copy bộ backup vào clone này, force-push 1 commit không cha
#                       (repo private làm nơi cất; xem DEPLOY.md để tạo + gắn deploy key)
set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$REPO/backup"
KEEP="${BACKUP_KEEP_DAYS:-14}"
TS="$(date +%Y%m%d-%H%M%S)"

cd "$REPO"
mkdir -p "$OUT"

# Chống chạy chồng (cron tick trùng lần chạy tay)
exec 9>/tmp/forum-backup.lock
flock -n 9 || { echo "$(date '+%F %T') — backup đang chạy, bỏ lượt"; exit 0; }

echo "=== $(date '+%F %T') bắt đầu backup ==="

# Lỗi giữa chừng -> xoá file dở dang (đừng để bản backup cụt trông như hợp lệ)
trap 'rc=$?; [ $rc -ne 0 ] && rm -f "$OUT/db-$TS.sql.gz" "$OUT/uploads-$TS.tar.gz" && \
      echo "!! backup THẤT BẠI (mã $rc) — đã xoá file dở"; exit $rc' EXIT

# 1) Database -> db-<ts>.sql.gz
docker compose exec -T db sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --databases "${MYSQL_DATABASE:-socialapp}" \
     --add-drop-database --routines --events --single-transaction --no-tablespaces' \
  | gzip -c > "$OUT/db-$TS.sql.gz"
echo "  DB   -> db-$TS.sql.gz ($(du -h "$OUT/db-$TS.sql.gz" | cut -f1))"

# 2) Volume uploads -> uploads-<ts>.tar.gz
VOL="${UPLOADS_VOLUME:-$(docker volume ls -q | grep -E '_uploads$' | head -1 || true)}"
if [ -n "$VOL" ] && docker volume inspect "$VOL" >/dev/null 2>&1; then
  docker run --rm -v "$VOL":/data:ro -v "$OUT":/out alpine \
    tar czf "/out/uploads-$TS.tar.gz" -C /data .
  echo "  file -> uploads-$TS.tar.gz ($(du -h "$OUT/uploads-$TS.tar.gz" | cut -f1))  [volume $VOL]"
else
  echo "  ! không tìm thấy volume uploads — đặt UPLOADS_VOLUME=... (bỏ qua phần ảnh)"
fi

# 3) Dọn bản cũ hơn KEEP ngày
find "$OUT" -maxdepth 1 -name 'db-*.sql.gz'      -mtime "+$KEEP" -print -delete || true
find "$OUT" -maxdepth 1 -name 'uploads-*.tar.gz' -mtime "+$KEEP" -print -delete || true

# 4) (tuỳ chọn) đẩy offsite
if [ -n "${BACKUP_RSYNC_DEST:-}" ]; then
  rsync -a --delete "$OUT/" "$BACKUP_RSYNC_DEST/" && echo "  offsite (rsync) -> $BACKUP_RSYNC_DEST"
fi
if [ -n "${BACKUP_RCLONE_DEST:-}" ]; then
  "${RCLONE_BIN:-rclone}" sync "$OUT/" "$BACKUP_RCLONE_DEST/" \
    --include 'db-*.sql.gz' --include 'uploads-*.tar.gz' \
    && echo "  offsite (rclone) -> $BACKUP_RCLONE_DEST"
fi
# Đẩy lên 1 repo git riêng (private). Ghi đè bằng 1 commit KHÔNG cha rồi force-push
# -> repo luôn chỉ bằng bộ backup đang giữ, lịch sử không phình.
# BACKUP_GIT_DIR = 1 clone riêng đã set remote + quyền push (deploy key).
if [ -n "${BACKUP_GIT_DIR:-}" ] && [ -d "$BACKUP_GIT_DIR/.git" ]; then
  rm -f "$BACKUP_GIT_DIR"/db-*.sql.gz "$BACKUP_GIT_DIR"/uploads-*.tar.gz
  cp "$OUT"/db-*.sql.gz "$OUT"/uploads-*.tar.gz "$BACKUP_GIT_DIR"/ 2>/dev/null || true
  git -C "$BACKUP_GIT_DIR" add -A
  _tree=$(git -C "$BACKUP_GIT_DIR" write-tree)
  _commit=$(echo "backup $TS" | git -C "$BACKUP_GIT_DIR" commit-tree "$_tree")
  git -C "$BACKUP_GIT_DIR" update-ref refs/heads/main "$_commit"
  git -C "$BACKUP_GIT_DIR" push -q -f origin main \
    && echo "  offsite (git) -> $(git -C "$BACKUP_GIT_DIR" remote get-url origin)"
fi

echo "=== xong. Đang giữ $(ls -1 "$OUT"/db-*.sql.gz 2>/dev/null | wc -l) bản DB," \
     "$(ls -1 "$OUT"/uploads-*.tar.gz 2>/dev/null | wc -l) bản uploads ==="
