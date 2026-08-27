<template>
    <div class="page">
        <div class="card">
            <div class="flex gap-4">
                <div class="flex flex-col items-center gap-1 flex-none w-16">
                    <BaseAvatar
                        :linkAvt="linkAvt"
                        :userCreatedPost="userCreatedPost"
                        :userId="post?.userCreatedPost"
                        :is-show="false"
                    />
                    <div
                        class="text-xs font-semibold text-center cursor-pointer hover:underline truncate w-full"
                        @click="goAuthor"
                    >
                        {{ userCreatedPost }}
                    </div>
                </div>

                <div class="min-w-0 flex-1">
                    <div class="flex items-start gap-2">
                        <div class="font-bold text-xl text-[var(--accent)] flex-1">{{ post?.title }}</div>
                        <div
                            class="relative flex-none"
                            v-if="post?.userCreatedPost == getItemLocal(LOCALKEYS.USER_ID)"
                        >
                            <DxButton icon="overflow" stylingMode="text" @click="isShowSetting = !isShowSetting" />
                            <div class="absolute right-0 z-20 w-32 flex flex-col bg-white border rounded-lg shadow p-1" v-if="isShowSetting">
                                <DxButton icon="edit" type="default" stylingMode="text" text="Chỉnh sửa" @click="ishowEditPost = true" />
                                <DxButton icon="trash" type="danger" stylingMode="text" text="Xoá" @click="handleDeletePost" />
                            </div>
                        </div>
                    </div>

                    <div class="text-sm muted mt-0.5">
                        {{ calculateTimeDifference(post?.postedAt) }} trước
                    </div>

                    <div class="my-3 whitespace-pre-wrap">{{ post?.body }}</div>

                    <img :src="linkPostImg" v-if="post?.postImg" class="max-w-md rounded-xl border cursor-zoom-in" @click="openLightbox(linkPostImg)" />

                    <div class="row-actions mt-3">
                        <DxButton
                            :icon="likedByMe ? 'like' : 'like'"
                            :type="likedByMe ? 'danger' : 'normal'"
                            :styling-mode="likedByMe ? 'contained' : 'outlined'"
                            :text="likedByMe ? 'Đã thích' : 'Thích'"
                            @click="toggleLike"
                        />
                        <span class="muted text-sm self-center">{{ post?.likesQuantity ?? 0 }} lượt thích</span>
                    </div>
                </div>
            </div>
        </div>

        <div class="card">
            <div class="section-title">Bình luận ({{ post?.comments?.length ?? 0 }})</div>

            <div class="pb-3 border-b">
                <div class="flex items-center gap-3 relative">
                    <BaseAvatar
                        :linkAvt="getItemLocal(LOCALKEYS.LINK_AVT)"
                        :userCreatedPost="getItemLocal(LOCALKEYS.USER_NAME)"
                        :is-show="false"
                    />
                    <div class="flex-1 relative">
                        <DxTextBox class="w-full" placeholder="Viết bình luận… (gõ @ để nhắc tên)" v-model="contentPost"
                            :value-change-event="'input'" @enter-key="commentPost" />
                        <div v-if="mentionOpen && mentionResults.length"
                            class="absolute z-30 left-0 right-0 top-full mt-1 bg-[var(--surface)] border rounded-lg shadow-lg overflow-hidden">
                            <button v-for="u in mentionResults" :key="u.userId" type="button"
                                class="w-full text-left px-3 py-2 text-sm hover:bg-[var(--brand-soft)] truncate"
                                @click="pickMention(u)">@{{ u.fullName }}</button>
                        </div>
                    </div>
                    <EmojiPicker direction="down" @pick="addCommentEmoji" />
                    <DxButton
                        :icon="commentImg ? 'photo' : 'image'"
                        :type="commentImg ? 'success' : 'normal'"
                        stylingMode="text"
                        hint="Đính kèm ảnh"
                        @click="pickCommentImg"
                    />
                    <input ref="commentFileEl" type="file" accept="image/*" class="hidden" @change="onCommentImg" />
                    <DxButton type="default" text="Gửi" @click="commentPost" />
                </div>
                <div v-if="commentImgPreview" class="mt-2 ml-14 relative inline-block">
                    <img :src="commentImgPreview" class="max-h-32 rounded-lg border" />
                    <button
                        class="absolute -top-2 -right-2 size-6 rounded-full bg-[var(--danger)] text-white text-xs leading-6 text-center shadow"
                        title="Bỏ ảnh"
                        @click="clearCommentImg"
                    >✕</button>
                </div>
            </div>

            <div v-if="!post?.comments?.length" class="state">Chưa có bình luận</div>
            <div
                class="py-3 border-b last:border-b-0"
                v-for="comment in post?.comments"
                :key="comment?.commentId"
            >
                <Comment :commentProps="comment" @refresh="getDataPostById" />
            </div>
        </div>

        <DxPopup
            title="Chỉnh sửa bài viết"
            v-model:visible="ishowEditPost"
            :width="700"
            :height="420"
            :hide-on-outside-click="true"
        >
            <EditPost
                :postId="id"
                :body="post?.body"
                :title="post?.title"
                @close="() => { ishowEditPost = false; getDataPostById() }"
                @post-fail="showDialog('Cập nhật bài viết thất bại')"
            />
        </DxPopup>
    </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, computed, inject, watch } from 'vue';
