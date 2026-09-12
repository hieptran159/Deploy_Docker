<template>
    <header class="app-header">
        <div class="app-header__inner">
            <span class="app-brand" @click="goHomeReload">
                <span class="app-brand__mark">HIPDN-EA</span><span class="app-brand__word">Diễn&nbsp;đàn</span>
            </span>

            <nav v-if="isLogin" class="hdr-tabs min-w-0" aria-label="Điều hướng chính">
                <button
                    v-for="t in primaryTabs"
                    :key="t.id"
                    class="hdr-tab"
                    :class="{ 'is-on': t.id === currentTabId }"
                    :aria-current="t.id === currentTabId ? 'page' : undefined"
                    :title="t.text"
                    @click="goTab(t.id)"
                >
                    <AppIcon :name="t.icon" :size="18" />
                    <span class="hdr-tab__text">{{ t.text }}</span>
                </button>
            </nav>
            <div class="flex-1"></div>

            <button class="hdr-icon" :title="isDark ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối'" @click="toggleTheme">
                <AppIcon :name="isDark ? 'sun' : 'moon'" :size="20" />
            </button>

            <!-- Chuông thông báo -->
            <div v-if="isLogin" class="relative">
                <button
                    class="hdr-icon"
                    :class="{ 'is-active': unread > 0 }"
                    title="Thông báo"
                    @click="toggleNotif"
                >
                    <AppIcon name="bell" :size="20" />
                    <span v-if="unread > 0" class="notif-dot">{{ unread > 99 ? '99+' : unread }}</span>
                </button>

                <div v-if="showNotif" class="notif-tray">
                    <div class="notif-tray__head">
                        <span>Thông báo</span>
                        <button class="link text-xs" @click.stop="readAll">Đánh dấu đã đọc</button>
                    </div>
                    <div v-if="notifLoading" class="state text-sm">Đang tải…</div>
                    <div v-else-if="notifs.length === 0" class="state text-sm">Chưa có thông báo nào</div>
                    <button
                        v-for="n in notifs"
                        :key="n.notificationId"
                        class="notif-row"
                        :class="{ 'notif-row--unread': !n.read }"
                        @click="onNotifClick(n)"
                    >
                        <img
                            v-if="avatarUrlOf(n)"
                            :src="avatarUrlOf(n)"
                            class="size-8 rounded-full object-cover flex-none"
                            @error="(e) => e.target.style.display = 'none'"
                        />
                        <div v-else class="avatar-fallback size-8 text-sm flex-none">
                            {{ (n.actorName || '?')[0] }}
                        </div>
                        <div class="min-w-0">
                            <div class="text-sm">{{ n.message }}</div>
                            <div class="text-xs muted">{{ timeAgo(n.createdAt) }}</div>
                        </div>
                    </button>
                    <button class="notif-tray__more" @click="openAllNotifs">Xem tất cả</button>
                </div>
            </div>

            <div v-if="isLogin" class="row-actions">
                <div class="hdr-user" @click="goMyProfile">
                    <BaseAvatar
                        :key="headerAvatarTick"
                        :link-avt="getItemLocal(LOCALKEYS.LINK_AVT)"
                        :user-created-post="getItemLocal(LOCALKEYS.USER_NAME)"
                        :is-show="false"
                    />
                    <span class="font-semibold text-[15px] hidden sm:inline">
                        {{ getItemLocal(LOCALKEYS.USER_NAME) }}
                    </span>
                </div>
                <button class="hdr-icon" title="Cài đặt" @click="() => route.push('/profile/edit')">
                    <AppIcon name="settings" :size="19" />
                </button>
                <button class="hdr-icon" title="Đăng xuất" @click="logout">
                    <AppIcon name="log-out" :size="19" />
                </button>
            </div>

            <div v-else class="row-actions">
                <button type="button" class="sign-btn sign-btn--ink" @click="signUp">Đăng ký</button>
                <button type="button" class="sign-btn" @click="() => route.push('/login')">Đăng nhập</button>
            </div>
        </div>
    </header>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, inject } from 'vue';
