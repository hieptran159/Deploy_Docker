package com.didan.social.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Mã hoá nội dung tin nhắn "at rest" (AES-256-GCM) trước khi lưu DB.
 *
 * - Khoá lấy từ `app.message.crypto-key` (base64 32 byte). Bỏ trống -> KHÔNG mã hoá
 *   (lưu plaintext như cũ), để tương thích deployment hiện có.
 * - Bản mã có tiền tố "enc1:" nên `decrypt` phân biệt được dòng đã mã hoá và dòng
 *   plaintext cũ (không cần migrate ồ ạt).
 * - Mọi lỗi giải mã -> trả lại chuỗi gốc + ghi log, không làm hỏng luồng đọc tin nhắn.
 *
 * KHÔNG phải E2EE: server vẫn thấy plaintext trong bộ nhớ (để gửi thông báo, parse @mention,
 * hiện preview tin nhắn cuối). Mục tiêu là chống lộ khi dump database.
 */
@Component
public class MessageCrypto {
    private static final Logger log = LoggerFactory.getLogger(MessageCrypto.class);
    private static final String PREFIX = "enc1:";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private final SecureRandom random = new SecureRandom();

    private final SecretKeySpec key;

    public MessageCrypto(@Value("${app.message.crypto-key:}") String base64Key) {
        SecretKeySpec k = null;
        if (base64Key != null && !base64Key.isBlank()) {
            try {
                byte[] raw = Base64.getDecoder().decode(base64Key.trim());
                if (raw.length != 16 && raw.length != 24 && raw.length != 32) {
                    log.error("app.message.crypto-key phải là base64 của 16/24/32 byte -> BỎ QUA, tin nhắn lưu plaintext");
                } else {
                    k = new SecretKeySpec(raw, "AES");
                    log.info("Mã hoá tin nhắn: BẬT (AES-{}-GCM)", raw.length * 8);
                }
            } catch (Exception e) {
                log.error("app.message.crypto-key không hợp lệ ({}) -> tin nhắn lưu plaintext", e.getMessage());
            }
        }
        this.key = k;
    }

    /** Mã hoá 1 chuỗi để lưu DB. Không có khoá / chuỗi rỗng -> trả nguyên. */
    public String encrypt(String plain) {
        if (key == null || plain == null || plain.isEmpty()) return plain;
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            log.error("encrypt tin nhắn lỗi: {} -> lưu plaintext", e.getMessage());
            return plain;
        }
    }

    /** Giải mã chuỗi đọc từ DB. Dòng plaintext cũ (không có tiền tố) -> trả nguyên. */
    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) return stored;
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] pt = c.doFinal(all, IV_LEN, all.length - IV_LEN);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("decrypt tin nhắn lỗi: {}", e.getMessage());
            return stored;
        }
    }
}
