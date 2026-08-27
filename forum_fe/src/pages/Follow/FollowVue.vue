<template>
    <div class="page">
        <div class="card">
            <div class="flex items-center gap-2 mb-3">
                <button
                    class="px-3 py-1.5 rounded-lg text-sm font-semibold"
                    :class="tab === 'followings' ? 'bg-[var(--brand-soft)] text-[var(--brand)]' : 'muted'"
                    @click="setTab('followings')"
                >
                    Đang theo dõi
                </button>
                <button
                    class="px-3 py-1.5 rounded-lg text-sm font-semibold"
                    :class="tab === 'followers' ? 'bg-[var(--brand-soft)] text-[var(--brand)]' : 'muted'"
                    @click="setTab('followers')"
                >
                    Người theo dõi
                </button>
            </div>

            <div v-if="loading" class="state">Đang tải…</div>
            <div v-else-if="list.length === 0" class="state">
                {{ tab === 'followings' ? 'Chưa theo dõi ai' : 'Chưa có người theo dõi' }}
            </div>

            <div
                v-for="item in list"
                :key="item.id"
                class="flex items-center gap-4 py-3 border-b last:border-b-0"
            >
                <BaseAvatar :linkAvt="item.avatar" :userCreatedPost="item.name" :userId="item.id" :isShow="false" />
                <div
                    class="font-semibold text-[15px] flex-1 min-w-0 truncate cursor-pointer hover:underline"
                    @click="() => router.push('/user/' + item.id)"
                >
                    {{ item.name }}
                </div>
                <DxButton type="success" icon="message" text="Nhắn tin" @click="() => messageUser(item)" />
                <DxButton
                    v-if="isMe && tab === 'followings'"
                    type="normal"
                    stylingMode="outlined"
                    text="Bỏ theo dõi"
                    @click="() => confirmUnfollow(item)"
                />
            </div>
        </div>
    </div>
</template>

<script setup>
import { getFollowings, getFollowers, unFollowApi } from '@/apis/follow';
import { getUserInfo } from "@/apis/user";
import { openDirectConversation } from '@/apis/chat';
import { onMounted, ref, computed, watch, inject } from 'vue';
import { useRouter } from 'vue-router';
import { getItemLocal, LOCALKEYS } from '@/storages/localStorage';
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
const tab = ref(router.currentRoute.value.query.tab === 'followers' ? 'followers' : 'followings');

const list = ref([]);
const loading = ref(false);

const setTab = (t) => {
    if (tab.value === t) return;
    tab.value = t;
    router.replace({ query: { ...router.currentRoute.value.query, tab: t } });
    load();
}

const load = async () => {
    loading.value = true;
    list.value = [];
    try {
        const api = tab.value === 'followers' ? getFollowers : getFollowings;
        const res = await api(targetUser.value);
        const ids = res?.data?.data?.userId || [];
        for (const id of ids) {
            try {
                const u = await getUserInfo(id);
                list.value.push({
                    id,
                    name: u?.data?.data?.fullName || id,
                    avatar: u?.data?.data?.avtUrl ? IMAGE_BASE + u.data.data.avtUrl : '',
                });
            } catch (e) {
                list.value.push({ id, name: id, avatar: '' });
            }
        }
    } catch (e) {
        list.value = [];
    } finally {
        loading.value = false;
    }
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

const confirmUnfollow = (item) => {
    openConfirm?.('Bỏ theo dõi', `Bỏ theo dõi ${item.name}?`, async () => {
        try {
            await unFollowApi(item.id);
            list.value = list.value.filter((u) => u.id !== item.id);
            toast?.('Đã bỏ theo dõi');
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Bỏ theo dõi thất bại');
        }
    }, { danger: true, confirmText: 'Bỏ theo dõi' });
}

watch(targetUser, load);
onMounted(load);
</script>
