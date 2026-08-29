# Chuyển hạ tầng: Windows (MySQL native) → Ubuntu (Docker Compose) + Cloudflare Tunnel

Runbook đã dùng thật, ngày 2026-08-29. Bổ sung cho `DEPLOY.md` (mục
"Chuyển dữ liệu sang hạ tầng mới") cho tình huống cụ thể:

| | Máy cũ | Máy mới |
|---|---|---|
| OS | Windows | Ubuntu (`198.18.128.51`, user `hp`) |
| Chạy app | `java -jar social/target/*.jar` + `npm run dev` | `docker compose` (stack đầy đủ) |
| MySQL | **native**, service `MySQL80`, cổng 3306, db `socialapp` | container `mysql:8.0` (volume `mysql-data`) |
| Ảnh upload | thư mục `social/uploads/images/` | volume `deploy_docker_uploads` (`/app/uploads`) |
| Public URL | localhost | `app./api./ws.<domain>` qua Cloudflare Tunnel |

> Máy cũ **không có Docker** → không dùng `scripts/db-export.sh` (script đó gọi
> `docker compose exec db mysqldump`). Dump thẳng bằng `mysqldump.exe` của bản
> MySQL native.

---

## Secret phải bê nguyên trạng sang máy mới

Lấy từ máy cũ, đặt vào `.env` của máy mới (mục D). **Không commit các giá trị thật.**

| Biến | Lấy ở đâu (máy cũ) | Ghi chú |
|---|---|---|
| `MESSAGE_CRYPTO_KEY` | `social/.env` | **Quan trọng nhất.** Sai/thiếu → tin nhắn chat đã mã hoá không giải mã được. Nếu máy cũ để trống thì máy mới cũng để trống. |
| `JWT_SECRET` | `social/.env` nếu có, không thì mặc định trong `.env.example` | Đổi giá trị → mọi token đang đăng nhập bị vô hiệu (user phải login lại). |
| `MYSQL_ROOT_PASSWORD` | mật khẩu root của MySQL native (app kết nối bằng pw này) | Mặc định trong repo: xem `.env.example`. |
| `SENDGRID_API_KEY` | `social/.env` | Nên **tạo key mới** trên SendGrid (key cũ đã lộ trong lịch sử git công khai). Để trống vẫn chạy — mã OTP in ra log backend. |

---

## A. [MÁY CŨ / Git Bash] Xuất dữ liệu

Tại thư mục repo (`.../Deploy_Docker`):

```bash
mkdir -p backup

# A1. Dump toàn bộ database từ MySQL native
"/c/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump.exe" -uroot -p<ROOT_PW> \
  --databases socialapp --add-drop-database --routines --events \
  --single-transaction --no-tablespaces \
  --result-file=backup/prod.sql

# A2. Nén thư mục ảnh upload (giữ nguyên cấp 'images/')
tar czf backup/uploads.tar.gz -C social/uploads images

# A3. Kiểm tra
ls -lh backup/
grep -c "INSERT INTO" backup/prod.sql     # ~21
tar tzf backup/uploads.tar.gz | head
```

`--result-file=` (thay cho `> file`) để tránh Git Bash chèn CRLF làm hỏng dump.

---

## B. [MÁY CŨ] Đẩy sang Ubuntu

```bash
scp backup/prod.sql backup/uploads.tar.gz hp@198.18.128.51:/tmp/
```

(Tuỳ chọn) tạo SSH key để khỏi nhập mật khẩu nhiều lần:

```bash
[ -f ~/.ssh/id_ed25519 ] || ssh-keygen -t ed25519 -N "" -f ~/.ssh/id_ed25519
cat ~/.ssh/id_ed25519.pub | ssh hp@198.18.128.51 \
  "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
```

---

## C. [MÁY MỚI] Cài Docker + lấy repo

```bash
ssh hp@198.18.128.51

sudo apt-get update
sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER
newgrp docker
docker compose version

git clone https://github.com/hieptran159/Deploy_Docker.git ~/Deploy_Docker
cd ~/Deploy_Docker
mkdir -p backup && mv /tmp/prod.sql /tmp/uploads.tar.gz backup/
```

