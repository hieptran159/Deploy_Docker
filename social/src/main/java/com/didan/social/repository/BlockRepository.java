package com.didan.social.repository;

import com.didan.social.entity.Blocks;
import com.didan.social.entity.keys.BlockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.didan.social.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockRepository extends JpaRepository<Blocks, BlockId> {

    boolean existsByBlockId_BlockerIdAndBlockId_BlockedId(String blockerId, String blockedId);

    List<Blocks> findByBlockId_BlockerId(String blockerId);

    // Hai truy vấn này chạy ở mọi lần dựng feed/tìm kiếm (tập loại trừ), nhưng chỉ đổi
    // khi ai đó bấm chặn/bỏ chặn -> FollowServiceImpl xoá cache ở đúng hai chỗ đó.
    @Cacheable(cacheNames = CacheConfig.BLOCKED_IDS, key = "#uid")
    @Query("SELECT b.blockId.blockedId FROM blocks b WHERE b.blockId.blockerId = :uid")
    List<String> blockedIdsOf(@Param("uid") String uid);

    @Cacheable(cacheNames = CacheConfig.BLOCKER_IDS, key = "#uid")
    @Query("SELECT b.blockId.blockerId FROM blocks b WHERE b.blockId.blockedId = :uid")
    List<String> blockerIdsOf(@Param("uid") String uid);
}
