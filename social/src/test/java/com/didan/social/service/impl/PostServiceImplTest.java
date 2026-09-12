package com.didan.social.service.impl;

import com.didan.social.dto.PollDTO;
import com.didan.social.dto.PostDTO;
import com.didan.social.entity.Posts;
import com.didan.social.entity.UserPosts;
import com.didan.social.entity.Users;
import com.didan.social.payload.request.CreatePostRequest;
import com.didan.social.repository.*;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.FollowService;
import com.didan.social.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostServiceImplTest {

    @Mock PostRepository postRepository;
    @Mock UserPostRepository userPostRepository;
    @Mock FileUploadsServiceImpl fileUploadsService;
    @Mock UserRepository userRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock CommentRepository commentRepository;
    @Mock AuthorizePathService authorizePathService;
    @Mock NotificationService notificationService;
    @Mock BlockRepository blockRepository;
    @Mock RepostRepository repostRepository;
    @Mock FollowService followService;
    @Mock PostHashtagRepository postHashtagRepository;
    @Mock BookmarkRepository bookmarkRepository;
    @Mock com.didan.social.service.PostViewThrottle postViewThrottle;
    @Mock com.didan.social.repository.PollRepository pollRepository;
    @Mock com.didan.social.repository.PollOptionRepository pollOptionRepository;
    @Mock com.didan.social.repository.PollVoteRepository pollVoteRepository;
    @Mock com.didan.social.repository.PostImageRepository postImageRepository;

    PostServiceImpl svc;
    static final String ME = "me-1";
    static final String OTHER = "other-2";

    @BeforeEach
    void setUp() throws Exception {
        svc = new PostServiceImpl(postRepository, userPostRepository, fileUploadsService, userRepository,
                postLikeRepository, commentRepository, authorizePathService, notificationService,
                blockRepository, repostRepository, followService, postHashtagRepository, bookmarkRepository,
                postViewThrottle, pollRepository, pollOptionRepository, pollVoteRepository,
                postImageRepository);
        when(authorizePathService.getUserIdAuthoried()).thenReturn(ME);
        Users me = new Users(); me.setUserId(ME); me.setFullName("Me");
        when(userRepository.findFirstByUserId(ME)).thenReturn(me);
    }

    private CreatePostRequest req(String title, String body, String draft, String visibility) {
        CreatePostRequest r = new CreatePostRequest();
        r.setTitle(title); r.setBody(body); r.setDraft(draft); r.setVisibility(visibility);
        return r;
    }

    private Posts post(String authorId, String status, String visibility, String title, String body) {
        Users author = new Users(); author.setUserId(authorId); author.setFullName(authorId);
        UserPosts up = new UserPosts(); up.setUsers(author);
        Posts p = new Posts();
        p.setPostId("p-1");
        p.setUserPos(up);
        p.setStatus(status);
        p.setVisibility(visibility);
        p.setTitle(title);
        p.setBody(body);
        return p;
    }

    // ---------- createPost ----------

    @Test
    void createPostFriendsVisibility() throws Exception {
        svc.createPost(req("T", "B", null, "friends"));
        ArgumentCaptor<Posts> c = ArgumentCaptor.forClass(Posts.class);
        verify(postRepository).save(c.capture());
        assertEquals("friends", c.getValue().getVisibility());
        assertEquals("published", c.getValue().getStatus());
    }

    @Test
    void createPostDefaultsToPublic() throws Exception {
        svc.createPost(req("T", "B", null, null));
        ArgumentCaptor<Posts> c = ArgumentCaptor.forClass(Posts.class);
        verify(postRepository).save(c.capture());
        assertEquals("public", c.getValue().getVisibility());
    }

    @Test
    void createPostPrivateVisibility() throws Exception {
        svc.createPost(req("T", "B", null, "private"));
        ArgumentCaptor<Posts> c = ArgumentCaptor.forClass(Posts.class);
        verify(postRepository).save(c.capture());
        assertEquals("private", c.getValue().getVisibility());
    }

    @Test
    void createDraftWithOnlyTitleOk() throws Exception {
        svc.createPost(req("Chỉ tiêu đề", null, "true", null));
        ArgumentCaptor<Posts> c = ArgumentCaptor.forClass(Posts.class);
        verify(postRepository).save(c.capture());
        assertEquals("draft", c.getValue().getStatus());
    }

    @Test
    void createDraftWithNothingRejected() {
        Exception e = assertThrows(Exception.class, () -> svc.createPost(req(null, "  ", "true", null)));
        assertTrue(e.getMessage().contains("Bản nháp"));
        verify(postRepository, never()).save(any());
    }

    @Test
    void createPublishedMissingBodyRejected() {
        Exception e = assertThrows(Exception.class, () -> svc.createPost(req("T", "", null, null)));
        assertTrue(e.getMessage().contains("Miss some fields"));
    }

    // ---------- publishPost ----------

    @Test
    void deleteByNonAuthorRejected() {
        when(postRepository.findFirstByPostId("p-1")).thenReturn(post(OTHER, "published", "public", "T", "B"));
        when(userPostRepository.findFirstByPosts_PostIdAndUsers_UserId("p-1", ME)).thenReturn(null);
        Exception e = assertThrows(Exception.class, () -> svc.deletePost("p-1"));
        assertTrue(e.getMessage().contains("không có quyền"));
        verify(postRepository, never()).delete(any());
    }

    @Test
    void publishByNonAuthorRejected() {
        when(postRepository.findFirstByPostId("p-1")).thenReturn(post(OTHER, "draft", "public", "T", "B"));
        Exception e = assertThrows(Exception.class, () -> svc.publishPost("p-1"));
        assertTrue(e.getMessage().contains("không có quyền"));
    }

    @Test
    void publishAlreadyPublishedRejected() {
        when(postRepository.findFirstByPostId("p-1")).thenReturn(post(ME, "published", "public", "T", "B"));
        Exception e = assertThrows(Exception.class, () -> svc.publishPost("p-1"));
        assertTrue(e.getMessage().contains("đã được đăng"));
    }

    @Test
    void publishDraftMissingContentRejected() {
        when(postRepository.findFirstByPostId("p-1")).thenReturn(post(ME, "draft", "public", "T", ""));
        Exception e = assertThrows(Exception.class, () -> svc.publishPost("p-1"));
        assertTrue(e.getMessage().contains("tiêu đề và nội dung"));
    }

    @Test
    void publishValidDraftSetsPublished() throws Exception {
        Posts p = post(ME, "draft", "public", "T", "B");
        when(postRepository.findFirstByPostId("p-1")).thenReturn(p);
        assertTrue(svc.publishPost("p-1"));
        assertEquals("published", p.getStatus());
        verify(postRepository).save(p);
    }

    // ---------- repost ----------

    @Test
    void repostFriendsOnlyRejected() {
        when(postRepository.findFirstByPostId("p-1")).thenReturn(post(OTHER, "published", "friends", "T", "B"));
        Exception e = assertThrows(Exception.class, () -> svc.repost("p-1", null));
        assertTrue(e.getMessage().contains("chỉ bạn bè"));
    }

    @Test
    void repostPrivateRejected() {
        when(postRepository.findFirstByPostId("p-1")).thenReturn(post(OTHER, "published", "private", "T", "B"));
        Exception e = assertThrows(Exception.class, () -> svc.repost("p-1", null));
        assertTrue(e.getMessage().contains("chỉ mình tôi"));
    }

    @Test
    void repostOwnPostRejected() {
        when(postRepository.findFirstByPostId("p-1")).thenReturn(post(ME, "published", "public", "T", "B"));
        Exception e = assertThrows(Exception.class, () -> svc.repost("p-1", null));
        assertTrue(e.getMessage().contains("tự chia sẻ"));
    }

    @Test
    void repostDraftRejected() {
        when(postRepository.findFirstByPostId("p-1")).thenReturn(post(OTHER, "draft", "public", "T", "B"));
        Exception e = assertThrows(Exception.class, () -> svc.repost("p-1", null));
        assertTrue(e.getMessage().contains("Không thể chia sẻ"));
    }

    @Test
    void repostIdempotentWhenAlreadyReposted() throws Exception {
        when(postRepository.findFirstByPostId("p-1")).thenReturn(post(OTHER, "published", "public", "T", "B"));
        when(repostRepository.existsByRepostId_UserIdAndRepostId_PostId(ME, "p-1")).thenReturn(true);
        assertTrue(svc.repost("p-1", null));
        verify(repostRepository, never()).save(any());
    }

    @Test
    void repostValidSavesAndNotifies() throws Exception {
        when(postRepository.findFirstByPostId("p-1")).thenReturn(post(OTHER, "published", "public", "T", "B"));
        when(repostRepository.existsByRepostId_UserIdAndRepostId_PostId(ME, "p-1")).thenReturn(false);
        assertTrue(svc.repost("p-1", "  hay  "));
        verify(repostRepository).save(any());
        verify(notificationService).pushUniquePerActor(eq(OTHER), eq(ME), eq("REPOST"), eq("p-1"), anyString());
    }

    // ---------- deletePost ----------

    /**
     * Entity Posts chỉ cascade sang user_posts / post_likes / user_comment.
     * bookmarks có khoá ngoại trỏ tới posts nhưng KHÔNG được cascade, nên nếu
     * deletePost không tự dọn thì mọi bài đã có người lưu đều xoá thất bại.
     * reposts không có khoá ngoại nhưng bỏ sót sẽ để lại hàng mồ côi trong feed.
     */
    @Test
    void deletePostClearsBookmarksHashtagsAndRepostsBeforeDeleting() throws Exception {
        Posts p = post(ME, "published", "public", "T", "B");
        when(postRepository.findFirstByPostId("p-1")).thenReturn(p);
        when(userPostRepository.findFirstByPosts_PostIdAndUsers_UserId("p-1", ME)).thenReturn(new UserPosts());
        when(commentRepository.findCommentIdNotInUserComment()).thenReturn(java.util.Collections.emptyList());

        assertTrue(svc.deletePost("p-1"));

        org.mockito.InOrder ord = inOrder(postHashtagRepository, bookmarkRepository, repostRepository, postRepository);
        ord.verify(postHashtagRepository).deleteByPostHashtagId_PostId("p-1");
        ord.verify(bookmarkRepository).deleteByBookmarkId_PostId("p-1");
        ord.verify(repostRepository).deleteByRepostId_PostId("p-1");
        ord.verify(postRepository).delete(p);
    }

    @Test
    void deletePostRejectsNonAuthor() {
        when(postRepository.findFirstByPostId("p-1")).thenReturn(post(OTHER, "published", "public", "T", "B"));
        when(userPostRepository.findFirstByPosts_PostIdAndUsers_UserId("p-1", ME)).thenReturn(null);
        Exception e = assertThrows(Exception.class, () -> svc.deletePost("p-1"));
        assertTrue(e.getMessage().contains("không có quyền"));
        verify(postRepository, never()).delete(any());
        verify(bookmarkRepository, never()).deleteByBookmarkId_PostId(anyString());
    }

    /* ---------- Bình chọn ---------- */

    /**
     * polls.post_id có khoá ngoại trỏ vào posts, y hệt bookmarks. Bỏ sót là xoá bài
     * thất bại 503 — đúng cái bẫy bookmarks đã gây ra một lần rồi. Thứ tự cũng quan
     * trọng: phiếu -> phương án -> cuộc bình chọn -> bài.
     */
    @Test
    void deletePostClearsPollTablesInForeignKeyOrder() throws Exception {
        Posts p = post(ME, "published", "public", "T", "B");
        when(postRepository.findFirstByPostId("p-1")).thenReturn(p);
        when(userPostRepository.findFirstByPosts_PostIdAndUsers_UserId("p-1", ME)).thenReturn(new UserPosts());
        when(commentRepository.findCommentIdNotInUserComment()).thenReturn(java.util.Collections.emptyList());
        when(pollRepository.findFirstByPostId("p-1"))
                .thenReturn(new com.didan.social.entity.Polls("poll-1", "p-1", new java.util.Date()));

        assertTrue(svc.deletePost("p-1"));

        org.mockito.InOrder ord = inOrder(pollVoteRepository, pollOptionRepository, pollRepository, postRepository);
        ord.verify(pollVoteRepository).deleteByPollIdIn(java.util.List.of("poll-1"));
        ord.verify(pollOptionRepository).deleteByPollIdIn(java.util.List.of("poll-1"));
        ord.verify(pollRepository).deleteByPostId("p-1");
        ord.verify(postRepository).delete(p);
    }

    @Test
    void deletePostKhongCoBinhChonThiKhongDungToiBangPoll() throws Exception {
        Posts p = post(ME, "published", "public", "T", "B");
        when(postRepository.findFirstByPostId("p-1")).thenReturn(p);
        when(userPostRepository.findFirstByPosts_PostIdAndUsers_UserId("p-1", ME)).thenReturn(new UserPosts());
        when(commentRepository.findCommentIdNotInUserComment()).thenReturn(java.util.Collections.emptyList());
        when(pollRepository.findFirstByPostId("p-1")).thenReturn(null);

        assertTrue(svc.deletePost("p-1"));

        verify(pollVoteRepository, never()).deleteByPollIdIn(any());
        verify(pollOptionRepository, never()).deleteByPollIdIn(any());
        verify(pollRepository, never()).deleteByPostId(anyString());
    }

    @Test
    void taoBaiVoiHaiPhuongAnTroLenThiTaoBinhChon() throws Exception {
        CreatePostRequest req = new CreatePostRequest();
        req.setTitle("Trưa nay ăn gì?");
        req.setBody("Chọn đi");
        req.setPollOptions(java.util.List.of("Cơm tấm", "Bún bò", "Phở"));

        assertNotNull(svc.createPost(req));

        verify(pollRepository).save(any(com.didan.social.entity.Polls.class));
        ArgumentCaptor<com.didan.social.entity.PollOptions> cap =
                ArgumentCaptor.forClass(com.didan.social.entity.PollOptions.class);
        verify(pollOptionRepository, times(3)).save(cap.capture());
        assertEquals(java.util.List.of("Cơm tấm", "Bún bò", "Phở"),
                cap.getAllValues().stream().map(com.didan.social.entity.PollOptions::getOptionText).toList());
        assertEquals(java.util.List.of(0, 1, 2),
                cap.getAllValues().stream().map(com.didan.social.entity.PollOptions::getPosition).toList(),
                "position phải giữ đúng thứ tự tác giả nhập");
    }

    @Test
    void duoiHaiPhuongAnThiKhongPhaiBinhChon() throws Exception {
        CreatePostRequest req = new CreatePostRequest();
        req.setTitle("T"); req.setBody("B");
        req.setPollOptions(java.util.List.of("Chỉ một"));
        assertNotNull(svc.createPost(req));
        verify(pollRepository, never()).save(any());

        // Phương án rỗng / chỉ khoảng trắng bị loại -> còn 1 -> vẫn không tạo
        req.setPollOptions(java.util.Arrays.asList("Có", "   ", "", null));
        assertNotNull(svc.createPost(req));
        verify(pollRepository, never()).save(any());
    }

    @Test
    void phuongAnBiLamSachVaChanSoLuong() throws Exception {
        CreatePostRequest req = new CreatePostRequest();
        req.setTitle("T"); req.setBody("B");
        java.util.List<String> many = new java.util.ArrayList<>();
        many.add("<script>alert(1)</script>");
        for (int i = 0; i < 30; i++) many.add("Phương án " + i);
        req.setPollOptions(many);

        assertNotNull(svc.createPost(req));

        ArgumentCaptor<com.didan.social.entity.PollOptions> cap =
                ArgumentCaptor.forClass(com.didan.social.entity.PollOptions.class);
        verify(pollOptionRepository, atLeastOnce()).save(cap.capture());
        assertEquals(10, cap.getAllValues().size(), "phải chặn ở 10 phương án");
        assertEquals("scriptalert(1)/script", cap.getAllValues().get(0).getOptionText(),
                "dấu < > phải bị bóc như mọi chuỗi hiển thị khác");
    }

    /** DTO bài viết có sẵn một cuộc bình chọn, dùng cho các test bỏ phiếu. */
    private PostDTO postWithPoll() {
        PollDTO poll = new PollDTO();
        poll.setPollId("poll-1");
        poll.setOptions(java.util.List.of(
                new PollDTO.Option("opt-a", "Cơm tấm", 3),
                new PollDTO.Option("opt-b", "Bún bò", 1)));
        poll.setTotalVotes(4);
        PostDTO dto = new PostDTO();
        dto.setPostId("p-1");
        dto.setPoll(poll);
        return dto;
    }

    @Test
    void boPhieuLuuDungPhieuCuaNguoiGoi() throws Exception {
        PostServiceImpl spy = spy(svc);
        doReturn(postWithPoll()).when(spy).getPostById("p-1");

        spy.vote("p-1", "opt-a");

        ArgumentCaptor<com.didan.social.entity.PollVotes> cap =
                ArgumentCaptor.forClass(com.didan.social.entity.PollVotes.class);
        verify(pollVoteRepository).save(cap.capture());
        assertEquals("poll-1", cap.getValue().getPollVoteId().getPollId());
        assertEquals(ME, cap.getValue().getPollVoteId().getUserId());
        assertEquals("opt-a", cap.getValue().getOptionId());
    }

    @Test
    void khongBoPhieuChoPhuongAnKhongThuocBinhChonNay() throws Exception {
        PostServiceImpl spy = spy(svc);
        doReturn(postWithPoll()).when(spy).getPostById("p-1");

        assertThrows(Exception.class, () -> spy.vote("p-1", "opt-cua-poll-khac"));
        verify(pollVoteRepository, never()).save(any());
    }

    /**
     * Đi qua getPostById nghĩa là dùng lại TOÀN BỘ luật hiển thị (riêng tư / bạn bè /
     * nháp / bị ẩn). Bài không xem được thì cũng không bỏ phiếu được.
     */
    @Test
    void khongBoPhieuVaoBaiKhongXemDuoc() throws Exception {
        PostServiceImpl spy = spy(svc);
        doReturn(null).when(spy).getPostById("p-1");

        assertThrows(Exception.class, () -> spy.vote("p-1", "opt-a"));
        verify(pollVoteRepository, never()).save(any());
    }

    @Test
    void baiKhongCoBinhChonThiKhongBoPhieuDuoc() throws Exception {
        PostServiceImpl spy = spy(svc);
        PostDTO khongPoll = new PostDTO(); khongPoll.setPostId("p-1");
        doReturn(khongPoll).when(spy).getPostById("p-1");

        assertThrows(Exception.class, () -> spy.vote("p-1", "opt-a"));
        verify(pollVoteRepository, never()).save(any());
    }

    @Test
    void rutPhieuXoaDungHangCuaMinh() throws Exception {
        when(pollRepository.findFirstByPostId("p-1"))
                .thenReturn(new com.didan.social.entity.Polls("poll-1", "p-1", new java.util.Date()));

        svc.unvote("p-1");

        verify(pollVoteRepository).deleteById(
                new com.didan.social.entity.keys.PollVoteId("poll-1", ME));
    }

    /* ---------- Nhiều ảnh ---------- */

    /**
     * post_images cũng có khoá ngoại trỏ vào posts — bỏ sót là xoá bài thất bại 503,
     * y hệt bookmarks và polls. Và phải xoá cả FILE, không thì để lại rác trên đĩa.
     */
    @Test
    void deletePostClearsImagesAndTheirFiles() throws Exception {
        Posts p = post(ME, "published", "public", "T", "B");
        when(postRepository.findFirstByPostId("p-1")).thenReturn(p);
        when(userPostRepository.findFirstByPosts_PostIdAndUsers_UserId("p-1", ME)).thenReturn(new UserPosts());
        when(commentRepository.findCommentIdNotInUserComment()).thenReturn(java.util.Collections.emptyList());
        when(postImageRepository.findByPostIdOrderByPositionAsc("p-1")).thenReturn(java.util.List.of(
                new com.didan.social.entity.PostImages("i-1", "p-1", "post/a.jpg", 0),
                new com.didan.social.entity.PostImages("i-2", "p-1", "post/b.jpg", 1)));

        assertTrue(svc.deletePost("p-1"));

        verify(fileUploadsService).deleteFile("post/a.jpg");
        verify(fileUploadsService).deleteFile("post/b.jpg");
        org.mockito.InOrder ord = inOrder(postImageRepository, postRepository);
        ord.verify(postImageRepository).deleteByPostId("p-1");
        ord.verify(postRepository).delete(p);
    }

    @Test
    void nhieuAnhLuuDungThuTuVaTenFileKhongDeNhau() throws Exception {
        CreatePostRequest req = new CreatePostRequest();
        req.setTitle("T"); req.setBody("B");
        req.setPostImgs(java.util.List.of(img("a.jpg"), img("b.jpg"), img("c.jpg")));
        when(fileUploadsService.storeFile(any(), eq("post"), anyString()))
                .thenAnswer(inv -> inv.getArgument(2) + ".jpg");

        assertNotNull(svc.createPost(req));

        ArgumentCaptor<com.didan.social.entity.PostImages> cap =
                ArgumentCaptor.forClass(com.didan.social.entity.PostImages.class);
        verify(postImageRepository, times(3)).save(cap.capture());
        assertEquals(java.util.List.of(0, 1, 2),
                cap.getAllValues().stream().map(com.didan.social.entity.PostImages::getPosition).toList());
        // storeFile đặt tên theo id truyền vào -> nhiều ảnh cùng postId sẽ đè lên nhau
        assertEquals(3, cap.getAllValues().stream()
                .map(com.didan.social.entity.PostImages::getUrl).distinct().count(),
                "mỗi ảnh phải ra một tên file khác nhau");
    }

    /** auto-poster và client cũ vẫn gửi postImg đơn lẻ — không được gãy. */
    @Test
    void motAnhKieuCuVanChayVaVanGhiVaoBangMoi() throws Exception {
        CreatePostRequest req = new CreatePostRequest();
        req.setTitle("T"); req.setBody("B");
        req.setPostImg(img("cu.jpg"));
        when(fileUploadsService.storeFile(any(), eq("post"), anyString())).thenReturn("x.jpg");

        assertNotNull(svc.createPost(req));

        verify(postImageRepository, times(1)).save(any());
        ArgumentCaptor<Posts> saved = ArgumentCaptor.forClass(Posts.class);
        verify(postRepository, atLeastOnce()).save(saved.capture());
        assertEquals("post/x.jpg", saved.getValue().getPostImg(),
                "cột cũ vẫn phải mang ảnh đầu tiên, làm cầu nối cho client cũ");
    }

    @Test
    void khongGuiAnhThiKhongDungToiBangAnh() throws Exception {
        CreatePostRequest req = new CreatePostRequest();
        req.setTitle("T"); req.setBody("B");
        assertNotNull(svc.createPost(req));
        verify(postImageRepository, never()).save(any());
        verify(postImageRepository, never()).deleteByPostId(anyString());
    }

    private org.springframework.web.multipart.MultipartFile img(String name) {
        return new org.springframework.mock.web.MockMultipartFile(
                "postImgs", name, "image/jpeg", new byte[]{1, 2, 3});
    }
}
