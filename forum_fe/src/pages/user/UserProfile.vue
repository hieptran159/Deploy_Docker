<template>
    <div class="page">
        <div class="card flex items-center gap-5">
            <img v-if="avatarOk && avatarUrl" :src="avatarUrl" @error="avatarOk = false" @load="avatarOk = true"
                class="size-20 rounded-full object-cover flex-none bg-gray-100" />
            <div v-else class="avatar-fallback size-20 text-3xl">
                {{ (user?.fullName || '?')[0] }}
            </div>

            <div class="min-w-0 flex-1">
                <div class="text-2xl font-bold truncate">
                    {{ user?.fullName || '…' }}
                    <span v-if="user?.nickname" class="text-base muted font-normal">({{ user.nickname }})</span>
                </div>
                <div v-if="user?.slogan" class="text-sm italic muted">“{{ user.slogan }}”</div>
                <div class="muted truncate">{{ user?.email }}</div>
                <div class="flex flex-wrap gap-x-5 gap-y-1 mt-2 text-sm">
                    <span class="link" @click="goFriends"><b>{{ friendCount }}</b> bạn bè</span>
                    <span><b>{{ user?.posts ?? 0 }}</b> bài viết</span>
                </div>
            </div>

            <div v-if="!isMe" class="flex flex-col gap-2 flex-none">
                <DxButton type="success" icon="message" text="Nhắn tin" @click="messageUser" />

                <DxButton v-if="fStatus === 'none'" type="default" icon="user" text="Kết bạn" @click="doSend" />
                <DxButton v-else-if="fStatus === 'pending_out'" type="normal" stylingMode="outlined" text="Huỷ lời mời" @click="doCancel" />
                <template v-else-if="fStatus === 'pending_in'">
                    <DxButton type="default" text="Chấp nhận kết bạn" @click="doAccept" />
                    <DxButton type="normal" stylingMode="outlined" text="Từ chối" @click="doDecline" />
                </template>
                <DxButton v-else-if="fStatus === 'friends'" type="normal" stylingMode="outlined" icon="check" text="Bạn bè" @click="doUnfriend" />

                <DxButton type="danger" stylingMode="text" icon="warning" text="Báo cáo" @click="report" />
            </div>
        </div>

        <div v-if="hasInfo" class="card">
            <div class="section-title">Thông tin</div>
            <div class="flex flex-col gap-1.5 text-sm">
                <div v-if="user?.phone" class="flex gap-2"><span class="w-28 flex-none muted">Số điện thoại</span><span>{{ user.phone }}</span></div>
                <div v-if="user?.address" class="flex gap-2"><span class="w-28 flex-none muted">Địa chỉ</span><span>{{ user.address }}</span></div>
                <div v-if="user?.hobbies" class="flex gap-2"><span class="w-28 flex-none muted">Sở thích</span><span class="whitespace-pre-wrap">{{ user.hobbies }}</span></div>
            </div>
        </div>

        <div class="card">
            <div class="section-title">Bài viết</div>
            <div v-if="loading" class="state">Đang tải…</div>
            <div v-else-if="!posts.length" class="state">Chưa có bài viết</div>
            <div
                v-for="post in posts"
                :key="post.postId"
                class="border rounded-xl p-3 my-2 cursor-pointer hover:bg-gray-50 transition"
                @click="() => route.push('/post/' + post.postId)"
            >
                <div class="font-semibold text-[#2577b1]">{{ post.title }}</div>
                <div class="muted text-sm line-clamp-2">{{ post.body }}</div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { DxButton } from 'devextreme-vue';
import { onMounted, ref, computed, watch, inject } from 'vue';
import { useRouter } from 'vue-router';
import { getUserInfo, reportUser } from '@/apis/user';
import { getPostById } from '@/apis/post';
import {
    friendStatus, getFriends, sendFriendRequest, cancelFriendRequest,
    acceptFriendRequest, declineFriendRequest, unfriend,
} from '@/apis/friend';
import { openDirectConversation } from '@/apis/chat';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { bumpNotifRefresh } from '@/storages/appState';
import { IMAGE_BASE } from '@/config';

