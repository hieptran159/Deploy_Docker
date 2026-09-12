<template>
    <div class="page page--wide">
        <div class="card">
            <div class="seg mb-4">
                <button
                    v-for="t in tabs" :key="t.key"
                    class="seg__btn"
                    :class="{ 'is-on': tab === t.key }"
                    @click="setTab(t.key)"
                >
                    {{ t.label }}<span v-if="t.key === 'incoming' && incomingCount"> ({{ incomingCount }})</span>
                </button>
            </div>

            <div v-if="loading" class="state">Đang tải…</div>
            <div v-else-if="list.length === 0" class="state">{{ emptyText }}</div>

            <div
                v-for="item in list"
                :key="item.id"
                class="flex items-center gap-4 py-3 border-b last:border-b-0"
            >
                <BaseAvatar :linkAvt="item.avatar" :userCreatedPost="item.name" :userId="item.id" />
                <div
                    class="font-semibold text-[15px] flex-1 min-w-0 truncate cursor-pointer hover:underline"
                    @click="() => router.push('/user/' + item.id)"
                >
                    {{ item.name }}
                </div>

                <template v-if="tab === 'friends'">
                    <button class="sign-btn sign-btn--ink" @click="() => messageUser(item)">
                        <AppIcon name="message-square" :size="16" /> Nhắn tin
                    </button>
                    <button type="button" class="sign-btn sign-btn--outline" @click="() => confirmUnfriend(item)">Huỷ kết bạn</button>
                </template>
                <template v-else-if="tab === 'incoming'">
                    <button type="button" class="sign-btn" @click="() => accept(item)">Chấp nhận</button>
                    <button type="button" class="sign-btn sign-btn--outline" @click="() => decline(item)">Từ chối</button>
                </template>
                <template v-else-if="tab === 'blocked'">
                    <button type="button" class="sign-btn sign-btn--outline" @click="() => doUnblock(item)">Bỏ chặn</button>
                </template>
                <template v-else>
                    <span class="text-xs muted">Đang chờ…</span>
                    <button type="button" class="sign-btn sign-btn--outline" @click="() => cancel(item)">Huỷ lời mời</button>
                </template>
            </div>

            <div v-if="!loading && list.length < allIds.length" class="pt-3 text-center">
                <button class="link text-sm" :disabled="loadingMore" @click="loadMore">
                    {{ loadingMore ? 'Đang tải…' : `Xem thêm (${allIds.length - list.length})` }}
                </button>
            </div>
        </div>
    </div>
</template>

<script setup>
import AppIcon from '@/components/AppIcon.vue';
import {
    getFriends, getIncomingRequests, getOutgoingRequests,
    acceptFriendRequest, declineFriendRequest, cancelFriendRequest, unfriend,
    getBlockedUsers, unblockUser,
} from '@/apis/friend';
import { getUserInfo } from "@/apis/user";
import { markReadByType } from '@/apis/notification';
import { openDirectConversation } from '@/apis/chat';
import { onMounted, ref, computed, watch, inject } from 'vue';
import { useRouter } from 'vue-router';
import { getItemLocal, LOCALKEYS } from '@/storages/localStorage';
import { bumpNotifRefresh } from '@/storages/appState';
import { IMAGE_BASE } from '@/config';
import BaseAvatar from '@/components/BaseAvatar.vue';

const router = useRouter();
const showDialog = inject('openDialogError');
const openConfirm = inject('openConfirm');
const toast = inject('toast');

const myId = getItemLocal(LOCALKEYS.USER_ID);
const targetUser = computed(() => router.currentRoute.value.query.user || myId);
const isMe = computed(() => targetUser.value === myId);

const allTabs = [
    { key: 'friends', label: 'Bạn bè' },
    { key: 'incoming', label: 'Lời mời kết bạn' },
    { key: 'outgoing', label: 'Đã gửi' },
    { key: 'blocked', label: 'Đã chặn' },
];
// xem hồ sơ người khác thì chỉ có tab "Bạn bè"
const tabs = computed(() => (isMe.value ? allTabs : [allTabs[0]]));

const validTab = (t) => tabs.value.some((x) => x.key === t);
const tab = ref(validTab(router.currentRoute.value.query.tab) ? router.currentRoute.value.query.tab : 'friends');

const PAGE = 10;
const list = ref([]);
const allIds = ref([]);
const loading = ref(false);
const loadingMore = ref(false);
const incomingCount = ref(0);

// Màn hình rỗng là lời mời hành động, không phải chỗ để thông báo "không có gì"
const emptyText = computed(() => ({
    friends: 'Chưa có bạn bè nào. Tìm người bạn biết ở mục Tìm người dùng rồi gửi lời mời.',
    incoming: 'Không có lời mời nào đang chờ bạn.',
    outgoing: 'Bạn chưa gửi lời mời nào.',
    blocked: 'Bạn chưa chặn ai. Người bị chặn sẽ không thấy bài và không nhắn tin được cho bạn.',
}[tab.value]));

