package com.didan.social.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache đọc bằng Redis.
 *
 * Redis ở đây KHÔNG giữ trạng thái nào cả — chỉ là bản sao tạm của vài truy vấn
 * đọc bị lặp lại nhiều nhất. Mất sạch Redis thì ứng dụng chạy y như trước khi có
 * nó, chỉ chậm hơn một chút. Đó là điều kiện để dám thêm một dịch vụ nữa vào
 * chuỗi phụ thuộc: nó không được phép làm sập cái gì khi chết.
 *
 * Hai hệ quả của nguyên tắc đó, đều cố ý:
 * - {@link #errorHandler()} NUỐT mọi lỗi Redis (kể cả timeout) và để lời gọi đi
 *   thẳng xuống DB, thay vì ném 503 lên người dùng.
 * - Không cache thứ gì mà đọc phải bản cũ là SAI, chứ không chỉ là cũ. Cụ thể là
 *   blacklist token: cache một câu "token này chưa bị thu hồi" đồng nghĩa với việc
 *   token đã đăng xuất vẫn dùng được cho tới hết TTL. Tra bảng blacklist_token là
 *   tra khoá chính, ~1ms — không đáng đánh đổi.
 *
 * Mặc định cache TẮT (spring.cache.type=none) để chạy test/dev không cần Redis;
 * docker-compose đặt CACHE_TYPE=redis.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    // Tên cache cũng là tiền tố khoá trong Redis: "friendIds::u-1".
    public static final String FRIEND_IDS = "friendIds";
    public static final String BLOCKED_IDS = "blockedIds";
    public static final String BLOCKER_IDS = "blockerIds";
    public static final String DEACTIVATED_IDS = "deactivatedIds";
    public static final String TRENDING = "trending";
    public static final String FEED_COUNT = "feedCount";

    /**
     * TTL đặt theo "đọc bản cũ bao lâu thì còn chấp nhận được", không phải theo độ nặng
     * của truy vấn.
     *
     * friendIds/blocked/blocker có xoá cache tường minh ở đúng chỗ đổi quan hệ, nên TTL
     * chỉ là lưới an toàn cho trường hợp lệnh xoá thất bại (Redis lag/chết đúng lúc đó —
     * errorHandler nuốt lỗi nên hàng cũ có thể sống sót).
     *
     * deactivatedIds thì CỐ Ý chỉ dựa vào TTL ngắn: nó đổi ở ba nơi rời rạc (tự vô hiệu
     * hoá, đăng nhập lại, xác thực 2 bước), rải @CacheEvict ra cả ba là thêm ba chỗ để
     * quên. Trễ 60 giây rồi bài mới biến mất khỏi feed là chấp nhận được.
     */
    static final Map<String, Duration> TTL = new LinkedHashMap<>();
    static {
        TTL.put(FRIEND_IDS, Duration.ofMinutes(10));
        TTL.put(BLOCKED_IDS, Duration.ofMinutes(10));
        TTL.put(BLOCKER_IDS, Duration.ofMinutes(10));
        TTL.put(DEACTIVATED_IDS, Duration.ofSeconds(60));
        TTL.put(TRENDING, Duration.ofMinutes(5));
        TTL.put(FEED_COUNT, Duration.ofSeconds(60));
    }

    /** Cache nào quên khai TTL vẫn phải hết hạn, không được nằm lại vĩnh viễn. */
    static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    /**
     * Dùng bộ tuần tự hoá mặc định (JDK serialization), KHÔNG đổi sang JSON.
     * GenericJackson2JsonRedisSerializer đọc số nguyên nhỏ ra thành Integer, nên một
     * hàm trả `long` (feedCount) sẽ nổ ClassCastException khi trúng cache. Cache là
     * dữ liệu tạm, có TTL, không ai đọc bằng mắt — không cần JSON cho đẹp.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer cacheTtlPerName() {
        return builder -> {
            builder.cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(DEFAULT_TTL));
            TTL.forEach((name, ttl) -> builder.withCacheConfiguration(name,
                    RedisCacheConfiguration.defaultCacheConfig().entryTtl(ttl)));
        };
    }

    /** Redis hỏng = coi như trượt cache. Không được biến sự cố cache thành sự cố site. */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                warn("đọc", cache, e);
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                warn("ghi", cache, e);
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                // Xoá hụt -> hàng cũ sống tới hết TTL. Vì thế TTL nào cũng phải hữu hạn.
                warn("xoá", cache, e);
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                warn("xoá sạch", cache, e);
            }
        };
    }

    /** Redis chết thì mọi request đều lỗi cache -> chỉ log 1 dòng mỗi phút, đừng ngập log. */
    private final AtomicLong lastLogAt = new AtomicLong();

    private void warn(String op, Cache cache, RuntimeException e) {
        long now = System.currentTimeMillis();
        long prev = lastLogAt.get();
        if (now - prev > 60_000L && lastLogAt.compareAndSet(prev, now)) {
            logger.warn("Cache '{}' {} lỗi -> bỏ qua cache, đọc thẳng DB: {}",
                    cache != null ? cache.getName() : "?", op, e.toString());
        }
    }
}
