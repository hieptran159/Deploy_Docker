<template>
    <div class="flex gap-3 py-3 cursor-pointer group" @click="viewDetail">
        <img
            v-show="showAvatar"
            :src="linkAvt"
            class="size-10 rounded-full object-cover flex-none bg-gray-100"
            @error="avatarErrored = true"
            @load="avatarErrored = false"
        />
        <div v-if="!showAvatar" class="avatar-fallback size-10 text-base">
            {{ (userCreatedPost || '?')[0] }}
        </div>

        <div class="min-w-0 flex-1">
            <div class="font-semibold text-[15px] text-[#2577b1] truncate group-hover:underline">
                {{ post?.title }}
            </div>
            <div class="text-sm muted mt-0.5">
                bởi
                <span class="link" @click.stop="goProfile">{{ userCreatedPost || '—' }}</span>
                · {{ calculateTimeDifference(post?.postedAt) }} trước
            </div>
        </div>

        <div class="flex items-center gap-2 flex-none self-center text-xs">
            <span class="px-2 py-1 rounded-full bg-rose-50 text-rose-600 font-semibold">
                ♥ {{ post?.likesQuantity ?? 0 }}
            </span>
            <span class="px-2 py-1 rounded-full bg-amber-50 text-amber-600 font-semibold">
                💬 {{ post?.commentsQuantity ?? 0 }}
            </span>
        </div>
    </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { getUserInfo } from '../../apis/user';
import { calculateTimeDifference } from '../../js/helper';
import { IMAGE_BASE } from '@/config';
import { useRouter } from 'vue-router';

const route = useRouter();

const props = defineProps({
    post: { type: Object }
});

const linkAvt = ref("");
const post = computed(() => props.post);
const userCreatedPost = ref("");
const avatarErrored = ref(false);

const showAvatar = computed(() => {
    const u = linkAvt.value;
    return !!u && !u.endsWith('/') && !u.endsWith('null') && !u.endsWith('undefined') && !avatarErrored.value;
});

const getDataUser = async () => {
    try {
        const data = await getUserInfo(post.value?.userCreatedPost);
        userCreatedPost.value = data?.data?.data?.fullName;
        const avt = data?.data?.data?.avtUrl;
        linkAvt.value = avt ? IMAGE_BASE + avt : "";
    } catch (error) {
        console.error(error);
    }
}

const viewDetail = () => {
    route.push(`/post/${post?.value?.postId}`);
}

const goProfile = () => {
    if (post.value?.userCreatedPost) route.push(`/user/${post.value.userCreatedPost}`);
}

onMounted(getDataUser);
</script>
