package com.didan.social.service;

import java.util.List;
import java.util.Map;

public interface BookmarkService {
    // Bật/tắt lưu bài; trả về trạng thái sau thao tác (true = đã lưu)
    boolean toggle(String postId) throws Exception;
    // Bài viết này đã được người dùng hiện tại lưu chưa
    boolean isBookmarked(String postId) throws Exception;
    // Danh sách id bài đã lưu của người dùng hiện tại (mới nhất trước)
    Map<String, Object> listMine() throws Exception;
    List<String> bookmarkedPostIds(String userId);
}
