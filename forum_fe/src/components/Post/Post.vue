<template>
  <div>
    <div v-if="post?.repostedBy" class="flex items-center gap-1.5 text-xs muted pt-2 pl-1">
        <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="17 1 21 5 17 9" /><path d="M3 11V9a4 4 0 0 1 4-4h14" />
            <polyline points="7 23 3 19 7 15" /><path d="M21 13v2a4 4 0 0 1-4 4H3" />
        </svg>
        <span><b class="text-[var(--text)]">{{ isMyRepost ? 'Bạn' : post.repostedBy }}</b> đã chia sẻ</span>
    </div>
    <div v-if="post?.repostNote" class="text-sm pl-1 pt-1 whitespace-pre-wrap">{{ post.repostNote }}</div>
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
            <span class="px-2 py-1 rounded-full bg-rose-50 text-rose-600 font-semibold inline-flex items-center gap-1">
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 1 0-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 0 0 0-7.78z" />
                </svg>
                {{ post?.likesQuantity ?? 0 }}
            </span>
            <span class="px-2 py-1 rounded-full bg-amber-50 text-amber-600 font-semibold inline-flex items-center gap-1">
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
                </svg>
                {{ post?.commentsQuantity ?? 0 }}
            </span>
            <button
                v-if="canRepost"
                class="px-2 py-1 rounded-full font-semibold transition inline-flex items-center gap-1"
                :class="post?.reposted ? 'bg-emerald-600 text-white' : 'bg-emerald-50 text-emerald-600 hover:bg-emerald-100'"
                :title="post?.reposted ? 'Bỏ chia sẻ' : 'Chia sẻ'"
                :disabled="busy"
                @click.stop="toggleRepost"
            >
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="17 1 21 5 17 9" /><path d="M3 11V9a4 4 0 0 1 4-4h14" />
                    <polyline points="7 23 3 19 7 15" /><path d="M21 13v2a4 4 0 0 1-4 4H3" />
                </svg>
                {{ post?.repostCount ?? 0 }}
            </button>
            <span v-else-if="post?.repostCount" class="px-2 py-1 rounded-full bg-emerald-50 text-emerald-600 font-semibold inline-flex items-center gap-1">
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="17 1 21 5 17 9" /><path d="M3 11V9a4 4 0 0 1 4-4h14" />
                    <polyline points="7 23 3 19 7 15" /><path d="M21 13v2a4 4 0 0 1-4 4H3" />
                </svg>
                {{ post.repostCount }}
            </span>
        </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, inject } from "vue";
import { calculateTimeDifference } from '../../js/helper';
import { IMAGE_BASE } from '@/config';
import { useRouter } from 'vue-router';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { repostPost, unrepostPost } from '@/apis/post';

const route = useRouter();
const toast = inject('toast', null);
const showDialog = inject('openDialogError', null);

const props = defineProps({
    post: { type: Object }
});

const post = computed(() => props.post);
const myId = getItemLocal(LOCALKEYS.USER_ID);
const isLogin = computed(() => getItemLocal(LOCALKEYS.ACCESS_TOKEN) != null);
// Tên + avatar tác giả đã đi kèm trong DTO feed -> không cần gọi /user/{id} cho từng thẻ
const userCreatedPost = computed(() => post.value?.authorName || '');
const linkAvt = computed(() => (post.value?.authorAvatar ? IMAGE_BASE + post.value.authorAvatar : ''));
const avatarErrored = ref(false);
const busy = ref(false);

const isMyRepost = computed(() => post.value?.repostedById && post.value.repostedById === myId);
const canRepost = computed(() => isLogin.value && post.value?.userCreatedPost && post.value.userCreatedPost !== myId);

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

const toggleRepost = async () => {
    if (busy.value) return;
    busy.value = true;
    const wasReposted = !!post.value.reposted;
    try {
        if (wasReposted) await unrepostPost(post.value.postId);
        else await repostPost(post.value.postId);
        // cập nhật lạc quan trên thẻ hiện tại
        post.value.reposted = !wasReposted;
        post.value.repostCount = Math.max(0, (post.value.repostCount || 0) + (wasReposted ? -1 : 1));
        toast?.(wasReposted ? 'Đã bỏ chia sẻ' : 'Đã chia sẻ bài viết');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thao tác thất bại');
    } finally {
        busy.value = false;
    }
}
</script>
