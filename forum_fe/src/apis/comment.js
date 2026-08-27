import { authApiFormData, authApi } from "@/storages/api";

export const createComment = (postId, data)=> {
    return authApiFormData.post(`/comment/post/${postId}`, data);
}

export const updateComment = (id, data) => {
    return authApiFormData.patch(`/comment/update/${id}`, data);
}

export const deleteComment = (id) => {
    return authApi.delete(`/comment/delete/${id}`);
}

export const likeCommentApi = (id, type = 'LIKE') => {
    return authApi.post(`/comment/${id}?type=${encodeURIComponent(type)}`);
}

export const unLikeCommentApi = (id) => {
    return authApi.delete(`/comment/${id}`);
}
