package com.didan.social.service.impl;

import com.didan.social.entity.Bookmarks;
import com.didan.social.entity.Posts;
import com.didan.social.entity.keys.BookmarkId;
import com.didan.social.repository.BookmarkRepository;
import com.didan.social.repository.PostRepository;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.BookmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookmarkServiceImpl implements BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final AuthorizePathService authorizePathService;

    @Autowired
    public BookmarkServiceImpl(BookmarkRepository bookmarkRepository,
                               PostRepository postRepository,
                               AuthorizePathService authorizePathService) {
        this.bookmarkRepository = bookmarkRepository;
        this.postRepository = postRepository;
        this.authorizePathService = authorizePathService;
    }

    @Override
    public boolean toggle(String postId) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Posts post = postRepository.findFirstByPostId(postId);
        if (post == null) {
            throw new Exception("Không tìm thấy bài viết");
        }
        Bookmarks existing = bookmarkRepository.findFirstByBookmarkId_PostIdAndBookmarkId_UserId(postId, userId);
        if (existing != null) {
            bookmarkRepository.delete(existing);
            return false;
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Bookmarks b = new Bookmarks(new BookmarkId(postId, userId), Timestamp.valueOf(now));
        bookmarkRepository.save(b);
        return true;
    }

    @Override
    public boolean isBookmarked(String postId) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        return bookmarkRepository.findFirstByBookmarkId_PostIdAndBookmarkId_UserId(postId, userId) != null;
    }

    @Override
    public Map<String, Object> listMine() throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        List<String> ids = bookmarkedPostIds(userId);
        Map<String, Object> out = new HashMap<>();
        out.put("postId", ids);
        out.put("quantity", ids.size());
        return out;
    }

    @Override
    public List<String> bookmarkedPostIds(String userId) {
        List<String> ids = new ArrayList<>();
        for (Bookmarks b : bookmarkRepository.findAllByBookmarkId_UserIdOrderByCreatedAtDesc(userId)) {
            if (b.getBookmarkId() != null) ids.add(b.getBookmarkId().getPostId());
        }
        return ids;
    }
}
