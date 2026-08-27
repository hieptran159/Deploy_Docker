package com.didan.social.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.didan.social.entity.Messages;
import com.didan.social.payload.request.SendMessageRequest;
import com.didan.social.repository.UserRepository;
import com.didan.social.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class SocketModule {
    private final Logger logger = LoggerFactory.getLogger(SocketModule.class);
    private final SocketIOServer server; // Khai báo một server socket
    private final SocketService socketService; // Khai báo một service để xử lý logic
    private final JwtUtils jwtUtils;
    @Autowired
    public SocketModule(SocketIOServer server, SocketService socketService, JwtUtils jwtUtils){ // Inject server socket và service vào
        this.jwtUtils = jwtUtils;
        this.server = server; // Gán server socket
        this.socketService = socketService; // Gán service
        server.addConnectListener(onConnected()); // Thêm listener khi có client kết nối
        server.addDisconnectListener(onDisconnected()); // Thêm listener khi có client ngắt kết nối
        server.addEventListener("send_message", SendMessageRequest.class, onChatReceived()); // Thêm listener khi có client gửi tin nhắn, với tên sự kiện là send_message và kiểu dữ liệu là Chat
        server.addEventListener("typing", Object.class, relayEvent("typing")); // "đang soạn tin"
        server.addEventListener("seen", Object.class, relayEvent("seen")); // "đã xem"
    }

    private Map<String, Object> payload(String userId){
        Map<String, Object> m = new HashMap<>();
        m.put("userId", userId);
        return m;
    }

    private Map<String, Object> presencePayload(String userId, boolean online){
        Map<String, Object> m = payload(userId);
        m.put("online", online);
        return m;
    }

    // Chuyển tiếp 1 sự kiện đơn giản (typing/seen) cho những người còn lại trong phòng, kèm userId
    private DataListener<Object> relayEvent(String eventName){
        return (senderClient, data, ackServer) -> {
            String accessToken = senderClient.getHandshakeData().getSingleUrlParam("token");
            try {
                jwtUtils.validateAccessToken(accessToken);
                String userId = jwtUtils.getUserIdFromAccessToken(accessToken);
                String conversationId = senderClient.getHandshakeData().getSingleUrlParam("conversationID");
                socketService.broadcastExcept(conversationId, eventName, payload(userId), senderClient);
            } catch (Exception e){
                logger.error(e.getMessage());
            }
        };
    }

    private DataListener<SendMessageRequest> onChatReceived(){ // Hàm xử lý khi có tin nhắn được gửi đến
        return (senderClient, data, ackServer) -> { // Trả về một listener xử lý khi có tin nhắn được gửi đến
            String accessToken = senderClient.getHandshakeData().getSingleUrlParam("token");
            try {
                jwtUtils.validateAccessToken(accessToken);
                String userId = jwtUtils.getUserIdFromAccessToken(accessToken);
                String conversationId = senderClient.getHandshakeData().getSingleUrlParam("conversationID"); // Lấy ra các tham số và giá trị của URL mà client gửi lên
                logger.info(String.format("%s[%s] -> %s",userId, senderClient.getSessionId().toString(), data.toString())); // In ra màn hình console thông tin của session và tin nhắn được gửi đến
                com.didan.social.dto.MessageDTO saved = socketService.saveMessage(userId, conversationId, "get_message", senderClient, data); // Lưu tin nhắn vào database và gửi lại tin nhắn đó cho tất cả client khác trong phòng qua hàm saveMessage của service
                if (ackServer != null && ackServer.isAckRequested()) {
                    ackServer.sendAckData(saved); // trả tin đã lưu (kèm id thật) cho chính người gửi
                }
            } catch (Exception e){
                logger.error(e.getMessage());
            }
        };
    }

    private ConnectListener onConnected(){ // Hàm xử lý khi có client kết nối
        return (client) -> { // Trả về một listener xử lý khi có client kết nối
            String accessToken = client.getHandshakeData().getSingleUrlParam("token");
            try {
                jwtUtils.validateAccessToken(accessToken);
                String userId = jwtUtils.getUserIdFromAccessToken(accessToken);
                String conversationId = client.getHandshakeData().getSingleUrlParam("conversationID"); // Lấy ra các tham số và giá trị của URL mà client gửi lên
                client.set("userId", userId); // lưu userId lên session để người khác biết
                client.joinRoom("user:" + userId); // phòng riêng để nhận thông báo realtime
                if (!StringUtils.hasText(conversationId)) {
                    // socket chỉ để nhận thông báo cá nhân, không tham gia phòng chat nào
                    logger.info(String.format("Notification socket connected - userId[%s]", userId));
                    return;
                }
                client.joinRoom(conversationId); // Thêm client vào phòng chat với id là roomId
                // cho người vừa vào biết ai đang online sẵn trong phòng
                for (SocketIOClient other : server.getRoomOperations(conversationId).getClients()) {
                    if (!other.getSessionId().equals(client.getSessionId())) {
                        String otherUid = other.get("userId");
                        if (otherUid != null) {
                            client.sendEvent("presence", presencePayload(otherUid, true));
                        }
                    }
                }
                socketService.broadcastExcept(conversationId, "presence", presencePayload(userId, true), client); // báo cho người khác biết mình online
                socketService.saveInfoMessage(conversationId, "get_message", client, String.format("%s joined to chat", userId)); // Lưu tin nhắn thông báo đã kết nối và gửi đó cho tất cả client khác trong phòng qua hàm saveInfoMessage của service
                logger.info(String.format("Socket ID[%s] - conversation ID[%s] - userId[%s]  Connected to chat module through", client.getSessionId().toString(), conversationId, userId)); // In ra màn hình console thông tin của session, phòng và tên của client vừa kết nối
            }catch (Exception e){
                logger.error(e.getMessage());
            }
        };
    }

    private DisconnectListener onDisconnected(){ // Hàm xử lý khi có client ngắt kết nối
        return (client) -> { // Trả về một listener xử lý khi có client kết nối
            String accessToken = client.getHandshakeData().getSingleUrlParam("token");
            try {
                jwtUtils.validateAccessToken(accessToken);
                String userId = jwtUtils.getUserIdFromAccessToken(accessToken);
                String conversationId = client.getHandshakeData().getSingleUrlParam("conversationID"); // Lấy ra các tham số và giá trị của URL mà client gửi lên
                if (!StringUtils.hasText(conversationId)) {
                    // socket thông báo cá nhân: không có phòng chat để xử lý
                    logger.info(String.format("Notification socket disconnected - userId[%s]", userId));
                    return;
                }
                socketService.broadcastExcept(conversationId, "presence", presencePayload(userId, false), client); // báo offline TRƯỚC khi rời phòng
                client.leaveRoom(conversationId);
                socketService.saveInfoMessage(conversationId, "get_message", client, String.format("%s disconnected", userId)); // Lưu tin nhắn thông báo đã kết nối và gửi đó cho tất cả client khác trong phòng qua hàm saveInfoMessage của service
                logger.info(String.format("Socket ID[%s] - conversation ID[%s] - userId[%s]  Disconnected to chat module through", client.getSessionId().toString(), conversationId, userId)); // In ra màn hình console thông tin của session, phòng và tên của client vừa kết nối
            }catch (Exception e){
                logger.error(e.getMessage());
            }
        };
    }
}