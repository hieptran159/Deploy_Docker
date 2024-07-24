import { authApiFormData, authApi } from "@/storages/api";

export const createComment = (postId, data)=> {
    return authApiFormData.post(`/comment/post/${postId}`, data);
}

export const likeCommentApi = (id) => {
    return authApi.post(`/comment/${id}`);
}

export const unLikeCommentApi = (id) => {
    return authApi.delete(`/comment/${id}`);
}
