package com.didan.social.repository;

import com.didan.social.entity.HashtagFollows;
import com.didan.social.entity.keys.HashtagFollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HashtagFollowRepository extends JpaRepository<HashtagFollows, HashtagFollowId> {

    @Query("SELECT f.hashtagFollowId.tag FROM hashtag_follows f "
         + "WHERE f.hashtagFollowId.userId = :uid ORDER BY f.hashtagFollowId.tag ASC")
    List<String> findTagsOfUser(@Param("uid") String userId);
}
