<template>
    <div class="page">
        <div class="card flex items-center gap-5">
            <img v-if="avatarOk && avatarUrl" :src="avatarUrl" @error="avatarOk = false" @load="avatarOk = true"
                class="size-20 rounded-full object-cover flex-none bg-gray-100" />
            <div v-else class="avatar-fallback size-20 text-3xl">
                {{ (user?.fullName || '?')[0] }}
            </div>

            <div class="min-w-0 flex-1">
                <div class="text-2xl font-bold truncate">{{ user?.fullName || '…' }}</div>
                <div class="muted truncate">{{ user?.email }}</div>
                <div class="flex flex-wrap gap-x-5 gap-y-1 mt-2 text-sm">
                    <span class="link" @click="goFollow('followers')"><b>{{ user?.followers ?? 0 }}</b> người theo dõi</span>
                    <span class="link" @click="goFollow('followings')"><b>{{ user?.followings ?? 0 }}</b> đang theo dõi</span>
                    <span><b>{{ user?.posts ?? 0 }}</b> bài viết</span>
                </div>
            </div>

            <div v-if="!isMe" class="flex flex-col gap-2 flex-none">
                <DxButton type="success" icon="message" text="Nhắn tin" @click="messageUser" />
                <DxButton
                    v-if="!isFollowing"
                    type="default" icon="user" text="Theo dõi" @click="follow"
                />
                <DxButton
                    v-else
                    type="normal" stylingMode="outlined" icon="check" text="Đang theo dõi" @click="unfollow"
                />
                <DxButton type="danger" stylingMode="text" icon="warning" text="Báo cáo" @click="report" />
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
import { followApi, unFollowApi, getFollowings } from '@/apis/follow';
import { openDirectConversation } from '@/apis/chat';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
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
const isFollowing = ref(false);

const isMe = computed(() => userId.value === myId);
const avatarUrl = computed(() => (user.value?.avtUrl ? IMAGE_BASE + user.value.avtUrl : ""));

const checkFollowing = async () => {
    if (isMe.value) { isFollowing.value = false; return; }
    try {
        const res = await getFollowings(myId);
        const ids = res?.data?.data?.userId || [];
        isFollowing.value = ids.includes(userId.value);
    } catch (e) {
        isFollowing.value = false;
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
    checkFollowing();
}

const follow = async () => {
    try {
        await followApi(userId.value);
        isFollowing.value = true;
        if (user.value) user.value.followers = (user.value.followers ?? 0) + 1;
        toast?.('Đã theo dõi');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Theo dõi thất bại');
    }
}
const unfollow = async () => {
    try {
        await unFollowApi(userId.value);
        isFollowing.value = false;
        if (user.value) user.value.followers = Math.max((user.value.followers ?? 1) - 1, 0);
        toast?.('Đã bỏ theo dõi');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Bỏ theo dõi thất bại');
    }
}
const report = () => {
    openConfirm?.('Báo cáo người dùng', `Báo cáo ${user.value?.fullName || 'người dùng này'}?`, async () => {
        try {
            await reportUser(userId.value);
            toast?.('Đã gửi báo cáo');
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Báo cáo thất bại');
        }
    }, { danger: true, confirmText: 'Báo cáo' });
}

const goFollow = (tab) => {
    route.push({ path: '/follow', query: { user: userId.value, tab } });
}

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
}

watch(userId, load);
onMounted(load);
</script>