---

## D. [MÁY MỚI] Tạo `.env`

Đổi `hipe.id.vn` thành domain thật; điền các secret ở bảng trên.

```bash
cd ~/Deploy_Docker
DOMAIN=hipe.id.vn

cat > .env <<ENV
FRONTEND_PORT=80
BACKEND_PORT=8081
SOCKET_PORT=8082
DB_PORT=3307

MYSQL_ROOT_PASSWORD=<ROOT_PW>
MYSQL_DATABASE=socialapp
JWT_SECRET=<JWT_SECRET>
MESSAGE_CRYPTO_KEY=<MESSAGE_CRYPTO_KEY>

SENDGRID_API_KEY=
SENDGRID_FROM_EMAIL=no-reply@$DOMAIN
SENDGRID_FROM_NAME=HIPDN-EA Team

PUBLIC_API_URL=https://api.$DOMAIN
PUBLIC_SOCKET_URL=https://ws.$DOMAIN
APP_CORS_ALLOWED_ORIGINS=https://app.$DOMAIN

RATELIMIT_TRUST_FORWARDED=true
ENV
```

`PUBLIC_API_URL` / `PUBLIC_SOCKET_URL` được **nướng vào bản build frontend** → đổi là
phải `docker compose up -d --build frontend` lại. `RATELIMIT_TRUST_FORWARDED=true` vì
backend đứng sau Cloudflare (proxy tin cậy đặt `X-Forwarded-For`).

---

## E. [MÁY MỚI] Dựng DB

```bash
cd ~/Deploy_Docker
docker compose up -d --build db

until [ "$(docker inspect -f '{{.State.Health.Status}}' forum-db 2>/dev/null)" = healthy ]; do
  echo "cho MySQL..."; sleep 3; done
echo "DB healthy"
```

Lần `up` đầu tiên với volume trống, MySQL tự chạy `social/db.sql` (schema + dữ liệu
mẫu). Bước F sẽ ghi đè bằng dữ liệu thật.

---

## F. [MÁY MỚI] Nạp dữ liệu thật  ⚠️ đúng thứ tự

`prod.sql` chứa `DROP DATABASE ... CREATE DATABASE` (do `--add-drop-database`), nên
phải **tắt backend trước** để nó không giữ kết nối vào DB sắp bị drop — nếu không,
backend chạy trên schema rỗng do `ddl-auto` tự tạo và **dữ liệu import không xuất hiện**.

```bash
cd ~/Deploy_Docker

# 1. tắt backend + frontend
docker compose stop backend frontend

# 2. nạp (không truyền tên db — dump tự CREATE + USE socialapp)
docker compose exec -T db mysql -uroot -p<ROOT_PW> < backup/prod.sql
echo "import exit=$?"

# 3. xác minh có dữ liệu
docker compose exec -T db mysql -uroot -p<ROOT_PW> socialapp -e "
SELECT 'users' t,COUNT(*) n FROM users
UNION ALL SELECT 'posts',COUNT(*) FROM posts
UNION ALL SELECT 'comments',COUNT(*) FROM comments
UNION ALL SELECT 'messages',COUNT(*) FROM messages;"

# 4. bật lại toàn bộ
docker compose up -d --build
```

> Dump có thể chứa bảng `flyway_schema_history` (di sản từ nhánh thử Flyway). Bản
> backend trên `main` không dùng Flyway → bảng đó nằm im, vô hại.

---

## G. [MÁY MỚI] Khôi phục ảnh upload

Giải nén vào **gốc volume** `deploy_docker_uploads` (mount tại `/app/uploads`), ra
`/app/uploads/images/<type>/...`:

```bash
cd ~/Deploy_Docker
docker compose stop backend
docker run --rm -v deploy_docker_uploads:/data -v "$PWD/backup:/in" alpine \
  sh -c "cd /data && tar xzf /in/uploads.tar.gz"
docker compose start backend

# kiểm tra
docker compose exec backend sh -c "ls -R /app/uploads/images | head -30"
```

