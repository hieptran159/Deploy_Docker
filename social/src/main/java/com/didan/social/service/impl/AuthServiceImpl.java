package com.didan.social.service.impl;


import com.didan.social.entity.BlacklistToken;
import com.didan.social.entity.BlacklistUser;
import com.didan.social.entity.Users;
import com.didan.social.payload.request.SignupRequest;
import com.didan.social.repository.BlacklistRepository;
import com.didan.social.repository.BlacklistUserRepository;
import com.didan.social.repository.UserRepository;
import com.didan.social.repository.UserSessionRepository;
import com.didan.social.entity.UserSessions;
import com.didan.social.service.AuthService;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.FileUploadsService;
import com.didan.social.service.MailService;
import com.didan.social.service.SessionService;
import com.didan.social.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class AuthServiceImpl implements AuthService {
    private final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    // Hash bcrypt "mồi" để so sánh giả khi email không tồn tại -> thời gian phản hồi đồng đều
    // (chống dò email bằng timing). Không phải mật khẩu của ai.
    private static final String DUMMY_BCRYPT = "$2a$10$T/nNYi4V1d6OO0o7yXOG3uXTQznoQ/1pNEb19wCF8HBFqNceyxb1O";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BlacklistRepository blacklistRepository;
    private final BlacklistUserRepository blacklistUserRepository;
    private final FileUploadsService fileUploadsService;
    private final MailService mailService;
    private final AuthorizePathService authorizePathService;
    private final JwtUtils jwtUtils;
    private final UserSessionRepository userSessionRepository;
    private final SessionService sessionService;
    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           BlacklistRepository blacklistRepository,
                           FileUploadsService fileUploadsService,
                           JwtUtils jwtUtils,
                           MailService mailService,
                           AuthorizePathService authorizePathService,
                           BlacklistUserRepository blacklistUserRepository,
                           UserSessionRepository userSessionRepository,
                           SessionService sessionService
    ){
        this.userSessionRepository = userSessionRepository;
        this.sessionService = sessionService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorizePathService = authorizePathService;
        this.blacklistRepository = blacklistRepository;
        this.blacklistUserRepository = blacklistUserRepository;
        this.fileUploadsService = fileUploadsService;
        this.mailService = mailService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Mở một PHIÊN MỚI cho thiết bị vừa đăng nhập và gắn cặp token vào đối tượng
     * Users để controller trả về. KHÔNG đụng tới phiên của thiết bị khác — đây
     * chính là điểm khác so với bản cũ (một ô token dùng chung cho cả tài khoản).
     */
    private void openSession(Users user, boolean remember) {
        String[] pair = sessionService.openSession(user.getUserId(), remember);
        user.setAccessToken(pair[0]);
        user.setPlainRefreshToken(pair[1]);
    }

    @Override
    public Users login(String email, String password, boolean remember) throws Exception{
        Users user = userRepository.findFirstByEmail(email);
        if(user == null) {
            // So sánh bcrypt giả để thời gian phản hồi ~ bằng lúc email có thật (chống dò email bằng timing)
            passwordEncoder.matches(password == null ? "" : password, DUMMY_BCRYPT);
            logger.error("Login: email not found {}", email);
            throw new Exception("Email and Password does not match");
        }
        BlacklistUser blacklistUser = blacklistUserRepository.findByUserId(user.getUserId());
        if (blacklistUser != null && blacklistUser.isActiveBan()){
            logger.error("User is blocked");
            throw new Exception("User is blocked");
        }
        if (passwordEncoder.matches(password, user.getPassword())){
            if (user.getEmailVerified() != null && user.getEmailVerified() == 0) {
                logger.error("Email not verified");
                throw new Exception("Email chưa được xác thực. Vui lòng nhập mã đã gửi tới hộp thư của bạn.");
            }
            // Đăng nhập lại = tự kích hoạt tài khoản đã vô hiệu hoá tạm thời
            if (user.getDeactivated() != null && user.getDeactivated() == 1) {
                user.setDeactivated(0);
                logger.info("Reactivated account on login: {}", user.getUserId());
            }
            // Xác thực 2 bước: gửi mã qua email, CHƯA cấp token
            if (user.getTwofaEnabled() != null && user.getTwofaEnabled() == 1) {
                String code = randomCode();
                user.setTwofaCode(code);
                user.setTwofaExpires(Timestamp.valueOf(
                        LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusMinutes(10)));
                userRepository.save(user);
                logger.info("[2fa] {} code={}", user.getEmail(), code);
                try {
                    mailService.sendTextEmail(user.getEmail(), "Mã đăng nhập 2 bước",
                            com.didan.social.utils.EmailTemplate.otp(
                                    "Xác thực đăng nhập",
                                    "Nhập mã dưới đây để hoàn tất đăng nhập.",
                                    code,
                                    "Nếu không phải bạn đăng nhập, hãy đổi mật khẩu ngay."));
                } catch (Exception mailEx) {
                    logger.error("send 2fa email failed: " + mailEx.getMessage());
                }
                user.setTwofaRequired(true);
                return user;
            }
            userRepository.save(user);
            openSession(user, remember);
            return user;
        }
        else {
            logger.error("Email and Password does not match");
            throw new Exception("Email and Password does not match");
        }
    }
    /*
     * Sign Up, required avatar file
     */
    @Override
    public Users signup(SignupRequest signupRequest) throws Exception{
        Users user = userRepository.findFirstByEmail(signupRequest.getEmail());
        if(user != null) {
            logger.error("Email is existed");
            throw new Exception("Email is existed");
        }
        if(signupRequest.getPassword().length() < 8) {
            logger.error("The minimum password should be 8");
            throw new Exception("Mật khẩu phải có ít nhất 8 ký tự");
        }
        else{
            UUID id = UUID.randomUUID();
            Users userSave = new Users();
            userSave.setIsAdmin(0);
            if (signupRequest.getAvatar() != null && !signupRequest.getAvatar().isEmpty()) {
                String fileName = fileUploadsService.storeFile(signupRequest.getAvatar(), "avatar", id.toString());
                userSave.setAvtUrl("avatar/" + fileName);
            } else {
                userSave.setAvtUrl("");
            }
            userSave.setUserId(id.toString());
            userSave.setEmail(signupRequest.getEmail());
            // Làm sạch tên hiển thị (bỏ '<' '>' và ký tự điều khiển, cắt 100 ký tự)
            String fullName = signupRequest.getFullName() == null ? "" :
                    signupRequest.getFullName().trim().replaceAll("[<>\\p{Cntrl}]", "");
            if (fullName.length() > 100) fullName = fullName.substring(0, 100);
            userSave.setFullName(fullName);
            userSave.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
            String dateString = signupRequest.getBirthday();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date utilDate = format.parse(dateString);
            java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
            userSave.setDob(sqlDate);
            userSave.setAccessToken(jwtUtils.generateAccessToken(id.toString()));
            String code = randomCode();
            userSave.setEmailVerified(0);
            userSave.setVerifyCode(code);
            userRepository.save(userSave);
            logger.info("[verify] signup {} code={}", signupRequest.getEmail(), code); // tiện test khi email chưa gửi được
            try {
                mailService.sendTextEmail(signupRequest.getEmail(), "Xác thực email đăng ký",
                        com.didan.social.utils.EmailTemplate.otp(
                                "Chào mừng bạn đến với diễn đàn!",
                                "Cảm ơn bạn đã đăng ký. Nhập mã dưới đây vào trang xác thực để kích hoạt tài khoản.",
                                code,
                                "Nếu bạn không tạo tài khoản này, hãy bỏ qua email."));
            } catch (Exception mailEx) {
                logger.error("send verify email failed: " + mailEx.getMessage());
            }
            return userSave;
        }
    }

    @Override
    public void logout() throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if(user == null) {
            logger.error("User is not existed");
            throw new Exception("User is not existed");
        }
        // Chỉ thu hồi phiên của CHÍNH thiết bị đang gọi. Trước đây lấy token từ ô
        // dùng chung trên hàng users nên đăng xuất ở máy này đá luôn máy khác.
        String token = authorizePathService.getAccessTokenAuthoried();
        if (!StringUtils.hasText(token)) {
            logger.error("Logout: khong doc duoc access token cua request");
            throw new Exception("Server error");
        }
        sessionService.closeSession(token);
    }

    @Override
    public Users refreshAccess(String refreshToken) throws Exception {
        if (!StringUtils.hasText(refreshToken)) throw new Exception("Thiếu refresh token");
        jwtUtils.validateRefreshToken(refreshToken);
        String userId = jwtUtils.getUserIdFromAccessToken(refreshToken);
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) throw new Exception("Người dùng không tồn tại");
        // Đối chiếu theo PHIÊN: mỗi thiết bị một hàng, nên refresh ở máy này không
        // làm hỏng token của máy khác.
        UserSessions session = userSessionRepository.findFirstByRefreshHash(JwtUtils.sha256Hex(refreshToken));
        if (session == null || !userId.equals(session.getUserId()))
            throw new Exception("Refresh token không hợp lệ");
        BlacklistUser blacklistUser = blacklistUserRepository.findByUserId(userId);
        if (blacklistUser != null && blacklistUser.isActiveBan()) throw new Exception("Tài khoản đã bị khóa");
        // Giữ nguyên loại phiên (ghi nhớ / tạm) khi xoay vòng token
        boolean remember = jwtUtils.isRememberRefreshToken(refreshToken);
        // Xoay vòng TRONG phiên đó: chặn refresh token vừa dùng rồi ghi cặp mới vào
        // cùng hàng. CỐ Ý không chặn access token cũ: client chỉ gọi refresh khi
        // access token đã bị từ chối (hết hạn), nên chặn thêm chẳng được gì mà lại
        // đá các tab khác của cùng thiết bị đang dùng dở token đó ra.
        try { blacklistRepository.save(new BlacklistToken(refreshToken)); } catch (Exception ignore) { }
        String access = jwtUtils.generateAccessToken(userId, remember);
        String newRefresh = jwtUtils.generateRefreshToken(userId, remember);
        session.setAccessToken(access);
        session.setRefreshHash(JwtUtils.sha256Hex(newRefresh));
        session.setRemember(remember ? 1 : 0);
        session.setLastUsedAt(new Date());
        userSessionRepository.save(session);
        user.setAccessToken(access);
        user.setPlainRefreshToken(newRefresh);
        return user;
    }

    @Override
    public Users verifyTwoFactor(String email, String code, boolean remember) throws Exception {
        Users user = userRepository.findFirstByEmail(email);
        if (user == null) throw new Exception("Email không tồn tại");
        if (user.getTwofaEnabled() == null || user.getTwofaEnabled() != 1)
            throw new Exception("Tài khoản không bật xác thực 2 bước");
        if (!StringUtils.hasText(user.getTwofaCode()) || code == null
                || !user.getTwofaCode().equalsIgnoreCase(code.trim()))
            throw new Exception("Mã xác thực không đúng");
        if (user.getTwofaExpires() == null || user.getTwofaExpires().before(new java.util.Date()))
            throw new Exception("Mã xác thực đã hết hạn. Vui lòng đăng nhập lại.");
        BlacklistUser blacklistUser = blacklistUserRepository.findByUserId(user.getUserId());
        if (blacklistUser != null && blacklistUser.isActiveBan())
            throw new Exception("User is blocked");
        user.setTwofaCode(null);
        user.setTwofaExpires(null);
        if (user.getDeactivated() != null && user.getDeactivated() == 1) {
            user.setDeactivated(0);
        }
        userRepository.save(user);
        openSession(user, remember);
        return user;
    }

    @Override
    public String requestForgot(String email) throws Exception{
        Users user = userRepository.findFirstByEmail(email);
        if(user == null) throw new Exception("Email is not existed");
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        Random random = new Random();

        for (int i = 0; i < 6; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            sb.append(randomChar);
        }
        String token = sb.toString();
        user.setResetToken(token);

        // Thiết lập hạn token
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime expTime = now.plusMinutes(10);
        Date resetExp = Timestamp.valueOf(expTime);
        user.setResetExp(resetExp);

        userRepository.save(user);

        // send email
        String html = com.didan.social.utils.EmailTemplate.otp(
                "Đặt lại mật khẩu",
                "Bạn (hoặc ai đó) đã yêu cầu đặt lại mật khẩu. Nhập mã dưới đây để tiếp tục. Mã có hiệu lực trong 10 phút.",
                token,
                "Nếu bạn không yêu cầu, hãy bỏ qua email và mật khẩu của bạn vẫn an toàn.");
        mailService.sendTextEmail(email, "Mã đặt lại mật khẩu", html);
        return token;
    }

    // Verify Token Reset
    @Override
    public String verifyToken(String token) throws Exception{
        Users user = userRepository.findFirstByResetToken(token);
        if (user == null) {
            logger.error("Request error");
            throw new Exception("Request error");
        }
        else {
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            Date nowDate = Timestamp.valueOf(now);
            if (nowDate.after(user.getResetExp())){
                logger.error("Expired token");
                return null;
            } else {
                return token;
            }
        }
    }

    @Override
    public boolean updatePassword(String token, String newPassword) throws Exception {
        if (StringUtils.hasText(verifyToken(token))){
            if (newPassword.length() < 8) {
                logger.error("The minimum password should be 8");
                return false;
            }
            else {
                Users user = userRepository.findFirstByResetToken(token);
                String passEncode = passwordEncoder.encode(newPassword);
                user.setPassword(passEncode);
                userRepository.save(user);
                logger.info("Update password successful");
                return true;
            }
        }
        else {
            logger.error("Cannot update password");
            return false;
        }
    }

    private String randomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        Random random = new Random();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    @Override
    public Users verifyEmail(String email, String code) throws Exception {
        Users user = userRepository.findFirstByEmail(email);
        if (user == null) {
            throw new Exception("Không tìm thấy tài khoản");
        }
        if (user.getEmailVerified() != null && user.getEmailVerified() == 1) {
            // đã xác thực rồi -> cấp token luôn cho tiện đăng nhập
        } else {
            if (!StringUtils.hasText(user.getVerifyCode()) || code == null
                    || !user.getVerifyCode().equalsIgnoreCase(code.trim())) {
                throw new Exception("Mã xác thực không đúng");
            }
            user.setEmailVerified(1);
            user.setVerifyCode(null);
        }
        userRepository.save(user);
        openSession(user, true);
        return user;
    }

    @Override
    public void resendVerify(String email) throws Exception {
        Users user = userRepository.findFirstByEmail(email);
        if (user == null) {
            throw new Exception("Không tìm thấy tài khoản");
        }
        if (user.getEmailVerified() != null && user.getEmailVerified() == 1) {
            throw new Exception("Email đã được xác thực");
        }
        String code = randomCode();
        user.setEmailVerified(0);
        user.setVerifyCode(code);
        userRepository.save(user);
        logger.info("[verify] resend {} code={}", email, code);
        mailService.sendTextEmail(email, "Mã xác thực email mới",
                com.didan.social.utils.EmailTemplate.otp(
                        "Mã xác thực mới",
                        "Đây là mã xác thực email mới cho tài khoản của bạn.",
                        code,
                        "Nếu bạn không yêu cầu, hãy bỏ qua email này."));
    }
}
