package com.didan.social.repository;

import com.didan.social.entity.PollOptions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface PollOptionRepository extends JpaRepository<PollOptions, String> {

    List<PollOptions> findByPollIdInOrderByPositionAsc(Collection<String> pollIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM poll_options o WHERE o.pollId IN :pollIds")
    void deleteByPollIdIn(@Param("pollIds") Collection<String> pollIds);
}
