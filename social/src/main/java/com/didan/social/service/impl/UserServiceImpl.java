package com.didan.social.service.impl;

import com.didan.social.dto.UserDTO;
import com.didan.social.entity.BlacklistUser;
import com.didan.social.entity.Posts;
import com.didan.social.entity.UserPosts;
import com.didan.social.entity.Users;
import com.didan.social.payload.request.EditUserRequest;
import com.didan.social.payload.request.UpdateProfileRequest;
import com.didan.social.repository.BlacklistUserRepository;
import com.didan.social.repository.UserRepository;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.UserService;
import com.didan.social.service.convertdto.ConvertDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class UserServiceImpl extends ConvertDTO implements UserService {
    private final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final AuthorizePathService authorizePathService;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadsServiceImpl fileUploadsService;
    private final BlacklistUserRepository blacklistUserRepository;
    private final com.didan.social.repository.PostRepository postRepository;
    private final com.didan.social.repository.CommentRepository commentRepository;
    private final com.didan.social.repository.BlacklistRepository blacklistRepository;
    private final com.didan.social.socket.RealtimeGateway realtimeGateway;
    private final com.didan.social.service.FollowService followService;
    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           AuthorizePathService authorizePathService,
                           PasswordEncoder passwordEncoder,
                           FileUploadsServiceImpl fileUploadsService,
                           BlacklistUserRepository blacklistUserRepository,
                           com.didan.social.repository.PostRepository postRepository,
                           com.didan.social.repository.CommentRepository commentRepository,
                           com.didan.social.repository.BlacklistRepository blacklistRepository,
                           com.didan.social.socket.RealtimeGateway realtimeGateway,
                           com.didan.social.service.FollowService followService
    ){
        this.userRepository = userRepository;
        this.authorizePathService = authorizePathService;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadsService = fileUploadsService;
        this.blacklistUserRepository = blacklistUserRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.blacklistRepository = blacklistRepository;
        this.realtimeGateway = realtimeGateway;
        this.followService = followService;
    }

    // Báo cho chính mình + bạn bè biết avatar mới (socket "user_avatar")
    private void broadcastAvatar(String userId, String avtUrl) {
        try {
            java.util.Map<String, Object> p = new java.util.HashMap<>();
            p.put("userId", userId);
            p.put("avtUrl", avtUrl);
            realtimeGateway.toUser(userId, "user_avatar", p);
            for (String fid : followService.friendIdsOf(userId)) {
                realtimeGateway.toUser(fid, "user_avatar", p);
            }
        } catch (Exception ignore) {}
    }
    @Override
    public List<UserDTO> getAllUser(){
        List<Users> users = userRepository.findAll();
        if (users.size() <= 0) {
            logger.info("No one");
            return Collections.emptyList();
        }
        List<UserDTO> userDTOS = new ArrayList<>();
        for (Users user : users){
            if (user.getDeactivated() != null && user.getDeactivated() == 1) continue;
            UserDTO userDTO = (UserDTO) convertToDTO(user);
            userDTOS.add(userDTO);
        }
        userDTOS.sort(Comparator.comparingInt(UserDTO::getFollowers).reversed());
        return userDTOS;
    }

    @Override
    public UserDTO getUserById(String userId) {
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.info("No user to found");
            return null;
        }
        boolean isOwner = false;
        try {
            isOwner = userId.equals(authorizePathService.getUserIdAuthoried());
        } catch (Exception ignored) {}
        // Tài khoản đang tự vô hiệu hoá -> chỉ chính chủ xem được
        if (!isOwner && user.getDeactivated() != null && user.getDeactivated() == 1) {
            return null;
        }
        UserDTO dto = (UserDTO) convertToDTO(user);
        if (!isOwner) {
            hidePrivate(dto);
        } else {
            dto.setTwoFactorEnabled(user.getTwofaEnabled() != null && user.getTwofaEnabled() == 1);
        }
        return dto;
    }

    private static boolean pub(Integer v) { return v == null || v == 1; }

    // Ẩn các trường riêng tư khi người xem không phải chủ hồ sơ
    private void hidePrivate(UserDTO d) {
        if (d == null) return;
        if (!Boolean.TRUE.equals(d.getNicknamePublic())) d.setNickname(null);
        if (!Boolean.TRUE.equals(d.getPhonePublic())) d.setPhone(null);
        if (!Boolean.TRUE.equals(d.getAddressPublic())) d.setAddress(null);
        if (!Boolean.TRUE.equals(d.getHobbiesPublic())) d.setHobbies(null);
        if (!Boolean.TRUE.equals(d.getSloganPublic())) d.setSlogan(null);
    }

    // Làm sạch chuỗi hiển thị do người dùng nhập: bỏ '<' '>' và ký tự điều khiển, cắt độ dài.
    static String clean(String s, int max) {
        if (s == null) return null;
        String out = s.trim().replaceAll("[<>\\p{Cntrl}&&[^\\r\\n\\t]]", "");
        return out.length() > max ? out.substring(0, max) : out;
    }

    @Override
    public boolean updateProfile(UpdateProfileRequest req) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        if (req.getFullName() != null && !req.getFullName().trim().isEmpty()) user.setFullName(clean(req.getFullName(), 100));
        if (req.getNickname() != null) user.setNickname(clean(req.getNickname(), 100));
        if (req.getPhone() != null) user.setPhone(clean(req.getPhone(), 30));
        if (req.getAddress() != null) user.setAddress(clean(req.getAddress(), 255));
        if (req.getHobbies() != null) user.setHobbies(clean(req.getHobbies(), 500));
        if (req.getSlogan() != null) user.setSlogan(clean(req.getSlogan(), 255));
        if (req.getNicknamePublic() != null) user.setNicknamePublic(req.getNicknamePublic());
        if (req.getPhonePublic() != null) user.setPhonePublic(req.getPhonePublic());
        if (req.getAddressPublic() != null) user.setAddressPublic(req.getAddressPublic());
        if (req.getHobbiesPublic() != null) user.setHobbiesPublic(req.getHobbiesPublic());
        if (req.getSloganPublic() != null) user.setSloganPublic(req.getSloganPublic());
        userRepository.save(user);
        return true;
    }

    @Override
    public List<UserDTO> searchUser(String searchName) throws Exception {
        List<Users> users = userRepository.findByFullNameContainingOrEmailLike(searchName, searchName);
        if (users.isEmpty()) {
            logger.info("No one");
            return Collections.emptyList();
        }
        List<UserDTO> userDTOS = new ArrayList<>();
        for (Users user : users){
            if (user.getDeactivated() != null && user.getDeactivated() == 1) continue;
            UserDTO userDTO = (UserDTO) convertToDTO(user);
            userDTOS.add(userDTO);
        }
        userDTOS.sort(Comparator.comparingInt(UserDTO::getFollowers).reversed());
        return userDTOS;
    }

    @Override
    public boolean updateUser(EditUserRequest editUserRequest) throws Exception {
        if (!StringUtils.hasText(editUserRequest.getPassword())){
            logger.error("The password is required");
            throw new Exception("The password is required");
        }
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        if (!passwordEncoder.matches(editUserRequest.getPassword(), user.getPassword())) {
            logger.error("Password is incorrect");
            throw new Exception("Password is incorrect");
        } else {
            if (StringUtils.hasText(editUserRequest.getEmail())) {
                Users user_email = userRepository.findFirstByEmail(editUserRequest.getEmail());
                if (user_email != null){
                    logger.info("Email is registed!");
                } else user.setEmail(editUserRequest.getEmail());
            }
            if (StringUtils.hasText(editUserRequest.getNewPassword())) {
                user.setPassword(passwordEncoder.encode(editUserRequest.getNewPassword()));
            }
            boolean avatarChanged = false;
            if (editUserRequest.getAvatar() != null && !editUserRequest.getAvatar().isEmpty()){
                if(StringUtils.hasText(user.getAvtUrl())){
                    fileUploadsService.deleteFile(user.getAvtUrl());
                }
                // Tên file kèm timestamp -> URL đổi mỗi lần -> client không dính cache ảnh cũ
                String fileName = fileUploadsService.storeFile(editUserRequest.getAvatar(), "avatar",
                        user.getUserId() + "-" + System.currentTimeMillis());
                user.setAvtUrl("avatar/"+fileName);
                avatarChanged = true;
            }
            userRepository.save(user);
            if (avatarChanged) broadcastAvatar(user.getUserId(), user.getAvtUrl());
            return true;
        }
    }

    @Override
    public boolean reportUser(String userId) throws Exception {
        String myId = authorizePathService.getUserIdAuthoried();
        if (myId.equals(userId)){
            logger.error("Can't report yourself");
            throw new Exception("Can't report yourself");
        }
        Users user = userRepository.findFirstByUserId(userId);
        if (user.getIsAdmin() == 1){
            logger.error("Can't report admin");
            throw new Exception("Can't report admin");
        }
        BlacklistUser blacklistUser = blacklistUserRepository.findByUserId(userId);
        if (blacklistUser == null){
            blacklistUser = new BlacklistUser();
            blacklistUser.setUserId(userId);
            blacklistUser.setReportedQuantity(1);
            blacklistUser.setBlockedAt(null);
            blacklistUserRepository.save(blacklistUser);
        } else {
            blacklistUser.setReportedQuantity(blacklistUser.getReportedQuantity() + 1);
            if (blacklistUser.getReportedQuantity() >= 1000){
                blacklistUser.setStatus("blocked");
                LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
                Date nowSql = Timestamp.valueOf(now);
                blacklistUser.setBlockedAt(nowSql);
            }
            blacklistUserRepository.save(blacklistUser);
        }
        return true;
    }

    @Override
    public boolean updateCover(org.springframework.web.multipart.MultipartFile cover) throws Exception {
        if (cover == null || cover.isEmpty()) {
            throw new Exception("Chưa chọn ảnh bìa");
        }
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            throw new Exception("User is not found");
        }
        if (org.springframework.util.StringUtils.hasText(user.getCoverUrl())) {
            try { fileUploadsService.deleteFile(user.getCoverUrl()); } catch (Exception ignore) { }
        }
        String fileName = fileUploadsService.storeFile(cover, "cover", user.getUserId() + "-" + System.currentTimeMillis());
        user.setCoverUrl("cover/" + fileName);
        userRepository.save(user);
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    @Override
    public boolean deactivateMyAccount(String password) throws Exception {
        String myId = authorizePathService.getUserIdAuthoried();
        Users me = userRepository.findFirstByUserId(myId);
        if (me == null) {
            throw new Exception("User is not found");
        }
        if (!org.springframework.util.StringUtils.hasText(password)
                || !passwordEncoder.matches(password, me.getPassword())) {
            throw new Exception("Mật khẩu không đúng");
        }
        me.setDeactivated(1);
        userRepository.save(me);
        // chặn token đang dùng -> đăng xuất ngay; đăng nhập lại sẽ tự kích hoạt
        if (org.springframework.util.StringUtils.hasText(me.getAccessToken())) {
            try { blacklistRepository.save(new com.didan.social.entity.BlacklistToken(me.getAccessToken())); } catch (Exception ignore) { }
        }
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    @Override
    public boolean setTwoFactor(boolean enable, String password) throws Exception {
        String myId = authorizePathService.getUserIdAuthoried();
        Users me = userRepository.findFirstByUserId(myId);
        if (me == null) {
            throw new Exception("User is not found");
        }
        if (!org.springframework.util.StringUtils.hasText(password)
                || !passwordEncoder.matches(password, me.getPassword())) {
            throw new Exception("Mật khẩu không đúng");
        }
        me.setTwofaEnabled(enable ? 1 : 0);
        if (!enable) {
            me.setTwofaCode(null);
            me.setTwofaExpires(null);
        }
        userRepository.save(me);
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    @Override
    public boolean deleteMyAccount(String password) throws Exception {
        String myId = authorizePathService.getUserIdAuthoried();
        Users me = userRepository.findFirstByUserId(myId);
        if (me == null) {
            throw new Exception("User is not found");
        }
        if (!org.springframework.util.StringUtils.hasText(password)
                || !passwordEncoder.matches(password, me.getPassword())) {
            throw new Exception("Mật khẩu không đúng");
        }
        if (me.getIsAdmin() == 1) {
            throw new Exception("Tài khoản quản trị không thể tự xoá");
        }

        // 1) xoá bài viết của mình (kèm ảnh) — cascade lo phần like/comment-join của bài
        for (UserPosts up : new ArrayList<>(me.getUserPosts())) {
            Posts p = up.getPosts();
            if (p == null) continue;
            if (org.springframework.util.StringUtils.hasText(p.getPostImg())) {
                try { fileUploadsService.deleteFile(p.getPostImg()); } catch (Exception ignore) { }
            }
            postRepository.delete(p);
        }

        // 2) chặn token đang dùng
        if (org.springframework.util.StringUtils.hasText(me.getAccessToken())) {
            try { blacklistRepository.save(new com.didan.social.entity.BlacklistToken(me.getAccessToken())); } catch (Exception ignore) { }
        }
        // 3) xoá avatar
        if (org.springframework.util.StringUtils.hasText(me.getAvtUrl())) {
            try { fileUploadsService.deleteFile(me.getAvtUrl()); } catch (Exception ignore) { }
        }

        // 4) xoá user -> cascade followers/postLikes/userPosts/participants/messages/userComments/commentLikes/blacklistUser
        userRepository.delete(me);
        userRepository.flush();

        // 5) dọn bình luận mồ côi (bình luận mình viết trên bài người khác)
        for (String cid : commentRepository.findCommentIdNotInUserComment()) {
            try { commentRepository.deleteById(cid); } catch (Exception ignore) { }
        }
        return true;
    }

    @Override
    protected Object convertToDTO(Object object) {
        if (!(object instanceof Users)){
            return null;
        }
        Users user = (Users) object;
        UserDTO userDTO = new UserDTO();
        List<String> postId = new ArrayList<>();
        userDTO.setUserId(user.getUserId());
        userDTO.setFullName(user.getFullName());
        userDTO.setEmail(user.getEmail());
        userDTO.setAvtUrl(user.getAvtUrl());
        userDTO.setCoverUrl(user.getCoverUrl());
        userDTO.setDob(user.getDob().toString());
        userDTO.setFollowers(user.getFolloweds().size());
        userDTO.setFollowings(user.getFollowers().size());
        userDTO.setPosts(user.getUserPosts().size());
        for (UserPosts userPosts : user.getUserPosts()){
            postId.add(userPosts.getUserPostId().getPostId());
        }
        userDTO.setPostId(postId);
        userDTO.setParticipantGroups(user.getParticipants().size());
        userDTO.setNickname(user.getNickname());
        userDTO.setPhone(user.getPhone());
        userDTO.setAddress(user.getAddress());
        userDTO.setHobbies(user.getHobbies());
        userDTO.setSlogan(user.getSlogan());
        userDTO.setNicknamePublic(pub(user.getNicknamePublic()));
        userDTO.setPhonePublic(pub(user.getPhonePublic()));
        userDTO.setAddressPublic(pub(user.getAddressPublic()));
        userDTO.setHobbiesPublic(pub(user.getHobbiesPublic()));
        userDTO.setSloganPublic(pub(user.getSloganPublic()));
        return userDTO;
    }
}
