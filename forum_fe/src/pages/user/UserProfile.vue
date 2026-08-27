<template>
    <div class="page">
        <div class="card flex items-center gap-5">
            <img v-if="avatarOk" :src="avatarUrl" @error="avatarOk = false"
                class="size-20 rounded-full object-cover flex-none" />
            <div v-else class="avatar-fallback size-20 text-3xl">
                {{ (user?.fullName || '?')[0] }}
            </div>

            <div class="min-w-0 flex-1">
                <div class="text-2xl font-bold truncate">{{ user?.fullName || '…' }}</div>
                <div class="muted truncate">{{ user?.email }}</div>
                <div class="flex flex-wrap gap-x-5 gap-y-1 mt-2 text-sm">
                    <span><b>{{ user?.followers ?? 0 }}</b> người theo dõi</span>
                    <span><b>{{ user?.followings ?? 0 }}</b> đang theo dõi</span>
                    <span><b>{{ user?.posts ?? 0 }}</b> bài viết</span>
                </div>
            </div>

            <div v-if="!isMe" class="flex flex-col gap-2 flex-none">
                <DxButton type="success" icon="message" text="Nhắn tin" @click="messageUser" />
                <DxButton type="default" icon="user" text="Theo dõi" @click="follow" />
                <DxButton type="normal" stylingMode="outlined" icon="remove" text="Bỏ theo dõi" @click="unfollow" />
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
import { followApi, unFollowApi } from '@/apis/follow';
import { openDirectConversation } from '@/apis/chat';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { IMAGE_BASE } from '@/config';

const showDialog = inject('openDialogError');

const route = useRouter();
const userId = computed(() => route.currentRoute.value.params.id);

const user = ref(null);
const posts = ref([]);
const avatarOk = ref(true);
const loading = ref(false);

const isMe = computed(() => userId.value === getItemLocal(LOCALKEYS.USER_ID));
const avatarUrl = computed(() => IMAGE_BASE + (user.value?.avtUrl || ""));

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
}

const follow = async () => {
    try { await followApi(userId.value); await load(); } catch (e) { showDialog?.('Thông báo', e?.description || 'Theo dõi thất bại'); }
}
const unfollow = async () => {
    try { await unFollowApi(userId.value); await load(); } catch (e) { showDialog?.('Thông báo', e?.description || 'Bỏ theo dõi thất bại'); }
}
const report = async () => {
    try {
        await reportUser(userId.value);
        showDialog?.('Thông báo', 'Đã gửi báo cáo người dùng này');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Báo cáo thất bại');
    }
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
