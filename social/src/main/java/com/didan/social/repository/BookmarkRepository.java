package com.didan.social.repository;

import com.didan.social.entity.Bookmarks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmarks, com.didan.social.entity.keys.BookmarkId> {
    Bookmarks findFirstByBookmarkId_PostIdAndBookmarkId_UserId(String postId, String userId);
    List<Bookmarks> findAllByBookmarkId_UserIdOrderByCreatedAtDesc(String userId);
}
