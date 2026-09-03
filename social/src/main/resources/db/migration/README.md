# Flyway migrations

Thư mục này chứa các file migration schema DB. Flyway chạy tự động lúc backend khởi động
(khi `FLYWAY_ENABLED=true`), theo đúng thứ tự version.

## Quy ước tên file

```
V<version>__<mô tả>.sql      ví dụ: V2__them_cot_pinned_vao_posts.sql
```

- `V1__baseline.sql` = ảnh chụp toàn bộ schema **hiện có** trên prod (chỉ CREATE TABLE,
  không có dữ liệu). Sinh bằng `mysqldump --no-data` từ DB đang chạy.
- Dùng `_` thay khoảng trắng, không dấu tiếng Việt trong tên file.
- **Không bao giờ sửa** một file migration đã được apply — luôn thêm file version mới.

## Cách hoạt động

| Tình huống | Flyway làm gì |
|---|---|
| DB prod đang chạy (có bảng, chưa có `flyway_schema_history`) | `baseline-on-migrate=true` → tạo bảng lịch sử, ghi nhận đã ở V1, **bỏ qua V1**, chỉ chạy V2+ |
| Deploy mới (DB rỗng) | Chạy tuần tự từ V1 |

`spring.jpa.hibernate.ddl-auto` nên để `validate` khi Flyway bật: Hibernate chỉ kiểm tra
entity khớp bảng, không tự `ALTER`.

## Thêm thay đổi schema mới

1. Tạo `V<n>__<mô tả>.sql` ở đây với câu lệnh DDL (`ALTER TABLE ...`).
2. Cập nhật entity JPA tương ứng.
3. `./mvnw test` (không cần DB) rồi deploy — Flyway apply V<n>, `validate` xác nhận khớp.
