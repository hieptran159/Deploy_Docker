import { authApi } from "@/storages/api";

export const getBlacklist = () => {
    return authApi.get('/admin/blacklist');
}

export const grantAdmin = (userId) => {
    return authApi.patch(`/admin/grant?userId=${encodeURIComponent(userId)}`);
}

export const banUser = (userId) => {
    return authApi.post(`/admin/ban/${userId}`);
}

export const unbanUser = (userId) => {
    return authApi.delete(`/admin/unban/${userId}`);
}
