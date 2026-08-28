package com.didan.social.repository;

import com.didan.social.entity.MessageReactions;
import com.didan.social.entity.keys.MessageReactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReactions, MessageReactionId> {

    List<MessageReactions> findByMessageReactionId_MessageId(String messageId);

    List<MessageReactions> findByMessageReactionId_MessageIdIn(Collection<String> messageIds);

    MessageReactions findByMessageReactionId_MessageIdAndMessageReactionId_UserId(String messageId, String userId);

    @Transactional
    void deleteByMessageReactionId_MessageIdAndMessageReactionId_UserId(String messageId, String userId);
}
