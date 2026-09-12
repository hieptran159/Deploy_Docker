import { authApi, authApiFormData } from '../storages/api.js';

export const getUserInfo = (id) => {
    return authApi.get(`/user/${id}`);
}

export const getImg = (name) => {
    return authApi.get(`/${name}`);
}

// Trả {items,total,page,totalPages}; mỗi item chỉ có userId/fullName/avtUrl/followers/posts.
// Lọc và phân trang PHÍA SERVER — trước đây tải toàn bộ người dùng về rồi lọc ở client
// (190 KB, 1,5s cho 43 người vì DTO đầy đủ chạm 4 quan hệ lazy mỗi người).
export const searchUserApi = (q = '', page = 0, size = 20) => {
    return authApi.get(`/user/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`);
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

// Đá mọi thiết bị KHÁC, giữ lại máy đang gọi. Trả về số máy đã đá.
export const revokeOtherSessions = () => {
    return authApi.delete('/user/sessions');
}
