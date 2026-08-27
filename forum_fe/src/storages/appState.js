import { ref } from 'vue';

// Hội thoại đang mở trên trang /chat (null nếu không ở trong hội thoại nào)
export const activeConversationId = ref(null);

// Bài viết đang mở trên trang /post/:id (null nếu không xem bài nào)
export const activePostId = ref(null);

// Tăng lên để yêu cầu header nạp lại thông báo ngay (không đợi poll)
export const notifRefreshTick = ref(0);
export const bumpNotifRefresh = () => { notifRefreshTick.value++; };

// Bạn bè đang online (id) — cập nhật từ socket "friend_presence" của header
export const onlinePeers = ref(new Set());
export const setPeerOnline = (userId, online) => {
    if (!userId) return;
    const next = new Set(onlinePeers.value);
    if (online) next.add(userId); else next.delete(userId);
    onlinePeers.value = next;
};
