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

export const joinConversation = (conversationId) => {
    return authApi.post(`/chat/join/${conversationId}`);
}

export const leaveConversation = (conversationId) => {
    return authApi.delete(`/chat/leave/${conversationId}`);
}

export const sendMessageRest = (conversationId, data) => {
    return authApiFormData.post(`/chat/send/${conversationId}`, data);
}
