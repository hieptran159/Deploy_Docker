package com.didan.social.controller;

import com.didan.social.entity.Users;
import com.didan.social.payload.ResponseData;
import com.didan.social.payload.request.SignupRequest;
import com.didan.social.service.AuthService;
import com.didan.social.service.FileUploadsService;
import com.didan.social.service.MailService;
import com.didan.social.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Auth")
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    @Autowired
    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @Operation(summary = "Login to forum app", description = "Request email and password")
    @PostMapping("/signin")
    public ResponseEntity<?> postLogin(@RequestParam String email, @RequestParam String password){
        ResponseData payload = new ResponseData();
        Map<String, String> response = new HashMap<>();
        try{
            Users user = authService.login(email, password);
            if (user != null && user.isTwofaRequired()) {
                payload.setDescription("Đã gửi mã xác thực 2 bước tới email của bạn");
                response.put("twoFactorRequired", "1");
                response.put("email", user.getEmail());
                payload.setData(response);
                return new ResponseEntity<>(payload, HttpStatus.OK);
            }
            if (user != null) {
                payload.setDescription("Login Successful");
                response.put("userId", user.getUserId());
                response.put("fullName", user.getFullName());
                response.put("email", user.getEmail());
                response.put("avatar", user.getAvtUrl());
                response.put("accessToken", user.getAccessToken());
                response.put("refreshToken", user.getRefreshToken());
                response.put("isAdmin", String.valueOf(user.getIsAdmin()));
                payload.setData(response);
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setDescription(e.getMessage());
            payload.setStatusCode(500);
            payload.setSuccess(false);
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Cấp access token mới từ refresh token",
            description = "Gửi refreshToken đang lưu; trả về cặp accessToken + refreshToken mới (token cũ bị thu hồi)")
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestParam String refreshToken){
        ResponseData payload = new ResponseData();
        Map<String, String> response = new HashMap<>();
        try {
            Users user = authService.refreshAccess(refreshToken);
            response.put("userId", user.getUserId());
            response.put("accessToken", user.getAccessToken());
            response.put("refreshToken", user.getRefreshToken());
            response.put("isAdmin", String.valueOf(user.getIsAdmin()));
            payload.setDescription("OK");
            payload.setData(response);
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setDescription(e.getMessage());
            payload.setStatusCode(500);
            payload.setSuccess(false);
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Bước 2 đăng nhập khi bật 2FA",
            description = "Nhập email + mã 6 ký tự đã nhận qua email; thành công trả token đăng nhập")
    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verifyTwoFactor(@RequestParam String email, @RequestParam String code){
        ResponseData payload = new ResponseData();
        Map<String, String> response = new HashMap<>();
        try {
            Users user = authService.verifyTwoFactor(email, code);
            payload.setDescription("Login Successful");
            response.put("userId", user.getUserId());
            response.put("fullName", user.getFullName());
            response.put("email", user.getEmail());
            response.put("avatar", user.getAvtUrl());
            response.put("accessToken", user.getAccessToken());
            response.put("refreshToken", user.getRefreshToken());
            response.put("isAdmin", String.valueOf(user.getIsAdmin()));
            payload.setData(response);
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setDescription(e.getMessage());
            payload.setStatusCode(500);
            payload.setSuccess(false);
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // Dùng @ModelAtrribute để upload file + Json trên form-data
    @Operation(summary = "Create account to access to forum app", description = "Require all properties")
    @PostMapping(value = "/signup", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> postSignup(@ModelAttribute SignupRequest signupRequest){
        ResponseData payload = new ResponseData();
        Map<String, String> response = new HashMap<>();
        try {
            Users user = authService.signup(signupRequest);
            if (user != null){
                payload.setDescription("Đăng ký thành công. Vui lòng nhập mã xác thực đã gửi tới email.");
                response.put("userId", user.getUserId());
                response.put("email", user.getEmail());
                response.put("needVerification", "1");
                payload.setData(response);
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setDescription(e.getMessage());
            payload.setStatusCode(500);
            payload.setSuccess(false);
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Logout from forum app",
            description = "When you logout, the access token will be added in blacklist token",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<?> logout(){
        ResponseData payload = new ResponseData();
        try{
            authService.logout();
            payload.setDescription("Logout successful");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        }catch (Exception e){
            payload.setDescription(e.getMessage());
            payload.setStatusCode(500);
            payload.setSuccess(false);
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Xác thực email bằng mã", description = "Nhập email + mã 6 ký tự đã nhận; thành công sẽ trả token đăng nhập")
    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String email, @RequestParam String code){
        ResponseData payload = new ResponseData();
        Map<String, String> response = new HashMap<>();
        try {
            Users user = authService.verifyEmail(email, code);
            payload.setDescription("Xác thực email thành công");
            response.put("userId", user.getUserId());
            response.put("fullName", user.getFullName());
            response.put("email", user.getEmail());
            response.put("avatar", user.getAvtUrl());
            response.put("accessToken", user.getAccessToken());
            response.put("refreshToken", user.getRefreshToken());
            response.put("isAdmin", String.valueOf(user.getIsAdmin()));
            payload.setData(response);
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setStatusCode(500);
            payload.setSuccess(false);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Gửi lại mã xác thực email")
    @PostMapping("/resend-verify")
    public ResponseEntity<?> resendVerify(@RequestParam String email){
        ResponseData payload = new ResponseData();
        try {
            authService.resendVerify(email);
            payload.setDescription("Đã gửi lại mã xác thực, kiểm tra hộp thư");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setStatusCode(500);
            payload.setSuccess(false);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Get CAPCHA/OTP to reset password", description = "Check CAPCHA is contained in email we will send you")
    @PostMapping("/token-reset")
    public ResponseEntity<?> getTokenReset(@RequestParam(value = "email") String email){
        ResponseData payload = new ResponseData();
        Map<String, String> data = new HashMap<>();
        try {
            String token = authService.requestForgot(email);
            if(StringUtils.hasText(token)){
                payload.setDescription("Email sent, please check inbox");
                data.put("token", token);
                payload.setData(data);
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setStatusCode(500);
            payload.setSuccess(false);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Reset/Change password", description = "Require CAPCHA you received in email")
    @PatchMapping("/reset")
    public ResponseEntity<?> updatePassword(@RequestParam String token, @RequestParam String newPassword){
        ResponseData payload = new ResponseData();
        try{
            if(authService.updatePassword(token, newPassword)){
                payload.setDescription("Reset password successful");
                return new ResponseEntity<>(payload, HttpStatus.OK);
            } else {
                payload.setDescription("Reset password false");
                payload.setStatusCode(422);
                return new ResponseEntity<>(payload, HttpStatus.OK);
            }
        } catch (Exception e){
            payload.setStatusCode(500);
            payload.setSuccess(false);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
