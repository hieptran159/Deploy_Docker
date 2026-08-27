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

Lần chạy đầu MySQL tự nạp `social/db.sql` (schema + dữ liệu mẫu). Bảng `notifications`
do backend tự tạo khi khởi động (`hibernate.ddl-auto=update`).

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
   ```

   (URL này được "nướng" vào bản build frontend nên phải build lại khi đổi.)
3. `docker compose up -d --build`

Đổi cổng publish: sửa `FRONTEND_PORT` / `BACKEND_PORT` / `SOCKET_PORT` / `DB_PORT` trong `.env`.

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

### Cách khác: thay luôn file seed

Muốn mọi lần `up` với volume trống đều dùng dữ liệu của bạn: ghi đè `social/db.sql`
bằng bản dump (`./scripts/db-export.sh social/db.sql`) rồi commit.

## Lưu ý

- Secrets (JWT key, mật khẩu DB, SendGrid) để trong `.env` — **không commit `.env`**.
  `docker-compose.yml` chỉ chứa giá trị mặc định cho môi trường dev.
- `social/compose.yml`, `social/docker-compose.yml` là bản cũ, bỏ qua — chỉ dùng
  `docker-compose.yml` ở gốc.
