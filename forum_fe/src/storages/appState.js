import { ref } from 'vue';

// Hội thoại đang mở trên trang /chat (null nếu không ở trong hội thoại nào)
export const activeConversationId = ref(null);

// Tăng lên để yêu cầu header nạp lại thông báo ngay (không đợi poll)
export const notifRefreshTick = ref(0);
export const bumpNotifRefresh = () => { notifRefreshTick.value++; };
