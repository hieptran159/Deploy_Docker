package com.didan.social.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTemplateTest {

    @Test
    void otpContainsCodeAndBrand() {
        String html = EmailTemplate.otp("Xác thực email", "Nhập mã bên dưới", "482913", "Mã hết hạn sau 10 phút");
        assertTrue(html.startsWith("<!doctype html"));
        assertTrue(html.contains("482913"));
        assertTrue(html.contains("HIPDN-EA Forum"));
        assertTrue(html.contains("Xác thực email"));
        assertTrue(html.contains("Mã hết hạn sau 10 phút"));
    }

    @Test
    void otpEscapesHtmlInInputs() {
        String html = EmailTemplate.otp("<script>alert(1)</script>", "a & b < c", "000000", "n");
        assertFalse(html.contains("<script>alert(1)</script>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("a &amp; b &lt; c"));
    }

    @Test
    void otpHandlesNullNote() {
        assertDoesNotThrow(() -> EmailTemplate.otp("h", "i", "123456", null));
    }
}
