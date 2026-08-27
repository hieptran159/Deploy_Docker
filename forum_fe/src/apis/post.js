import { authApi, authApiFormData } from "@/storages/api";

export const getListPostApi = (page = 1)=> {
    return authApi.get(`/post/get?page=${page}`);
}

export const getFeedPages = () => {
    return authApi.get('/post/pages');
}

export const getPostById = (id) => {
    return authApi.get(`/post/${id}`);
}

export const likePostApi = (id, type = 'LIKE') => {
    return authApi.post(`/post/${id}?type=${encodeURIComponent(type)}`);
}

export const unLikePostApi = (id) => {
    return authApi.delete(`/post/${id}`);
}

export const createdPost = (data) => {
    return authApiFormData.post("/post/new", data);
}

export const updatePost = (id, data) => {
    return authApiFormData.patch(`/post/update/${id}`, data);
}

export const searchPost = (key) => {
    return authApi.get(`/post/search?name=${key}`);
}

export const deletePost = (id) => {
    return authApi.delete(`/post/delete/${id}`);
}