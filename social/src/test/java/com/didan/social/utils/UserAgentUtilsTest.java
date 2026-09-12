package com.didan.social.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Nhãn thiết bị cho màn hình "Thiết bị đang đăng nhập".
 *
 * Bẫy chính là thứ tự so khớp: Edge/Opera/Cốc Cốc đều tự nhận là Chrome, Chrome
 * tự nhận là Safari, còn UA của Android có luôn cả chữ "Linux".
 */
class UserAgentUtilsTest {

    @Test
    void chromeTrenWindows() {
        assertEquals("Chrome trên Windows", UserAgentUtils.label(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"));
    }

    @Test
    void edgeKhongBiDocNhamThanhChrome() {
        assertEquals("Edge trên Windows", UserAgentUtils.label(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Edg/128.0.0.0"));
    }

    @Test
    void cocCocKhongBiDocNhamThanhChrome() {
        assertEquals("Cốc Cốc trên Windows", UserAgentUtils.label(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) coc_coc_browser/120.0.0 Chrome/114.0.0.0 Safari/537.36"));
    }

    @Test
    void androidKhongBiDocNhamThanhLinux() {
        assertEquals("Chrome trên Android", UserAgentUtils.label(
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"));
    }

    @Test
    void safariTrenIPhone() {
        assertEquals("Safari trên iPhone", UserAgentUtils.label(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 "
                        + "(KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"));
    }

    @Test
    void firefoxTrenLinux() {
        assertEquals("Firefox trên Linux", UserAgentUtils.label(
                "Mozilla/5.0 (X11; Linux x86_64; rv:130.0) Gecko/20100101 Firefox/130.0"));
    }

    @Test
    void rongHoacKhongDocDuocThiTraVeKhongRo() {
        assertEquals(UserAgentUtils.UNKNOWN, UserAgentUtils.label(null));
        assertEquals(UserAgentUtils.UNKNOWN, UserAgentUtils.label(""));
        assertEquals(UserAgentUtils.UNKNOWN, UserAgentUtils.label("curl/8.4.0"));
    }

    @Test
    void chiDoanDuocMotVeThiHienMotVe() {
        assertEquals("Windows", UserAgentUtils.label("Windows NT 10.0"));
        assertEquals("Firefox", UserAgentUtils.label("Firefox/130.0"));
    }
}
