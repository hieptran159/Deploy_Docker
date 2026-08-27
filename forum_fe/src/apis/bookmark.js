import { authApi } from '@/storages/api';

export const toggleBookmark = (postId) => {
    return authApi.post(`/bookmark/${postId}`);
}

export const checkBookmark = (postId) => {
    return authApi.get(`/bookmark/check/${postId}`);
}

export const getMyBookmarks = () => {
    return authApi.get('/bookmark/mine');
}