const showDialog = inject('openDialogError');
const openConfirm = inject('openConfirm');
const toast = inject('toast');

const route = useRouter();
const myId = getItemLocal(LOCALKEYS.USER_ID);
const userId = computed(() => route.currentRoute.value.params.id);

const user = ref(null);
const posts = ref([]);
const avatarOk = ref(true);
const loading = ref(false);
const fStatus = ref('none');       // none|pending_out|pending_in|friends|self
const friendCount = ref(0);

const isMe = computed(() => userId.value === myId);
const avatarUrl = computed(() => (user.value?.avtUrl ? IMAGE_BASE + user.value.avtUrl : ""));
const hasInfo = computed(() => !!(user.value?.phone || user.value?.address || user.value?.hobbies));

const refreshStatus = async () => {
    try {
        friendCount.value = (await getFriends(userId.value))?.data?.data?.quantity || 0;
    } catch (e) { friendCount.value = 0; }
    if (isMe.value) { fStatus.value = 'self'; return; }
    try {
        fStatus.value = (await friendStatus(userId.value))?.data?.data?.status || 'none';
    } catch (e) {
        fStatus.value = 'none';
    }
}

const load = async () => {
    user.value = null;
    posts.value = [];
    avatarOk.value = true;
    loading.value = true;
    try {
        const res = await getUserInfo(userId.value);
        user.value = res?.data?.data || null;
        const ids = user.value?.postId || [];
        const results = await Promise.allSettled(ids.map((id) => getPostById(id)));
        posts.value = results
            .filter((r) => r.status === "fulfilled")
            .map((r) => r.value?.data?.data)
            .filter(Boolean);
    } catch (error) {
        console.log(error);
    } finally {
        loading.value = false;
    }
    refreshStatus();
}

const wrap = (fn, okMsg) => async () => {
    try {
        await fn(userId.value);
        if (okMsg) toast?.(okMsg);
        bumpNotifRefresh();
        await refreshStatus();
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thao tác thất bại');
    }
};
const doSend = wrap(sendFriendRequest, 'Đã gửi lời mời kết bạn');
const doCancel = wrap(cancelFriendRequest, 'Đã huỷ lời mời');
const doAccept = wrap(acceptFriendRequest, 'Đã kết bạn');
const doDecline = wrap(declineFriendRequest, 'Đã từ chối');
const doUnfriend = () => {
    openConfirm?.('Huỷ kết bạn', `Huỷ kết bạn với ${user.value?.fullName || 'người này'}?`, async () => {
        try {
            await unfriend(userId.value);
            toast?.('Đã huỷ kết bạn');
            await refreshStatus();
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Huỷ kết bạn thất bại');
        }
    }, { danger: true, confirmText: 'Huỷ kết bạn' });
};

const report = () => {
    openConfirm?.('Báo cáo người dùng', `Báo cáo ${user.value?.fullName || 'người dùng này'}?`, async () => {
        try {
            await reportUser(userId.value);
            toast?.('Đã gửi báo cáo');
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Báo cáo thất bại');
        }
    }, { danger: true, confirmText: 'Báo cáo' });
};

const goFriends = () => {
    route.push({ path: '/follow', query: { user: userId.value, tab: 'friends' } });
};

const messageUser = async () => {
    try {
        const res = await openDirectConversation(userId.value);
        const conv = res?.data?.data;
        if (conv?.conversationId) {
            route.push({
                path: '/chat',
                query: { c: conv.conversationId, name: user.value?.fullName || 'Tin nhắn riêng' },
            });
        } else {
            showDialog?.('Thông báo', 'Không mở được cuộc trò chuyện');
        }
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Không mở được cuộc trò chuyện');
    }
};

watch(userId, load);
onMounted(load);
</script>
