package com.didan.social.utils;

import com.didan.social.entity.BlacklistToken;
import com.didan.social.repository.BlacklistRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtils {
    private static Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    // Băm token trước khi lưu DB (token vốn ngẫu nhiên entropy cao -> SHA-256 là đủ, không cần salt/bcrypt).
    public static String sha256Hex(String s) {
        if (s == null) return null;
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : d) sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }

    private final BlacklistRepository blacklistRepository;
    @Autowired
    public JwtUtils(BlacklistRepository blacklistRepository){
        this.blacklistRepository = blacklistRepository;
    }
    @Value("${jwt.secretkey}")
    private String secretKey;
    // Có thể chỉnh qua application.properties / biến môi trường.
    @Value("${jwt.access-expiration-ms:86400000}")   // mặc định 1 ngày
    private long accessExpirationMs;
    @Value("${jwt.refresh-expiration-ms:2592000000}") // mặc định 30 ngày
    private long refreshExpirationMs;

    /** Hạn của refresh token dài nhất — dùng để dọn phiên đã quá hạn. */
    public long getRefreshExpirationMs() { return refreshExpirationMs; }
    // Phiên KHÔNG "ghi nhớ đăng nhập": token ngắn -> refresh token (30') trượt theo hoạt động,
    // ngồi im quá 30' là hết phiên. access token (15') buộc gọi /auth/refresh để gia hạn.
    @Value("${jwt.short-access-expiration-ms:900000}")   // 15 phút
    private long shortAccessExpirationMs;
    @Value("${jwt.short-refresh-expiration-ms:1800000}") // 30 phút
    private long shortRefreshExpirationMs;

    /**
     * Định danh ngẫu nhiên cho từng token.
     *
     * Không có nó thì JWT là TẤT ĐỊNH từ (subject, remember, iat, exp), mà iat/exp
     * chỉ tính theo GIÂY — hai lần đăng nhập trong cùng một giây sinh ra token
     * giống hệt nhau. Hậu quả thật đã gặp: hai thiết bị dùng chung một refresh
     * token, và khi xoay vòng thì đụng khoá duy nhất
     * uk_user_sessions_refresh nên /auth/refresh hỏng.
     */
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CLAIM_JTI = "jti";

    private static String newJti() {
        byte[] b = new byte[9];   // 72 bit, đủ duy nhất mà token không dài thêm nhiều
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_REFRESH = "refresh";
    private static final String CLAIM_REMEMBER = "rmb";

    // Mã hóa data, email thành accessToken dùng để xác thực người dùng
    public String generateAccessToken(String data){
        return generateAccessToken(data, true);
    }

    public String generateAccessToken(String data, boolean remember){
        Date now = new Date();
        Date exp = new Date(now.getTime() + (remember ? accessExpirationMs : shortAccessExpirationMs));
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey));
        return Jwts.builder().signWith(key).subject(data).claim(CLAIM_JTI, newJti())
                .issuedAt(now).expiration(exp).compact();
    }

    // Refresh token: sống lâu hơn, mang claim typ=refresh để phân biệt với access token.
    public String generateRefreshToken(String data){
        return generateRefreshToken(data, true);
    }

    public String generateRefreshToken(String data, boolean remember){
        Date now = new Date();
        Date exp = new Date(now.getTime() + (remember ? refreshExpirationMs : shortRefreshExpirationMs));
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey));
        return Jwts.builder().signWith(key).subject(data).claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_REMEMBER, remember).claim(CLAIM_JTI, newJti())
                .issuedAt(now).expiration(exp).compact();
    }

    // Đọc cờ "ghi nhớ" từ refresh token (token cũ không có claim -> coi như true, giữ hành vi cũ).
    public boolean isRememberRefreshToken(String refreshToken){
        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey));
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(refreshToken).getPayload();
            Boolean b = c.get(CLAIM_REMEMBER, Boolean.class);
            return b == null || b;
        } catch (Exception e) {
            return true;
        }
    }

    // Xác thực refresh token: chữ ký hợp lệ, chưa hết hạn, đúng loại "refresh", không nằm trong blacklist.
    public void validateRefreshToken(String refreshToken) throws Exception {
        try {
            if (blacklistRepository.findFirstByToken(refreshToken) != null) {
                throw new Exception("Refresh token đã bị thu hồi");
            }
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(refreshToken).getPayload();
            if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new Exception("Không phải refresh token");
            }
        } catch (ExpiredJwtException e) {
            throw new Exception("Refresh token hết hạn");
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            throw new Exception("Refresh token không hợp lệ");
        }
    }

    // Lấy token từ header
    public String getTokenFromHeader(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization"); // Lấy token từ header
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){ // Kiểm tra bearerToken khác null và có bắt đầu bằng Bearer
            return bearerToken.substring(7); // Trả về token
        } else return null;
    }

    // Giải mã accessToken để lấy userId
    public String getUserIdFromAccessToken(String accessToken){
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey)); // Giải mã secretKey
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(accessToken).getPayload(); // Giải mã accessToken để lấy payload (data) trong accessToken 
        return claims.getSubject().toString(); // Lấy key subject trong payload để trả về email
    }

    // Xác thực accessToken
    public void validateAccessToken(String accessToken) throws Exception {
        try{
            BlacklistToken blacklistToken = blacklistRepository.findFirstByToken(accessToken);
            if(blacklistToken != null) {
                logger.error("Invalid access token");
                throw new Exception("Invalid access token"); // Nếu token ở trong blacklist thì không xác thực
            }
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(accessToken).getPayload();
            if (TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) { // Không cho dùng refresh token thay access token
                logger.error("Refresh token used as access token");
                throw new Exception("Invalid access token");
            }
        }catch(MalformedJwtException e){ // Nếu access token không hợp lệ thì bắn lỗi
            logger.error("Invalid access token");
            throw new Exception("Invalid access token");
        }catch(ExpiredJwtException e) { // Nếu access token hết hạn thì bắn lỗi
            logger.error("Expired access token");
            throw new Exception("Expired access token");
        }catch(UnsupportedJwtException e){ // Nếu access token không được hỗ trợ thì bắn lỗi
            logger.error("Unsupported access token");
            throw new Exception("Unsupported access token");
        }catch(IllegalArgumentException e){ // Nếu không có thông tin trong access token thì bắn lỗi
            logger.error("Empty access token");
            throw new Exception("Empty access token");
        }
    }
}
