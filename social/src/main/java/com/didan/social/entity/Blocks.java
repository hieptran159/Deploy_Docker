package com.didan.social.entity;

import com.didan.social.entity.keys.BlockId;
import jakarta.persistence.*;

import java.util.Date;

/**
 * A (blockerId) đã chặn B (blockedId). Quan hệ một chiều.
 * Bảng tự tạo bởi ddl-auto=update.
 */
@Entity(name = "blocks")
@Table(name = "blocks", indexes = {
        @Index(name = "idx_blocks_blocked", columnList = "blocked_id")
})
public class Blocks {
    @EmbeddedId
    private BlockId blockId;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    public Blocks() {
    }

    public Blocks(BlockId blockId, Date createdAt) {
        this.blockId = blockId;
        this.createdAt = createdAt;
    }

    public BlockId getBlockId() { return blockId; }
    public void setBlockId(BlockId blockId) { this.blockId = blockId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
