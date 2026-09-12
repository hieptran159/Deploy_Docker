package com.didan.social.repository;

import com.didan.social.entity.PollVotes;
import com.didan.social.entity.keys.PollVoteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVotes, PollVoteId> {

    /** [optionId, số phiếu] — gộp sẵn ở DB, khỏi kéo từng phiếu về đếm trong bộ nhớ. */
    @Query("SELECT v.optionId, COUNT(v) FROM poll_votes v WHERE v.pollVoteId.pollId IN :pollIds GROUP BY v.optionId")
    List<Object[]> countByOptionForPolls(@Param("pollIds") Collection<String> pollIds);

    /** Phiếu của chính người đang xem, nạp theo lô cho cả feed. */
    List<PollVotes> findByPollVoteId_PollIdInAndPollVoteId_UserId(Collection<String> pollIds, String userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM poll_votes v WHERE v.pollVoteId.pollId IN :pollIds")
    void deleteByPollIdIn(@Param("pollIds") Collection<String> pollIds);
}
