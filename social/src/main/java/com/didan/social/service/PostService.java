package com.didan.social.service;

import com.didan.social.dto.PostDTO;
import com.didan.social.payload.request.CreatePostRequest;
import com.didan.social.payload.request.EditPostRequest;

import java.util.List;

public interface PostService {
    // Tạo 1 bài viết
    String createPost(CreatePostRequest createPostRequest) throws Exception;

    // Lấy tất cả các bài viết
    List<PostDTO> getAllPosts() throws Exception;

    // Lấy tất cả các bài viết theo trang
    List<PostDTO> getAllPostsByPage(int index) throws Exception;

    // Tổng số trang của feed (kích thước trang 10)
    java.util.Map<String, Object> feedPageInfo() throws Exception;

    // Bảng tin bạn bè: bài + lượt chia sẻ của bạn bè (đã chấp nhận) và của chính mình
    List<PostDTO> getFriendsFeed(int index) throws Exception;
    java.util.Map<String, Object> friendsFeedPageInfo() throws Exception;

    // Lấy bài viết theo id
    PostDTO getPostById(String postId) throws Exception;

    // Bản nháp của tôi (phân trang, mới nhất trước); trả {items,total,page,totalPages}
    java.util.Map<String, Object> getMyDrafts(int page, int size) throws Exception;
    // Đăng một bản nháp (đổi status + cập nhật postedAt = bây giờ)
    boolean publishPost(String postId) throws Exception;
    // Đăng TẤT CẢ bản nháp của tôi có đủ tiêu đề + nội dung; trả {published,skipped}
    java.util.Map<String, Object> publishAllDrafts() throws Exception;
    // Xoá TẤT CẢ bản nháp của tôi; trả số đã xoá
    int deleteAllDrafts() throws Exception;

    // Chia sẻ (repost) bài viết
    boolean repost(String postId, String note) throws Exception;
    boolean unrepost(String postId) throws Exception;
    // Danh sách bài mà userId đã chia sẻ (mới nhất trước), phân trang; kèm total/totalPages
    java.util.Map<String, Object> getRepostsOf(String userId, int page, int size) throws Exception;

    // Bài theo hashtag (mới nhất trước), phân trang; kèm total/totalPages/tag
    java.util.Map<String, Object> getPostsByTag(String tag, int page, int size) throws Exception;

    // Hashtag phổ biến: [{tag, count}], nhiều nhất trước
    java.util.List<java.util.Map<String, Object>> getTrendingHashtags(int limit) throws Exception;
    // Lịch sử bài đã đăng của userId (mới nhất trước), phân trang; kèm total/totalPages
    java.util.Map<String, Object> getPostsByUser(String userId, int page, int size) throws Exception;

    // Tìm kiếm bài viết theo tiêu đề / nội dung (không phân biệt hoa thường), phân trang.
    // page bắt đầu từ 0, size 1..50.
    List<PostDTO> getPostByTitle(String searchName, int page, int size) throws Exception;

    // Like bài viết
    boolean likePost(String postId, String type) throws Exception;

    // Unlike bài viết
    boolean unlikePost(String postId) throws Exception;

    // Cập nhật bài viết
    PostDTO updatePost(String postId, EditPostRequest editPostRequest) throws Exception;

    // Xóa bài viết
    boolean deletePost(String postId) throws Exception;
}
