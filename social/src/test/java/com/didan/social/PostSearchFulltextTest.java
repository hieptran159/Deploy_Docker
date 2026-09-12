package com.didan.social;

import com.didan.social.utils.SearchQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hành vi FULLTEXT của MySQL với TIẾNG VIỆT — chạy trên MySQL thật, vì đây là thứ
 * KHÔNG suy luận được.
 *
 * Câu hỏi cần trả lời: có chuyển thẳng tìm kiếm sang FULLTEXT được không?
 * Nếu innodb_ft_min_token_size = 3 thì mọi âm tiết 2 chữ ("bò", "ăn", "gì") nằm
 * ngoài chỉ mục, và chuyển thẳng là lặng lẽ mất kết quả. SearchQuery tồn tại để
 * né chuyện đó; test này chứng minh mối nguy là có thật chứ không phải mình tưởng.
 *
 * Cần Docker (như SocialApplicationTests). Chạy ở CI; máy dev không có Docker thì
 * loại bằng -Dtest='!SocialApplicationTests,!PostSearchFulltextTest'.
 */
@SpringBootTest
@Testcontainers
class PostSearchFulltextTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    /**
     * Sinh khoá tại chỗ thay vì viết sẵn chuỗi base64 — chuỗi đó trông y hệt một
     * secret thật và bị máy quét bí mật chặn PR (đúng việc của nó).
     * jwt.secretkey cần base64 của ít nhất 32 byte.
     */
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("jwt.secretkey", () -> Base64.getEncoder()
                .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        // cổng 0 -> OS tự chọn cổng rảnh, tránh đụng :8082 khi chạy CI
        registry.add("socket-server.port", () -> 0);
    }

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM user_posts");
        jdbc.update("DELETE FROM posts");
        insert("p-pho", "Quán phở ngon nhất Hà Nội", "Bài về phở và bún bò Huế.");
        insert("p-brics", "Thủ tướng dự hội nghị BRICS", "Nội dung về hợp tác quốc tế.");
        insert("p-body", "Chuyện bên lề", "Ở đây có nhắc tới BRICS một lần trong thân bài.");
    }

    private void insert(String id, String title, String body) {
        jdbc.update("INSERT INTO posts (post_id, title, body, posted_at, status, visibility, views) "
                + "VALUES (?, ?, ?, NOW(), 'published', 'public', 0)", id, title, body);
    }

    private int minTokenSize() {
        Integer v = jdbc.queryForObject(
                "SELECT @@innodb_ft_min_token_size", Integer.class);
        return v == null ? 3 : v;
    }

    private List<String> ftSearch(String raw) {
        return jdbc.queryForList(
                "SELECT post_id FROM posts WHERE MATCH(title, body) AGAINST (? IN BOOLEAN MODE) "
                        + "ORDER BY MATCH(title, body) AGAINST (? IN BOOLEAN MODE) DESC, post_id",
                String.class, SearchQuery.booleanExpression(raw), SearchQuery.booleanExpression(raw));
    }

    private List<String> likeSearch(String raw) {
        String p = "%" + raw.toLowerCase() + "%";
        return jdbc.queryForList(
                "SELECT post_id FROM posts WHERE LOWER(title) LIKE ? OR LOWER(body) LIKE ? ORDER BY post_id",
                String.class, p, p);
    }

    /** Cả hai chỉ mục của V12 phải tồn tại, không thì MATCH ... AGAINST sẽ ném lỗi. */
    @Test
    void haiChiMucFulltextDaDuocTao() {
        for (String idx : List.of("ft_posts_title_body", "ft_posts_title")) {
            Integer n = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.statistics "
                            + "WHERE table_schema = DATABASE() AND table_name = 'posts' "
                            + "AND index_name = ?", Integer.class, idx);
            assertNotNull(n);
            assertTrue(n > 0, "thiếu chỉ mục " + idx);
        }
    }

    @Test
    void fulltextTimDuocTuDuDai() {
        List<String> ids = ftSearch("BRICS");
        assertTrue(ids.contains("p-brics"), "phải tìm được bài có BRICS ở tiêu đề");
        assertTrue(ids.contains("p-body"), "phải tìm được bài có BRICS trong thân bài");
    }

    /**
     * Ghi lại một sự thật đã ĐO chứ không đoán: chỉ mục GỘP không hề ưu tiên tiêu đề.
     *
     * Lần đầu viết, mình đã tưởng MATCH(title, body) tự biết title quan trọng hơn.
     * CI bác lại: bài chỉ nhắc BRICS một lần trong thân bài xếp TRÊN bài có BRICS ở
     * tiêu đề, vì văn bản ngắn hơn nên tf-idf cao hơn. Đó là lý do V12 phải tạo
     * thêm chỉ mục riêng cho title.
     */
    @Test
    void chiMucGopKHONGuuTienTieuDe() {
        List<String> ids = jdbc.queryForList(
                "SELECT post_id FROM posts WHERE MATCH(title, body) AGAINST (? IN BOOLEAN MODE) "
                        + "ORDER BY MATCH(title, body) AGAINST (? IN BOOLEAN MODE) DESC, post_id",
                String.class, "+BRICS*", "+BRICS*");
        assertEquals("p-body", ids.get(0),
                "nếu chỗ này đổi thành p-brics thì MySQL đã tự ưu tiên tiêu đề, và "
                        + "chỉ mục ft_posts_title là thừa — gỡ nó đi");
    }

    /** Xếp hạng THẬT mà ứng dụng dùng: điểm tiêu đề trước, rồi điểm chung. */
    @Test
    void xepHangCuaUngDungDuaTieuDeLenTruoc() {
        List<String> ids = jdbc.queryForList(
                "SELECT post_id FROM posts WHERE MATCH(title, body) AGAINST (? IN BOOLEAN MODE) "
                        + "ORDER BY MATCH(title) AGAINST (? IN BOOLEAN MODE) DESC, "
                        + "MATCH(title, body) AGAINST (? IN BOOLEAN MODE) DESC, post_id",
                String.class, "+BRICS*", "+BRICS*", "+BRICS*");
        assertEquals("p-brics", ids.get(0),
                "khớp ở tiêu đề phải lên đầu — đây là thứ LIKE cũ (sắp theo ngày) không làm được");
        assertTrue(ids.contains("p-body"), "bài khớp ở thân bài vẫn phải nằm trong kết quả");
    }

    /**
     * Điểm mấu chốt của cả thay đổi này. Nếu test này fail (tức FULLTEXT vẫn tìm
     * được "bò") thì SearchQuery là thừa và nên gỡ.
     */
    @Test
    void fulltextKHONGtimDuocAmTietHaiChuCuaTiengViet() {
        int min = minTokenSize();
        assertEquals(3, min, "giả định của SearchQuery dựa trên ngưỡng này");

        assertTrue(likeSearch("bò").contains("p-pho"), "LIKE vẫn tìm được 'bò'");
        assertTrue(ftSearch("bò").isEmpty(),
                "FULLTEXT bỏ qua token ngắn hơn " + min + " ký tự — đây chính là lý do phải rơi về LIKE");

        // Và SearchQuery phải nhận ra điều đó để không đi đường FULLTEXT
        assertFalse(SearchQuery.canUseFulltext("bò", min));
        assertFalse(SearchQuery.canUseFulltext("bún bò", min));
        assertTrue(SearchQuery.canUseFulltext("BRICS", min));
    }

    @Test
    void nhieuTuLaGiaoChuKhongPhaiHop() {
        // "+a* +b*" = phải có CẢ HAI, giống ngữ nghĩa LIKE cũ
        assertTrue(ftSearch("BRICS quốc").contains("p-brics"));
        assertFalse(ftSearch("BRICS phở").contains("p-brics"),
                "không bài nào chứa cả BRICS lẫn phở -> không được trả về");
    }
}
