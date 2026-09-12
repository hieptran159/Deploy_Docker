import { authApi } from "@/storages/api";

export const getBlacklist = () => {
    return authApi.get('/admin/blacklist');
}

// Không có cờ admin trong response đăng nhập -> dò quyền bằng một endpoint chỉ admin gọi được
export const checkIsAdmin = async () => {
    try {
        await authApi.get('/admin/blacklist');
        return true;
    } catch (e) {
        return false;
    }
}

export const getAdminStats = () => {
    return authApi.get('/admin/stats');
}

export const getAdminLogs = (page = 0, size = 30) => {
    return authApi.get(`/admin/logs?page=${page}&size=${size}`);
}

export const grantAdmin = (userId) => {
    return authApi.patch(`/admin/grant?userId=${encodeURIComponent(userId)}`);
}

// days > 0 = cấm tạm thời rồi tự hết; 0 = vĩnh viễn
export const banUser = (userId, days = 0) => {
    return authApi.post(`/admin/ban/${userId}?days=${days}`);
}

export const unbanUser = (userId) => {
    return authApi.delete(`/admin/unban/${userId}`);
}
