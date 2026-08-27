<template>
    <div class="flex gap-3">
        <BaseAvatar
            :linkAvt="linkAvt"
            :userCreatedPost="userComments"
            :userId="comment?.userComments"
            :is-show="false"
        />
        <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2">
                <span
                    class="font-semibold text-[15px] cursor-pointer hover:underline"
                    @click="goAuthor"
                >
                    {{ userComments || '—' }}
                </span>
                <span class="text-xs muted">· ♥ {{ comment?.commentLikes ?? 0 }}</span>
            </div>
            <div class="mt-0.5 whitespace-pre-wrap">{{ comment.content }}</div>

            <div class="row-actions mt-1.5 text-xs">
                <button class="link" @click="likePost">Yêu thích</button>
                <button class="link" @click="unLikePost">Bỏ thích</button>
                <template v-if="isOwner">
                    <span class="text-gray-300">|</span>
                    <button class="link" @click="isShowEditComment = true">Sửa</button>
                    <button class="link text-[var(--danger)]" @click="handleDeleteComment">Xoá</button>
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

const comment = ref(props.commentProps);
const userComments = ref();
const linkAvt = ref();
const isShowEditComment = ref(false);
const showDialog = inject("openDialogError");

const isOwner = computed(() => comment.value?.userComments == getItemLocal(LOCALKEYS.USER_ID));

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

const likePost = async () => {
    try { await likeCommentApi(comment.value.commentId); emits('refresh'); } catch (e) { console.log(e); }
}
const unLikePost = async () => {
    try { await unLikeCommentApi(comment.value.commentId); emits('refresh'); } catch (e) { console.log(e); }
}
const handleDeleteComment = async () => {
    try { await deleteComment(comment.value.commentId); emits('refresh'); } catch (e) { console.log(e); }
}

onMounted(getDataUser);
</script>
