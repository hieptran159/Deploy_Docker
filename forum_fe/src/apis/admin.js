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

export const grantAdmin = (userId) => {
    return authApi.patch(`/admin/grant?userId=${encodeURIComponent(userId)}`);
}

export const banUser = (userId) => {
    return authApi.post(`/admin/ban/${userId}`);
}

export const unbanUser = (userId) => {
    return authApi.delete(`/admin/unban/${userId}`);
}
