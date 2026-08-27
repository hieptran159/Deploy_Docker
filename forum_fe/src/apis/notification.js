import { authApi } from "@/storages/api";

export const getNotifications = () => {
    return authApi.get('/notification');
}

export const getUnreadCount = () => {
    return authApi.get('/notification/unread-count');
}

export const markAllRead = () => {
    return authApi.patch('/notification/read-all');
}

export const markRead = (id) => {
    return authApi.patch(`/notification/${id}/read`);
}

export const markReadByTarget = (targetId) => {
    return authApi.patch(`/notification/target/${targetId}/read`);
}
