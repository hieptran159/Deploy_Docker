<template>
    <div class="content flex justify-center flex-wrap">
        <div class="w-[80%] border p-5 rounded mb-5 flex items-center">
            <img v-if="avatarOk" :src="avatarUrl" @error="avatarOk = false" width="80" class="rounded" />
            <div v-else class="size-[80px] rounded flex bg-red-600 items-center justify-center font-bold text-3xl text-white">
                {{ (user?.fullName || '?')[0] }}
            </div>

            <div class="ml-5 flex-1">
                <div class="text-[28px] font-bold">{{ user?.fullName }}</div>
                <div class="text-gray-600">{{ user?.email }}</div>
                <div class="flex gap-4 mt-1 text-gray-700">
                    <span><b>{{ user?.followers ?? 0 }}</b> người theo dõi</span>
                    <span><b>{{ user?.followings ?? 0 }}</b> đang theo dõi</span>
                    <span><b>{{ user?.posts ?? 0 }}</b> bài viết</span>
                </div>
            </div>

            <div v-if="!isMe" class="flex flex-col gap-2">
                <DxButton type="default" @click="follow">Theo dõi</DxButton>
                <DxButton type="danger" @click="unfollow">Bỏ theo dõi</DxButton>
            </div>
        </div>

        <div class="w-[80%] border p-5 rounded">
            <span class="text-[24px] text-orange-400 font-bold">Bài viết</span>
            <div v-if="posts.length === 0" class="text-gray-500 mt-2">Chưa có bài viết</div>
            <div
                v-for="post in posts"
                :key="post.postId"
                class="border rounded p-3 my-2 cursor-pointer hover:bg-orange-50"
                @click="() => route.push('/post/' + post.postId)"
            >
                <div class="font-bold text-[#2577b1]">{{ post.title }}</div>
                <div class="text-gray-600 line-clamp-2">{{ post.body }}</div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { DxButton } from 'devextreme-vue';
import { onMounted, ref, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { getUserInfo } from '@/apis/user';
import { getPostById } from '@/apis/post';
import { followApi, unFollowApi } from '@/apis/follow';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { IMAGE_BASE } from '@/config';

const route = useRouter();
const userId = computed(() => route.currentRoute.value.params.id);

const user = ref(null);
const posts = ref([]);
const avatarOk = ref(true);

const isMe = computed(() => userId.value === getItemLocal(LOCALKEYS.USER_ID));
const avatarUrl = computed(() => IMAGE_BASE + (user.value?.avtUrl || ""));

const load = async () => {
    user.value = null;
    posts.value = [];
    avatarOk.value = true;
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
    }
}

const follow = async () => {
    try { await followApi(userId.value); await load(); } catch (e) { console.log(e); }
}
const unfollow = async () => {
    try { await unFollowApi(userId.value); await load(); } catch (e) { console.log(e); }
}

watch(userId, load);
onMounted(load);
</script>
