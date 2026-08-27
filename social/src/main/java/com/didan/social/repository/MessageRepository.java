package com.didan.social.repository;

import com.didan.social.entity.Messages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Messages, String> {
    List<Messages> findAllByConversations_ConversationIdOrderBySentAt(String conversationId);

    Messages findFirstByConversations_ConversationIdOrderBySentAtDesc(String conversationId);

    // Tin nhắn mới nhất của mỗi hội thoại trong danh sách (1 truy vấn)
    @Query("SELECT m FROM messages m WHERE m.conversations.conversationId IN :ids " +
           "AND m.sentAt = (SELECT MAX(m2.sentAt) FROM messages m2 WHERE m2.conversations.conversationId = m.conversations.conversationId)")
    List<Messages> findLatestForConversations(@Param("ids") Collection<String> ids);
}
