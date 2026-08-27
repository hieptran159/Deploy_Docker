package com.didan.social.service.impl;

import com.didan.social.dto.ConversationDTO;
import com.didan.social.dto.MessageDTO;
import com.didan.social.entity.*;
import com.didan.social.entity.keys.ParticipantId;
import com.didan.social.payload.request.SendMessageRequest;
import com.didan.social.repository.ConversationRepository;
import com.didan.social.repository.MessageRepository;
import com.didan.social.repository.ParticipantRepository;
import com.didan.social.repository.UserRepository;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.ChatService;
import com.didan.social.service.FileUploadsService;
import com.didan.social.socket.RealtimeGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class ChatServiceImpl implements ChatService {
    private final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final FileUploadsService fileUploadsService;
    private final MessageRepository messageRepository;
    private final AuthorizePathService authorizePathService;
    private final com.didan.social.service.NotificationService notificationService;
    private final com.didan.social.service.FollowService followService;
    private final RealtimeGateway realtimeGateway;
    @Autowired
    public ChatServiceImpl(ConversationRepository conversationRepository,
                           UserRepository userRepository,
                           ParticipantRepository participantRepository,
                           FileUploadsService fileUploadsService,
                           MessageRepository messageRepository,
                           AuthorizePathService authorizePathService,
                           com.didan.social.service.NotificationService notificationService,
                           com.didan.social.service.FollowService followService,
                           RealtimeGateway realtimeGateway){
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.fileUploadsService = fileUploadsService;
        this.messageRepository = messageRepository;
        this.authorizePathService = authorizePathService;
        this.notificationService = notificationService;
        this.followService = followService;
        this.realtimeGateway = realtimeGateway;
    }

    // Bắn thông báo "đã nhắn tin cho bạn" cho các participant khác trong hội thoại 1-1
    private void notifyDirectMessage(Conversations conversation, Users sender) {
        try {
            String name = conversation.getConversationName();
            if (name == null || !name.startsWith("dm:")) return;
            for (Participants p : participantRepository.findAllByConversations_ConversationId(conversation.getConversationId())) {
                if (p.getUsers() == null) continue;
                String pid = p.getUsers().getUserId();
                if (!pid.equals(sender.getUserId())) {
                    notificationService.pushUnique(pid, sender.getUserId(), "MESSAGE",
                            conversation.getConversationId(), sender.getFullName() + " đã nhắn tin cho bạn");
                }
            }
        } catch (Exception e) {
            logger.error("notifyDirectMessage failed: " + e.getMessage());
        }
    }
    @Override
    public String createConversation(String conversationName) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        Conversations conversation = new Conversations();
        String conversationId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Date nowSql = Timestamp.valueOf(now);
        conversation.setConversationId(conversationId);
        conversation.setConversationName(conversationName);
        conversation.setCreatedAt(nowSql);
        conversationRepository.save(conversation);
        Participants participant = new Participants();
        participant.setUsers(user);
        participant.setConversations(conversation);
        participant.setParticipantId(new ParticipantId(conversationId, user.getUserId()));
        participantRepository.save(participant);
        return conversationId;
    }

    @Override
    public ConversationDTO openDirectConversation(String otherUserId) throws Exception {
        String myId = authorizePathService.getUserIdAuthoried();
        if (myId == null || myId.equals(otherUserId)) {
            logger.error("Cannot open a direct conversation with yourself");
            throw new Exception("Cannot open a direct conversation with yourself");
        }
        Users me = userRepository.findFirstByUserId(myId);
        Users other = userRepository.findFirstByUserId(otherUserId);
        if (me == null || other == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        // Tên tất định cho cặp người dùng -> cả hai phía luôn ra cùng một phòng
        String first = myId.compareTo(otherUserId) <= 0 ? myId : otherUserId;
        String second = myId.compareTo(otherUserId) <= 0 ? otherUserId : myId;
        String directName = "dm:" + first + ":" + second;

        Conversations conversation = conversationRepository.findFirstByConversationName(directName);
        if (conversation == null) {
            conversation = new Conversations();
            conversation.setConversationId(UUID.randomUUID().toString());
            conversation.setConversationName(directName);
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            conversation.setCreatedAt(Timestamp.valueOf(now));
            conversationRepository.save(conversation);
        }
        addParticipantIfAbsent(conversation, me);
        addParticipantIfAbsent(conversation, other);

        ConversationDTO conversationDTO = new ConversationDTO();
        conversationDTO.setConversationId(conversation.getConversationId());
        conversationDTO.setConversationName(conversation.getConversationName());
        conversationDTO.setCreatedAt(conversation.getCreatedAt().toString());
        return conversationDTO;
    }

    @Override
    public boolean addMember(String conversationId, String userId) throws Exception {
        String myId = authorizePathService.getUserIdAuthoried();
        Conversations conversation = conversationRepository.findFirstByConversationId(conversationId);
        if (conversation == null) {
            logger.error("There is no conversation");
            throw new Exception("There is no conversation");
        }
        if (participantRepository.findFirstByConversations_ConversationIdAndUsers_UserId(conversationId, myId) == null) {
            logger.error("You are not in this conversation");
            throw new Exception("You are not in this conversation");
        }
        Users target = userRepository.findFirstByUserId(userId);
        if (target == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        if (participantRepository.findFirstByConversations_ConversationIdAndUsers_UserId(conversationId, userId) != null) {
            return true; // đã ở trong nhóm
        }
        if (!followService.areFriends(myId, userId)) {
            logger.error("Chỉ có thể thêm bạn bè vào nhóm");
            throw new Exception("Chỉ có thể thêm bạn bè vào nhóm");
        }
        addParticipantIfAbsent(conversation, target);
        return true;
    }

    @Override
    public List<Map<String, String>> getMembers(String conversationId) throws Exception {
        String myId = authorizePathService.getUserIdAuthoried();
        if (participantRepository.findFirstByConversations_ConversationIdAndUsers_UserId(conversationId, myId) == null) {
            logger.error("You are not in this conversation");
            throw new Exception("You are not in this conversation");
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (Participants p : participantRepository.findAllByConversations_ConversationId(conversationId)) {
            Users u = p.getUsers();
            if (u == null) continue;
            Map<String, String> m = new HashMap<>();
            m.put("userId", u.getUserId());
            m.put("fullName", u.getFullName());
            m.put("avtUrl", u.getAvtUrl());
            result.add(m);
        }
        return result;
    }

    private void addParticipantIfAbsent(Conversations conversation, Users user) {
        Participants existing = participantRepository.findFirstByConversations_ConversationIdAndUsers_UserId(
                conversation.getConversationId(), user.getUserId());
        if (existing == null) {
            Participants participant = new Participants();
            participant.setParticipantId(new ParticipantId(conversation.getConversationId(), user.getUserId()));
            participant.setConversations(conversation);
            participant.setUsers(user);
            participantRepository.save(participant);
        }
    }

    @Override
    public ConversationDTO joinConversation(String conversationId) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        Conversations conversation = conversationRepository.findFirstByConversationId(conversationId);
        if (conversation == null) {
            logger.error("There is no conversation");
            throw new Exception("There is no conversation");
        }
        Participants findParticipant = participantRepository.findFirstByConversations_ConversationIdAndUsers_UserId(conversationId, user.getUserId());
        if (findParticipant != null) {
            logger.error("User has already joined conversation");
            throw new Exception("User has already joined conversation");
        }
        Participants participant = new Participants();
        participant.setParticipantId(new ParticipantId(conversationId, user.getUserId()));
        participant.setConversations(conversation);
        participant.setUsers(user);
        participantRepository.save(participant);
        ConversationDTO conversationDTO = new ConversationDTO();
        conversationDTO.setConversationId(conversationId);
        conversationDTO.setConversationName(conversation.getConversationName());
        conversationDTO.setCreatedAt(conversation.getCreatedAt().toString());
        return conversationDTO;
    }

    @Override
    public boolean leaveConversation(String conversationId) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        Conversations conversation = conversationRepository.findFirstByConversationId(conversationId);
        if (conversation == null){
            logger.error("There is no conversation");
            throw new Exception("There is no conversation");
        }
        Participants findParticipant = participantRepository.findFirstByConversations_ConversationIdAndUsers_UserId(conversationId, user.getUserId());
        if (findParticipant == null) {
            logger.error("User has not joined conversation yet");
            throw new Exception("User has not joined conversation yet");
        }
        participantRepository.delete(findParticipant);
        return true;
    }

    @Override
    public MessageDTO sendMessage(String conversationId, SendMessageRequest sendMessageRequest) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        Conversations conversation = conversationRepository.findFirstByConversationId(conversationId);
        if (conversation == null) {
            logger.error("There is no conversation");
            throw new Exception("There is no conversation");
        }
        Participants findParticipant = participantRepository.findFirstByConversations_ConversationIdAndUsers_UserId(conversationId, user.getUserId());
        if (findParticipant == null) {
            logger.error("User has not joined conversation yet");
            throw new Exception("User has not joined conversation yet");
        }
        Messages message = new Messages();
        String messageId = UUID.randomUUID().toString();
        message.setMessageId(messageId);
        message.setContent(sendMessageRequest.getContent());
        String fileName = null;
        if (sendMessageRequest.getMessageImg() != null && !sendMessageRequest.getMessageImg().isEmpty()){
            fileName = fileUploadsService.storeFile(sendMessageRequest.getMessageImg(), "message", messageId);
            message.setMessageImg("message/"+fileName);
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Date nowSql = Timestamp.valueOf(now);
        message.setSentAt(nowSql);
        message.setUsers(user);
        message.setConversations(conversation);
        messageRepository.save(message);
        notifyDirectMessage(conversation, user);
        return new MessageDTO(messageId,
                                sendMessageRequest.getContent(),
                                "message/"+fileName,
                                nowSql.toString(),
                                conversationId,
                                user.getUserId());
    }

    @Override
    public List<ConversationDTO> getAllConversation() throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        List<Participants> participants = participantRepository.findAllByUsers_UserId(user.getUserId());
        if (participants.size() <= 0){
            logger.error("You hasnot joined any conversations yet");
            throw new Exception("You hasnot joined any conversations yet");
        }
        List<ConversationDTO> conversationDTOs = new ArrayList<>();
        for (Participants participant : participants){
            ConversationDTO conversationDTO = new ConversationDTO();
            Conversations conversation = conversationRepository.findFirstByConversationId(participant.getConversations().getConversationId());
            conversationDTO.setConversationId(conversation.getConversationId());
            conversationDTO.setConversationName(conversation.getConversationName());
            conversationDTO.setCreatedAt(conversation.getCreatedAt().toString());
            Messages last = messageRepository.findFirstByConversations_ConversationIdOrderBySentAtDesc(conversation.getConversationId());
            if (last != null) {
                conversationDTO.setLastMessage(last.getContent());
                conversationDTO.setLastMessageImg(last.getMessageImg());
                conversationDTO.setLastMessageAt(last.getSentAt() != null ? last.getSentAt().toString() : null);
                conversationDTO.setLastSenderId(last.getUsers() != null ? last.getUsers().getUserId() : null);
            }
            conversationDTOs.add(conversationDTO);
        }
        // sắp xếp theo tin nhắn mới nhất, chưa có tin thì theo ngày tạo
        conversationDTOs.sort(Comparator.comparing(
                (ConversationDTO c) -> c.getLastMessageAt() != null ? c.getLastMessageAt() : c.getCreatedAt()
        ).reversed());
        return conversationDTOs;
    }

    @Override
    public List<ConversationDTO> searchConversation(String conversationName) throws Exception {
        List<ConversationDTO> conversationDTOs = new ArrayList<>();
        List<Conversations> conversations = conversationRepository.findAllByConversationNameContainingOrderByCreatedAtDesc(conversationName);
        if (conversations.size() <= 0) {
            logger.error("Not found conversations");
            throw new Exception("Not found conversations");
        }
        for (Conversations conversation : conversations){
            ConversationDTO conversationDTO = new ConversationDTO();
            conversationDTO.setConversationId(conversation.getConversationId());
            conversationDTO.setConversationName(conversation.getConversationName());
            conversationDTO.setCreatedAt(conversation.getCreatedAt().toString());
            conversationDTOs.add(conversationDTO);
        }
        return conversationDTOs;
    }

    @Override
    public List<MessageDTO> getAllMessagesInConversation(String conversationId) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        Participants participants = participantRepository.findFirstByConversations_ConversationIdAndUsers_UserId(conversationId, user.getUserId());
        if (participants == null){
            logger.error("You hasnot joined any conversations yet");
            throw new Exception("You hasnot joined any conversations yet");
        }
        List<Messages> messages = messageRepository.findAllByConversations_ConversationIdOrderBySentAt(conversationId);
        if (messages.size() <= 0){
            return null;
        }
        List<MessageDTO> messageDTOs = new ArrayList<>();
        for (Messages message : messages){
            messageDTOs.add(toDTO(message, conversationId));
        }
        return messageDTOs;
    }

    private MessageDTO toDTO(Messages message, String conversationId) {
        boolean recalled = Boolean.TRUE.equals(message.getRecalled());
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setMessageId(message.getMessageId());
        messageDTO.setContent(recalled ? "" : message.getContent());
        messageDTO.setMessageImg(recalled ? null : message.getMessageImg());
        messageDTO.setSentAt(message.getSentAt() != null ? message.getSentAt().toString() : null);
        messageDTO.setSenderId(message.getUsers() != null ? message.getUsers().getUserId() : null);
        messageDTO.setConversationId(conversationId);
        messageDTO.setRecalled(recalled);
        return messageDTO;
    }

    @Override
    public MessageDTO editMessage(String messageId, String content) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        if (content == null || content.trim().isEmpty()) {
            throw new Exception("Nội dung tin nhắn không được để trống");
        }
        Messages message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            throw new Exception("Không tìm thấy tin nhắn");
        }
        if (message.getUsers() == null || !userId.equals(message.getUsers().getUserId())) {
            throw new Exception("Bạn không thể sửa tin nhắn của người khác");
        }
        if (Boolean.TRUE.equals(message.getRecalled())) {
            throw new Exception("Tin nhắn đã thu hồi, không thể sửa");
        }
        message.setContent(content.trim());
        messageRepository.save(message);
        String conversationId = message.getConversations() != null ? message.getConversations().getConversationId() : null;
        MessageDTO dto = toDTO(message, conversationId);
        realtimeGateway.toRoom(conversationId, "message_updated", dto);
        return dto;
    }

    @Override
    public MessageDTO recallMessage(String messageId) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Messages message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            throw new Exception("Không tìm thấy tin nhắn");
        }
        if (message.getUsers() == null || !userId.equals(message.getUsers().getUserId())) {
            throw new Exception("Bạn không thể thu hồi tin nhắn của người khác");
        }
        if (!Boolean.TRUE.equals(message.getRecalled()) && message.getMessageImg() != null) {
            try { fileUploadsService.deleteFile(message.getMessageImg()); } catch (Exception ignore) { }
        }
        message.setRecalled(true);
        message.setContent("");
        message.setMessageImg(null);
        messageRepository.save(message);
        String conversationId = message.getConversations() != null ? message.getConversations().getConversationId() : null;
        MessageDTO dto = toDTO(message, conversationId);
        realtimeGateway.toRoom(conversationId, "message_updated", dto);
        return dto;
    }
}
