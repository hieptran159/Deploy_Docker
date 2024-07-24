import { authApi, authApiFormData } from "@/storages/api";

export const getListPostApi = (page = 1)=> {
    return authApi.get(`/post/get?page=${page}`);
}

export const getPostById = (id) => {
    return authApi.get(`/post/${id}`);
}

export const likePostApi = (id) => {
    return authApi.post(`/post/${id}`);
}

export const unLikePostApi = (id) => {
    return authApi.delete(`/post/${id}`);
}

export const createdPost = (data) => {
    return authApiFormData.post("/post/new", data);
}

export const searchPost = (key) => {
    return authApi.get(`/post/search?name=${key}`);
}

export const deletePost = (id) => {
    return authApi.delete(`/post/delete/${id}`);
}