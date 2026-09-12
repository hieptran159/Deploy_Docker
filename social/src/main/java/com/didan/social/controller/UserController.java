package com.didan.social.controller;

import com.didan.social.dto.SessionDTO;
import com.didan.social.dto.UserDTO;
import com.didan.social.payload.ResponseData;
import com.didan.social.payload.request.EditUserRequest;
import com.didan.social.payload.request.UpdateProfileRequest;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.SessionService;
import com.didan.social.service.impl.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "User")
@RequestMapping("/user")
public class UserController {
    @Autowired
    UserServiceImpl userService;
    @Autowired
    SessionService sessionService;
    @Autowired
    AuthorizePathService authorizePathService;

    @Operation(summary = "Thiết bị đang đăng nhập của tôi",
            description = "Mỗi hàng là một phiên; current = thiết bị đang gọi API này",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/sessions")
    public ResponseEntity<?> mySessions() {
        ResponseData payload = new ResponseData();
        try {
            String userId = authorizePathService.getUserIdAuthoried();
            List<SessionDTO> data = sessionService.listSessions(
                    userId, authorizePathService.getAccessTokenAuthoried());
            payload.setData(data);
            payload.setDescription("Danh sách thiết bị đang đăng nhập");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Đăng xuất mọi thiết bị khác",
            description = "Giữ lại đúng thiết bị đang gọi",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/sessions")
    public ResponseEntity<?> revokeOtherSessions() {
        ResponseData payload = new ResponseData();
        try {
            int n = sessionService.closeOtherSessions(
                    authorizePathService.getUserIdAuthoried(),
                    authorizePathService.getAccessTokenAuthoried());
            payload.setData(n);
            payload.setDescription("Đã đăng xuất " + n + " thiết bị khác");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Đăng xuất một thiết bị", description = "Thu hồi đúng phiên đó, các máy khác không ảnh hưởng",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> revokeSession(@PathVariable String sessionId) {
        ResponseData payload = new ResponseData();
        try {
            sessionService.closeSessionById(authorizePathService.getUserIdAuthoried(), sessionId);
            payload.setDescription("Đã đăng xuất thiết bị");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e) {
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Xoá tài khoản của tôi", description = "Cần mật khẩu hiện tại; xoá vĩnh viễn",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping
    public ResponseEntity<?> deleteMyAccount(@RequestParam String password){
        ResponseData payload = new ResponseData();
        try {
            userService.deleteMyAccount(password);
            payload.setDescription("Đã xoá tài khoản");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Tự vô hiệu hoá tài khoản tạm thời", description = "Cần mật khẩu; đăng nhập lại để kích hoạt",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/deactivate")
    public ResponseEntity<?> deactivateMyAccount(@RequestParam String password){
        ResponseData payload = new ResponseData();
        try {
            userService.deactivateMyAccount(password);
            payload.setDescription("Đã vô hiệu hoá tài khoản");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Bật xác thực 2 bước (mã qua email khi đăng nhập)", description = "Cần mật khẩu",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/2fa/enable")
    public ResponseEntity<?> enableTwoFactor(@RequestParam String password){
        return toggleTwoFactor(true, password);
    }

    @Operation(summary = "Tắt xác thực 2 bước", description = "Cần mật khẩu",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/2fa/disable")
    public ResponseEntity<?> disableTwoFactor(@RequestParam String password){
        return toggleTwoFactor(false, password);
    }

    private ResponseEntity<?> toggleTwoFactor(boolean enable, String password){
        ResponseData payload = new ResponseData();
        try {
            userService.setTwoFactor(enable, password);
            payload.setDescription(enable ? "Đã bật xác thực 2 bước" : "Đã tắt xác thực 2 bước");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Get all users (KHÔNG DÙNG NỮA — dùng /user/search)",
            description = "Get all users",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/getAllUser")
    public ResponseEntity<?> getAllUser(){
        ResponseData payload = new ResponseData();
        try{
            List<UserDTO> data = userService.getAllUser();
            if (data != null){
                payload.setData(data);
                payload.setDescription("Get all users successful");
            } else {
                payload.setDescription("No users are here");
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        }catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    @Operation(summary = "Get detail an user",
            description = "Enter the id user you want get detail",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable String userId){
        ResponseData payload = new ResponseData();
        try {
            UserDTO data = userService.getUserById(userId);
            if (data != null){
                payload.setData(data);
                payload.setDescription("Get user successful");
            } else {
                payload.setDescription("No user is here");
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        }catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Tìm người dùng",
            description = "Phân trang phía server; chỉ trả id/tên/ảnh + số theo dõi + số bài. "
                    + "`name` là tên cũ của `q`, giữ lại cho client cũ.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/search")
    public ResponseEntity<?> searchUser(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size){
        ResponseData payload = new ResponseData();
        try{
            payload.setData(userService.searchUsersLite(q != null ? q : name, page, size));
            payload.setDescription("Danh sách người dùng");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        }catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Đổi ảnh bìa hồ sơ", description = "Không cần mật khẩu",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping(value = "/cover", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> patchCover(@RequestParam("cover") org.springframework.web.multipart.MultipartFile cover){
        ResponseData payload = new ResponseData();
        try {
            userService.updateCover(cover);
            payload.setDescription("Đã cập nhật ảnh bìa");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setDescription(e.getMessage());
            payload.setStatusCode(500);
            payload.setSuccess(false);
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Đổi ảnh đại diện", description = "Không cần mật khẩu",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping(value = "/avatar", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> patchAvatar(@RequestParam("avatar") org.springframework.web.multipart.MultipartFile avatar){
        ResponseData payload = new ResponseData();
        try {
            userService.updateAvatar(avatar);
            payload.setDescription("Đã cập nhật ảnh đại diện");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setDescription(e.getMessage());
            payload.setStatusCode(500);
            payload.setSuccess(false);
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Edit user info", description = "Require password",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping(value = "/edit", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> patchEdit(@ModelAttribute EditUserRequest editUserRequest){
        ResponseData payload = new ResponseData();
        try {
            if (userService.updateUser(editUserRequest)){
                payload.setDescription("Edit user successful");
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setDescription(e.getMessage());
            payload.setStatusCode(500);
            payload.setSuccess(false);
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Cập nhật hồ sơ mở rộng (nickname, phone, address, hobbies, slogan) + cờ công khai",
            description = "Không cần mật khẩu; chỉ sửa hồ sơ của chính mình",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/profile")
    public ResponseEntity<?> patchProfile(@RequestBody UpdateProfileRequest req){
        ResponseData payload = new ResponseData();
        try {
            userService.updateProfile(req);
            payload.setDescription("Cập nhật hồ sơ thành công");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setDescription(e.getMessage());
            payload.setStatusCode(500);
            payload.setSuccess(false);
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Report user", description = "Require userId to report user",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{userId}")
    public ResponseEntity<?> reportUser(@PathVariable String userId){
        ResponseData payload = new ResponseData();
        try {
            if (userService.reportUser(userId)){
                payload.setDescription(String.format("Report user %s successful", userId));
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setDescription(e.getMessage());
            payload.setStatusCode(500);
            payload.setSuccess(false);
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
