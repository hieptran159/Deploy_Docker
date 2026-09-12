#!/usr/bin/env bash
# Thu nhỏ các ảnh ĐÃ LƯU trong volume uploads: avatar / avatar nhóm chat / ảnh bìa.
#
# Vì sao cần: trước Sep 2026, avatar dùng chung giới hạn 1600px với ảnh bài viết, và một
# ảnh PNG nằm trong giới hạn đó thì được lưu y nguyên bytes gốc. Kết quả đo trên production:
# một avatar 516x507 nặng 503 KB, trong khi mọi nơi đều vẽ nó ở 40px. Bản vá trong
# FileUploadsServiceImpl chỉ áp dụng cho ảnh upload TỪ GIỜ; script này xử lý ảnh cũ.
#
#   ./scripts/shrink-uploads.sh              # chỉ xem sẽ làm gì (mặc định, không sửa gì)
#   ./scripts/shrink-uploads.sh --apply      # thực sự ghi đè
#
# GIỮ NGUYÊN tên file và định dạng (png vẫn là png). Không đổi đuôi vì đường dẫn ảnh
# được lưu trong DB (`users.avt_url`, `users.cover_url`, `conversations.avatar_url`) —
# đổi đuôi ở đây là làm chết ảnh. Nên script này chỉ thu nhỏ kích thước: với ảnh PNG
# chụp ảnh thật thì được khoảng 4 lần. Muốn ăn đủ ~24 lần (png -> jpg) thì chỉ cần
# upload lại ảnh đó một lần qua giao diện, backend mới sẽ tự chuyển định dạng.
#
# Chạy ImageMagick trong một container tạm, KHÔNG cần cài gì lên máy chủ. Cần Docker và
# container `forum-backend` đang chạy (để lấy đúng tên volume).
#
# An toàn: ghi đè trực tiếp trong volume. Ảnh upload đã nằm trong scripts/backup.sh
# (uploads-<ts>.tar.gz) — nên chạy backup trước nếu muốn chắc chắn quay lại được.
set -euo pipefail
cd "$(dirname "$0")/.."

APPLY=0
[ "${1:-}" = "--apply" ] && APPLY=1

AVATAR_MAX="${UPLOAD_AVATAR_MAX_DIMENSION:-256}"
COVER_MAX="${UPLOAD_COVER_MAX_DIMENSION:-1280}"

command -v docker >/dev/null 2>&1 || { echo "Không thấy docker." >&2; exit 1; }

VOL="$(docker inspect forum-backend \
        --format '{{range .Mounts}}{{if eq .Destination "/app/uploads"}}{{.Name}}{{end}}{{end}}' 2>/dev/null || true)"
if [ -z "$VOL" ]; then
    echo "Không tìm được volume uploads từ container forum-backend." >&2
    echo "Container có đang chạy không?  docker compose ps" >&2
    exit 1
fi
echo "Volume uploads: $VOL"
echo "Giới hạn: avatar/conversation ${AVATAR_MAX}px, cover ${COVER_MAX}px"
[ "$APPLY" = "1" ] && echo "CHẾ ĐỘ: ghi đè thật" || echo "CHẾ ĐỘ: chỉ xem (thêm --apply để ghi)"
echo

docker run --rm -v "$VOL":/data -e APPLY="$APPLY" \
    -e AVATAR_MAX="$AVATAR_MAX" -e COVER_MAX="$COVER_MAX" \
    alpine:3.20 sh -s <<'INNER'
set -eu
apk add --no-cache imagemagick >/dev/null 2>&1

shrink_dir() {
    dir="$1"; max="$2"
    [ -d "$dir" ] || { echo "(bỏ qua, không có $dir)"; return 0; }
    echo "== $dir  -> tối đa ${max}px"
    echo "   trước: $(du -sh "$dir" | cut -f1)"
    # -maxdepth 1: chỉ file ảnh ngay trong thư mục đó
    find "$dir" -maxdepth 1 -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' \) | while read -r f; do
        w=$(identify -format '%w' "$f" 2>/dev/null || echo 0)
        h=$(identify -format '%h' "$f" 2>/dev/null || echo 0)
        [ "$w" = 0 ] && continue
        long=$w; [ "$h" -gt "$w" ] && long=$h
        size=$(wc -c < "$f")
        if [ "$long" -le "$max" ] && [ "$size" -lt 120000 ]; then continue; fi
        if [ "$APPLY" = "1" ]; then
            # '>' = chỉ thu nhỏ, không bao giờ phóng to. -strip bỏ EXIF/ICC.
            mogrify -resize "${max}x${max}>" -strip -quality 82 "$f"
            echo "   sửa  ${f##*/}  ${w}x${h} ${size}B -> $(identify -format '%wx%h' "$f") $(wc -c < "$f")B"
        else
            echo "   sẽ sửa  ${f##*/}  ${w}x${h}  ${size}B"
        fi
    done
    echo "   sau:   $(du -sh "$dir" | cut -f1)"
}

shrink_dir /data/images/avatar "$AVATAR_MAX"
shrink_dir /data/images/conversation "$AVATAR_MAX"
shrink_dir /data/images/cover "$COVER_MAX"
INNER

echo
if [ "$APPLY" = "1" ]; then
    echo "Xong. Trình duyệt của người dùng có thể còn ảnh cũ trong cache tới 4 tiếng"
    echo "(Cache-Control: max-age=14400) — tên file không đổi nên không ép làm mới được."
else
    echo "Chưa sửa gì. Chạy lại với --apply để ghi đè."
fi
