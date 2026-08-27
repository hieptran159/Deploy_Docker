package com.didan.social.repository;

import com.didan.social.entity.Notifications;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notifications, String> {
    List<Notifications> findTop50ByRecipientIdOrderByCreatedAtDesc(String recipientId);
    // Phân trang (trang thông báo đầy đủ). Trả List -> chỉ SELECT có LIMIT/OFFSET, không COUNT.
    List<Notifications> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);
    List<Notifications> findByRecipientIdAndIsRead(String recipientId, int isRead);
    long countByRecipientIdAndIsRead(String recipientId, int isRead);
    Notifications findFirstByRecipientIdAndTypeAndTargetIdAndIsRead(String recipientId, String type, String targetId, int isRead);
    Notifications findFirstByRecipientIdAndActorIdAndTypeAndTargetIdAndIsRead(String recipientId, String actorId, String type, String targetId, int isRead);
}
