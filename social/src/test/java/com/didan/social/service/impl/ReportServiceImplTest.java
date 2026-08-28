package com.didan.social.service.impl;

import com.didan.social.entity.Posts;
import com.didan.social.repository.CommentRepository;
import com.didan.social.repository.PostRepository;
import com.didan.social.repository.ReportRepository;
import com.didan.social.repository.UserRepository;
import com.didan.social.service.AdminLogService;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.CommentService;
import com.didan.social.service.PostService;
import com.didan.social.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceImplTest {

    @Mock ReportRepository reportRepository;
    @Mock UserRepository userRepository;
    @Mock PostRepository postRepository;
    @Mock CommentRepository commentRepository;
    @Mock AuthorizePathService authorizePathService;
    @Mock UserService userService;
    @Mock PostService postService;
    @Mock CommentService commentService;
    @Mock AdminLogService adminLogService;

    ReportServiceImpl svc;
    static final String ME = "reporter-1";
    static final String PID = "post-9";

    @BeforeEach
    void setUp() throws Exception {
        svc = new ReportServiceImpl(reportRepository, userRepository, postRepository, commentRepository,
                authorizePathService, userService, postService, commentService, adminLogService);
        ReflectionTestUtils.setField(svc, "autoHideThreshold", 3);
        when(authorizePathService.getUserIdAuthoried()).thenReturn(ME);
    }

    private Posts publishedPost() {
        Posts p = new Posts();
        p.setPostId(PID);
        p.setStatus("published");
        return p;
    }

    @Test
    void invalidTypeRejected() {
        Exception e = assertThrows(Exception.class, () -> svc.create("SOMETHING", PID, "x"));
        assertTrue(e.getMessage().contains("không hợp lệ"));
    }

    @Test
    void reportPostBelowThresholdDoesNotHide() throws Exception {
        Posts p = publishedPost();
        when(postRepository.findFirstByPostId(PID)).thenReturn(p);
        when(reportRepository.findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(ME, "POST", PID, "OPEN"))
                .thenReturn(null);
        when(reportRepository.countByTargetTypeAndTargetIdAndStatus("POST", PID, "OPEN")).thenReturn(2L);

        assertTrue(svc.create("POST", PID, "spam"));

        verify(reportRepository).save(any());
        assertEquals("published", p.getStatus());
        verify(postRepository, never()).save(p);
    }

    @Test
    void reportPostAtThresholdAutoHides() throws Exception {
        Posts p = publishedPost();
        when(postRepository.findFirstByPostId(PID)).thenReturn(p);
        when(reportRepository.findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(ME, "POST", PID, "OPEN"))
                .thenReturn(null);
        when(reportRepository.countByTargetTypeAndTargetIdAndStatus("POST", PID, "OPEN")).thenReturn(3L);

        assertTrue(svc.create("POST", PID, "spam"));

        assertEquals("hidden", p.getStatus());
        verify(postRepository).save(p);
    }

    @Test
    void alreadyHiddenPostNotTouchedAgain() throws Exception {
        Posts p = publishedPost();
        p.setStatus("hidden");
        when(postRepository.findFirstByPostId(PID)).thenReturn(p);
        when(reportRepository.findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(ME, "POST", PID, "OPEN"))
                .thenReturn(null);

        assertTrue(svc.create("POST", PID, "spam"));

        verify(reportRepository, times(1)).save(any());
        verify(postRepository, never()).save(any());
    }

    @Test
    void duplicateOpenReportIsNoop() throws Exception {
        when(postRepository.findFirstByPostId(PID)).thenReturn(publishedPost());
        when(reportRepository.findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(ME, "POST", PID, "OPEN"))
                .thenReturn(new com.didan.social.entity.Reports());

        assertTrue(svc.create("POST", PID, "spam"));

        verify(reportRepository, never()).save(any());
    }

    @Test
    void thresholdZeroDisablesAutoHide() throws Exception {
        ReflectionTestUtils.setField(svc, "autoHideThreshold", 0);
        Posts p = publishedPost();
        when(postRepository.findFirstByPostId(PID)).thenReturn(p);
        when(reportRepository.findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(ME, "POST", PID, "OPEN"))
                .thenReturn(null);

        assertTrue(svc.create("POST", PID, "spam"));

        assertEquals("published", p.getStatus());
        verify(reportRepository, never()).countByTargetTypeAndTargetIdAndStatus(any(), any(), any());
    }
}