const setTab = (t) => {
    if (tab.value === t) return;
    tab.value = t;
    router.replace({ query: { ...router.currentRoute.value.query, tab: t } });
    load();
}

// tên+avatar kèm sẵn trong response danh sách (backend trả `users`) -> khỏi gọi /user/{id}
let briefCache = {};

const hydrate = async (ids) => {
    // Chạy song song thay vì tuần tự; id nào đã có trong briefCache thì không gọi mạng.
    return Promise.all(ids.map(async (id) => {
        const b = briefCache[id];
        if (b) {
            return { id, name: b.fullName || id, avatar: b.avtUrl ? IMAGE_BASE + b.avtUrl : '' };
        }
        try {
            const u = await getUserInfo(id);
            return {
                id,
                name: u?.data?.data?.fullName || id,
                avatar: u?.data?.data?.avtUrl ? IMAGE_BASE + u.data.data.avtUrl : '',
            };
        } catch (e) {
            return { id, name: id, avatar: '' };
        }
    }));
}

const loadIncomingCount = async () => {
    if (!isMe.value) { incomingCount.value = 0; return; }
    try {
        const res = await getIncomingRequests();
        incomingCount.value = res?.data?.data?.userId?.length || 0;
    } catch (e) { incomingCount.value = 0; }
}

const load = async () => {
    loading.value = true;
    list.value = [];
    allIds.value = [];
    briefCache = {};
    try {
        let data = {};
        if (tab.value === 'friends') {
            data = (await getFriends(targetUser.value))?.data?.data || {};
        } else if (tab.value === 'incoming') {
            data = (await getIncomingRequests())?.data?.data || {};
        } else if (tab.value === 'blocked') {
            data = (await getBlockedUsers())?.data?.data || {};
        } else {
            data = (await getOutgoingRequests())?.data?.data || {};
        }
        const ids = data.userId || [];
        if (Array.isArray(data.users)) {
            for (const u of data.users) if (u?.userId) briefCache[u.userId] = u;
        }
        allIds.value = ids;
        list.value = await hydrate(ids.slice(0, PAGE));
        // đang ở tab "incoming" thì lấy luôn số đếm từ đây, khỏi gọi thêm request
        if (tab.value === 'incoming') incomingCount.value = ids.length;
    } catch (e) {
        list.value = [];
        allIds.value = [];
    } finally {
        loading.value = false;
    }
    if (tab.value !== 'incoming') loadIncomingCount();
}

const loadMore = async () => {
    if (loadingMore.value) return;
    loadingMore.value = true;
    try {
        const next = allIds.value.slice(list.value.length, list.value.length + PAGE);
        list.value = [...list.value, ...(await hydrate(next))];
    } finally {
        loadingMore.value = false;
    }
}

// Bỏ 1 người khỏi danh sách đang hiển thị + danh sách id gốc (sau khi chấp nhận/từ chối/…)
const removeItem = (id) => {
    list.value = list.value.filter((u) => u.id !== id);
    allIds.value = allIds.value.filter((x) => x !== id);
}

const messageUser = async (item) => {
    try {
        const res = await openDirectConversation(item.id);
        const conv = res?.data?.data;
        if (conv?.conversationId) {
            router.push({ path: '/chat', query: { c: conv.conversationId, name: item.name } });
        }
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Không mở được cuộc trò chuyện');
    }
}

const accept = async (item) => {
    try {
        await acceptFriendRequest(item.id);
        removeItem(item.id);
        toast?.(`Đã kết bạn với ${item.name}`);
        bumpNotifRefresh();
        loadIncomingCount();
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Chấp nhận thất bại');
    }
}
const decline = async (item) => {
    try {
        await declineFriendRequest(item.id);
        removeItem(item.id);
        toast?.('Đã từ chối');
        loadIncomingCount();
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Từ chối thất bại');
    }
}
const cancel = async (item) => {
    try {
        await cancelFriendRequest(item.id);
        removeItem(item.id);
        toast?.('Đã huỷ lời mời');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Huỷ thất bại');
    }
}
const doUnblock = async (item) => {
    try {
        await unblockUser(item.id);
        removeItem(item.id);
        toast?.('Đã bỏ chặn');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Bỏ chặn thất bại');
    }
}

const confirmUnfriend = (item) => {
    openConfirm?.('Huỷ kết bạn', `Huỷ kết bạn với ${item.name}?`, async () => {
        try {
            await unfriend(item.id);
            removeItem(item.id);
            toast?.('Đã huỷ kết bạn');
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Huỷ kết bạn thất bại');
        }
    }, { danger: true, confirmText: 'Huỷ kết bạn' });
}

const clearFriendNotifs = async () => {
    if (!isMe.value) return;
    try {
        await Promise.all([markReadByType('FRIEND_REQUEST'), markReadByType('FRIEND_ACCEPT')]);
        bumpNotifRefresh();
    } catch (e) { /* ignore */ }
};

watch(targetUser, () => { if (!validTab(tab.value)) tab.value = 'friends'; load(); });
onMounted(() => { load(); clearFriendNotifs(); });
</script>
