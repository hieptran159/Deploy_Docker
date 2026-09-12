<template>
  <div>
    <div v-if="post?.repostedBy" class="repost-flag">
        <AppIcon name="repeat" :size="13" :stroke-width="2.4" />
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
                <span v-if="post?.visibility === 'friends'" class="post-badge ml-1"><AppIcon name="users" :size="12" /> Bạn bè</span>
                <span v-else-if="post?.visibility === 'private'" class="post-badge ml-1"><AppIcon name="lock" :size="12" /> Chỉ mình tôi</span>
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
                <AppIcon name="edit" :size="16" />
                Sửa
            </button>
            <span class="act-pill" style="cursor: default">
                <AppIcon name="heart" :size="16" />
                <span class="tnum">{{ post?.likesQuantity ?? 0 }}</span>
            </span>
            <span class="act-pill" style="cursor: default">
                <AppIcon name="message-circle" :size="16" />
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
                <AppIcon name="repeat" :size="16" />
                <span class="tnum">{{ post?.repostCount ?? 0 }}</span>
            </button>
            <span v-else-if="post?.repostCount" class="act-pill" style="cursor: default">
                <AppIcon name="repeat" :size="16" />
                <span class="tnum">{{ post.repostCount }}</span>
            </span>
        </div>
    </div>

    <AppModal
        v-if="editing"
        title="Sửa bài viết"
        v-model:open="editing"
        :width="700"
    >
        <EditPost
            :postId="post.postId"
            :title="post.title"
            :body="post.body"
            :visibility="post.visibility || 'public'"
            @close="() => { editing = false; emit('refresh') }"
            @post-fail="showDialog?.('Thông báo', 'Cập nhật bài viết thất bại')"
        />
    </AppModal>
  </div>
</template>

<script setup>
import { computed, ref, inject } from "vue";
import AppModal from '@/components/ui/AppModal.vue';
import { calculateTimeDifference } from '../../js/helper';
import { IMAGE_BASE } from '@/config';
import { useRouter } from 'vue-router';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { repostPost, unrepostPost } from '@/apis/post';
import { avatarUpdates } from '@/storages/appState';
import EditPost from '@/components/Post/EditPost.vue';
import AppIcon from '@/components/AppIcon.vue';

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
