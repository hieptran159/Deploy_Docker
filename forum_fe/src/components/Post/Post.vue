<template>
  <div>
    <div v-if="post?.repostedBy" class="flex items-center gap-1.5 text-xs muted pt-2 pl-1">
        <span>🔁</span>
        <span><b class="text-[var(--text)]">{{ post.repostedBy }}</b> đã chia sẻ</span>
    </div>
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
            <div v-if="excerpt" class="text-sm text-[var(--text)] mt-1 line-clamp-2">{{ excerpt }}</div>
        </div>

        <div class="flex items-center gap-2 flex-none self-center text-xs">
            <span class="px-2 py-1 rounded-full bg-rose-50 text-rose-600 font-semibold">
                ♥ {{ post?.likesQuantity ?? 0 }}
            </span>
            <span class="px-2 py-1 rounded-full bg-amber-50 text-amber-600 font-semibold">
                💬 {{ post?.commentsQuantity ?? 0 }}
            </span>
            <span v-if="post?.repostCount" class="px-2 py-1 rounded-full bg-emerald-50 text-emerald-600 font-semibold">
                🔁 {{ post.repostCount }}
            </span>
        </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { calculateTimeDifference } from '../../js/helper';
import { IMAGE_BASE } from '@/config';
import { useRouter } from 'vue-router';

const route = useRouter();

const props = defineProps({
    post: { type: Object }
});

const post = computed(() => props.post);
// Tên + avatar tác giả đã đi kèm trong DTO feed -> không cần gọi /user/{id} cho từng thẻ
const userCreatedPost = computed(() => post.value?.authorName || '');
const linkAvt = computed(() => (post.value?.authorAvatar ? IMAGE_BASE + post.value.authorAvatar : ''));
const avatarErrored = ref(false);

const showAvatar = computed(() => {
    const u = linkAvt.value;
    return !!u && !u.endsWith('/') && !u.endsWith('null') && !u.endsWith('undefined') && !avatarErrored.value;
});

const excerpt = computed(() => {
    const b = (post.value?.body || '').trim();
    return b.length > 140 ? b.slice(0, 140) + '…' : b;
});

const viewDetail = () => {
    route.push(`/post/${post?.value?.postId}`);
}

const goProfile = () => {
    if (post.value?.userCreatedPost) route.push(`/user/${post.value.userCreatedPost}`);
}
</script>
