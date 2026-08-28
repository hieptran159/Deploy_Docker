package com.didan.social.controller;

import com.didan.social.dto.UserDTO;
import com.didan.social.payload.ResponseData;
import com.didan.social.payload.request.EditUserRequest;
import com.didan.social.payload.request.UpdateProfileRequest;
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

    @Operation(summary = "Get all users",
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

    @Operation(summary = "Search user",
            description = "Enter the name user you want to find",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/search")
    public ResponseEntity<?> searchUser(@RequestParam(name = "name") String name){
        ResponseData payload = new ResponseData();
        try{
            List<UserDTO> data = userService.searchUser(name);
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
