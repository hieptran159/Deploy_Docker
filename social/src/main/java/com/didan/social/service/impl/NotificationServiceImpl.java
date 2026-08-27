package com.didan.social.service.impl;

import com.didan.social.dto.NotificationDTO;
import com.didan.social.entity.Notifications;
import com.didan.social.entity.Users;
import com.didan.social.repository.NotificationRepository;
import com.didan.social.repository.UserRepository;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuthorizePathService authorizePathService;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   AuthorizePathService authorizePathService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.authorizePathService = authorizePathService;
    }

    @Override
    public void push(String recipientId, String actorId, String type, String targetId, String message) {
        try {
            if (!StringUtils.hasText(recipientId) || recipientId.equals(actorId)) {
                return; // không tự thông báo cho chính mình
            }
            Notifications n = new Notifications();
            n.setNotificationId(UUID.randomUUID().toString());
            n.setRecipientId(recipientId);
            n.setActorId(actorId);
            n.setType(type);
            n.setTargetId(targetId);
            n.setMessage(message);
            n.setIsRead(0);
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            n.setCreatedAt(Timestamp.valueOf(now));
            notificationRepository.save(n);
        } catch (Exception e) {
            // thông báo là phụ, không được làm hỏng luồng chính
            logger.error("Could not push notification: " + e.getMessage());
        }
    }

    @Override
    public void pushUnique(String recipientId, String actorId, String type, String targetId, String message) {
        try {
            if (!StringUtils.hasText(recipientId) || recipientId.equals(actorId)) {
                return;
            }
            Notifications existing = notificationRepository
                    .findFirstByRecipientIdAndTypeAndTargetIdAndIsRead(recipientId, type, targetId, 0);
            if (existing != null) {
                return; // đã có thông báo chưa đọc cùng loại/đối tượng -> không spam thêm
            }
        } catch (Exception e) {
            logger.error("pushUnique check failed: " + e.getMessage());
        }
        push(recipientId, actorId, type, targetId, message);
    }

    @Override
    public List<NotificationDTO> listMine() throws Exception {
        String myId = authorizePathService.getUserIdAuthoried();
        List<Notifications> items = notificationRepository.findTop50ByRecipientIdOrderByCreatedAtDesc(myId);
        List<NotificationDTO> result = new ArrayList<>();
        for (Notifications n : items) {
            NotificationDTO dto = new NotificationDTO();
            dto.setNotificationId(n.getNotificationId());
            dto.setActorId(n.getActorId());
            dto.setType(n.getType());
            dto.setTargetId(n.getTargetId());
            dto.setMessage(n.getMessage());
            dto.setRead(n.getIsRead() == 1);
            dto.setCreatedAt(n.getCreatedAt() == null ? null : n.getCreatedAt().toString());
            if (StringUtils.hasText(n.getActorId())) {
                Users actor = userRepository.findFirstByUserId(n.getActorId());
                if (actor != null) {
                    dto.setActorName(actor.getFullName());
                    dto.setActorAvatar(actor.getAvtUrl());
                }
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    public long unreadCountMine() throws Exception {
        String myId = authorizePathService.getUserIdAuthoried();
        return notificationRepository.countByRecipientIdAndIsRead(myId, 0);
    }

    @Override
    public void markAllRead() throws Exception {
        String myId = authorizePathService.getUserIdAuthoried();
        List<Notifications> unread = notificationRepository.findByRecipientIdAndIsRead(myId, 0);
        for (Notifications n : unread) {
            n.setIsRead(1);
        }
        notificationRepository.saveAll(unread);
    }

    @Override
    public void pushUniquePerActor(String recipientId, String actorId, String type, String targetId, String message) {
        try {
            if (!StringUtils.hasText(recipientId) || recipientId.equals(actorId)) {
                return;
            }
            Notifications existing = notificationRepository
                    .findFirstByRecipientIdAndActorIdAndTypeAndTargetIdAndIsRead(recipientId, actorId, type, targetId, 0);
            if (existing != null) {
                return;
            }
        } catch (Exception e) {
            logger.error("pushUniquePerActor check failed: " + e.getMessage());
        }
        push(recipientId, actorId, type, targetId, message);
    }

    @Override
    public int markReadByTarget(String targetId) throws Exception {
        String myId = authorizePathService.getUserIdAuthoried();
        List<Notifications> unread = notificationRepository.findByRecipientIdAndIsRead(myId, 0);
        List<Notifications> matched = new ArrayList<>();
        for (Notifications n : unread) {
            if (targetId != null && targetId.equals(n.getTargetId())) {
                n.setIsRead(1);
                matched.add(n);
            }
        }
        if (!matched.isEmpty()) {
            notificationRepository.saveAll(matched);
        }
        return matched.size();
    }

    @Override
    public void markRead(String notificationId) throws Exception {
        String myId = authorizePathService.getUserIdAuthoried();
        Notifications n = notificationRepository.findById(notificationId).orElse(null);
        if (n == null || !myId.equals(n.getRecipientId())) {
            throw new Exception("Notification is not found");
        }
        n.setIsRead(1);
        notificationRepository.save(n);
    }
}