> Tên volume = `<tên-thư-mục-repo-viết-thường>_uploads`. Repo tên `Deploy_Docker`
> → `deploy_docker_uploads`. Clone vào thư mục tên khác thì đổi cho khớp
> (`docker volume ls`).

---

## H. [MÁY MỚI] Cloudflare Tunnel (3 hostname)

Frontend là static + gọi API từ trình duyệt người xem → phải expose **cả 3**:
FE (80), REST (8081), Socket.IO (8082).

```bash
sudo curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 \
  -o /usr/local/bin/cloudflared
sudo chmod +x /usr/local/bin/cloudflared

cloudflared tunnel login          # mở link, chọn zone domain
cloudflared tunnel create forum   # ghi lại TUNNEL_ID
```

```bash
DOMAIN=hipe.id.vn
TID=$(cloudflared tunnel list --output json \
  | python3 -c "import sys,json;print([t['id'] for t in json.load(sys.stdin) if t['name']=='forum'][0])")

mkdir -p ~/.cloudflared
cat > ~/.cloudflared/config.yml <<CFG
tunnel: $TID
credentials-file: $HOME/.cloudflared/$TID.json
ingress:
  - hostname: app.$DOMAIN
    service: http://localhost:80
  - hostname: api.$DOMAIN
    service: http://localhost:8081
  - hostname: ws.$DOMAIN
    service: http://localhost:8082
  - service: http_status:404
CFG

for h in app api ws; do cloudflared tunnel route dns forum $h.$DOMAIN; done

sudo cloudflared --config ~/.cloudflared/config.yml service install
sudo systemctl restart cloudflared
systemctl status cloudflared --no-pager | head -15
```

Bắt buộc: FE qua Cloudflare là `https://` → `PUBLIC_API_URL` / `PUBLIC_SOCKET_URL`
cũng phải `https://` (mixed content bị chặn). App auth bằng header `Authorization`,
không dùng cookie → không dính SameSite/domain khi qua tunnel. WebSocket qua tunnel
chạy bình thường.

---

## I. Kiểm tra

```bash
# nội bộ máy mới
curl -s -o /dev/null -w "api  -> %{http_code}\n" http://localhost:8081/api-docs
curl -s -o /dev/null -w "feed -> %{http_code}\n" "http://localhost:8081/post/get?page=1"
curl -s -o /dev/null -w "fe   -> %{http_code}\n" http://localhost/

# qua tunnel
curl -sf https://api.$DOMAIN/api-docs | head -c 120 ; echo
curl -sf https://app.$DOMAIN/ | grep -o '<title>[^<]*</title>'

docker compose logs backend | grep -E "Started SocialApplication|APPLICATION FAILED|Caused by"
```

Rồi mở `https://app.<domain>`: đăng nhập tài khoản cũ → mở bài có ảnh (uploads) →
gửi 1 tin nhắn chat (xác nhận `MESSAGE_CRYPTO_KEY` đúng).

---

## J. Dọn dẹp

```bash
# máy mới
rm -f ~/Deploy_Docker/backup/prod.sql ~/Deploy_Docker/backup/uploads.tar.gz
# máy cũ
rm -f backup/prod.sql backup/uploads.tar.gz
```

---

## Vận hành sau này

```bash
cd ~/Deploy_Docker
docker compose logs -f backend        # xem log
docker compose ps                     # trạng thái
docker compose pull && docker compose up -d --build   # cập nhật khi có code mới (git pull trước)
docker compose down                   # dừng, GIỮ dữ liệu
docker compose down -v                # dừng + XOÁ dữ liệu (db + ảnh)
```

Sao lưu định kỳ:

```bash
docker compose exec -T db mysqldump -uroot -p<ROOT_PW> \
  --databases socialapp --single-transaction --no-tablespaces \
  > backup/socialapp-$(date +%F).sql
docker run --rm -v deploy_docker_uploads:/data -v "$PWD/backup:/out" alpine \
  tar czf /out/uploads-$(date +%F).tar.gz -C /data .
```

Cần giữ an toàn (mất là hỏng dữ liệu tương ứng): `MESSAGE_CRYPTO_KEY`,
`JWT_SECRET`, `MYSQL_ROOT_PASSWORD`.
