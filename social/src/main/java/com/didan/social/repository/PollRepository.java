package com.didan.social.repository;

import com.didan.social.entity.Polls;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Polls, String> {

    /** Nạp theo LÔ cho cả feed — mỗi bài một truy vấn là N+1. */
    List<Polls> findByPostIdIn(Collection<String> postIds);

    Polls findFirstByPostId(String postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM polls p WHERE p.postId = :postId")
    void deleteByPostId(@Param("postId") String postId);
}
