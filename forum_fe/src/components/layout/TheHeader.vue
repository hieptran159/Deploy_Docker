<template>
    <header class="app-header">
        <div class="app-header__inner">
            <span class="app-brand" @click="() => route.push('/')">HIPDN-EA&nbsp;Forum</span>

            <div v-if="isLogin" class="flex-1 min-w-0">
                <DxTabs
                    :selected-index="0"
                    :dataSource="options"
                    styling-mode="secondary"
                    @item-click="selectChange"
                />
            </div>
            <div v-else class="flex-1"></div>

            <DxButton
                :icon="isDark ? 'sun' : 'moon'"
                hint="Đổi giao diện sáng/tối"
                stylingMode="text"
                @click="toggleTheme"
            />

            <!-- Chuông thông báo -->
            <div v-if="isLogin" class="relative">
                <button
                    class="relative flex items-center justify-center size-9 rounded-lg hover:bg-gray-100"
                    :class="{ 'text-[var(--brand)]': unread > 0 }"
                    title="Thông báo"
                    @click="toggleNotif"
                >
                    <svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                    </svg>
                    <span
                        v-if="unread > 0"
                        class="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-[var(--danger)] text-white text-[11px] leading-none font-bold flex items-center justify-center ring-2 ring-[var(--surface)]"
                    >{{ unread > 99 ? '99+' : unread }}</span>
                    <span
                        v-if="unread > 0"
                        class="absolute -top-1 -right-1 size-[18px] rounded-full bg-[var(--danger)] opacity-60 animate-ping"
                    ></span>
                </button>

                <div
                    v-if="showNotif"
                    class="absolute right-0 mt-2 w-80 max-h-[70vh] overflow-y-auto bg-white border rounded-xl shadow-lg z-[60]"
                >
                    <div class="flex items-center justify-between px-3 py-2 border-b">
                        <span class="font-bold text-sm">Thông báo</span>
                        <button class="link text-xs" @click.stop="readAll">Đánh dấu đã đọc</button>
                    </div>
                    <div v-if="notifLoading" class="state text-sm">Đang tải…</div>
                    <div v-else-if="notifs.length === 0" class="state text-sm">Chưa có thông báo</div>
                    <button
                        v-for="n in notifs"
                        :key="n.notificationId"
                        class="w-full text-left flex gap-3 px-3 py-2.5 border-b last:border-b-0 hover:bg-gray-50"
                        :class="{ 'bg-[var(--brand-soft)]': !n.read }"
                        @click="onNotifClick(n)"
                    >
                        <div class="avatar-fallback size-8 text-sm flex-none">
                            {{ (n.actorName || '?')[0] }}
                        </div>
                        <div class="min-w-0">
                            <div class="text-sm">{{ n.message }}</div>
                            <div class="text-xs muted">{{ timeAgo(n.createdAt) }}</div>
                        </div>
                    </button>
                </div>
            </div>

            <div v-if="isLogin" class="row-actions">
                <div
                    class="flex items-center gap-2 cursor-pointer px-2 py-1 rounded-lg hover:bg-gray-100"
                    @click="() => route.push('/profile/edit')"
                >
                    <BaseAvatar
                        :link-avt="getItemLocal(LOCALKEYS.LINK_AVT)"
                        :user-created-post="getItemLocal(LOCALKEYS.USER_NAME)"
                        :is-show="false"
                    />
                    <span class="font-semibold text-[15px] hidden sm:inline">
                        {{ getItemLocal(LOCALKEYS.USER_NAME) }}
                    </span>
                </div>
                <DxButton icon="runner" hint="Đăng xuất" type="normal" stylingMode="text" @click="logout" />
            </div>

            <div v-else class="row-actions">
                <DxButton type="success" stylingMode="contained" @click="signUp">Đăng ký</DxButton>
                <DxButton type="default" stylingMode="contained" @click="() => route.push('/login')">Đăng nhập</DxButton>
            </div>
        </div>
    </header>
</template>

