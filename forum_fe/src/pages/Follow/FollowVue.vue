<template>
    <div class="page">
        <div class="card">
            <div class="flex items-center gap-2 mb-3">
                <button
                    v-for="t in tabs" :key="t.key"
                    class="px-3 py-1.5 rounded-lg text-sm font-semibold"
                    :class="tab === t.key ? 'bg-[var(--brand-soft)] text-[var(--brand)]' : 'muted'"
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
                    <DxButton type="success" icon="message" text="Nhắn tin" @click="() => messageUser(item)" />
                    <DxButton type="normal" stylingMode="outlined" text="Huỷ kết bạn" @click="() => confirmUnfriend(item)" />
                </template>
                <template v-else-if="tab === 'incoming'">
                    <DxButton type="default" text="Chấp nhận" @click="() => accept(item)" />
                    <DxButton type="normal" stylingMode="outlined" text="Từ chối" @click="() => decline(item)" />
                </template>
                <template v-else>
                    <span class="text-xs muted">Đang chờ…</span>
                    <DxButton type="normal" stylingMode="outlined" text="Huỷ lời mời" @click="() => cancel(item)" />
                </template>
            </div>
        </div>
    </div>
</template>

<script setup>
import {
    getFriends, getIncomingRequests, getOutgoingRequests,
    acceptFriendRequest, declineFriendRequest, cancelFriendRequest, unfriend,
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
import { DxButton } from 'devextreme-vue';

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
];
// xem hồ sơ người khác thì chỉ có tab "Bạn bè"
const tabs = computed(() => (isMe.value ? allTabs : [allTabs[0]]));

const validTab = (t) => tabs.value.some((x) => x.key === t);
const tab = ref(validTab(router.currentRoute.value.query.tab) ? router.currentRoute.value.query.tab : 'friends');

const list = ref([]);
const loading = ref(false);
const incomingCount = ref(0);

const emptyText = computed(() => ({
    friends: 'Chưa có bạn bè',
    incoming: 'Không có lời mời nào',
    outgoing: 'Chưa gửi lời mời nào',
}[tab.value]));

const setTab = (t) => {
    if (tab.value === t) return;
    tab.value = t;
    router.replace({ query: { ...router.currentRoute.value.query, tab: t } });
    load();
}

const hydrate = async (ids) => {
    const out = [];
    for (const id of ids) {
        try {
            const u = await getUserInfo(id);
            out.push({
                id,
                name: u?.data?.data?.fullName || id,
                avatar: u?.data?.data?.avtUrl ? IMAGE_BASE + u.data.data.avtUrl : '',
            });
        } catch (e) {
            out.push({ id, name: id, avatar: '' });
        }
    }
    return out;
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
    try {
        let ids = [];
        if (tab.value === 'friends') {
            ids = (await getFriends(targetUser.value))?.data?.data?.userId || [];
        } else if (tab.value === 'incoming') {
            ids = (await getIncomingRequests())?.data?.data?.userId || [];
        } else {
            ids = (await getOutgoingRequests())?.data?.data?.userId || [];
        }
        list.value = await hydrate(ids);
    } catch (e) {
        list.value = [];
    } finally {
        loading.value = false;
    }
    loadIncomingCount();
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
        list.value = list.value.filter((u) => u.id !== item.id);
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
        list.value = list.value.filter((u) => u.id !== item.id);
        toast?.('Đã từ chối');
        loadIncomingCount();
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Từ chối thất bại');
    }
}
const cancel = async (item) => {
    try {
        await cancelFriendRequest(item.id);
        list.value = list.value.filter((u) => u.id !== item.id);
        toast?.('Đã huỷ lời mời');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Huỷ thất bại');
    }
}
const confirmUnfriend = (item) => {
    openConfirm?.('Huỷ kết bạn', `Huỷ kết bạn với ${item.name}?`, async () => {
        try {
            await unfriend(item.id);
            list.value = list.value.filter((u) => u.id !== item.id);
            toast?.('Đã huỷ kết bạn');
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Huỷ kết bạn thất bại');
        }
    }, { danger: true, confirmText: 'Huỷ kết bạn' });
}

const clearFriendNotifs = async () => {
    if (!isMe.value) return;
    try {
        await markReadByType('FRIEND_REQUEST');
        await markReadByType('FRIEND_ACCEPT');
        bumpNotifRefresh();
    } catch (e) { /* ignore */ }
};

watch(targetUser, () => { if (!validTab(tab.value)) tab.value = 'friends'; load(); });
onMounted(() => { load(); clearFriendNotifs(); });
</script>
