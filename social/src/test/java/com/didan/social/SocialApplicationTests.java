package com.didan.social;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Nạp toàn bộ Spring context trên một MySQL tạm (Testcontainers) — chạy được ở bất kỳ
 * đâu có Docker (CI, máy dev), không cần dựng sẵn MySQL. Cũng đồng thời kiểm tra chuỗi
 * migration Flyway (V1..Vn) chạy sạch trên DB rỗng + Hibernate `validate` khớp entity.
 */
@SpringBootTest(properties = {
        // jwt.secretkey không còn giá trị mặc định (fail-fast) -> cấp khoá test: base64
        // của đúng 32 byte ("0123456789abcdef" x2).
        "jwt.secretkey=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        // netty-socketio: cổng 0 -> OS tự chọn cổng rảnh (tránh đụng :8082 khi chạy CI).
        "socket-server.port=0"
})
@Testcontainers
class SocialApplicationTests {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void contextLoads() {
    }

}
