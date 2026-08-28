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
import java.util.Date;

@Component
public class JwtUtils {
    private static Logger logger = LoggerFactory.getLogger(JwtUtils.class);
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

    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_REFRESH = "refresh";

    // Mã hóa data, email thành accessToken dùng để xác thực người dùng
    public String generateAccessToken(String data){
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpirationMs);
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey));
        return Jwts.builder().signWith(key).subject(data).issuedAt(now).expiration(exp).compact();
    }

    // Refresh token: sống lâu hơn, mang claim typ=refresh để phân biệt với access token.
    public String generateRefreshToken(String data){
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshExpirationMs);
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey));
        return Jwts.builder().signWith(key).subject(data).claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(now).expiration(exp).compact();
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
