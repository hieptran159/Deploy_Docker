package com.didan.social.service.impl;

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

    PostServiceImpl svc;
    static final String ME = "me-1";
    static final String OTHER = "other-2";

    @BeforeEach
    void setUp() throws Exception {
        svc = new PostServiceImpl(postRepository, userPostRepository, fileUploadsService, userRepository,
                postLikeRepository, commentRepository, authorizePathService, notificationService,
                blockRepository, repostRepository, followService, postHashtagRepository);
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
}
