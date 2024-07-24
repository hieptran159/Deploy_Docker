import { authApi } from "@/storages/api";

export const followApi = (id) => {
    return authApi.post(`follow/${id}`);
}

export const unFollowApi = (id) => {
    return authApi.delete(`follow/${id}`);
}

export const getAllFollow = (id) => {
    return authApi.get(`/follow/followings/${id}`);
}