import { authApi, authApiFormData } from "@/storages/api";

export const getListPostApi = (page = 1)=> {
    return authApi.get(`/post/get?page=${page}`);
}

export const getFeedPages = () => {
    return authApi.get('/post/pages');
}

export const getFriendsFeed = (page = 1) => {
    return authApi.get(`/post/feed/friends?page=${page}`);
}

export const getFriendsFeedPages = () => {
    return authApi.get('/post/feed/friends/pages');
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

export const getDrafts = () => {
    return authApi.get('/post/drafts');
}

export const publishPost = (id) => {
    return authApi.patch(`/post/publish/${id}`);
}

export const repostPost = (id, note = '') => {
    return authApi.post(`/post/${id}/repost${note ? `?note=${encodeURIComponent(note)}` : ''}`);
}

export const unrepostPost = (id) => {
    return authApi.delete(`/post/${id}/repost`);
}

export const getRepostsOf = (userId) => {
    return authApi.get(`/post/reposts/${userId}`);
}

export const updatePost = (id, data) => {
    return authApiFormData.patch(`/post/update/${id}`, data);
}

export const searchPost = (key, page = 0, size = 10) => {
    return authApi.get(`/post/search?name=${encodeURIComponent(key)}&page=${page}&size=${size}`);
}

export const deletePost = (id) => {
    return authApi.delete(`/post/delete/${id}`);
}