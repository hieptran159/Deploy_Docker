#!/usr/bin/env bash
# Xuất toàn bộ database ra 1 file .sql (mang sang máy khác / sao lưu).
#   ./scripts/db-export.sh [đường-dẫn-file.sql]
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f .env ] && { set -a; . ./.env; set +a; }
PW="${MYSQL_ROOT_PASSWORD:-tranhiep12345}"
DB="${MYSQL_DATABASE:-socialapp}"

OUT="${1:-backup/${DB}-$(date +%Y%m%d-%H%M%S).sql}"
mkdir -p "$(dirname "$OUT")"

docker compose exec -T db sh -c \
  "exec mysqldump -uroot -p'${PW}' --databases '${DB}' --add-drop-database --routines --events --single-transaction --no-tablespaces" \
  > "$OUT"

echo "Đã xuất database -> $OUT"
