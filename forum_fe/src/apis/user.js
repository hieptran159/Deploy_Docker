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

export const editUser = (data) => {
    return authApiFormData.patch('/user/edit', data);
}

// Cập nhật hồ sơ mở rộng (JSON, không cần mật khẩu)
export const updateProfile = (data) => {
    return authApi.patch('/user/profile', data);
}

export const reportUser = (userId) => {
    return authApi.post(`/user/${userId}`);
}
