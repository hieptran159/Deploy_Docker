import { authApi } from "@/storages/api";

// { quantity, userId: [...] }
export const getFriends = (id) => authApi.get(`/friend/list/${id}`);
export const getIncomingRequests = () => authApi.get('/friend/requests/incoming');
export const getOutgoingRequests = () => authApi.get('/friend/requests/outgoing');
// { status: self|none|pending_out|pending_in|friends }
export const friendStatus = (id) => authApi.get(`/friend/status/${id}`);

export const sendFriendRequest = (id) => authApi.post(`/friend/request/${id}`);
export const acceptFriendRequest = (id) => authApi.post(`/friend/accept/${id}`);
export const declineFriendRequest = (id) => authApi.delete(`/friend/decline/${id}`);
export const cancelFriendRequest = (id) => authApi.delete(`/friend/cancel/${id}`);
export const unfriend = (id) => authApi.delete(`/friend/${id}`);
