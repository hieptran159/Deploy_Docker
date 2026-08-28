// Nguồn duy nhất cho việc điều hướng khi bấm vào một thông báo.
// Dùng chung bởi chuông thông báo (TheHeader) và trang Thông báo đầy đủ.

export const POST_TYPES = ['COMMENT', 'COMMENT_LIKE', 'POST_LIKE', 'MENTION', 'REPLY', 'REPOST'];
const COMMENT_ANCHOR_TYPES = ['COMMENT', 'COMMENT_LIKE', 'MENTION', 'REPLY'];

/**
 * @param {{type:string, targetId?:string, refId?:string, actorId?:string, actorName?:string}} n
 * @returns {import('vue-router').RouteLocationRaw | null} vị trí để router.push, hoặc null
 */
export function notifRoute(n) {
	if (!n) return null;
	if (COMMENT_ANCHOR_TYPES.includes(n.type) && n.targetId && n.refId) {
		return { path: `/post/${n.targetId}`, query: { comment: n.refId } };
	}
	if (POST_TYPES.includes(n.type) && n.targetId) return { path: `/post/${n.targetId}` };
	if (n.type === 'MESSAGE' && n.targetId) {
		return { path: '/chat', query: { c: n.targetId, name: n.actorName || '' } };
	}
	if (n.type === 'FRIEND_REQUEST') return { path: '/follow', query: { tab: 'incoming' } };
	if (n.actorId) return { path: `/user/${n.actorId}` };
	return null;
}
