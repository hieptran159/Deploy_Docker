package com.didan.social.service.impl;

import com.didan.social.dto.BlacklistUserDTO;
import com.didan.social.entity.BlacklistToken;
import com.didan.social.entity.BlacklistUser;
import com.didan.social.entity.Users;
import com.didan.social.repository.BlacklistRepository;
import com.didan.social.repository.BlacklistUserRepository;
import com.didan.social.repository.BlockRepository;
import com.didan.social.repository.BookmarkRepository;
import com.didan.social.repository.CommentRepository;
import com.didan.social.repository.ConversationRepository;
import com.didan.social.repository.MessageRepository;
import com.didan.social.repository.PostRepository;
import com.didan.social.repository.ReportRepository;
import com.didan.social.repository.AdminLogRepository;
import com.didan.social.repository.UserRepository;
import com.didan.social.service.AdminLogService;
import com.didan.social.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {
    private final AuthorizePathServiceImpl authorizePathService;
    private final UserRepository userRepository;
    private final BlacklistUserRepository blacklistUserRepository;
    private final BlacklistRepository blacklistTokenRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final BookmarkRepository bookmarkRepository;
    private final BlockRepository blockRepository;
    private final ReportRepository reportRepository;
    private final AdminLogRepository adminLogRepository;
    private final AdminLogService adminLogService;
    private final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);
    @Autowired
    public AdminServiceImpl(AuthorizePathServiceImpl authorizePathService, UserRepository userRepository,
                            BlacklistUserRepository blacklistUserRepository, BlacklistRepository blacklistTokenRepository,
                            PostRepository postRepository, CommentRepository commentRepository,
                            ConversationRepository conversationRepository, MessageRepository messageRepository,
                            BookmarkRepository bookmarkRepository, BlockRepository blockRepository,
                            ReportRepository reportRepository, AdminLogRepository adminLogRepository,
                            AdminLogService adminLogService) {
        this.authorizePathService = authorizePathService;
        this.userRepository = userRepository;
        this.blacklistUserRepository = blacklistUserRepository;
        this.blacklistTokenRepository = blacklistTokenRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.blockRepository = blockRepository;
        this.reportRepository = reportRepository;
        this.adminLogRepository = adminLogRepository;
        this.adminLogService = adminLogService;
    }

    @Override
    public java.util.List<com.didan.social.entity.AdminLog> getLogs(int page, int size) throws Exception {
        authAdmin();
        if (page < 0) page = 0;
        if (size < 1) size = 30;
        if (size > 100) size = 100;
        return adminLogRepository.findByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(page, size));
    }

    @Override
    public java.util.Map<String, Object> getStats() throws Exception {
        authAdmin();
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("users", userRepository.count());
        m.put("admins", userRepository.countByIsAdmin(1));
        m.put("posts", postRepository.countPublished());
        m.put("drafts", postRepository.countByStatus("draft"));
        m.put("comments", commentRepository.count());
        m.put("conversations", conversationRepository.count());
        m.put("messages", messageRepository.count());
        m.put("bookmarks", bookmarkRepository.count());
        m.put("blocks", blockRepository.count());
        m.put("reportsOpen", reportRepository.countByStatus("OPEN"));
        m.put("reportsResolved", reportRepository.countByStatus("RESOLVED"));
        m.put("reportsDismissed", reportRepository.countByStatus("DISMISSED"));
        m.put("bannedUsers", blacklistUserRepository.countByStatus("blocked"));

        // Bài đăng theo ngày, 14 ngày gần nhất
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        java.time.LocalDate from = today.minusDays(13);
        java.sql.Timestamp since = java.sql.Timestamp.valueOf(from.atStartOfDay());
        java.util.Map<String, Long> byDay = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 14; i++) byDay.put(from.plusDays(i).toString(), 0L);
        for (Object[] row : postRepository.countPostsPerDaySince(since)) {
            String d = row[0].toString().substring(0, 10);
            long c = ((Number) row[1]).longValue();
            if (byDay.containsKey(d)) byDay.put(d, c);
        }
        java.util.List<java.util.Map<String, Object>> series = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Long> e : byDay.entrySet()) {
            java.util.Map<String, Object> pt = new java.util.LinkedHashMap<>();
            pt.put("date", e.getKey());
            pt.put("count", e.getValue());
            series.add(pt);
        }
        m.put("postsPerDay", series);
        return m;
    }
    @Override
    public boolean grantAdmin(String userIdGranted) throws Exception {
        Users user = authAdmin();
        Users user_grant = userRepository.findFirstByUserId(userIdGranted);
        if (user_grant == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        if (user_grant.getIsAdmin() == 1){
            logger.error("User is already admin");
            throw new Exception("User is already admin");
        }
        user_grant.setIsAdmin(1);
        userRepository.save(user_grant);
        adminLogService.record("GRANT_ADMIN", "USER", userIdGranted, user_grant.getFullName());
        return true;
    }

    @Override
    public List<BlacklistUserDTO> getAllBlacklistUser() throws Exception {
        Users user = authAdmin();
        List<BlacklistUser> blacklistUsers = blacklistUserRepository.findAll();
        if (blacklistUsers.isEmpty()) {
            logger.info("No one in blacklist");
            return Collections.emptyList();
        }
        List<BlacklistUserDTO> blacklists = new ArrayList<>();
        for (BlacklistUser blacklistUser : blacklistUsers){
            BlacklistUserDTO blacklistUserDTO = new BlacklistUserDTO();
            blacklistUserDTO.setUserId(blacklistUser.getUserId());
            blacklistUserDTO.setFullName(blacklistUser.getUsers().getFullName());
            blacklistUserDTO.setEmail(blacklistUser.getUsers().getEmail());
            blacklistUserDTO.setReportStatus(blacklistUser.getStatus());
            blacklistUserDTO.setReportedQuantity(blacklistUser.getReportedQuantity());
            if (blacklistUser.getBlockedAt() != null){
                blacklistUserDTO.setBlockedAt(blacklistUser.getBlockedAt().toString());
            }
            blacklists.add(blacklistUserDTO);
        }
        return blacklists;
    }

    @Override
    public boolean blockUser(String userId) throws Exception {
        Users user = authAdmin();
        Users user_block = userRepository.findFirstByUserId(userId);
        if (user_block == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        if (StringUtils.hasText(user_block.getAccessToken())){
            blacklistTokenRepository.save(new BlacklistToken(user_block.getAccessToken()));
        }
        BlacklistUser blacklistUser = blacklistUserRepository.findByUserId(userId);
        if (blacklistUser != null){
            if (blacklistUser.getStatus().equals("blocked")){
                logger.error("User is already in blacklist");
                throw new Exception("User is already in blacklist");
            } else {
                blacklistUser.setStatus("blocked");
                LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
                Date nowSql = Timestamp.valueOf(now);
                blacklistUser.setBlockedAt(nowSql);
                blacklistUserRepository.save(blacklistUser);
                adminLogService.record("BAN_USER", "USER", userId, user_block.getFullName());
                return true;
            }
        } else {
            BlacklistUser newUser = new BlacklistUser();
            newUser.setUserId(userId);
            newUser.setStatus("blocked");
            newUser.setReportedQuantity(1);
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            Date nowSql = Timestamp.valueOf(now);
            newUser.setBlockedAt(nowSql);
            blacklistUserRepository.save(newUser);
            adminLogService.record("BAN_USER", "USER", userId, user_block.getFullName());
            return true;
        }
    }

    @Override
    public boolean unblockUser(String userId) throws Exception {
        Users user = authAdmin();
        Users user_unblock = userRepository.findFirstByUserId(userId);
        if (user_unblock == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        BlacklistUser blacklistUser = blacklistUserRepository.findByUserId(userId);
        if (blacklistUser == null){
            logger.error("User is not in blacklist");
            throw new Exception("User is not in blacklist");
        } else {
            if (blacklistUser.getStatus().equals("blocked")){
                blacklistUser.setStatus("pending");
                blacklistUser.setBlockedAt(null);
                blacklistUser.setReportedQuantity(0);
                blacklistUserRepository.save(blacklistUser);
                adminLogService.record("UNBAN_USER", "USER", userId, user_unblock.getFullName());
                return true;
            } else {
                logger.error("User has not been blocked");
                throw new Exception("User has not been blocked");
            }
        }
    }

    public Users authAdmin() throws Exception{
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        if (user.getIsAdmin() == 0){
            logger.error("You are not admin");
            throw new Exception("You are not admin");
        }
        return user;
    }
}
