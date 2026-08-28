package com.didan.social.utils;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HashtagUtilsTest {

    @Test
    void extractsLowercasesAndDedups() {
        Set<String> tags = HashtagUtils.extract("Chào #Vue và #vue", "học #Spring #spring-boot");
        assertTrue(tags.contains("vue"));
        assertTrue(tags.contains("spring"));
        assertEquals(2, tags.size()); // #vue/#Vue gộp 1, "#spring-boot" chỉ lấy "spring"
    }

    @Test
    void supportsUnicodeAndUnderscore() {
        Set<String> tags = HashtagUtils.extract("#hà_nội #chủ_đề", null);
        assertTrue(tags.contains("hà_nội"));
        assertTrue(tags.contains("chủ_đề"));
    }

    @Test
    void skipsAllDigitTag() {
        assertTrue(HashtagUtils.extract("giá #2024 #sale2024", "").contains("sale2024"));
        assertFalse(HashtagUtils.extract("giá #2024", "").contains("2024"));
    }

    @Test
    void capsAtTwentyTags() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) sb.append(" #tag").append(i);
        assertEquals(20, HashtagUtils.extract(sb.toString()).size());
    }

    @Test
    void normalizeStripsHashAndValidates() {
        assertEquals("vue", HashtagUtils.normalize("#Vue"));
        assertEquals("vue", HashtagUtils.normalize("  vue "));
        assertNull(HashtagUtils.normalize("#has space"));
        assertNull(HashtagUtils.normalize(""));
        assertNull(HashtagUtils.normalize("123"));
        assertNull(HashtagUtils.normalize(null));
    }
}
