<template>
  <div>
    <div v-if="post?.repostedBy" class="repost-flag">
        <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="17 1 21 5 17 9" /><path d="M3 11V9a4 4 0 0 1 4-4h14" />
            <polyline points="7 23 3 19 7 15" /><path d="M21 13v2a4 4 0 0 1-4 4H3" />
        </svg>
        <span>{{ isMyRepost ? 'Bạn' : post.repostedBy }} đã chia sẻ</span>
    </div>
    <div v-if="post?.repostNote" class="text-sm pl-[18px] pt-1 whitespace-pre-wrap measure">{{ post.repostNote }}</div>

    <div
        class="post-row py-3 cursor-pointer group"
        :class="{ 'post-row--repost': !!post?.repostedBy, 'post-row--draft': post?.status === 'draft' }"
        @click="viewDetail"
    >
        <!-- dòng tác giả -->
        <div class="flex items-center gap-2">
            <img
                v-show="showAvatar"
                :src="linkAvt"
                class="size-9 rounded-full object-cover flex-none bg-gray-100"
                @error="avatarErrored = true"
                @load="avatarErrored = false"
            />
            <div v-if="!showAvatar" class="avatar-fallback size-9 text-sm flex-none">
                {{ (userCreatedPost || '?')[0] }}
            </div>
            <div class="text-sm muted min-w-0 truncate">
                <span class="link font-medium text-[var(--text)]" @click.stop="goProfile">{{ userCreatedPost || '—' }}</span>
                · {{ calculateTimeDifference(post?.postedAt) }} trước
                <span v-if="post?.editedAt">· đã chỉnh sửa</span>
                <span v-if="post?.visibility === 'friends'" class="post-badge ml-1">👥 Bạn bè</span>
                <span v-else-if="post?.visibility === 'private'" class="post-badge ml-1">🔒 Chỉ mình tôi</span>
            </div>
        </div>

        <!-- nội dung: tiêu đề + trích đoạn -->
        <div class="post-title">{{ post?.title }}</div>
        <div v-if="excerpt" class="text-sm mt-1.5 line-clamp-3 whitespace-pre-wrap measure">{{ excerpt }}</div>

        <!-- hashtag -->
        <div v-if="hashtags.length" class="flex flex-wrap gap-1.5 mt-3">
            <button
                v-for="t in hashtags"
                :key="t"
                class="tag-chip tag-chip--sm"
                @click.stop="goTag(t)"
            >#{{ t }}</button>
        </div>

        <!-- thanh thao tác -->
        <!-- Số liệu là DỮ LIỆU nên để im lặng; chỉ nút chia sẻ đổi sang đỏ son
             khi chính bạn đã chia sẻ — đúng quy tắc "đỏ son = hành động của bạn". -->
        <div class="row-actions mt-3">
            <button
                v-if="isMyPost"
                class="act-pill"
                title="Sửa bài viết"
                @click.stop="editing = true"
            >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 20h9" /><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
                </svg>
                Sửa
            </button>
            <span class="act-pill" style="cursor: default">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 1 0-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 0 0 0-7.78z" />
                </svg>
                <span class="tnum">{{ post?.likesQuantity ?? 0 }}</span>
            </span>
            <span class="act-pill" style="cursor: default">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
                </svg>
                <span class="tnum">{{ post?.commentsQuantity ?? 0 }}</span>
            </span>
            <button
                v-if="canRepost"
                class="act-pill"
                :class="{ 'is-on': post?.reposted }"
                :title="post?.reposted ? 'Bỏ chia sẻ' : 'Chia sẻ'"
                :disabled="busy"
                @click.stop="toggleRepost"
            >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="17 1 21 5 17 9" /><path d="M3 11V9a4 4 0 0 1 4-4h14" />
                    <polyline points="7 23 3 19 7 15" /><path d="M21 13v2a4 4 0 0 1-4 4H3" />
                </svg>
                <span class="tnum">{{ post?.repostCount ?? 0 }}</span>
            </button>
            <span v-else-if="post?.repostCount" class="act-pill" style="cursor: default">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="17 1 21 5 17 9" /><path d="M3 11V9a4 4 0 0 1 4-4h14" />
                    <polyline points="7 23 3 19 7 15" /><path d="M21 13v2a4 4 0 0 1-4 4H3" />
                </svg>
                <span class="tnum">{{ post.repostCount }}</span>
            </span>
        </div>
    </div>

    <DxPopup
        v-if="editing"
        title="Sửa bài viết"
        v-model:visible="editing"
        :width="700"
        :height="420"
        :hide-on-outside-click="true"
    >
        <EditPost
            :postId="post.postId"
            :title="post.title"
            :body="post.body"
            :visibility="post.visibility || 'public'"
            @close="() => { editing = false; emit('refresh') }"
            @post-fail="showDialog?.('Thông báo', 'Cập nhật bài viết thất bại')"
        />
    </DxPopup>
  </div>
</template>

<script setup>
import { computed, ref, inject } from "vue";
import { DxPopup } from 'devextreme-vue';
import { calculateTimeDifference } from '../../js/helper';
import { IMAGE_BASE } from '@/config';
import { useRouter } from 'vue-router';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { repostPost, unrepostPost } from '@/apis/post';
import { avatarUpdates } from '@/storages/appState';
import EditPost from '@/components/Post/EditPost.vue';

const route = useRouter();
const toast = inject('toast', null);
const showDialog = inject('openDialogError', null);

const emit = defineEmits(['refresh']);
const props = defineProps({
    post: { type: Object }
});

const editing = ref(false);

const post = computed(() => props.post);
const myId = getItemLocal(LOCALKEYS.USER_ID);
const isLogin = computed(() => getItemLocal(LOCALKEYS.ACCESS_TOKEN) != null);
// Tên + avatar tác giả đã đi kèm trong DTO feed -> không cần gọi /user/{id} cho từng thẻ
const userCreatedPost = computed(() => post.value?.authorName || '');
const linkAvt = computed(() => {
    const upd = post.value?.userCreatedPost && avatarUpdates.value[post.value.userCreatedPost];
    const path = upd || post.value?.authorAvatar;
    return path ? IMAGE_BASE + path : '';
});
const avatarErrored = ref(false);
const busy = ref(false);

const isMyPost = computed(() => isLogin.value && post.value?.userCreatedPost && post.value.userCreatedPost === myId);
const isMyRepost = computed(() => post.value?.repostedById && post.value.repostedById === myId);
const canRepost = computed(() => isLogin.value && post.value?.userCreatedPost && post.value.userCreatedPost !== myId);

const showAvatar = computed(() => {
    const u = linkAvt.value;
    return !!u && !u.endsWith('/') && !u.endsWith('null') && !u.endsWith('undefined') && !avatarErrored.value;
});

const excerpt = computed(() => {
    const b = (post.value?.body || '').trim();
    return b.length > 240 ? b.slice(0, 240) + '…' : b;
});

const hashtags = computed(() => post.value?.hashtags || []);
const goTag = (t) => route.push('/tag/' + encodeURIComponent(t));

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
