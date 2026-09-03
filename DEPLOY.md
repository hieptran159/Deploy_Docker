# Triển khai bằng Docker

Toàn bộ hệ thống (frontend + backend + MySQL) đóng gói trong `docker-compose.yml` ở thư mục gốc.

## Chạy lần đầu

```bash
# (tuỳ chọn) tạo file cấu hình riêng
cp .env.example .env

# build + chạy nền
docker compose up -d --build
```

Xong. Truy cập:

| Thành phần | Địa chỉ |
|---|---|
| Giao diện | http://localhost |
| REST API | http://localhost:8081 (Swagger: `/api-docs.html`) |
| Socket.IO | http://localhost:8082 |
| MySQL | `localhost:3307`, db `socialapp`, user `root` |

Volume DB trống ở lần chạy đầu → backend chạy **Flyway** lúc khởi động, tạo toàn bộ
schema từ `social/src/main/resources/db/migration/V1__baseline.sql`. Không còn nạp
`social/db.sql` tự động và không còn `ddl-auto=update` — Hibernate chỉ `validate`
(kiểm tra entity khớp bảng). Đổi schema về sau = thêm file `V2__*.sql`, `V3__*.sql`.

Muốn nạp sẵn một bản dump có dữ liệu (chuyển hạ tầng): `./scripts/db-import.sh <file.sql>`
sau khi container `db` đã chạy, trước khi `backend` khởi động (hoặc restart backend sau đó).

Lệnh thường dùng:

```bash
docker compose logs -f backend      # xem log
docker compose ps                   # trạng thái
docker compose down                 # dừng (giữ dữ liệu)
docker compose down -v              # dừng + XOÁ dữ liệu (db + ảnh upload)
```

## Deploy lên máy/server khác

1. Copy cả thư mục repo sang máy đích (hoặc `git clone`).
2. Tạo `.env`, đặt URL công khai mà **trình duyệt** gọi tới backend:

   ```
   PUBLIC_API_URL=http://<domain-hoặc-IP>:8081
   PUBLIC_SOCKET_URL=http://<domain-hoặc-IP>:8082
   # nếu FE khác origin với API (vd sau reverse-proxy / tunnel), khai báo origin FE:
   APP_CORS_ALLOWED_ORIGINS=http://<domain-frontend>
   ```

   (`PUBLIC_*` được "nướng" vào bản build frontend nên phải build lại khi đổi.)
3. `docker compose up -d --build`

Đổi cổng publish: sửa `FRONTEND_PORT` / `BACKEND_PORT` / `SOCKET_PORT` / `DB_PORT` trong `.env`.

## Chạy local + expose ra ngoài bằng Cloudflare Tunnel

Frontend của web là **static + gọi API từ trình duyệt người xem**. Nên nếu chỉ tunnel
mỗi frontend, trình duyệt người xem sẽ gọi `localhost:8081/8082` = máy của **họ** → hỏng.
Phải expose **cả 3**: FE, REST (8081), Socket.IO (8082).

### 1. Một tunnel, nhiều hostname

`~/.cloudflared/config.yml`:

```yaml
tunnel: <TUNNEL_ID>
credentials-file: /home/<user>/.cloudflared/<TUNNEL_ID>.json

ingress:
  - hostname: app.example.com          # Frontend
    service: http://localhost:80        # dùng cổng FRONTEND_PORT; nếu chạy vite dev thì :5173
  - hostname: api.example.com          # REST API
    service: http://localhost:8081
  - hostname: ws.example.com           # Socket.IO (chat, thông báo, presence)
    service: http://localhost:8082
  - service: http_status:404
```

Tạo DNS: `cloudflared tunnel route dns <TUNNEL_ID> app.example.com` (làm cho cả 3).
Chạy: `cloudflared tunnel run <TUNNEL_ID>`.

### 2. Trỏ frontend vào URL public của backend

`.env` (URL này **nướng vào bản build FE** → đổi là phải build lại):

```
PUBLIC_API_URL=https://api.example.com
PUBLIC_SOCKET_URL=https://ws.example.com
APP_CORS_ALLOWED_ORIGINS=https://app.example.com
```

Rồi `docker compose up -d --build` (hoặc `npm run build` nếu chạy FE ngoài Docker).
`IMAGE_BASE` tự bám theo `PUBLIC_API_URL` nên ảnh upload cũng chạy đúng.

### Bắt buộc / lưu ý

