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
    private final com.didan.social.service.FollowService followService;
    // userId -> mốc thời gian nhận heartbeat gần nhất (còn khoá = đang online)
    private final java.util.concurrent.ConcurrentHashMap<String, Long> lastSeen = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long ONLINE_TTL_MS = 45_000L;
    @Autowired
    public SocketModule(SocketIOServer server, SocketService socketService, JwtUtils jwtUtils,
                        com.didan.social.service.FollowService followService){ // Inject server socket và service vào
        this.jwtUtils = jwtUtils;
        this.server = server; // Gán server socket
        this.socketService = socketService; // Gán service
        this.followService = followService;
        server.addConnectListener(onConnected()); // Thêm listener khi có client kết nối
        server.addDisconnectListener(onDisconnected()); // Thêm listener khi có client ngắt kết nối
        server.addEventListener("send_message", SendMessageRequest.class, onChatReceived()); // Thêm listener khi có client gửi tin nhắn, với tên sự kiện là send_message và kiểu dữ liệu là Chat
        server.addEventListener("typing", Object.class, relayEvent("typing")); // "đang soạn tin"
        server.addEventListener("seen", Object.class, relayEvent("seen")); // "đã xem"
        server.addEventListener("hb", Object.class, onHeartbeat());   // client báo còn sống mỗi ~20s
        server.addEventListener("bye", Object.class, onBye());        // client sắp đóng tab
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

    private boolean isOnline(String userId){
        Long ts = lastSeen.get(userId);
        return ts != null && System.currentTimeMillis() - ts <= ONLINE_TTL_MS;
    }

    private void notifyFriends(String userId, boolean online){
        try {
            for (String fid : followService.friendIdsOf(userId)) {
                server.getRoomOperations("user:" + fid).sendEvent("friend_presence", presencePayload(userId, online));
            }
        } catch (Exception e) {
            logger.error("notifyFriends failed: " + e.getMessage());
        }
    }

    // đánh dấu user online (từ connect hoặc heartbeat); nếu vừa chuyển sang online -> báo bạn bè
    private void markOnline(String userId){
        if (userId == null) return;
        boolean wasOnline = isOnline(userId);
        lastSeen.put(userId, System.currentTimeMillis());
        if (!wasOnline) notifyFriends(userId, true);
    }

    private void markOffline(String userId){
        if (userId == null) return;
        if (lastSeen.remove(userId) != null) {
            logger.info(String.format("presence off: userId[%s]", userId));
            notifyFriends(userId, false);
        }
    }

    // dọn những user quá hạn heartbeat (đóng tab/mất mạng mà không kịp "bye")
    private void sweepStale(){
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> e : lastSeen.entrySet()) {
            if (now - e.getValue() > ONLINE_TTL_MS) markOffline(e.getKey());
        }
    }

    // Khi user mở socket thông báo: đánh dấu online + cho user biết bạn nào đang online.
    private void handleFriendPresenceOnConnect(SocketIOClient client, String userId){
        try {
            sweepStale();
            markOnline(userId);
            for (String fid : followService.friendIdsOf(userId)) {
                if (isOnline(fid)) client.sendEvent("friend_presence", presencePayload(fid, true));
            }
        } catch (Exception ex) {
            logger.error("friend presence connect failed: " + ex.getMessage());
        }
    }

    private DataListener<Object> onHeartbeat(){
        return (client, data, ack) -> {
            try {
                String userId = client.get("userId");
                if (userId == null) userId = jwtUtils.getUserIdFromAccessToken(client.getHandshakeData().getSingleUrlParam("token"));
                markOnline(userId);
                sweepStale();
            } catch (Exception e) {
                logger.error("hb failed: " + e.getMessage());
            }
        };
    }

    private DataListener<Object> onBye(){
        return (client, data, ack) -> {
            try {
                String userId = client.get("userId");
                if (userId == null) userId = jwtUtils.getUserIdFromAccessToken(client.getHandshakeData().getSingleUrlParam("token"));
                markOffline(userId);
            } catch (Exception e) {
                logger.error("bye failed: " + e.getMessage());
            }
        };
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
                String postId = client.getHandshakeData().getSingleUrlParam("postID");
                if (StringUtils.hasText(postId)) {
                    // socket theo dõi 1 bài viết (bình luận realtime)
                    client.joinRoom("post:" + postId);
                    logger.info(String.format("Post socket connected - userId[%s] postId[%s]", userId, postId));
                    return;
                }
                if (!StringUtils.hasText(conversationId)) {
                    // socket chỉ để nhận thông báo cá nhân + theo dõi bạn bè online
                    handleFriendPresenceOnConnect(client, userId);
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
            // xác định userId & loại socket: ưu tiên session, dự phòng bằng token
            String userId = client.get("userId");
            String conversationId = null;
            String postId = null;
            try {
                conversationId = client.getHandshakeData().getSingleUrlParam("conversationID");
                postId = client.getHandshakeData().getSingleUrlParam("postID");
                if (userId == null) {
                    String accessToken = client.getHandshakeData().getSingleUrlParam("token");
                    userId = jwtUtils.getUserIdFromAccessToken(accessToken);
                }
            } catch (Exception e) {
                logger.error("onDisconnected parse failed: " + e.getMessage());
            }
            logger.info(String.format("onDisconnected - userId[%s] conv[%s] post[%s]", userId, conversationId, postId));

            if (StringUtils.hasText(postId)) {
                return; // socket theo dõi bài viết
            }
            if (!StringUtils.hasText(conversationId)) {
                // socket thông báo cá nhân: đánh dấu offline ngay nếu netty bắt được disconnect
                markOffline(userId);
                return;
            }
            try {
                socketService.broadcastExcept(conversationId, "presence", presencePayload(userId, false), client);
                client.leaveRoom(conversationId);
                socketService.saveInfoMessage(conversationId, "get_message", client, String.format("%s disconnected", userId));
                logger.info(String.format("Chat socket disconnected - conv[%s] userId[%s]", conversationId, userId));
            } catch (Exception e){
                logger.error(e.getMessage());
            }
        };
    }
}