import { LOCALKEYS, getItemLocal, setItemLocal, clearAuth } from '@/storages/localStorage';
import { useRouter } from 'vue-router';
import { logout as logoutApi } from '@/apis/auth';
import { checkIsAdmin } from '@/apis/admin';
import { getNotifications, markAllRead, markRead } from '@/apis/notification';
import { notifRoute, POST_TYPES } from '@/js/notifTarget';
import { timeAgo } from '@/js/helper';
import { activeConversationId, activePostId, notifRefreshTick, setPeerOnline, onlinePeers, applyAvatarUpdate } from '@/storages/appState';
import { SOCKET_URL, IMAGE_BASE } from '@/config';
import { io } from 'socket.io-client';
import BaseAvatar from '../BaseAvatar.vue';
import AppIcon from '../AppIcon.vue';

const route = useRouter();
const toast = inject('toast');
const openConfirm = inject('openConfirm');

/* ---------- theme ---------- */
const isDark = ref(document.documentElement.dataset.theme === 'dark');
const toggleTheme = () => {
    isDark.value = !isDark.value;
    if (isDark.value) {
        document.documentElement.dataset.theme = 'dark';
        try { localStorage.setItem('theme', 'dark'); } catch (e) { /* ignore */ }
    } else {
        delete document.documentElement.dataset.theme;
        try { localStorage.setItem('theme', 'light'); } catch (e) { /* ignore */ }
    }
}

/* ---------- auth / admin ---------- */
const isAdmin = ref(getItemLocal(LOCALKEYS.IS_ADMIN) === true);
const isLogin = ref(getItemLocal(LOCALKEYS.ACCESS_TOKEN) != null);

const primaryTabs = computed(() => {
    const base = [
        // tên icon nay theo bộ AppIcon, không còn theo font icon DevExtreme
        { id: 0, text: "Trang chủ", icon: "home" },
        // bong bóng thoại, không phải phong bì — phong bì là ẩn dụ email
        { id: 1, text: "Nhắn tin", icon: "message-square" },
        { id: 2, text: "Bạn bè", icon: "users" },
        { id: 3, text: "Tìm người dùng", icon: "search" },
    ];
    // khiên, không dùng lại bánh răng của nút "Cài đặt" để hai thứ không lẫn nhau
    if (isAdmin.value) base.push({ id: 4, text: "Quản trị", icon: "shield" });
    return base;
})
const routeById = { 0: '/', 1: '/chat', 2: '/follow', 3: '/users', 4: '/admin' };

const currentTabId = computed(() => {
    const p = route.currentRoute.value.path;
    for (const [id, r] of Object.entries(routeById)) {
        if (r === '/' ? p === '/' : p.startsWith(r)) return Number(id);
    }
    return -1;
});
// Trước đây dùng DxTabs. Ở những route không thuộc tab nào (/post/:id, /tag/:tag,
// /saved, /drafts, /notifications, /profile/edit) chỉ số tính ra -1, mà DevExtreme
// quy chuẩn -1 thành phần tử CUỐI nên header sáng nhầm tab "Tìm người dùng";
// selected-item = null cũng không bỏ chọn được. Nav thường cho đúng ngữ nghĩa
// "không tab nào đang mở", đồng thời bỏ được loạt CSS ghi đè !important.

// bấm logo -> tải lại toàn bộ trang chủ (F5)
const goHomeReload = () => {
    if (route.currentRoute.value.path === '/') window.location.reload();
    else window.location.assign('/');
};
const goTab = (id) => { route.push(routeById[id] || '/'); }
const signUp = () => route.push('/signup');
const goMyProfile = () => {
    const id = getItemLocal(LOCALKEYS.USER_ID);
    route.push(id ? '/user/' + id : '/profile/edit');
};

const doLogout = async () => {
    try { await logoutApi(); } catch (e) { console.log(e); }
    clearAuth();
    isAdmin.value = false;
    stopNotifPoll();
    route.push('/login');
}
const logout = () => {
    if (openConfirm) openConfirm('Đăng xuất', 'Bạn có chắc muốn đăng xuất?', doLogout, { danger: true, confirmText: 'Đăng xuất' });
    else doLogout();
}

