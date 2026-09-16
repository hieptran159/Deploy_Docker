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
import java.util.Date;
import java.util.List;
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
    @Mock com.didan.social.repository.HashtagFollowRepository hashtagFollowRepository;
    @Mock com.didan.social.repository.CategoryRepository categoryRepository;

    PostServiceImpl svc;
    static final String ME = "me-1";
    static final String OTHER = "other-2";

    @BeforeEach
    void setUp() throws Exception {
        svc = new PostServiceImpl(postRepository, userPostRepository, fileUploadsService, userRepository,
                postLikeRepository, commentRepository, authorizePathService, notificationService,
                blockRepository, repostRepository, followService, postHashtagRepository, bookmarkRepository,
                postViewThrottle, pollRepository, pollOptionRepository, pollVoteRepository,
                postImageRepository, hashtagFollowRepository, categoryRepository);
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
        // basePostDTO() duyệt hai tập này bằng .stream()/.size() — rỗng thay vì null,
        // để bài test nào lỡ đi tới convertToDTO() (như updatePost) không NPE.
        p.setPostLikes(new java.util.HashSet<>());
        p.setUserComments(new java.util.HashSet<>());
        return p;
    }

    // ---------- createPost ----------

    @Test
    void createPostFriendsVisibility() throws Exception {
        // Đã có bài lọt duyệt trước đó -> đăng thẳng, không rơi vào "pending" của
        // người mới. Test này kiểm tra VISIBILITY, không phải luật duyệt lần đầu —
        // mock rõ ràng để không phụ thuộc giá trị mặc định 0 của Mockito cho long.
        when(postRepository.countEverPublishedByAuthor(ME)).thenReturn(1L);
        svc.createPost(req("T", "B", null, "friends"));
        ArgumentCaptor<Posts> c = ArgumentCaptor.forClass(Posts.class);
        verify(postRepository).save(c.capture());
        assertEquals("friends", c.getValue().getVisibility());
        assertEquals("published", c.getValue().getStatus());
    }

    // ---- Duyệt bài lần đầu (giống nội quy AowVN: chỉ chặn bài ĐẦU TIÊN) ----

    @Test
    void baiDauTienCuaTaiKhoanMoiThiChoDuyet() throws Exception {
        when(postRepository.countEverPublishedByAuthor(ME)).thenReturn(0L);
        svc.createPost(req("T", "B", null, null));
        ArgumentCaptor<Posts> c = ArgumentCaptor.forClass(Posts.class);
        verify(postRepository).save(c.capture());
        assertEquals("pending", c.getValue().getStatus());
    }

    @Test
    void tuBaiThuHaiDangThangKhongCanDuyet() throws Exception {
        when(postRepository.countEverPublishedByAuthor(ME)).thenReturn(3L);
        svc.createPost(req("T", "B", null, null));
        ArgumentCaptor<Posts> c = ArgumentCaptor.forClass(Posts.class);
        verify(postRepository).save(c.capture());
        assertEquals("published", c.getValue().getStatus());
    }

    @Test
    void banNhapKhongBiChoDuyetDuKhachChuaCoBaiNao() throws Exception {
        // Nháp không lên feed nên không cần qua hàng chờ; đăng luôn phải mock lại
        // vì countEverPublishedByAuthor mặc định vẫn 0.
        when(postRepository.countEverPublishedByAuthor(ME)).thenReturn(0L);
        svc.createPost(req("Chỉ tiêu đề", null, "true", null));
        ArgumentCaptor<Posts> c = ArgumentCaptor.forClass(Posts.class);
        verify(postRepository).save(c.capture());
        assertEquals("draft", c.getValue().getStatus());
    }

    // ---- Chuyên mục ----

    @Test
    void chonChuyenMucHopLeThiLuuLai() throws Exception {
        when(postRepository.countEverPublishedByAuthor(ME)).thenReturn(1L);
        when(categoryRepository.existsById("cat-1")).thenReturn(true);
        CreatePostRequest r = req("T", "B", null, null);
        r.setCategoryId("cat-1");
        svc.createPost(r);
        ArgumentCaptor<Posts> c = ArgumentCaptor.forClass(Posts.class);
        verify(postRepository).save(c.capture());
        assertEquals("cat-1", c.getValue().getCategoryId());
    }

    @Test
    void chuyenMucKhongTonTaiThiBoQuaKhongChanDangBai() throws Exception {
        when(postRepository.countEverPublishedByAuthor(ME)).thenReturn(1L);
        when(categoryRepository.existsById("da-bi-xoa")).thenReturn(false);
        CreatePostRequest r = req("T", "B", null, null);
        r.setCategoryId("da-bi-xoa");
        svc.createPost(r);
        ArgumentCaptor<Posts> c = ArgumentCaptor.forClass(Posts.class);
        verify(postRepository).save(c.capture());
        assertNull(c.getValue().getCategoryId());
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

    /* ---------- Theo dõi hashtag ---------- */

    @Test
    void theoDoiHashtagChuanHoaTagTruocKhiLuu() throws Exception {
        svc.followTag("  #TIN_Tuc  ");

        ArgumentCaptor<com.didan.social.entity.HashtagFollows> cap =
                ArgumentCaptor.forClass(com.didan.social.entity.HashtagFollows.class);
        verify(hashtagFollowRepository).save(cap.capture());
        assertEquals(ME, cap.getValue().getHashtagFollowId().getUserId());
        assertEquals("tin_tuc", cap.getValue().getHashtagFollowId().getTag(),
                "phải bỏ #, cắt khoảng trắng và hạ chữ thường như HashtagUtils");
    }

    @Test
    void tagKhongHopLeThiTuChoi() {
        assertThrows(Exception.class, () -> svc.followTag("có khoảng trắng"));
        // HashtagUtils chỉ nhận [chữ, số, _] — gạch ngang không hợp lệ
        assertThrows(Exception.class, () -> svc.followTag("tin-tuc"));
        assertThrows(Exception.class, () -> svc.followTag("12345"));
        assertThrows(Exception.class, () -> svc.followTag(null));
        verify(hashtagFollowRepository, never()).save(any());
    }

    @Test
    void boTheoDoiXoaDungHangCuaMinh() throws Exception {
        svc.unfollowTag("#Tin");
        verify(hashtagFollowRepository).deleteById(
                new com.didan.social.entity.keys.HashtagFollowId(ME, "tin"));
    }

    /**
     * `IN ()` với tập rỗng là SQL không hợp lệ. Chưa theo dõi tag nào thì phải trả
     * về rỗng chứ không được chạm vào truy vấn.
     */
    @Test
    void chuaTheoDoiTagNaoThiKhongChayTruyVanIN() throws Exception {
        when(hashtagFollowRepository.findTagsOfUser(ME)).thenReturn(java.util.List.of());

        java.util.Map<String, Object> m = svc.getFollowedTagsFeed(0, 10);

        assertEquals(0L, m.get("total"));
        assertTrue(((java.util.List<?>) m.get("items")).isEmpty());
        verify(postHashtagRepository, never()).findPostsByTags(any(), any(), anyString(), any());
        verify(postHashtagRepository, never()).countPostsByTags(any(), any(), anyString());
    }

    @Test
    void feedHashtagDungDungCacTagDangTheoDoi() throws Exception {
        when(hashtagFollowRepository.findTagsOfUser(ME)).thenReturn(java.util.List.of("tin", "vuejs"));
        when(postHashtagRepository.findPostsByTags(any(), any(), anyString(), any()))
                .thenReturn(java.util.List.of());
        when(postHashtagRepository.countPostsByTags(any(), any(), anyString())).thenReturn(0L);

        java.util.Map<String, Object> m = svc.getFollowedTagsFeed(0, 10);

        assertEquals(java.util.List.of("tin", "vuejs"), m.get("tags"));
        verify(postHashtagRepository).findPostsByTags(eq(java.util.List.of("tin", "vuejs")),
                any(), anyString(), any());
    }

    // ---- Lượt xem của KHÁCH phải phân biệt theo IP ----
    // Nhánh lấy IP trong countView từng là code chết: getUserIdAuthoried() trả về chuỗi
    // "anonymousUser" nên meId không bao giờ null, mọi khách dùng chung một khoá chống
    // trùng, và mỗi bài chỉ đếm được 1 lượt xem khách trong mỗi 30 phút.

    private Posts postDeDem(String id) {
        Posts p = new Posts();
        p.setPostId(id);
        p.setViews(0);
        return p;
    }

    private void datIpCuaRequest(String ip) {
        org.springframework.mock.web.MockHttpServletRequest req =
                new org.springframework.mock.web.MockHttpServletRequest();
        req.setRemoteAddr(ip);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(req));
    }

    @org.junit.jupiter.api.AfterEach
    void donRequestContext() {
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void khachXemThiKhoaChongTrungLaIPchuKhongPhaiMotKhoaChung() {
        datIpCuaRequest("203.0.113.9");
        when(postViewThrottle.shouldCount(anyString(), anyString())).thenReturn(true);

        svc.countView(postDeDem("p-1"), null);   // null = chưa đăng nhập

        verify(postViewThrottle).shouldCount("p-1", "203.0.113.9");
        verify(postRepository).incrementViews("p-1");
    }

    @Test
    void haiIpKhacNhauLaHaiNguoiXemKhacNhau() {
        when(postViewThrottle.shouldCount(anyString(), anyString())).thenReturn(true);

        datIpCuaRequest("198.51.100.1");
        svc.countView(postDeDem("p-1"), null);
        datIpCuaRequest("198.51.100.2");
        svc.countView(postDeDem("p-1"), null);

        verify(postViewThrottle).shouldCount("p-1", "198.51.100.1");
        verify(postViewThrottle).shouldCount("p-1", "198.51.100.2");
        verify(postRepository, times(2)).incrementViews("p-1");
    }

    @Test
    void daDangNhapThiKhoaLaUserIdChuKhongPhaiIP() {
        datIpCuaRequest("203.0.113.9");
        when(postViewThrottle.shouldCount(anyString(), anyString())).thenReturn(true);

        svc.countView(postDeDem("p-1"), ME);

        verify(postViewThrottle).shouldCount("p-1", ME);
    }

    @Test
    void quaNguongChongTrungThiKhongCongThem() {
        datIpCuaRequest("203.0.113.9");
        when(postViewThrottle.shouldCount(anyString(), anyString())).thenReturn(false);

        svc.countView(postDeDem("p-1"), null);

        verify(postRepository, never()).incrementViews(anyString());
    }

    // ---- Khách xem feed: meId = null, KHÔNG được nổ ----
    // Đổi getUserIdAuthoried() thành "ném lỗi khi chưa đăng nhập" làm meId có thể null ở
    // những đường trước đây luôn nhận chuỗi "anonymousUser". Feed là trang public dễ thấy
    // nhất, nên khoá lại: khách vẫn chạy, và các tham số native IN nhận sentinel "-" thay
    // vì rỗng (IN () là SQL không hợp lệ).

    @Test
    void khachXemFeedThiDungSentinelVaKhongNem() throws Exception {
        when(authorizePathService.getUserIdAuthoried()).thenThrow(new Exception("Not Authorized"));

        assertDoesNotThrow(() -> svc.getAllPostsByPage(1));

        verify(postRepository).feedPage(
                argThat(c -> c.contains("-")), argThat(c -> c.contains("-")), eq("-"), eq(10), eq(0));
        verify(followService, never()).friendIdsOf(any());
        verify(blockRepository, never()).blockedIdsOf(any());
    }

    @Test
    void khachXemSoTrangThiCungKhongNem() throws Exception {
        when(authorizePathService.getUserIdAuthoried()).thenThrow(new Exception("Not Authorized"));

        java.util.Map<String, Object> info = svc.feedPageInfo();

        assertEquals(1, info.get("totalPages"), "không có bài nào -> vẫn phải là 1 trang");
        verify(postRepository).feedCount(argThat(c -> c.contains("-")), argThat(c -> c.contains("-")), eq("-"));
    }

    // Nổi bật cũng là trang khách xem được (nằm trong permit list), nên nó phải chịu được
    // meId = null y như feed thường — cùng một cái bẫy sentinel "-".
    @Test
    void khachXemBangTinNoiBatThiCungDungSentinelVaKhongNem() throws Exception {
        when(authorizePathService.getUserIdAuthoried()).thenThrow(new Exception("Not Authorized"));

        assertDoesNotThrow(() -> svc.getHotFeed(1));

        verify(postRepository).hotFeedPage(
                argThat(c -> c.contains("-")), argThat(c -> c.contains("-")), eq("-"), eq(10), eq(0));
        verify(followService, never()).friendIdsOf(any());
    }

    // Trang 0 / trang âm đến từ ?page= trên URL, người dùng gõ tay được. Không kẹp lại thì
    // OFFSET thành số âm và MySQL ném lỗi cú pháp.
    @Test
    void trangNhoHonMotCuaBangTinNoiBatBiKepVeTrangDau() throws Exception {
        when(authorizePathService.getUserIdAuthoried()).thenThrow(new Exception("Not Authorized"));

        svc.getHotFeed(0);

        verify(postRepository).hotFeedPage(any(), any(), eq("-"), eq(10), eq(0));
    }

    // ---- Duyệt / từ chối bài đang chờ — chỉ admin ----

    private Users admin() {
        Users a = new Users();
        a.setUserId(ME);
        a.setIsAdmin(1);
        return a;
    }

    @Test
    void nguoiThuongKhongDuyetBaiDuoc() throws Exception {
        // admin() không được gọi -> userRepository trả về me từ @BeforeEach (isAdmin mặc định 0)
        assertThrows(Exception.class, () -> svc.getPendingPosts(0, 20));
        assertThrows(Exception.class, () -> svc.approvePendingPost("p-1"));
        assertThrows(Exception.class, () -> svc.rejectPendingPost("p-1", "spam"));
    }

    @Test
    void adminDuyetBaiThiChuyenSangPublishedVaBaoChoTacGia() throws Exception {
        when(userRepository.findFirstByUserId(ME)).thenReturn(admin());
        Posts p = post(OTHER, "pending", null, "Bài chờ", "nội dung");
        when(postRepository.findFirstByPostId("p-1")).thenReturn(p);
        UserPosts up = p.getUserPost();
        when(userPostRepository.findFirstByPosts_PostId("p-1")).thenReturn(up);

        assertTrue(svc.approvePendingPost("p-1"));

        ArgumentCaptor<Posts> c = ArgumentCaptor.forClass(Posts.class);
        verify(postRepository).save(c.capture());
        assertEquals("published", c.getValue().getStatus());
        verify(notificationService).push(eq(OTHER), isNull(), eq("POST_APPROVED"), eq("p-1"), anyString());
    }

    @Test
    void adminTuChoiBaiThiXoaBaiVaBaoLyDoChoTacGia() throws Exception {
        when(userRepository.findFirstByUserId(ME)).thenReturn(admin());
        Posts p = post(OTHER, "pending", null, "Bài spam", "nội dung");
        when(postRepository.findFirstByPostId("p-1")).thenReturn(p);
        when(userPostRepository.findFirstByPosts_PostId("p-1")).thenReturn(p.getUserPost());
        when(commentRepository.findCommentIdNotInUserComment()).thenReturn(java.util.List.of());

        assertTrue(svc.rejectPendingPost("p-1", "Nội dung vi phạm nội quy"));

        verify(postRepository).delete(p);
        verify(notificationService).push(eq(OTHER), isNull(), eq("POST_REJECTED"), isNull(),
                argThat(msg -> msg.contains("Nội dung vi phạm nội quy")));
    }

    @Test
    void khongDuocDuyetBaiKhongOTrangThaiChoDuyet() throws Exception {
        when(userRepository.findFirstByUserId(ME)).thenReturn(admin());
        Posts p = post(OTHER, "published", null, "Đã đăng rồi", "nội dung");
        when(postRepository.findFirstByPostId("p-1")).thenReturn(p);

        assertThrows(Exception.class, () -> svc.approvePendingPost("p-1"));
        verify(postRepository, never()).save(any());
    }

    // ---- Danh sách chuyên mục ----

    // ---- Sửa bài: đổi chuyên mục ----

    /**
     * post() helper (đã có sẵn ở trên) gắn cho bài một UserPosts KHÔNG có UserPostId —
     * đủ cho mọi test khác nhưng updatePost() đi tới basePostDTO(), và basePostDTO đọc
     * post.getUserPost().getUserPostId().getUserId() — thiếu là NPE. Vá đúng object đó
     * (không phải tạo một UserPosts rời không liên quan), rồi cũng dùng nó làm kết quả
     * mock cho findFirstByPosts_PostIdAndUsers_UserId vì service lấy `.getPosts()` từ đó.
     */
    private com.didan.social.entity.UserPosts userPost(Posts p, String userId) {
        com.didan.social.entity.UserPosts up = p.getUserPost();
        up.setUserPostId(new com.didan.social.entity.keys.UserPostId(p.getPostId(), userId));
        up.setPosts(p); // post() helper chỉ nối một chiều Posts -> UserPosts, chiều ngược lại thiếu
        return up;
    }

    @Test
    void suaBaiKhongGuiCategoryIdThiGiuNguyenChuyenMucCu() throws Exception {
        Posts p = post(ME, "published", "public", "T", "B");
        p.setPostedAt(new Date());
        p.setCategoryId("cat-cu");
        when(userPostRepository.findFirstByPosts_PostIdAndUsers_UserId("p-1", ME)).thenReturn(userPost(p, ME));

        com.didan.social.payload.request.EditPostRequest r = new com.didan.social.payload.request.EditPostRequest();
        r.setTitle("Tiêu đề mới");
        svc.updatePost("p-1", r);

        assertEquals("cat-cu", p.getCategoryId());
    }

    @Test
    void suaBaiGuiChuoiRongThiBoChuyenMuc() throws Exception {
        Posts p = post(ME, "published", "public", "T", "B");
        p.setPostedAt(new Date());
        p.setCategoryId("cat-cu");
        when(userPostRepository.findFirstByPosts_PostIdAndUsers_UserId("p-1", ME)).thenReturn(userPost(p, ME));

        com.didan.social.payload.request.EditPostRequest r = new com.didan.social.payload.request.EditPostRequest();
        r.setCategoryId("");
        svc.updatePost("p-1", r);

        assertNull(p.getCategoryId());
    }

    @Test
    void suaBaiDoiSangChuyenMucHopLe() throws Exception {
        Posts p = post(ME, "published", "public", "T", "B");
        p.setPostedAt(new Date());
        when(userPostRepository.findFirstByPosts_PostIdAndUsers_UserId("p-1", ME)).thenReturn(userPost(p, ME));
        when(categoryRepository.existsById("cat-moi")).thenReturn(true);

        com.didan.social.payload.request.EditPostRequest r = new com.didan.social.payload.request.EditPostRequest();
        r.setCategoryId("cat-moi");
        svc.updatePost("p-1", r);

        assertEquals("cat-moi", p.getCategoryId());
    }

    @Test
    void layDuocBaiChoDuyetCuaChinhMinh() throws Exception {
        Posts p = post(ME, "pending", "public", "Bài của tôi", "nội dung");
        p.setPostedAt(new Date());
        userPost(p, ME); // basePostDTO() cần UserPostId — post() helper không tự set
        when(postRepository.findMyPending(ME)).thenReturn(List.of(p));

        List<PostDTO> out = svc.getMyPendingPosts();

        assertEquals(1, out.size());
        assertEquals("pending", out.get(0).getStatus());
    }

    @Test
    void layDanhSachChuyenMuc() throws Exception {
        when(categoryRepository.findAllByOrderByPositionAscNameAsc()).thenReturn(java.util.List.of(
                new com.didan.social.entity.Category("c1", "Hỏi đáp", "hoi-dap", 1)));

        java.util.List<com.didan.social.dto.CategoryDTO> out = svc.getCategories();

        assertEquals(1, out.size());
        assertEquals("Hỏi đáp", out.get(0).getName());
    }
}
