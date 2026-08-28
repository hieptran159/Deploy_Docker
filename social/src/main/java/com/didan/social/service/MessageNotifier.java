package com.didan.social.service;

import com.didan.social.entity.Conversations;
import com.didan.social.entity.Participants;
import com.didan.social.entity.Users;
import com.didan.social.repository.ParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gửi thông báo cho các thành viên khác khi có tin nhắn (cả DM lẫn nhóm).
 * - Người bị @nhắc tên (@[Tên](userId)) -> thông báo riêng, luôn hiện.
 * - Người còn lại -> pushUnique (gộp cho tới khi đọc) để không spam.
 */
@Component
public class MessageNotifier {
    private final Logger logger = LoggerFactory.getLogger(MessageNotifier.class);
    private static final Pattern MENTION = Pattern.compile("@\\[[^\\]]+\\]\\(([0-9a-fA-F\\-]{8,})\\)");

    private final NotificationService notificationService;
    private final ParticipantRepository participantRepository;

    @Autowired
    public MessageNotifier(NotificationService notificationService, ParticipantRepository participantRepository) {
        this.notificationService = notificationService;
        this.participantRepository = participantRepository;
    }

    public void notifyMessage(Conversations conv, Users sender, String content) {
        try {
            String name = conv.getConversationName();
            boolean dm = name != null && (name.startsWith("dm:") || name.startsWith("dm_"));
            String senderName = sender.getFullName();
            String generic = dm
                    ? senderName + " đã nhắn tin cho bạn"
                    : senderName + " đã nhắn tin trong nhóm " + (name != null ? name : "");

            Set<String> mentioned = new HashSet<>();
            if (content != null) {
                Matcher m = MENTION.matcher(content);
                while (m.find()) mentioned.add(m.group(1));
            }

            for (Participants p : participantRepository.findAllByConversations_ConversationId(conv.getConversationId())) {
                if (p.getUsers() == null) continue;
                String pid = p.getUsers().getUserId();
                if (pid.equals(sender.getUserId())) continue;
                boolean muted = p.getMuted() != null && p.getMuted() == 1;
                if (muted && !mentioned.contains(pid)) continue; // tắt thông báo hội thoại (vẫn báo khi bị @nhắc)
                if (mentioned.contains(pid)) {
                    notificationService.push(pid, sender.getUserId(), "MESSAGE", conv.getConversationId(),
                            senderName + " đã nhắc đến bạn trong " + (dm ? "một tin nhắn" : "nhóm " + name));
                } else {
                    notificationService.pushUnique(pid, sender.getUserId(), "MESSAGE", conv.getConversationId(), generic);
                }
            }
        } catch (Exception e) {
            logger.error("notifyMessage failed: " + e.getMessage());
        }
    }
}