const refreshAdminFlag = async () => {
    if (!getItemLocal(LOCALKEYS.ACCESS_TOKEN)) { isAdmin.value = false; return; }
    const stored = getItemLocal(LOCALKEYS.IS_ADMIN);
    if (stored === true || stored === false) { isAdmin.value = stored; return; }
    const ok = await checkIsAdmin();
    setItemLocal(LOCALKEYS.IS_ADMIN, ok);
    isAdmin.value = ok;
}

/* ---------- notifications ---------- */
const unread = ref(0);
const headerAvatarTick = ref(0);
const notifs = ref([]);
const showNotif = ref(false);
const notifLoading = ref(false);
let pollTimer = null;
let seenIds = new Set();
let firstPoll = true;

const targetOf = (n) => {
    const loc = notifRoute(n);
    return loc ? () => route.push(loc) : null;
}

const markReadLocal = async (n) => {
    if (n.read) return;
    n.read = true;
    unread.value = Math.max(unread.value - 1, 0);
    try { await markRead(n.notificationId); } catch (e) { /* ignore */ }
}

const avatarUrlOf = (n) => {
    const a = n?.actorAvatar;
    if (!a || String(a).includes('null') || String(a).includes('undefined')) return '';
    return IMAGE_BASE + a;
}

const toastNotif = (n) => {
    const go = targetOf(n);
    toast?.(n.message, {
        type: 'info',
        sub: timeAgo(n.createdAt),
        avatar: (n.actorName || '?').charAt(0),
        avatarUrl: avatarUrlOf(n),
        onClick: () => { markReadLocal(n); if (go) go(); },
        duration: 6000,
    });
}

// gộp: lấy danh sách, tính số chưa đọc, và bắn toast cho thông báo mới
const pollNotifs = async () => {
    if (!getItemLocal(LOCALKEYS.ACCESS_TOKEN)) return;
    try {
        const res = await getNotifications();
        const list = res?.data?.data || [];
        unread.value = list.filter((n) => !n.read).length;
        if (showNotif.value) notifs.value = list;

        if (!firstPoll) {
            const fresh = list.filter((n) =>
                !n.read
                && !seenIds.has(n.notificationId)
                // đang mở đúng hội thoại đó -> không cần toast
                && !(n.type === 'MESSAGE' && n.targetId && n.targetId === activeConversationId.value)
                // đang xem đúng bài viết đó -> không cần toast bình luận / thích
                && !(POST_TYPES.includes(n.type) && n.targetId && n.targetId === activePostId.value)
                // đang ở trang Bạn bè -> không cần toast lời mời / chấp nhận kết bạn
                && !((n.type === 'FRIEND_REQUEST' || n.type === 'FRIEND_ACCEPT') && route.currentRoute.value.path === '/follow')
            );
            if (fresh.length > 3) {
                toast?.(`Bạn có ${fresh.length} thông báo mới`, { type: 'info', onClick: () => { showNotif.value = true; }, duration: 6000 });
            } else {
                fresh.forEach(toastNotif);
            }
        }
        seenIds = new Set(list.map((n) => n.notificationId));
        firstPoll = false;
    } catch (e) { /* ignore */ }
}

const toggleNotif = async () => {
    showNotif.value = !showNotif.value;
    if (!showNotif.value) return;
    notifLoading.value = true;
    try {
        const res = await getNotifications();
        notifs.value = res?.data?.data || [];
        seenIds = new Set(notifs.value.map((n) => n.notificationId));
        // mở chuông = xem hết -> đánh dấu đã đọc ngay (giữ highlight trong lần mở này)
        if (unread.value > 0) {
            unread.value = 0;
            try { await markAllRead(); } catch (e) { /* ignore */ }
        }
    } catch (e) {
        notifs.value = [];
    } finally {
        notifLoading.value = false;
    }
}

const onVisible = () => {
    if (document.visibilityState === 'visible') pollNotifs();
}