import { getPostById } from '@/apis/post';
import { createComment } from '@/apis/comment';
import { likePostApi, unLikePostApi, deletePost } from '@/apis/post';
import { getUserInfo, getAllUsers } from '@/apis/user';
import { markReadByTarget } from '@/apis/notification';
import { useRouter } from 'vue-router';
import { calculateTimeDifference } from '@/js/helper';
import Comment from '../comment/Comment.vue';
import { DxTextBox, DxButton, DxPopup } from 'devextreme-vue';
import BaseAvatar from '../BaseAvatar.vue';
import EmojiPicker from '@/components/EmojiPicker.vue';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { activePostId, bumpNotifRefresh } from '@/storages/appState';
import EditPost from '@/components/Post/EditPost.vue';
import { IMAGE_BASE } from '@/config';

const  route = useRouter();

const id = ref(route.currentRoute._value.params.id);
const post = ref();
const linkPostImg = ref();
const contentPost = ref();
const userCreatedPost = ref();
const linkAvt = ref();
const showDialog = inject("openDialogError");
const openConfirm = inject("openConfirm");
const toast = inject("toast");
const openLightbox = inject("openLightbox", () => {});
const isShowSetting = ref(false);
const ishowEditPost = ref(false);
const commentImg = ref(null);
const commentImgPreview = ref('');
const commentFileEl = ref(null);

/* ---------- @nhắc tên ---------- */
const allUsers = ref([]);
const mentionOpen = ref(false);
const mentionResults = ref([]);
const MENTION_TAIL = /@([^\s@[\]]{0,20})$/;

const loadUsers = async () => {
    try {
        const res = await getAllUsers();
        allUsers.value = (res?.data?.data || []).filter((u) => u.userId !== getItemLocal(LOCALKEYS.USER_ID));
    } catch (e) { allUsers.value = []; }
};

watch(contentPost, (val) => {
    const mm = (val || '').match(MENTION_TAIL);
    if (!mm) { mentionOpen.value = false; return; }
    const q = mm[1].toLowerCase();
    mentionResults.value = allUsers.value
        .filter((u) => (u.fullName || '').toLowerCase().includes(q))
        .slice(0, 6);
    mentionOpen.value = mentionResults.value.length > 0;
});

const pickMention = (u) => {
    contentPost.value = (contentPost.value || '').replace(MENTION_TAIL, `@[${u.fullName}](${u.userId}) `);
    mentionOpen.value = false;
};

const getDataPostById = async() => {
    const data =  await getPostById(id.value);
    post.value = data?.data?.data;
    linkPostImg.value = post.value?.postImg ? IMAGE_BASE + post.value.postImg : '';
}

const getDataUser = async() => {
    try {
        const data = await getUserInfo(post.value?.userCreatedPost);
        userCreatedPost.value = data?.data?.data?.fullName;
        linkAvt.value = IMAGE_BASE + data?.data?.data?.avtUrl;
    } catch (error) {
        console.error(error);
    }
}

const addCommentEmoji = (e) => { contentPost.value = (contentPost.value || '') + e; };

const pickCommentImg = () => commentFileEl.value?.click();
const onCommentImg = (e) => {
    const f = e.target.files[0] || null;
    commentImg.value = f;
    if (commentImgPreview.value) URL.revokeObjectURL(commentImgPreview.value);
    commentImgPreview.value = f ? URL.createObjectURL(f) : '';
};
const clearCommentImg = () => {
    commentImg.value = null;
    if (commentImgPreview.value) URL.revokeObjectURL(commentImgPreview.value);
    commentImgPreview.value = '';
    if (commentFileEl.value) commentFileEl.value.value = '';
};

const commentPost = async()=> {
    if (!contentPost.value && !commentImg.value) return;
    try {
        const payload = { content: contentPost.value || '' };
        if (commentImg.value) payload.commentImg = commentImg.value;
        await createComment(id.value, payload);
        contentPost.value = '';
        clearCommentImg();
        await getDataPostById();
    } catch (error) {
        showDialog?.('Thông báo', error?.description || 'Gửi bình luận thất bại');
    }
}

const myId = getItemLocal(LOCALKEYS.USER_ID);
const likedByMe = computed(() => (post.value?.userLikedPost || []).includes(myId));

const toggleLike = async() => {
    try {
        if (likedByMe.value) await unLikePostApi(id.value);
        else await likePostApi(id.value);
        await getDataPostById();
    } catch (error) {
        showDialog?.('Thông báo', error?.description || 'Thao tác thất bại');
    }
}

const handleDeletePost = () => {
    openConfirm?.('Xoá bài viết', 'Bạn chắc chắn muốn xoá bài viết này?', async () => {
        try {
            await deletePost(id.value);
            toast?.('Đã xoá bài viết');
            route.push('/');
        } catch (error) {
            showDialog?.('Thông báo', error?.description || 'Xoá bài viết thất bại');
        }
    }, { danger: true, confirmText: 'Xoá' });
}

const goAuthor = () => {
    if (post.value?.userCreatedPost) route.push(`/user/${post.value.userCreatedPost}`);
}

const markPostNotifsRead = async () => {
    try {
        await markReadByTarget(id.value);
        bumpNotifRefresh();
    } catch (e) { /* ignore */ }
}

onMounted(async() => {
    activePostId.value = id.value;
    loadUsers();
    await getDataPostById();
    await getDataUser();
    // đang xem bài này -> đánh dấu đã đọc các thông báo bình luận / thích của bài
    markPostNotifsRead();
})

onBeforeUnmount(() => {
    activePostId.value = null;
})

</script>