<script setup>
import DxTabs from 'devextreme-vue/tabs';
import DxButton from 'devextreme-vue/button';
import { ref, computed, watch, onMounted, onBeforeUnmount, inject } from 'vue';
import { LOCALKEYS, getItemLocal, delItemLocal, setItemLocal } from '@/storages/localStorage';
import { useRouter } from 'vue-router';
import { logout as logoutApi } from '@/apis/auth';
import { checkIsAdmin } from '@/apis/admin';
import { getNotifications, markAllRead, markRead } from '@/apis/notification';
import { timeAgo } from '@/js/helper';
import { activeConversationId, activePostId, notifRefreshTick } from '@/storages/appState';
import BaseAvatar from '../BaseAvatar.vue';

const route = useRouter();
const toast = inject('toast');

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

const options = computed(() => {
    const base = [
        { id: 0, text: "Trang chủ", icon: "home" },
        { id: 1, text: "Nhắn tin", icon: "textdocument" },
        { id: 2, text: "Bạn bè", icon: "group" },
        { id: 3, text: "Tìm người dùng", icon: "search" },
    ];
    if (isAdmin.value) base.push({ id: 4, text: "Quản trị", icon: "preferences" });
    return base;
})

const routeById = { 0: '/', 1: '/chat', 2: '/follow', 3: '/users', 4: '/admin' };
const selectChange = (e) => { route.push(routeById[e.itemData.id] || '/'); }
const signUp = () => route.push('/signup');

const logout = async () => {
    try { await logoutApi(); } catch (e) { console.log(e); }
    [LOCALKEYS.ACCESS_TOKEN, LOCALKEYS.USER_ID, LOCALKEYS.LINK_AVT, LOCALKEYS.USER_NAME, LOCALKEYS.IS_ADMIN]
        .forEach(delItemLocal);
    isAdmin.value = false;
    stopNotifPoll();
    route.push('/login');
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
const notifs = ref([]);
const showNotif = ref(false);
const notifLoading = ref(false);
let pollTimer = null;
let seenIds = new Set();
let firstPoll = true;

const POST_TYPES = ['COMMENT', 'COMMENT_LIKE', 'POST_LIKE'];
const targetOf = (n) => {
    if (POST_TYPES.includes(n.type) && n.targetId) return () => route.push(`/post/${n.targetId}`);
    if (n.type === 'MESSAGE' && n.targetId) return () => route.push({ path: '/chat', query: { c: n.targetId, name: n.actorName || '' } });
    if (n.type === 'FRIEND_REQUEST') return () => route.push({ path: '/follow', query: { tab: 'incoming' } });
    if (n.actorId) return () => route.push(`/user/${n.actorId}`);
    return null;
}

const markReadLocal = async (n) => {
    if (n.read) return;
    n.read = true;
    unread.value = Math.max(unread.value - 1, 0);
    try { await markRead(n.notificationId); } catch (e) { /* ignore */ }
}

const toastNotif = (n) => {
    const go = targetOf(n);
    toast?.(n.message, {
        type: 'info',
        sub: timeAgo(n.createdAt),
        avatar: (n.actorName || '?').charAt(0),
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

const readAll = async () => {
    try { await markAllRead(); } catch (e) { /* ignore */ }
    unread.value = 0;
    notifs.value = notifs.value.map((n) => ({ ...n, read: true }));
}

const startNotifPoll = () => {
    stopNotifPoll();
    firstPoll = true;
    pollNotifs();
    pollTimer = setInterval(pollNotifs, 20000);
}
const stopNotifPoll = () => {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
}

const onDocClick = (e) => {
    if (showNotif.value && !e.target.closest('.app-header')) showNotif.value = false;
}

onMounted(() => {
    refreshAdminFlag();
    if (isLogin.value) startNotifPoll();
    document.addEventListener('click', onDocClick);
    document.addEventListener('visibilitychange', onVisible);
});
onBeforeUnmount(() => {
    stopNotifPoll();
    document.removeEventListener('click', onDocClick);
    document.removeEventListener('visibilitychange', onVisible);
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
