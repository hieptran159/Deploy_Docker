package com.didan.social;

import com.didan.social.config.CacheConfig;
import com.didan.social.repository.BlockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
@AutoConfigureMockMvc
@Testcontainers
class SocialApplicationTests {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    BlockRepository blockRepository;

    @Autowired
    com.didan.social.repository.PostRepository postRepository;

    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void contextLoads() {
    }

    /**
     * Bảng tin "Nổi bật" là truy vấn native (POW/TIMESTAMPDIFF, hai truy vấn con đếm
     * thích/bình luận) — không có gì kiểm tra nó lúc biên dịch: sai tên cột hay sai cú pháp
     * MySQL thì phải tới lúc có người mở tab đó trên production mới lộ.
     *
     * Phiên bản đầu của test này chạy trên DB RỖNG và vì thế đã cho lọt một lỗi 503 ra
     * production: câu lệnh chạy được, nhưng không có dòng nào để POW phải tính. Bài học:
     * với một biểu thức số học thì "truy vấn chạy được" và "truy vấn tính được" là hai
     * chuyện khác nhau — phải có ít nhất một dòng thật.
     *
     * Dòng đó cố ý để `posted_at` Ở TƯƠNG LAI, vì đó đúng là tình trạng dữ liệu trên
     * production: posted_at ghi theo giờ VN rồi đọc lại ở máy chạy UTC nên luôn sớm hơn
     * NOW() 7 tiếng. Không kẹp GREATEST thì POW nhận cơ số âm và MySQL ném
     * "DOUBLE value is out of range".
     */
    @Test
    void truyVanBangTinNoiBatChayDuocVoiBaiCoMocThoiGianOTuongLai() {
        jdbc.update("INSERT IGNORE INTO users (user_id, full_name, email, password, "
                + "profile_avatar, date_of_birth, is_admin) VALUES (?,?,?,?,?,?,?)",
                "u-hot-test", "Người kiểm thử", "hot-test@example.com", "x", "", "1990-01-01", 0);
        jdbc.update("INSERT IGNORE INTO posts (post_id, title, body, posted_at, status) "
                + "VALUES (?,?,?, NOW() + INTERVAL 7 HOUR, 'published')",
                "p-hot-test", "Bài mốc thời gian tương lai", "thân bài");
        jdbc.update("INSERT IGNORE INTO user_posts (post_id, user_id) VALUES (?,?)",
                "p-hot-test", "u-hot-test");

        java.util.List<String> khongLoaiAi = java.util.List.of("-");
        java.util.List<String> khongCoBanBe = java.util.List.of("-");

        java.util.List<Object[]> rows = postRepository.hotFeedPage(khongLoaiAi, khongCoBanBe, "-", 10, 0);

        assertTrue(rows.stream().anyMatch(r -> "p-hot-test".equals(r[0])),
                "bài có posted_at ở tương lai phải nằm trong bảng tin Nổi bật, không được làm vỡ truy vấn");
        assertTrue(postRepository.hotFeedCount(khongLoaiAi, khongCoBanBe, "-") >= 1,
                "đếm bài Nổi bật phải thấy bài vừa chèn");
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

    /**
     * Ảnh upload được khai là bất biến (tên file mang dấu thời gian, đổi ảnh = đổi URL).
     * Kiểm tra header thật sự đi ra chứ không chỉ là đã gọi đúng builder — và giữ nó khỏi
     * bị một `setCachePeriod` nào đó ghi đè về sau.
     */
    @Test
    void anhUploadPhaiCoHeaderCacheBatBien() throws Exception {
        String h = mockMvc.perform(get("/images/avatar/1706554952940-avatar.png"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("Cache-Control");

        assertNotNull(h, "thiếu hẳn Cache-Control");
        assertTrue(h.contains("immutable"), "phải có immutable, đang là: " + h);
        assertTrue(h.contains("public"), "phải là public để CDN cũng cache được, đang là: " + h);
        assertTrue(h.contains("max-age=31536000"), "phải là 1 năm, đang là: " + h);
    }
}
