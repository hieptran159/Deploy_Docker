package com.didan.social.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Điều kiện để dám thêm Redis vào chuỗi phụ thuộc: Redis chết KHÔNG được làm hỏng site.
 *
 * Mặc định của Spring thì ngược lại — lỗi cache ném thẳng ra ngoài, tức là Redis sập
 * đúng lúc thì mọi trang có @Cacheable trả 503, dù MySQL vẫn khoẻ. Bài test này giữ
 * đúng hai lời hứa của CacheConfig: nuốt lỗi, và mọi cache đều có hạn.
 */
class CacheConfigTest {

    private final CacheConfig config = new CacheConfig();

    private final Cache cache = new ConcurrentMapCache("friendIds");

    @Test
    void loiRedisKhiDOCThiBoQua_khongNemLenNguoiDung() {
        CacheErrorHandler h = config.errorHandler();
        assertDoesNotThrow(() -> h.handleCacheGetError(
                new RedisConnectionFailureException("Unable to connect to Redis"), cache, "u-1"));
    }

    @Test
    void loiRedisKhiGHI_XOA_XOASACH_cungKhongNem() {
        CacheErrorHandler h = config.errorHandler();
        RedisConnectionFailureException down = new RedisConnectionFailureException("Redis is down");
        assertDoesNotThrow(() -> h.handleCachePutError(down, cache, "u-1", List.of("u-2")));
        assertDoesNotThrow(() -> h.handleCacheEvictError(down, cache, "u-1"));
        assertDoesNotThrow(() -> h.handleCacheClearError(down, cache));
    }

    /**
     * Xoá cache có thể THẤT BẠI (chính là trường hợp trên: errorHandler nuốt lỗi xoá).
     * Khi đó hàng cũ chỉ còn TTL để tự chết. Một cache không TTL trong tình huống đó là
     * dữ liệu sai nằm lại vĩnh viễn — nên mọi tên cache khai báo đều phải có hạn.
     */
    @Test
    void moiTenCacheDeuPhaiKhaiTTL() throws Exception {
        List<String> thieu = new ArrayList<>();
        for (Field f : CacheConfig.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != String.class) continue;
            String name = (String) f.get(null);
            if (!CacheConfig.TTL.containsKey(name)) thieu.add(f.getName() + " (\"" + name + "\")");
        }
        assertTrue(thieu.isEmpty(), "Cache chưa khai TTL trong CacheConfig.TTL: " + thieu);
    }

    @Test
    void khongCacheNaoDuocSongQuaLau() {
        CacheConfig.TTL.forEach((name, ttl) -> {
            assertTrue(ttl.compareTo(Duration.ZERO) > 0, name + ": TTL phải > 0");
            assertTrue(ttl.compareTo(Duration.ofHours(1)) <= 0,
                    name + ": TTL " + ttl + " quá dài cho dữ liệu có thể xoá hụt");
        });
        assertTrue(CacheConfig.DEFAULT_TTL.compareTo(Duration.ZERO) > 0,
                "cache quên khai TTL vẫn phải hết hạn, không được nằm lại vĩnh viễn");
    }
}
