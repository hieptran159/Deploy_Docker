package com.didan.social.controller;

import com.didan.social.dto.ConversationDTO;
import com.didan.social.dto.MessageDTO;
import com.didan.social.payload.ResponseData;
import com.didan.social.payload.request.EditMessageRequest;
import com.didan.social.payload.request.RenameConversationRequest;
import com.didan.social.payload.request.SendMessageRequest;
import com.didan.social.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Chat")
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;
    @Autowired
    public ChatController(ChatService chatService){
        this.chatService = chatService;
    }
    // Create Conversation
    @Operation(summary = "Create conversation to invite everyone to box chat",
                description = "Create conversation to invite everyone to box chat",
                security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/create")
    public ResponseEntity<?> createConversation(@RequestParam String conversationName){
        ResponseData payload = new ResponseData();
        Map<String, String> data = new HashMap<>();
        try {
            String conversationId = chatService.createConversation(conversationName);
            if (StringUtils.hasText(conversationId)){
                payload.setDescription("Create a conversation successful");
                data.put("conversationId: ", conversationId);
                data.put("conversationName:", conversationName);
                payload.setData(data);
            } else {
                payload.setDescription("Failed to create a conversation");
                payload.setStatusCode(422);
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    // Open Direct Conversation (1-1)
    @Operation(summary = "Open (or create) a 1-1 conversation with a user",
            description = "Both users become participants immediately",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/direct/{user_id}")
    public ResponseEntity<?> openDirectConversation(@PathVariable(name = "user_id") String userId){
        ResponseData payload = new ResponseData();
        try {
            ConversationDTO data = chatService.openDirectConversation(userId);
            if (data != null){
                payload.setDescription("Open direct conversation successful");
                payload.setData(data);
            } else {
                payload.setDescription("Cannot open direct conversation");
                payload.setStatusCode(422);
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // Members of a conversation
    @Operation(summary = "List members of a conversation",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{conversation_id}/members")
    public ResponseEntity<?> getMembers(@PathVariable("conversation_id") String conversationId){
        ResponseData payload = new ResponseData();
        try {
            payload.setData(chatService.getMembers(conversationId));
            payload.setDescription("Load members successful");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // Add a member to a group
    @Operation(summary = "Add another user to a group conversation",
            description = "Người gọi phải là thành viên của nhóm",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{conversation_id}/members/{user_id}")
    public ResponseEntity<?> addMember(@PathVariable("conversation_id") String conversationId,
                                      @PathVariable("user_id") String userId){
        ResponseData payload = new ResponseData();
        try {
            chatService.addMember(conversationId, userId);
            payload.setDescription("Added member successful");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // Remove a member from a group
    @Operation(summary = "Remove a member from a group conversation",
            description = "Người gọi phải là thành viên của nhóm; không áp dụng cho tin nhắn riêng",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{conversation_id}/members/{user_id}")
    public ResponseEntity<?> removeMember(@PathVariable("conversation_id") String conversationId,
                                          @PathVariable("user_id") String userId){
        ResponseData payload = new ResponseData();
        try {
            chatService.removeMember(conversationId, userId);
            payload.setDescription("Removed member successful");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // Rename a group conversation
    @Operation(summary = "Rename a group conversation",
            description = "Người gọi phải là thành viên của nhóm; không áp dụng cho tin nhắn riêng",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/conversation/{conversation_id}/name")
    public ResponseEntity<?> renameConversation(@PathVariable("conversation_id") String conversationId,
                                                @RequestBody RenameConversationRequest request){
        ResponseData payload = new ResponseData();
        try {
            chatService.renameConversation(conversationId, request == null ? null : request.getName());
            payload.setDescription("Renamed conversation successful");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // Đặt ảnh đại diện nhóm
    @Operation(summary = "Đặt ảnh đại diện nhóm", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping(value = "/conversation/{conversation_id}/avatar", consumes = {org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> setConversationAvatar(@PathVariable("conversation_id") String conversationId,
                                                   @RequestParam("avatar") org.springframework.web.multipart.MultipartFile avatar){
        ResponseData payload = new ResponseData();
        try {
            java.util.Map<String, String> d = new java.util.HashMap<>();
            d.put("avatarUrl", chatService.setConversationAvatar(conversationId, avatar));
            payload.setData(d);
            payload.setDescription("Đã cập nhật ảnh nhóm");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // Bật/tắt thông báo cho một hội thoại (theo từng người dùng)
    @Operation(summary = "Tắt/bật thông báo cho hội thoại", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/conversation/{conversation_id}/mute")
    public ResponseEntity<?> muteConversation(@PathVariable("conversation_id") String conversationId,
                                              @RequestParam("muted") boolean muted){
        ResponseData payload = new ResponseData();
        try {
            chatService.setConversationMuted(conversationId, muted);
            payload.setDescription(muted ? "Đã tắt thông báo hội thoại" : "Đã bật lại thông báo hội thoại");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // Join Conversation
    @Operation(summary = "Join conversation to chat",
            description = "Enter the id conversation to join",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/join/{conversation_id}")
    public ResponseEntity<?> joinConversation(@PathVariable(name = "conversation_id") String conversationId){
        ResponseData payload = new ResponseData();
        try {
            ConversationDTO data = chatService.joinConversation(conversationId);
            if (data != null){
                payload.setDescription(String.format("Join conversation %s successful", conversationId));
                payload.setData(data);
            } else {
                payload.setDescription("Cannot join this conversation");
                payload.setStatusCode(422);
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    // Leave Conversation
    @Operation(summary = "Leave the conversation",
            description = "Enter id conversation you want to leave. When you leave the conversation, you cannot chat in it",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/leave/{conversation_id}")
    public ResponseEntity<?> leaveConversation(@PathVariable(name = "conversation_id") String conversationId){
        ResponseData payload = new ResponseData();
        try {
            boolean data = chatService.leaveConversation(conversationId);
            if (data){
                payload.setDescription(String.format("Leave conversation %s successful", conversationId));
            } else {
                payload.setDescription("Cannot leave this conversation");
                payload.setStatusCode(422);
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    // Send Message
    @Operation(summary = "Send a message",
            description = "You can send a message in conversations you joined",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/send/{conversation_id}")
    public ResponseEntity<?> sendMessage(@PathVariable("conversation_id") String conversationId, @ModelAttribute SendMessageRequest sendMessageRequest){
        ResponseData payload = new ResponseData();
        try {
            MessageDTO data = chatService.sendMessage(conversationId, sendMessageRequest);
            if (data != null){
                payload.setDescription(String.format("Send message in conversation %s successful", conversationId));
                payload.setData(data);
            } else {
                payload.setDescription("Send message failed");
                payload.setStatusCode(422);
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    // Edit Message
    @Operation(summary = "Edit your message",
            description = "Only the sender can edit, and only a message that is not recalled",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/message/{message_id}")
    public ResponseEntity<?> editMessage(@PathVariable("message_id") String messageId, @RequestBody EditMessageRequest request){
        ResponseData payload = new ResponseData();
        try {
            MessageDTO data = chatService.editMessage(messageId, request == null ? null : request.getContent());
            payload.setDescription("Edit message successful");
            payload.setData(data);
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    // Thả cảm xúc cho tin nhắn
    @Operation(summary = "Thả cảm xúc cho tin nhắn", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/message/{message_id}/react")
    public ResponseEntity<?> reactMessage(@PathVariable("message_id") String messageId,
                                          @RequestParam("emoji") String emoji){
        ResponseData payload = new ResponseData();
        try {
            payload.setData(chatService.reactMessage(messageId, emoji));
            payload.setDescription("OK");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Operation(summary = "Bỏ cảm xúc đã thả cho tin nhắn", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/message/{message_id}/react")
    public ResponseEntity<?> unreactMessage(@PathVariable("message_id") String messageId){
        ResponseData payload = new ResponseData();
        try {
            payload.setData(chatService.unreactMessage(messageId));
            payload.setDescription("OK");
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // Recall (unsend) Message
    @Operation(summary = "Recall your message",
            description = "Only the sender can recall the message",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/message/{message_id}")
    public ResponseEntity<?> recallMessage(@PathVariable("message_id") String messageId){
        ResponseData payload = new ResponseData();
        try {
            MessageDTO data = chatService.recallMessage(messageId);
            payload.setDescription("Recall message successful");
            payload.setData(data);
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    // Get All Conversation User joined
    @GetMapping("/conversation/alls")
    @Operation(summary = "Get all conversations you joined",
            description = "Get all conversations you joined",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> getAllConversations(){
        ResponseData payload = new ResponseData();
        try {
            List<ConversationDTO> data = chatService.getAllConversation();
            if (data != null){
                payload.setDescription("Load all conversations you joined successful");
                payload.setData(data);
            } else {
                payload.setDescription("Failed to load conversations");
                payload.setStatusCode(422);
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    // Search Conversation to Join
    @Operation(summary = "Search conversations you want to find",
            description = "Enter the name conversation you want to find",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/conversation")
    public ResponseEntity<?> searchConversation(@RequestParam(name = "name") String conversationName){
        ResponseData payload = new ResponseData();
        try {
            List<ConversationDTO> data = chatService.searchConversation(conversationName);
            if (data != null){
                payload.setDescription("Found conversations successful");
                payload.setData(data);
            } else {
                payload.setDescription("There is no conversation");
                payload.setStatusCode(422);
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    // Get All Chat in Conversation User Join
    @Operation(summary = "Get all messages in a conversation you joined",
            description = "Entr the id conversation you want to get all messages",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/conversation/{conversation_id}")
    public ResponseEntity<?> getAllMessages(@PathVariable("conversation_id") String conversationId){
        ResponseData payload = new ResponseData();
        try {
            List<MessageDTO> data = chatService.getAllMessagesInConversation(conversationId);
            if (data != null){
                payload.setDescription("Load all messages successful");
                payload.setData(data);
            } else {
                payload.setDescription("There is no message in conversation");
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setSuccess(false);
            payload.setStatusCode(500);
            payload.setDescription(e.getMessage());
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
