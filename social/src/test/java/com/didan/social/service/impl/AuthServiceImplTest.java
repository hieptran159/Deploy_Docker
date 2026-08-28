package com.didan.social.service.impl;

import com.didan.social.entity.Users;
import com.didan.social.repository.BlacklistRepository;
import com.didan.social.repository.BlacklistUserRepository;
import com.didan.social.repository.UserRepository;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.FileUploadsService;
import com.didan.social.service.MailService;
import com.didan.social.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock BlacklistRepository blacklistRepository;
    @Mock BlacklistUserRepository blacklistUserRepository;
    @Mock FileUploadsService fileUploadsService;
    @Mock MailService mailService;
    @Mock AuthorizePathService authorizePathService;
    @Mock JwtUtils jwtUtils;

    AuthServiceImpl svc;
    static final String EMAIL = "u@example.com";

    @BeforeEach
    void setUp() {
        svc = new AuthServiceImpl(userRepository, passwordEncoder, blacklistRepository, fileUploadsService,
                jwtUtils, mailService, authorizePathService, blacklistUserRepository);
        when(jwtUtils.generateAccessToken(anyString())).thenReturn("AT");
        when(jwtUtils.generateRefreshToken(anyString())).thenReturn("RT");
    }

    private Users user(Integer twofa) {
        Users u = new Users();
        u.setUserId("u-1");
        u.setEmail(EMAIL);
        u.setPassword("hash");
        u.setIsAdmin(0);
        u.setTwofaEnabled(twofa);
        return u;
    }

    // ---------- login + 2FA ----------

    @Test
    void loginWithoutTwoFactorIssuesTokens() throws Exception {
        when(userRepository.findFirstByEmail(EMAIL)).thenReturn(user(0));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

        Users out = svc.login(EMAIL, "pw");

        assertFalse(out.isTwofaRequired());
        assertEquals("AT", out.getAccessToken());
        assertEquals("RT", out.getPlainRefreshToken());
        assertEquals(com.didan.social.utils.JwtUtils.sha256Hex("RT"), out.getRefreshToken());
    }

    @Test
    void loginWithTwoFactorSendsCodeAndWithholdsTokens() throws Exception {
        Users u = user(1);
        when(userRepository.findFirstByEmail(EMAIL)).thenReturn(u);
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

        Users out = svc.login(EMAIL, "pw");

        assertTrue(out.isTwofaRequired());
        assertNull(out.getAccessToken());
        assertNotNull(u.getTwofaCode());
        assertNotNull(u.getTwofaExpires());
        verify(userRepository).save(u);
        verify(jwtUtils, never()).generateAccessToken(anyString());
    }

    // ---------- verifyTwoFactor ----------

    private Users twofaPending(String code, LocalDateTime exp) {
        Users u = user(1);
        u.setTwofaCode(code);
        u.setTwofaExpires(Timestamp.valueOf(exp));
        return u;
    }

    @Test
    void verifyWrongCodeRejected() {
        when(userRepository.findFirstByEmail(EMAIL)).thenReturn(twofaPending("ABC123", LocalDateTime.now().plusMinutes(5)));
        Exception e = assertThrows(Exception.class, () -> svc.verifyTwoFactor(EMAIL, "ZZZ999"));
        assertTrue(e.getMessage().contains("không đúng"));
    }

    @Test
    void verifyExpiredCodeRejected() {
        when(userRepository.findFirstByEmail(EMAIL)).thenReturn(twofaPending("ABC123", LocalDateTime.now().minusMinutes(1)));
        Exception e = assertThrows(Exception.class, () -> svc.verifyTwoFactor(EMAIL, "ABC123"));
        assertTrue(e.getMessage().contains("hết hạn"));
    }

    @Test
    void verifyWhenTwoFactorDisabledRejected() {
        when(userRepository.findFirstByEmail(EMAIL)).thenReturn(user(0));
        Exception e = assertThrows(Exception.class, () -> svc.verifyTwoFactor(EMAIL, "ABC123"));
        assertTrue(e.getMessage().contains("không bật"));
    }

    @Test
    void verifyValidCodeIssuesTokensAndClearsCode() throws Exception {
        Users u = twofaPending("abc123", LocalDateTime.now().plusMinutes(5));
        when(userRepository.findFirstByEmail(EMAIL)).thenReturn(u);

        Users out = svc.verifyTwoFactor(EMAIL, "ABC123"); // so khớp không phân biệt hoa thường

        assertEquals("AT", out.getAccessToken());
        assertEquals("RT", out.getPlainRefreshToken());
        assertEquals(com.didan.social.utils.JwtUtils.sha256Hex("RT"), out.getRefreshToken());
        assertNull(u.getTwofaCode());
        assertNull(u.getTwofaExpires());
        verify(userRepository).save(u);
    }
}
