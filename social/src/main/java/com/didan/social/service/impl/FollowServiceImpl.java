package com.didan.social.service.impl;

import com.didan.social.dto.FollowDTO;
import com.didan.social.entity.Blocks;
import com.didan.social.entity.Followers;
import com.didan.social.entity.Users;
import com.didan.social.entity.keys.BlockId;
import com.didan.social.entity.keys.FollowerId;
import com.didan.social.repository.BlockRepository;
import com.didan.social.repository.FollowRepository;
import com.didan.social.repository.UserRepository;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.FollowService;
import com.didan.social.service.NotificationService;
import com.didan.social.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowServiceImpl implements FollowService {
    private static final String PENDING = "pending";
    private static final String ACCEPTED = "accepted";

    private final Logger logger = LoggerFactory.getLogger(FollowServiceImpl.class);
    private final FollowRepository followRepository;
    private final AuthorizePathService authorizePathService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BlockRepository blockRepository;

    @Autowired
    public FollowServiceImpl(FollowRepository followRepository,
                             AuthorizePathService authorizePathService,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             BlockRepository blockRepository) {
        this.followRepository = followRepository;
        this.authorizePathService = authorizePathService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.blockRepository = blockRepository;
    }

    private boolean isAccepted(Followers f) {
        return f != null && (f.getStatus() == null || ACCEPTED.equals(f.getStatus()));
    }
    private boolean isPending(Followers f) {
        return f != null && PENDING.equals(f.getStatus());
    }
    private Followers row(String a, String b) {
        return followRepository.findFirstByUsers1_UserIdAndUsers2_UserId(a, b);
    }
    private Users requireUser(String userId, String msg) throws Exception {
        Users u = userRepository.findFirstByUserId(userId);
        if (u == null) { logger.error(msg); throw new Exception(msg); }
        return u;
    }

    // Nạp tên + avatar cho danh sách id bằng 1 truy vấn gộp, giữ nguyên thứ tự.
    private FollowDTO withUsers(FollowDTO dto) {
        List<String> ids = dto.getUserId();
        if (ids == null || ids.isEmpty()) {
            dto.setUsers(new ArrayList<>());
            return dto;
        }
        Map<String, Users> byId = new HashMap<>();
        for (Users u : userRepository.findAllById(ids)) byId.put(u.getUserId(), u);
        List<FollowDTO.UserBrief> briefs = new ArrayList<>(ids.size());
        for (String id : ids) {
            Users u = byId.get(id);
            briefs.add(new FollowDTO.UserBrief(
                    id,
                    u != null ? u.getFullName() : id,
                    u != null ? u.getAvtUrl() : ""));
        }
        dto.setUsers(briefs);
        return dto;
    }

    @Override
    public boolean areFriends(String a, String b) {
        if (a == null || b == null || a.equals(b)) return false;
        return isAccepted(row(a, b)) || isAccepted(row(b, a));
    }

    @Override
    // Nằm trên đường đi của MỌI request feed/tìm kiếm/xem bài (lọc bài "bạn bè"),
    // và chỉ đổi khi có người kết bạn hoặc huỷ kết bạn -> đáng cache nhất trong app.
    @Cacheable(cacheNames = CacheConfig.FRIEND_IDS, key = "#userId", unless = "#userId == null")
    public java.util.List<String> friendIdsOf(String userId) {
        java.util.List<String> ids = new ArrayList<>();
        if (userId == null) return ids;
        for (Followers f : followRepository.findAcceptedOf(userId)) {
            String a = f.getUsers1() != null ? f.getUsers1().getUserId() : null;
            String b = f.getUsers2() != null ? f.getUsers2().getUserId() : null;
            String other = userId.equals(a) ? b : a;
            if (other != null && !ids.contains(other)) ids.add(other);
        }
        return ids;
    }

    @Override
    public String friendStatus(String userId) throws Exception {
        String me = authorizePathService.getUserIdAuthoried();
        if (me.equals(userId)) return "self";
        if (blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(me, userId)) return "blocked_out";
        if (blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(userId, me)) return "blocked_in";
        Followers out = row(me, userId);
        Followers in = row(userId, me);
        if (isAccepted(out) || isAccepted(in)) return "friends";
        if (isPending(out)) return "pending_out";
        if (isPending(in)) return "pending_in";
        return "none";
    }

    @Override
    public boolean sendRequest(String userId) throws Exception {
        String me = authorizePathService.getUserIdAuthoried();
        if (me.equals(userId)) throw new Exception("Không thể tự kết bạn với chính mình");
        if (isBlockedEither(me, userId)) throw new Exception("Không thể gửi lời mời tới người này");
        Users other = requireUser(userId, "The user isnt existed");
        if (other.getDeactivated() != null && other.getDeactivated() == 1) {
            throw new Exception("Người dùng này hiện không khả dụng");
        }
        Users meUser = userRepository.findFirstByUserId(me);

        Followers out = row(me, userId);
        Followers in = row(userId, me);
        if (isAccepted(out) || isAccepted(in)) throw new Exception("Hai người đã là bạn bè");
        if (isPending(out)) throw new Exception("Bạn đã gửi lời mời cho người này");
        if (isPending(in)) {
            // đối phương đã gửi lời mời cho mình -> chấp nhận luôn
            in.setStatus(ACCEPTED);
            followRepository.save(in);
            notificationService.push(userId, me, "FRIEND_ACCEPT", me,
                    meUser.getFullName() + " đã chấp nhận lời mời kết bạn");
            return true;
        }
        Followers f = new Followers();
        f.setFolId(new FollowerId(me, userId));
        f.setStatus(PENDING);
        followRepository.save(f);
        notificationService.push(userId, me, "FRIEND_REQUEST", me,
                meUser.getFullName() + " đã gửi cho bạn lời mời kết bạn");
        return true;
    }

    @Override
    // Xoá SẠCH cache thay vì đúng hai người: id người đang gọi nằm bên trong hàm,
    // SpEL không lấy được, mà xoá thiếu một chiều là quan hệ bạn bè lệch nhau. Kết bạn
    // xảy ra vài lần một ngày — tính lại toàn bộ cũng không tốn gì.
    @CacheEvict(cacheNames = CacheConfig.FRIEND_IDS, allEntries = true)
    public boolean acceptRequest(String requesterId) throws Exception {
        String me = authorizePathService.getUserIdAuthoried();
        Followers in = row(requesterId, me);
        if (!isPending(in)) throw new Exception("Không có lời mời kết bạn từ người này");
        in.setStatus(ACCEPTED);
        followRepository.save(in);
        Users meUser = userRepository.findFirstByUserId(me);
        notificationService.push(requesterId, me, "FRIEND_ACCEPT", me,
                meUser.getFullName() + " đã chấp nhận lời mời kết bạn");
        return true;
    }

    @Override
    public boolean declineRequest(String requesterId) throws Exception {
        String me = authorizePathService.getUserIdAuthoried();
        Followers in = row(requesterId, me);
        if (!isPending(in)) throw new Exception("Không có lời mời kết bạn từ người này");
        followRepository.delete(in);
        return true;
    }

    @Override
    public boolean cancelRequest(String userId) throws Exception {
        String me = authorizePathService.getUserIdAuthoried();
        Followers out = row(me, userId);
        if (!isPending(out)) throw new Exception("Bạn chưa gửi lời mời cho người này");
        followRepository.delete(out);
        return true;
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.FRIEND_IDS, allEntries = true)
    public boolean unfriend(String userId) throws Exception {
        String me = authorizePathService.getUserIdAuthoried();
        Followers out = row(me, userId);
        Followers in = row(userId, me);
        boolean done = false;
        if (isAccepted(out)) { followRepository.delete(out); done = true; }
        if (isAccepted(in)) { followRepository.delete(in); done = true; }
        if (!done) throw new Exception("Hai người chưa là bạn bè");
        return true;
    }

    @Override
    public FollowDTO getFriends(String userId) throws Exception {
        requireUser(userId, "User is not found");
        List<String> ids = new ArrayList<>();
        for (Followers f : followRepository.findAcceptedOf(userId)) {
            String a = f.getUsers1() != null ? f.getUsers1().getUserId() : null;
            String b = f.getUsers2() != null ? f.getUsers2().getUserId() : null;
            String other = userId.equals(a) ? b : a;
            if (other != null && !ids.contains(other)) ids.add(other);
        }
        return withUsers(new FollowDTO(ids.size(), ids));
    }

    @Override
    public FollowDTO getIncomingRequests() throws Exception {
        String me = authorizePathService.getUserIdAuthoried();
        List<String> ids = new ArrayList<>();
        for (Followers f : followRepository.findAllByUsers2_UserIdAndStatus(me, PENDING)) {
            if (f.getUsers1() != null) ids.add(f.getUsers1().getUserId());
        }
        return withUsers(new FollowDTO(ids.size(), ids));
    }

    @Override
    public FollowDTO getOutgoingRequests() throws Exception {
        String me = authorizePathService.getUserIdAuthoried();
        List<String> ids = new ArrayList<>();
        for (Followers f : followRepository.findAllByUsers1_UserIdAndStatus(me, PENDING)) {
            if (f.getUsers2() != null) ids.add(f.getUsers2().getUserId());
        }
        return withUsers(new FollowDTO(ids.size(), ids));
    }

    // ----- Chặn người dùng -----

    @Override
    // Chặn vừa đổi quan hệ chặn, vừa XOÁ quan hệ bạn bè hai chiều -> phải dọn cả ba cache.
    @CacheEvict(cacheNames = {CacheConfig.FRIEND_IDS, CacheConfig.BLOCKED_IDS, CacheConfig.BLOCKER_IDS},
            allEntries = true)
    public boolean blockUser(String userId) throws Exception {
        String me = authorizePathService.getUserIdAuthoried();
        if (me.equals(userId)) throw new Exception("Không thể tự chặn chính mình");
        requireUser(userId, "The user isnt existed");
        if (blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(me, userId)) {
            return true; // đã chặn rồi -> coi như thành công
        }
        // Huỷ mọi quan hệ bạn bè / lời mời 2 chiều
        Followers out = row(me, userId);
        Followers in = row(userId, me);
        if (out != null) followRepository.delete(out);
        if (in != null) followRepository.delete(in);
        blockRepository.save(new Blocks(new BlockId(me, userId), new Date()));
        return true;
    }

    @Override
    @CacheEvict(cacheNames = {CacheConfig.BLOCKED_IDS, CacheConfig.BLOCKER_IDS}, allEntries = true)
    public boolean unblockUser(String userId) throws Exception {
        String me = authorizePathService.getUserIdAuthoried();
        if (!blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(me, userId)) {
            throw new Exception("Bạn chưa chặn người này");
        }
        blockRepository.deleteById(new BlockId(me, userId));
        return true;
    }

    @Override
    public FollowDTO getBlocked() throws Exception {
        String me = authorizePathService.getUserIdAuthoried();
        List<String> ids = blockRepository.blockedIdsOf(me);
        return withUsers(new FollowDTO(ids.size(), ids));
    }

    @Override
    public java.util.Set<String> blockRelatedIds(String userId) {
        java.util.Set<String> ids = new java.util.HashSet<>();
        if (userId == null) return ids;
        ids.addAll(blockRepository.blockedIdsOf(userId));
        ids.addAll(blockRepository.blockerIdsOf(userId));
        return ids;
    }

    @Override
    public boolean isBlockedEither(String a, String b) {
        if (a == null || b == null) return false;
        return blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(a, b)
                || blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(b, a);
    }
}
