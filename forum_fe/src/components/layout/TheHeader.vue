<template>
    <header class="app-header">
        <div class="app-header__inner">
            <span class="app-brand" @click="() => route.push('/')">HIP&nbsp;Forum</span>

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
                <button class="relative p-1.5 rounded-lg hover:bg-gray-100" @click="toggleNotif">
                    <span class="dx-icon dx-icon-bell text-[20px]"></span>
                    <span
                        v-if="unread > 0"
                        class="absolute -top-0.5 -right-0.5 min-w-[16px] h-4 px-1 rounded-full bg-[var(--danger)] text-white text-[10px] font-bold flex items-center justify-center"
                    >{{ unread > 99 ? '99+' : unread }}</span>
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
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import { LOCALKEYS, getItemLocal, delItemLocal, setItemLocal } from '@/storages/localStorage';
import { useRouter } from 'vue-router';
import { logout as logoutApi } from '@/apis/auth';
import { checkIsAdmin } from '@/apis/admin';
import { getNotifications, getUnreadCount, markAllRead, markRead } from '@/apis/notification';
import { timeAgo } from '@/js/helper';
import BaseAvatar from '../BaseAvatar.vue';

const route = useRouter();

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

const loadUnread = async () => {
    if (!getItemLocal(LOCALKEYS.ACCESS_TOKEN)) return;
    try {
        const res = await getUnreadCount();
        unread.value = Number(res?.data?.data?.count || 0);
    } catch (e) { /* ignore */ }
}

const toggleNotif = async () => {
    showNotif.value = !showNotif.value;
    if (!showNotif.value) return;
    notifLoading.value = true;
    try {
        const res = await getNotifications();
        notifs.value = res?.data?.data || [];
    } catch (e) {
        notifs.value = [];
    } finally {
        notifLoading.value = false;
    }
}

const onNotifClick = async (n) => {
    showNotif.value = false;
    if (!n.read) {
        try { await markRead(n.notificationId); } catch (e) { /* ignore */ }
        unread.value = Math.max(unread.value - 1, 0);
    }
    if (n.type === 'COMMENT' && n.targetId) route.push(`/post/${n.targetId}`);
    else if (n.actorId) route.push(`/user/${n.actorId}`);
}

const readAll = async () => {
    try { await markAllRead(); } catch (e) { /* ignore */ }
    unread.value = 0;
    notifs.value = notifs.value.map((n) => ({ ...n, read: true }));
}

const startNotifPoll = () => {
    stopNotifPoll();
    loadUnread();
    pollTimer = setInterval(loadUnread, 60000);
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
});
onBeforeUnmount(() => {
    stopNotifPoll();
    document.removeEventListener('click', onDocClick);
});

watch(route.currentRoute, () => {
    const wasLogin = isLogin.value;
    isLogin.value = getItemLocal(LOCALKEYS.ACCESS_TOKEN) != null;
    isAdmin.value = getItemLocal(LOCALKEYS.IS_ADMIN) === true;
    if (isLogin.value && !wasLogin) startNotifPoll();
    if (isLogin.value) loadUnread();
})
</script>
