import { authApi } from '../storages/api.js';

export const getUserInfo = (id) => {
    return authApi.get(`/user/${id}`);
}

export const getImg = (name) => {
    return authApi.get(`/${name}`);
}