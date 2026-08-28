package com.didan.social.service.impl;

import com.didan.social.entity.Blocks;
import com.didan.social.entity.Followers;
import com.didan.social.entity.Users;
import com.didan.social.entity.keys.BlockId;
import com.didan.social.repository.BlockRepository;
import com.didan.social.repository.FollowRepository;
import com.didan.social.repository.UserRepository;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FollowServiceImplTest {

    @Mock FollowRepository followRepository;
    @Mock AuthorizePathService authorizePathService;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock BlockRepository blockRepository;

    FollowServiceImpl svc;

    private static final String ME = "me-1";
    private static final String OTHER = "other-2";

    @BeforeEach
    void setUp() throws Exception {
        svc = new FollowServiceImpl(followRepository, authorizePathService, userRepository,
                notificationService, blockRepository);
        when(authorizePathService.getUserIdAuthoried()).thenReturn(ME);
    }

    private Users user(String id, Integer deactivated) {
        Users u = new Users();
        u.setUserId(id);
        u.setFullName(id);
        u.setDeactivated(deactivated);
        return u;
    }

    @Test
    void blockSelfRejected() {
        Exception e = assertThrows(Exception.class, () -> svc.blockUser(ME));
        assertTrue(e.getMessage().contains("chính mình"));
    }

    @Test
    void blockRemovesFriendRowsAndSavesBlock() throws Exception {
        when(userRepository.findFirstByUserId(OTHER)).thenReturn(user(OTHER, null));
        when(blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(ME, OTHER)).thenReturn(false);
        Followers out = new Followers();
        when(followRepository.findFirstByUsers1_UserIdAndUsers2_UserId(ME, OTHER)).thenReturn(out);
        when(followRepository.findFirstByUsers1_UserIdAndUsers2_UserId(OTHER, ME)).thenReturn(null);

        assertTrue(svc.blockUser(OTHER));

        verify(followRepository).delete(out);
        verify(blockRepository).save(any(Blocks.class));
    }

    @Test
    void blockWhenAlreadyBlockedIsNoop() throws Exception {
        when(userRepository.findFirstByUserId(OTHER)).thenReturn(user(OTHER, null));
        when(blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(ME, OTHER)).thenReturn(true);

        assertTrue(svc.blockUser(OTHER));

        verify(blockRepository, never()).save(any());
        verify(followRepository, never()).delete(any());
    }

    @Test
    void unblockWhenNotBlockedRejected() {
        when(blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(ME, OTHER)).thenReturn(false);
        Exception e = assertThrows(Exception.class, () -> svc.unblockUser(OTHER));
        assertTrue(e.getMessage().contains("chưa chặn"));
    }

    @Test
    void unblockDeletesRow() throws Exception {
        when(blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(ME, OTHER)).thenReturn(true);
        assertTrue(svc.unblockUser(OTHER));
        verify(blockRepository).deleteById(any(BlockId.class));
    }

    @Test
    void friendStatusReflectsBlockDirection() throws Exception {
        when(blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(ME, OTHER)).thenReturn(true);
        assertEquals("blocked_out", svc.friendStatus(OTHER));

        when(blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(ME, OTHER)).thenReturn(false);
        when(blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(OTHER, ME)).thenReturn(true);
        assertEquals("blocked_in", svc.friendStatus(OTHER));
    }

    @Test
    void friendStatusSelfAndNone() throws Exception {
        assertEquals("self", svc.friendStatus(ME));

        when(blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(anyString(), anyString())).thenReturn(false);
        when(followRepository.findFirstByUsers1_UserIdAndUsers2_UserId(anyString(), anyString())).thenReturn(null);
        assertEquals("none", svc.friendStatus(OTHER));
    }

    @Test
    void isBlockedEitherChecksBothDirections() {
        when(blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId("a", "b")).thenReturn(false);
        when(blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId("b", "a")).thenReturn(true);
        assertTrue(svc.isBlockedEither("a", "b"));
        assertFalse(svc.isBlockedEither(null, "b"));
    }

    @Test
    void sendRequestRejectedWhenBlocked() {
        when(blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(ME, OTHER)).thenReturn(true);
        Exception e = assertThrows(Exception.class, () -> svc.sendRequest(OTHER));
        assertTrue(e.getMessage().contains("Không thể gửi lời mời"));
    }

    @Test
    void sendRequestRejectedWhenTargetDeactivated() {
        when(blockRepository.existsByBlockId_BlockerIdAndBlockId_BlockedId(anyString(), anyString())).thenReturn(false);
        when(userRepository.findFirstByUserId(OTHER)).thenReturn(user(OTHER, 1));
        Exception e = assertThrows(Exception.class, () -> svc.sendRequest(OTHER));
        assertTrue(e.getMessage().contains("không khả dụng"));
    }

    @Test
    void sendRequestToSelfRejected() {
        Exception e = assertThrows(Exception.class, () -> svc.sendRequest(ME));
        assertTrue(e.getMessage().contains("chính mình"));
    }
}
