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

// Trường dạng mảng (pollOptions, postImgs) phải LẶP LẠI cùng một khoá thì Spring
// mới gom vào List. Để axios tự serialize thì ra `pollOptions[0]`, phụ thuộc auto-grow
// của data binder, dễ vỡ hơn -> tự dựng FormData.
const toForm = (data) => {
    const { pollOptions, postImgs, ...rest } = data || {};
    if (!pollOptions?.length && !postImgs?.length) return rest;
    const form = new FormData();
    Object.entries(rest).forEach(([k, v]) => { if (v !== null && v !== undefined) form.append(k, v); });
    (pollOptions || []).forEach((o) => form.append('pollOptions', o));
    (postImgs || []).forEach((f) => form.append('postImgs', f));
    return form;
}

export const createdPost = (data) => {
    return authApiFormData.post("/post/new", toForm(data));
}

export const getDrafts = (page = 0, size = 20) => {
    return authApi.get(`/post/drafts?page=${page}&size=${size}`);
}

export const publishPost = (id) => {
    return authApi.patch(`/post/publish/${id}`);
}

export const publishAllDrafts = () => {
    return authApi.patch('/post/drafts/publish-all');
}

export const deleteAllDrafts = () => {
    return authApi.delete('/post/drafts/all');
}

export const repostPost = (id, note = '') => {
    return authApi.post(`/post/${id}/repost${note ? `?note=${encodeURIComponent(note)}` : ''}`);
}

export const unrepostPost = (id) => {
    return authApi.delete(`/post/${id}/repost`);
}

export const getRepostsOf = (userId, page = 0, size = 10) => {
    return authApi.get(`/post/reposts/${userId}?page=${page}&size=${size}`);
}

export const getPostsByTag = (tag, page = 0, size = 10) => {
    return authApi.get(`/post/by-tag/${encodeURIComponent(tag)}?page=${page}&size=${size}`);
}

export const getTrendingHashtags = (limit = 10) => {
    return authApi.get(`/post/hashtags/trending?limit=${limit}`);
}

export const getPostsByUser = (userId, page = 0, size = 10) => {
    return authApi.get(`/post/by-user/${userId}?page=${page}&size=${size}`);
}

export const updatePost = (id, data) => {
    return authApiFormData.patch(`/post/update/${id}`, toForm(data));
}

export const searchPost = (key, page = 0, size = 10) => {
    return authApi.get(`/post/search?name=${encodeURIComponent(key)}&page=${page}&size=${size}`);
}

export const deletePost = (id) => {
    return authApi.delete(`/post/delete/${id}`);
}

export const votePoll = (postId, optionId) => {
    return authApi.post(`/post/${postId}/vote?optionId=${encodeURIComponent(optionId)}`);
}

export const unvotePoll = (postId) => {
    return authApi.delete(`/post/${postId}/vote`);
}

/* ---------- theo dõi hashtag ---------- */

export const getFollowedTags = () => {
    return authApi.get('/post/hashtags/following');
}

export const followTag = (tag) => {
    return authApi.post(`/post/hashtags/${encodeURIComponent(tag)}/follow`);
}

export const unfollowTag = (tag) => {
    return authApi.delete(`/post/hashtags/${encodeURIComponent(tag)}/follow`);
}

// page bắt đầu từ 0 (khác /post/get bắt đầu từ 1)
export const getFollowedTagsFeed = (page = 0, size = 10) => {
    return authApi.get(`/post/feed/hashtags?page=${page}&size=${size}`);
}
