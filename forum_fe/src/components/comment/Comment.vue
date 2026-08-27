<template>
    <div class="flex gap-3">
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
                <span class="text-xs muted">· {{ comment?.commentLikes ?? 0 }} thích</span>
            </div>
            <div v-if="comment.content" class="mt-0.5 whitespace-pre-wrap">{{ comment.content }}</div>
            <img
                v-if="commentImgUrl"
                :src="commentImgUrl"
                class="mt-1.5 max-w-xs rounded-lg border"
                @error="(e) => e.target.style.display = 'none'"
            />

            <div class="row-actions mt-1.5 text-xs">
                <button
                    class="link"
                    :class="{ 'font-bold text-[var(--danger)]': likedByMe }"
                    @click="toggleLike"
                >
                    {{ likedByMe ? '♥ Đã thích' : '♡ Thích' }}
                </button>
                <template v-if="isOwner">
                    <span class="text-gray-300">|</span>
                    <button class="link" @click="isShowEditComment = true">Sửa</button>
                    <button class="link text-[var(--danger)]" @click="confirmDelete">Xoá</button>
                </template>
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
import { onMounted, ref, computed, inject } from 'vue';
import { useRouter } from 'vue-router';
import { timeAgo, formatDateTime } from '@/js/helper';
import { getUserInfo } from '../../apis/user';
import { DxPopup } from 'devextreme-vue';
import { likeCommentApi, unLikeCommentApi, deleteComment } from '@/apis/comment';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { IMAGE_BASE } from '@/config';
import BaseAvatar from '../BaseAvatar.vue';
import EditComments from './EditComments.vue';

const props = defineProps({
    commentProps: { type: Object }
})

const emits = defineEmits(['refresh']);
const route = useRouter();

const comment = computed(() => props.commentProps || {});
const userComments = ref();
const linkAvt = ref();
const isShowEditComment = ref(false);
const showDialog = inject("openDialogError");
const openConfirm = inject("openConfirm");
const toast = inject("toast");

const myId = getItemLocal(LOCALKEYS.USER_ID);
const isOwner = computed(() => comment.value?.userComments == myId);
const likedByMe = computed(() => (comment.value?.userLikes || []).includes(myId));
const commentImgUrl = computed(() => {
    const p = comment.value?.commentImg;
    return p && !String(p).includes('null') ? IMAGE_BASE + p : '';
});

const getDataUser = async () => {
    try {
        const data = await getUserInfo(comment.value?.userComments);
        userComments.value = data?.data?.data?.fullName;
        linkAvt.value = IMAGE_BASE + data?.data?.data?.avtUrl;
    } catch (e) {
        console.log(e);
    }
}

const goAuthor = () => {
    if (comment.value?.userComments) route.push(`/user/${comment.value.userComments}`);
}

const toggleLike = async () => {
    try {
        if (likedByMe.value) await unLikeCommentApi(comment.value.commentId);
        else await likeCommentApi(comment.value.commentId);
        emits('refresh');
    } catch (e) { console.log(e); }
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

onMounted(getDataUser);
</script>
