<template>
    <div class="flex gap-3 rounded-lg -mx-1 px-1 transition-colors" :id="'comment-' + (comment.commentId || '')">
        <BaseAvatar
            :linkAvt="linkAvt"
            :userCreatedPost="userComments"
            :userId="comment?.userComments"
            :is-show="false"
        />
        <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2 flex-wrap">
                <span
                    class="font-semibold text-[15px] cursor-pointer hover:underline"
                    @click="goAuthor"
                >
                    {{ userComments || '—' }}
                </span>
                <span v-if="comment?.commentAt" class="text-xs muted" :title="formatDateTime(comment.commentAt)">
                    · {{ timeAgo(comment.commentAt) }}
                </span>
                <span v-if="comment?.editedAt" class="text-xs muted" :title="formatDateTime(comment.editedAt)">· đã sửa</span>
            </div>
            <div v-if="comment.content" class="mt-0.5 whitespace-pre-wrap">
                <template v-for="(p, i) in contentParts" :key="i"><span
                    v-if="p.mention" class="text-[var(--brand)] font-semibold cursor-pointer hover:underline"
                    @click="() => route.push('/user/' + p.id)">@{{ p.mention }}</span><template v-else>{{ p.t }}</template></template>
            </div>
            <img
                v-if="commentImgUrl"
                :src="commentImgUrl"
                class="mt-1.5 max-w-xs rounded-lg border cursor-zoom-in"
                @click="openLightbox(commentImgUrl)"
                @error="(e) => e.target.style.display = 'none'"
            />

            <div class="row-actions mt-1.5 text-xs items-center">
                <ReactionBar
                    :my-reaction="comment.myReaction"
                    :counts="comment.reactionCounts || {}"
                    @react="reactComment"
                    @unreact="unreactComment"
                />
                <template v-if="!isReply && isLogin">
                    <button class="link" @click="showReply = !showReply">Trả lời</button>
                </template>
                <template v-if="isOwner">
                    <span class="text-gray-300">|</span>
                    <button class="link" @click="isShowEditComment = true">Sửa</button>
                    <button class="link text-[var(--danger)]" @click="confirmDelete">Xoá</button>
                </template>
                <template v-else-if="isLogin">
                    <span class="text-gray-300">|</span>
                    <button class="link text-[var(--danger)]" @click="confirmReport">Báo cáo</button>
                </template>
            </div>

            <!-- ô trả lời -->
            <div v-if="showReply" class="flex items-center gap-2 mt-2">
                <DxTextBox v-model="replyText" class="flex-1" placeholder="Viết trả lời…" @enter-key="sendReply" />
                <DxButton type="default" text="Gửi" @click="sendReply" />
                <DxButton stylingMode="text" text="Huỷ" @click="() => { showReply = false; replyText = '' }" />
            </div>

            <!-- danh sách trả lời -->
            <div v-if="replies.length" class="mt-2 pl-3 border-l-2 border-[var(--border)] flex flex-col gap-3">
                <Comment
                    v-for="r in replies"
                    :key="r.commentId"
                    :commentProps="r"
                    :is-reply="true"
                    :post-id="postId"
                    @refresh="() => emits('refresh')"
                />
            </div>
        </div>

        <DxPopup
            title="Chỉnh sửa bình luận"
            v-model:visible="isShowEditComment"
            :width="600"
            :height="300"
            :hide-on-outside-click="true"
        >
            <EditComments
                :commentId="comment?.commentId"
                :content="comment?.content"
                @close="() => { isShowEditComment = false; emits('refresh') }"
                @update-fail="() => { isShowEditComment = false }"
            />
        </DxPopup>
    </div>
</template>

<script setup>
import { ref, computed, inject } from 'vue';
import { useRouter } from 'vue-router';
import { timeAgo, formatDateTime } from '@/js/helper';
import { DxPopup, DxTextBox, DxButton } from 'devextreme-vue';
import { likeCommentApi, unLikeCommentApi, deleteComment, createComment } from '@/apis/comment';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { IMAGE_BASE } from '@/config';
import BaseAvatar from '../BaseAvatar.vue';
import EditComments from './EditComments.vue';
import ReactionBar from '@/components/ReactionBar.vue';

const props = defineProps({
    commentProps: { type: Object },
    replies: { type: Array, default: () => [] },
    isReply: { type: Boolean, default: false },
    postId: { type: String, default: '' },
})

const emits = defineEmits(['refresh']);
const route = useRouter();

const comment = computed(() => props.commentProps || {});
// Tên + avatar tác giả đi kèm trong DTO -> không gọi /user/{id} cho từng bình luận
const userComments = computed(() => comment.value?.authorName || '');
const linkAvt = computed(() => (comment.value?.authorAvatar ? IMAGE_BASE + comment.value.authorAvatar : ''));
const isLogin = computed(() => getItemLocal(LOCALKEYS.ACCESS_TOKEN) != null);
const isShowEditComment = ref(false);
const showReply = ref(false);
const replyText = ref('');
const showDialog = inject("openDialogError");
const openConfirm = inject("openConfirm");
const openReport = inject("openReport");
const toast = inject("toast");
const openLightbox = inject("openLightbox", () => {});

const myId = getItemLocal(LOCALKEYS.USER_ID);
const isOwner = computed(() => comment.value?.userComments == myId);
const commentImgUrl = computed(() => {
    const p = comment.value?.commentImg;
    return p && !String(p).includes('null') ? IMAGE_BASE + p : '';
});

// tách @[Tên](userId) thành phần văn bản + phần nhắc tên
const contentParts = computed(() => {
    const text = comment.value?.content || '';
    const re = /@\[([^\]]+)\]\(([0-9a-fA-F-]{8,})\)/g;
    const out = [];
    let last = 0, m;
    while ((m = re.exec(text)) !== null) {
        if (m.index > last) out.push({ t: text.slice(last, m.index) });
        out.push({ mention: m[1], id: m[2] });
        last = m.index + m[0].length;
    }
    if (last < text.length) out.push({ t: text.slice(last) });
    return out;
});

const goAuthor = () => {
    if (comment.value?.userComments) route.push(`/user/${comment.value.userComments}`);
}

const needLogin = () => {
    if (isLogin.value) return false;
    route.push('/login');
    return true;
}

const reactComment = async (type) => {
    if (needLogin()) return;
    try {
        await likeCommentApi(comment.value.commentId, type);
        emits('refresh');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thao tác thất bại');
    }
}
const unreactComment = async () => {
    if (needLogin()) return;
    try {
        await unLikeCommentApi(comment.value.commentId);
        emits('refresh');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thao tác thất bại');
    }
}
const sendReply = async () => {
    const text = (replyText.value || '').trim();
    if (!text) return;
    try {
        await createComment(props.postId, { content: text, parentId: comment.value.commentId });
        replyText.value = '';
        showReply.value = false;
        emits('refresh');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Gửi trả lời thất bại');
    }
}

const confirmReport = () => {
    openReport?.('COMMENT', comment.value.commentId, 'bình luận');
}

const confirmDelete = () => {
    openConfirm?.('Xoá bình luận', 'Xoá bình luận này?', async () => {
        try {
            await deleteComment(comment.value.commentId);
            toast?.('Đã xoá bình luận');
            emits('refresh');
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Xoá thất bại');
        }
    }, { danger: true, confirmText: 'Xoá' });
}
</script>
