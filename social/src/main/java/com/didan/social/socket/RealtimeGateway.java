package com.didan.social.socket;

import com.corundumstudio.socketio.SocketIOServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Cầu nối để các service (không giữ SocketIOClient) đẩy sự kiện realtime
 * xuống client qua netty-socketio.
 *  - "user:<userId>" : phòng riêng của từng người, dùng cho thông báo.
 *  - "<conversationId>" : phòng hội thoại, dùng cho cập nhật tin nhắn.
 */
@Component
public class RealtimeGateway {
    private final Logger logger = LoggerFactory.getLogger(RealtimeGateway.class);
    private final SocketIOServer server;

    @Autowired
    public RealtimeGateway(SocketIOServer server) {
        this.server = server;
    }

    public static String userRoom(String userId) {
        return "user:" + userId;
    }

    public void toUser(String userId, String event, Object payload) {
        if (userId == null) return;
        try {
            server.getRoomOperations(userRoom(userId)).sendEvent(event, payload);
        } catch (Exception e) {
            logger.error("RealtimeGateway.toUser failed: " + e.getMessage());
        }
    }

    public void toRoom(String room, String event, Object payload) {
        if (room == null) return;
        try {
            server.getRoomOperations(room).sendEvent(event, payload);
        } catch (Exception e) {
            logger.error("RealtimeGateway.toRoom failed: " + e.getMessage());
        }
    }
}