const onNotifClick = (n) => {
    showNotif.value = false;
    markReadLocal(n);
    const go = targetOf(n);
    if (go) go();
}

const openAllNotifs = () => {
    showNotif.value = false;
    route.push('/notifications');
}

const readAll = async () => {
    try { await markAllRead(); } catch (e) { /* ignore */ }
    unread.value = 0;
    notifs.value = notifs.value.map((n) => ({ ...n, read: true }));
}

/* ---------- socket thông báo realtime (poll vẫn giữ làm dự phòng) ---------- */
let notifSocket = null;
let notifDebounce = null;
let hbTimer = null;
const bumpPoll = () => {
    clearTimeout(notifDebounce);
    notifDebounce = setTimeout(pollNotifs, 400);
}
const sendBye = () => { try { notifSocket?.emit('bye'); } catch (e) { /* ignore */ } };
const startNotifSocket = () => {
    stopNotifSocket();
    const token = getItemLocal(LOCALKEYS.ACCESS_TOKEN);
    if (!token) return;
    try {
        notifSocket = io(SOCKET_URL, { transports: ['websocket'], query: { token } });
        notifSocket.on('notification', bumpPoll);
        notifSocket.on('friend_presence', (p) => { if (p) setPeerOnline(p.userId, !!p.online); });
        notifSocket.on('user_avatar', (p) => {
            if (!p?.userId) return;
            applyAvatarUpdate(p.userId, p.avtUrl);
            if (p.userId === getItemLocal(LOCALKEYS.USER_ID)) {
                setItemLocal(LOCALKEYS.LINK_AVT, p.avtUrl ? IMAGE_BASE + p.avtUrl : '');
                headerAvatarTick.value++;
            }
        });
        notifSocket.on('connect', () => { try { notifSocket.emit('hb'); } catch (e) { /* ignore */ } });
        clearInterval(hbTimer);
        hbTimer = setInterval(() => { try { notifSocket?.emit('hb'); } catch (e) { /* ignore */ } }, 20000);
    } catch (e) { /* ignore */ }
}
const stopNotifSocket = () => {
    clearInterval(hbTimer);
    clearTimeout(notifDebounce);
    if (notifSocket) { sendBye(); notifSocket.removeAllListeners(); notifSocket.disconnect(); notifSocket = null; }
    onlinePeers.value = new Set();
}

const startNotifPoll = () => {
    stopNotifPoll();
    firstPoll = true;
    pollNotifs();
    pollTimer = setInterval(pollNotifs, 20000);
    startNotifSocket();
}
const stopNotifPoll = () => {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
    stopNotifSocket();
}

const onDocClick = (e) => {
    if (showNotif.value && !e.target.closest('.app-header')) showNotif.value = false;
}

const onPageHide = () => { sendBye(); };
onMounted(() => {
    refreshAdminFlag();
    if (isLogin.value) startNotifPoll();
    document.addEventListener('click', onDocClick);
    document.addEventListener('visibilitychange', onVisible);
    window.addEventListener('pagehide', onPageHide);
    window.addEventListener('beforeunload', onPageHide);
});
onBeforeUnmount(() => {
    stopNotifPoll();
    document.removeEventListener('click', onDocClick);
    document.removeEventListener('visibilitychange', onVisible);
    window.removeEventListener('pagehide', onPageHide);
    window.removeEventListener('beforeunload', onPageHide);
});

watch(route.currentRoute, () => {
    const wasLogin = isLogin.value;
    isLogin.value = getItemLocal(LOCALKEYS.ACCESS_TOKEN) != null;
    isAdmin.value = getItemLocal(LOCALKEYS.IS_ADMIN) === true;
    if (isLogin.value && !wasLogin) startNotifPoll();
    else if (isLogin.value) pollNotifs();
})

// Chat.vue yêu cầu nạp lại thông báo ngay (vd: vừa đọc tin nhắn trong hội thoại)
watch(notifRefreshTick, () => {
    if (isLogin.value) pollNotifs();
})
</script>
