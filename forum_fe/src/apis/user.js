import { authApi, authApiFormData } from '../storages/api.js';

export const getUserInfo = (id) => {
    return authApi.get(`/user/${id}`);
}

export const getImg = (name) => {
    return authApi.get(`/${name}`);
}

export const searchUserApi = (name) => {
    return authApi.get(`/user/search?name=${name}`);
}

export const getAllUsers = () => {
    return authApi.get('/user/getAllUser');
}

// Dùng cho gợi ý @nhắc-tên: /user/getAllUser khá nặng (N+1 + DTO đầy đủ) nên chỉ gọi
// 1 lần mỗi phiên, chia sẻ promise cho mọi lần mở bài. Lỗi -> xoá cache để lần sau thử lại.
let _allUsersPromise = null;
export const getAllUsersCached = () => {
    if (!_allUsersPromise) {
        _allUsersPromise = getAllUsers().catch((e) => { _allUsersPromise = null; throw e; });
    }
    return _allUsersPromise;
}

export const editUser = (data) => {
    return authApiFormData.patch('/user/edit', data);
}

export const updateCover = (file) => {
    return authApiFormData.patch('/user/cover', { cover: file });
}

// Endpoint riêng, KHÔNG cần mật khẩu (giống ảnh bìa). /user/edit vẫn đòi mật khẩu
// vì nó đổi được cả email lẫn mật khẩu — đổi ảnh thì không đáng phải qua cửa đó.
export const updateAvatar = (file) => {
    return authApiFormData.patch('/user/avatar', { avatar: file });
}

// Cập nhật hồ sơ mở rộng (JSON, không cần mật khẩu)
export const updateProfile = (data) => {
    return authApi.patch('/user/profile', data);
}

export const reportUser = (userId) => {
    return authApi.post(`/user/${userId}`);
}

export const deleteAccount = (password) => {
    return authApi.delete(`/user?password=${encodeURIComponent(password)}`);
}

export const deactivateAccount = (password) => {
    return authApi.post(`/user/deactivate?password=${encodeURIComponent(password)}`);
}

// Thiết bị đang đăng nhập (bảng user_sessions, mỗi hàng một máy)
export const getMySessions = () => {
    return authApi.get('/user/sessions');
}

export const revokeSession = (sessionId) => {
    return authApi.delete(`/user/sessions/${encodeURIComponent(sessionId)}`);
}
