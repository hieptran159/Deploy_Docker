import { authApi, authApiFormData } from "@/storages/api";

export const getMyConversations = () => {
    return authApi.get('/chat/conversation/alls');
}

export const searchConversations = (name) => {
    return authApi.get(`/chat/conversation?name=${encodeURIComponent(name)}`);
}

export const getMessages = (conversationId) => {
    return authApi.get(`/chat/conversation/${conversationId}`);
}

export const createConversation = (name) => {
    return authApi.post(`/chat/create?conversationName=${encodeURIComponent(name)}`);
}

export const openDirectConversation = (userId) => {
    return authApi.post(`/chat/direct/${userId}`);
}

export const addMember = (conversationId, userId) => {
    return authApi.post(`/chat/${conversationId}/members/${userId}`);
}

export const getMembers = (conversationId) => {
    return authApi.get(`/chat/${conversationId}/members`);
}

export const joinConversation = (conversationId) => {
    return authApi.post(`/chat/join/${conversationId}`);
}

export const leaveConversation = (conversationId) => {
    return authApi.delete(`/chat/leave/${conversationId}`);
}

export const sendMessageRest = (conversationId, data) => {
    return authApiFormData.post(`/chat/send/${conversationId}`, data);
}

export const renameConversation = (conversationId, name) => {
    return authApi.patch(`/chat/conversation/${conversationId}/name`, { name });
}

export const removeMember = (conversationId, userId) => {
    return authApi.delete(`/chat/${conversationId}/members/${userId}`);
}

export const editMessage = (messageId, content) => {
    return authApi.patch(`/chat/message/${messageId}`, { content });
}

export const recallMessage = (messageId) => {
    return authApi.delete(`/chat/message/${messageId}`);
}

export const reactMessage = (messageId, emoji) => {
    return authApi.post(`/chat/message/${messageId}/react?emoji=${encodeURIComponent(emoji)}`);
}

export const unreactMessage = (messageId) => {
    return authApi.delete(`/chat/message/${messageId}/react`);
}

export const setConversationAvatar = (conversationId, file) => {
    return authApiFormData.patch(`/chat/conversation/${conversationId}/avatar`, { avatar: file });
}
