#!/usr/bin/env bash
# Deploy: kéo code mới, build, chờ backend healthy, rồi mới dọn rác build.
#
#   ./scripts/deploy.sh              # kéo code + build + dọn
#   ./scripts/deploy.sh --no-pull    # chỉ build lại code đang có
#
# Vì sao có script này thay vì gõ `docker compose up -d --build`: mỗi lần build để lại
# lớp cache cũ và một image bị bỏ nhãn. Compose KHÔNG có hook sau khi build, nên chỗ duy
# nhất gắn được việc dọn là chính cái lệnh mình gõ. Đo trên máy thật sau vài lần deploy
# trong một ngày: 13,1 GB build cache + 4,1 GB image thừa = 17 GB, gần nửa đĩa đang dùng.
#
# Dọn theo TRẦN chứ không xoá sạch: cache là thứ làm lần build sau nhanh. Giữ 5 GB thì
# lớp Maven/npm đang dùng vẫn còn, chỉ bỏ lịch sử của các lần build cũ.
set -euo pipefail
cd "$(dirname "$0")/.."

PULL=1
[ "${1:-}" = "--no-pull" ] && PULL=0

KEEP_CACHE="${DEPLOY_KEEP_CACHE:-5GB}"
PORT="${BACKEND_PORT:-8081}"
[ -f .env ] && PORT="$(grep -E '^BACKEND_PORT=' .env | tail -1 | cut -d= -f2 || true)"
PORT="${PORT:-8081}"

echo "== Đĩa trước khi deploy"
df -h / | tail -1

if [ "$PULL" = "1" ]; then
    echo
    echo "== Kéo code mới"
    git pull --ff-only
fi
echo "   HEAD: $(git log --oneline -1)"

echo
echo "== Build + khởi động"
docker compose up -d --build

echo
echo "== Chờ backend healthy (tối đa 180 giây)"
ok=0
for i in $(seq 1 60); do
    if curl -fsS -m 3 "http://localhost:${PORT}/actuator/health" >/dev/null 2>&1; then
        ok=1
        echo "   backend UP sau ~$((i * 3))s"
        break
    fi
    sleep 3
done

if [ "$ok" != "1" ]; then
    echo >&2
    echo "LỖI: backend không lên sau 180 giây. KHÔNG dọn gì cả — image cũ được giữ lại" >&2
    echo "để còn quay về được. Xem log:  docker compose logs --tail 80 backend" >&2
    exit 1
fi

# Chỉ dọn KHI ĐÃ chắc bản mới chạy được. Image "dangling" chính là bản build trước đó;
# xoá nó trước khi biết bản mới có sống không là tự tay đốt đường lui.
echo
echo "== Dọn rác build (giữ ${KEEP_CACHE} cache)"
docker image prune -f || echo "   (bỏ qua lỗi dọn image)"
# --keep-storage bị đổi tên thành --max-used-space ở Docker mới -> thử lần lượt.
# KHÔNG rơi về `prune -f` trần: xoá sạch cache còn tệ hơn là để nguyên, vì lần build
# sau phải tải lại toàn bộ dependency.
docker builder prune -f --keep-storage "$KEEP_CACHE" 2>/dev/null \
    || docker builder prune -f --max-used-space "$KEEP_CACHE" 2>/dev/null \
    || echo "   (Docker này không nhận --keep-storage lẫn --max-used-space, bỏ qua)"

echo
echo "== Đĩa sau khi deploy"
df -h / | tail -1
docker compose ps --format 'table {{.Name}}\t{{.Status}}'
