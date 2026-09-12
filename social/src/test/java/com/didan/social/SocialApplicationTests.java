package com.didan.social;

import com.didan.social.config.CacheConfig;
import com.didan.social.repository.BlockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        "socket-server.port=0",
        // Bật cache bằng bản trong RAM: đủ để kiểm tra @Cacheable có ăn hay không mà
        // không phải dựng thêm một container Redis.
        "spring.cache.type=simple"
})
@Testcontainers
class SocialApplicationTests {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    CacheManager cacheManager;

    @Autowired
    BlockRepository blockRepository;

    @Test
    void contextLoads() {
    }

    /**
     * Annotation Cacheable đặt trên phương thức của repository Spring Data chỉ ăn nếu bean
     * repository được bọc thêm một lớp proxy cache. Không có gì báo lỗi khi nó KHÔNG ăn:
     * annotation nằm im, truy vấn vẫn chạy, mọi test khác vẫn xanh. Nên phải kiểm tra
     * bằng cách nhìn thẳng vào cache.
     */
    @Test
    void cacheAnTrenPhuongThucCuaRepository() {
        String ai = "khong-ton-tai";
        blockRepository.blockedIdsOf(ai);

        Cache cache = cacheManager.getCache(CacheConfig.BLOCKED_IDS);
        assertNotNull(cache, "thiếu cache " + CacheConfig.BLOCKED_IDS);
        assertNotNull(cache.get(ai), "@Cacheable trên BlockRepository.blockedIdsOf không có tác dụng");
    }

}
