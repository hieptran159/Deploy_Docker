<template>
  <div>
    <div v-if="post?.repostedBy" class="repost-flag">
        <AppIcon name="repeat" :size="13" :stroke-width="2.4" />
        <span>{{ isMyRepost ? 'Bạn' : post.repostedBy }} đã chia sẻ</span>
    </div>
    <div v-if="post?.repostNote" class="text-sm pl-[18px] pt-1 whitespace-pre-wrap measure">{{ post.repostNote }}</div>

    <div
        class="entry cursor-pointer group"
        :class="{ 'entry--repost': !!post?.repostedBy, 'entry--draft': post?.status === 'draft' }"
        @click="viewDetail"
    >
        <!-- Cột lề: giờ đăng. Đây là DỮ LIỆU, không phải khoảng trắng canh lề. -->
        <div class="entry__margin">
            <span class="entry__time">{{ formatTime(post?.postedAt) }}</span>
        </div>

        <div class="entry__body">
            <!-- dòng tác giả: ngăn bằng khoảng trắng, KHÔNG nối bằng dấu chấm giữa -->
            <div class="flex items-center gap-2 flex-wrap">
                <img
                    v-show="showAvatar"
                    :src="linkAvt"
                    class="size-6 rounded-full object-cover flex-none bg-gray-100"
                    @error="avatarErrored = true"
                    @load="avatarErrored = false"
                />
                <div v-if="!showAvatar" class="avatar-fallback size-6 text-xs flex-none">
                    {{ (userCreatedPost || '?')[0] }}
                </div>
                <span class="link text-sm font-semibold text-[var(--text)]" @click.stop="goProfile">{{ userCreatedPost || '—' }}</span>
                <span v-if="post?.editedAt" class="text-xs muted">đã sửa</span>
                <span v-if="post?.visibility === 'friends'" class="post-badge"><AppIcon name="users" :size="12" /> Bạn bè</span>
                <span v-else-if="post?.visibility === 'private'" class="post-badge"><AppIcon name="lock" :size="12" /> Chỉ mình tôi</span>
                <span v-if="post?.categoryName" class="post-badge">{{ post.categoryName }}</span>
            </div>

            <!-- nội dung: tiêu đề + trích đoạn -->
            <div class="post-title">{{ post?.title }}</div>
            <div v-if="excerpt" class="text-sm mt-1.5 line-clamp-2 whitespace-pre-wrap measure">{{ excerpt }}</div>

            <PollBox v-if="post?.poll" :poll="post.poll" :post-id="post.postId" @update:poll="(p) => (post.poll = p)" />

            <!-- hashtag -->
            <div v-if="hashtags.length" class="tag-list mt-3">
                <button
                    v-for="t in hashtags"
                    :key="t"
                    class="tag-chip tag-chip--sm"
                    @click.stop="goTag(t)"
                >{{ t }}</button>
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
            <ReactionBar
                :my-reaction="post?.myReaction"
                :counts="post?.reactionCounts || {}"
                @click.stop
                @react="reactPost"
                @unreact="unreactPost"
            />
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
            <span v-if="post?.views > 0" class="text-xs muted ml-auto tnum">{{ post.views.toLocaleString('vi-VN') }} lượt xem</span>
            </div>
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
            :categoryId="post.categoryId || ''"
            @close="() => { editing = false; emit('refresh') }"
            @post-fail="showDialog?.('Thông báo', 'Cập nhật bài viết thất bại')"
        />
    </AppModal>
  </div>
</template>

<script setup>
import { computed, ref, inject } from "vue";
import AppModal from '@/components/ui/AppModal.vue';
import { calculateTimeDifference, formatTime } from '../../js/helper';
import { IMAGE_BASE } from '@/config';
import { useRouter } from 'vue-router';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { repostPost, unrepostPost, likePostApi, unLikePostApi } from '@/apis/post';
import ReactionBar from '@/components/ReactionBar.vue';
import { avatarUpdates } from '@/storages/appState';
import EditPost from '@/components/Post/EditPost.vue';
import AppIcon from '@/components/AppIcon.vue';
import PollBox from '@/components/Post/PollBox.vue';

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

const needLogin = () => {
    if (isLogin.value) return false;
    route.push('/login');
    return true;
}

const reactPost = async (type) => {
    if (needLogin()) return;
    try {
        await likePostApi(post.value.postId, type);
        const prevType = post.value.myReaction;
        const counts = { ...(post.value.reactionCounts || {}) };
        if (prevType) counts[prevType] = Math.max(0, (counts[prevType] || 0) - 1);
        counts[type] = (counts[type] || 0) + 1;
        post.value.reactionCounts = counts;
        post.value.myReaction = type;
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thao tác thất bại');
    }
}

const unreactPost = async () => {
    if (needLogin()) return;
    try {
        await unLikePostApi(post.value.postId);
        const prevType = post.value.myReaction;
        if (prevType) {
            const counts = { ...(post.value.reactionCounts || {}) };
            counts[prevType] = Math.max(0, (counts[prevType] || 0) - 1);
            post.value.reactionCounts = counts;
        }
        post.value.myReaction = null;
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thao tác thất bại');
    }
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