- **Toàn HTTPS**: FE qua Cloudflare là `https://` → `PUBLIC_API_URL` / `PUBLIC_SOCKET_URL`
  cũng phải `https://`, không được `http://` (mixed content bị chặn).
- **CORS**: request giờ là cross-origin (`app.` → `api.`). `APP_CORS_ALLOWED_ORIGINS`
  mặc định `*` là chạy được; đặt `https://app.example.com` để khoá chặt. Socket.IO
  (netty-socketio) tự phản chiếu Origin nên không cần cấu hình thêm.
- **WebSocket** qua Cloudflare Tunnel hoạt động bình thường (socket.io cũng tự fallback
  long-polling nếu cần).
- App xác thực bằng header `Authorization`, **không dùng cookie** → không dính vấn đề
  SameSite / domain khi qua tunnel.
- Nếu chạy FE bằng `npm run dev` (Vite :5173) sau tunnel: thêm
  `server.allowedHosts: ['app.example.com']` trong `forum_fe/vite.config.js`, hoặc dùng
  bản build production (nginx trong Docker) cho gọn.
- Nếu dùng nginx trong Docker làm FE: `forum_fe/nginx.conf` có `server_name` ghim domain
  cũ → đổi thành `_` hoặc hostname Cloudflare của bạn.
- Có thể gộp còn **1 hostname** bằng cách cho nginx (FE) reverse-proxy `/api` và
  `/socket.io` về `localhost:8081/8082` → khi đó FE và API cùng origin, **khỏi CORS**.
  Nhưng phải viết lại block `location /api` trong `nginx.conf` (bản hiện tại trỏ vào trang
  tài liệu, không phải API thật) + thêm header `Upgrade`/`Connection` cho socket.

## Chuyển dữ liệu sang hạ tầng mới

**Máy cũ** — xuất database ra 1 file:

```bash
./scripts/db-export.sh                 # -> backup/socialapp-YYYYmmdd-HHMMSS.sql
# hoặc chỉ định tên:  ./scripts/db-export.sh backup/prod.sql
```

Kèm theo, sao lưu ảnh người dùng đã upload (volume `uploads`):

```bash
docker run --rm -v deploy_docker_uploads:/data -v "$PWD/backup":/out alpine \
  tar czf /out/uploads.tar.gz -C /data .
```

**Máy mới** — chép 2 file `*.sql` và `uploads.tar.gz` sang, rồi:

```bash
docker compose up -d --build db          # chạy riêng db trước
./scripts/db-import.sh backup/prod.sql    # nạp dữ liệu

docker run --rm -v deploy_docker_uploads:/data -v "$PWD/backup":/in alpine \
  sh -c "cd /data && tar xzf /in/uploads.tar.gz"

docker compose up -d --build             # chạy nốt backend + frontend
```

> Tên volume mặc định là `<tên-thư-mục>_uploads` / `<tên-thư-mục>_mysql-data`
> (xem `docker volume ls`). Thư mục repo tên `Deploy_Docker` → `deploy_docker_uploads`.

## Thay đổi schema DB (Flyway)

- Schema được version hoá trong `social/src/main/resources/db/migration/`.
- Thêm cột/bảng: tạo `V<n>__mô_tả.sql` (n tăng dần) với câu `ALTER TABLE ...`, cập nhật
  entity JPA tương ứng, `./mvnw test`, rồi deploy — Flyway apply lúc backend khởi động,
  Hibernate `validate` xác nhận khớp.
- **Không sửa** file migration đã apply. Không dùng lại `ddl-auto=update`.
- Xem log Flyway lúc khởi động: `docker compose logs backend | grep -i flyway`.
- Quay lui khẩn cấp: đặt `DDL_AUTO=update` và `FLYWAY_ENABLED=false` trong `.env` rồi
  `docker compose up -d backend`.

## Lưu ý

- Secrets (JWT key, mật khẩu DB, SendGrid) để trong `.env` — **không commit `.env`**.
- **`JWT_SECRET` bắt buộc có trong `.env`** — không còn giá trị mặc định, thiếu là
  `docker compose` báo lỗi ngay. Tạo:  `openssl rand -base64 32`. Đổi khoá này = mọi user
  đang đăng nhập bị đá ra, phải login lại một lần.
- `APP_CORS_ALLOWED_ORIGINS` mặc định `*` (mọi origin gọi được API). Deploy public nên
  đặt cụ thể danh sách origin FE, cách nhau dấu phẩy.
- `social/compose.yml`, `social/docker-compose.yml` là bản cũ, bỏ qua — chỉ dùng
  `docker-compose.yml` ở gốc.
