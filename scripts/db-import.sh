#!/usr/bin/env bash
# Nạp 1 file .sql vào database đang chạy (khôi phục / chuyển hạ tầng).
#   ./scripts/db-import.sh <đường-dẫn-file.sql>
set -euo pipefail
cd "$(dirname "$0")/.."

IN="${1:?Cách dùng: ./scripts/db-import.sh <file.sql>}"
[ -f "$IN" ] || { echo "Không thấy file: $IN" >&2; exit 1; }

[ -f .env ] && { set -a; . ./.env; set +a; }
PW="${MYSQL_ROOT_PASSWORD:-tranhiep12345}"

docker compose exec -T db sh -c "exec mysql -uroot -p'${PW}'" < "$IN"

echo "Đã nạp $IN vào database. Khởi động lại backend nếu cần: docker compose restart backend"
