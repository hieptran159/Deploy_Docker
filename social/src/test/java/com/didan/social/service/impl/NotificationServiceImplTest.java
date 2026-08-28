package com.didan.social.service.impl;

import com.didan.social.repository.NotificationRepository;
import com.didan.social.repository.UserRepository;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.socket.RealtimeGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;
    @Mock AuthorizePathService authorizePathService;
    @Mock RealtimeGateway realtimeGateway;

    NotificationServiceImpl svc;

    @BeforeEach
    void setUp() throws Exception {
        svc = new NotificationServiceImpl(notificationRepository, userRepository, authorizePathService, realtimeGateway);
        when(authorizePathService.getUserIdAuthoried()).thenReturn("me");
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq("me"), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
    }

    private Pageable capture() throws Exception {
        ArgumentCaptor<Pageable> c = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByRecipientIdOrderByCreatedAtDesc(eq("me"), c.capture());
        return c.getValue();
    }

    @Test
    void negativePageClampedToZero() throws Exception {
        svc.listMinePaged(-5, 10);
        assertEquals(0, capture().getPageNumber());
    }

    @Test
    void zeroSizeDefaultsTo20() throws Exception {
        svc.listMinePaged(0, 0);
        assertEquals(20, capture().getPageSize());
    }

    @Test
    void oversizeClampedTo50() throws Exception {
        svc.listMinePaged(2, 999);
        Pageable p = capture();
        assertEquals(2, p.getPageNumber());
        assertEquals(50, p.getPageSize());
    }
}
