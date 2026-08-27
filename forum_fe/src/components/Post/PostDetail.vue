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
                        <DxButton
                            v-else-if="post?.userCreatedPost"
                            icon="warning" type="danger" stylingMode="text" hint="Báo cáo bài viết"
                            @click="reportPost"
                        />
                    </div>

                    <div class="text-sm muted mt-0.5">
                        {{ calculateTimeDifference(post?.postedAt) }} trước
                    </div>

                    <div class="my-3 whitespace-pre-wrap">{{ post?.body }}</div>

                    <img :src="linkPostImg" v-if="post?.postImg" class="max-w-md rounded-xl border cursor-zoom-in" @click="openLightbox(linkPostImg)" />

                    <div class="row-actions mt-3 items-center">
                        <ReactionBar
                            :my-reaction="post?.myReaction"
                            :counts="post?.reactionCounts || {}"
                            @react="reactPost"
                            @unreact="unreactPost"
                        />
                        <DxButton
                            icon="bookmark"
                            :type="bookmarked ? 'success' : 'normal'"
                            :styling-mode="bookmarked ? 'contained' : 'outlined'"
                            :text="bookmarked ? 'Đã lưu' : 'Lưu'"
                            @click="toggleBookmarkBtn"
                        />
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
                            @input="onCommentType" @enter-key="commentPost" @focus-out="onCommentBlur" />
                        <div v-if="mentionOpen && mentionResults.length"
                            class="absolute z-30 left-0 right-0 top-full mt-1 bg-[var(--surface)] border rounded-lg shadow-lg overflow-hidden">
                            <button v-for="u in mentionResults" :key="u.userId" type="button"
                                class="w-full text-left px-3 py-2 text-sm hover:bg-[var(--brand-soft)] truncate flex items-center gap-2"
                                @mousedown.prevent="pickMention(u)">
                                <span class="avatar-fallback size-6 text-xs">{{ (u.fullName || '?')[0] }}</span>
                                <span class="truncate">{{ u.fullName }}</span>
                            </button>
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
                v-for="c in threadedComments"
                :key="c.commentId"
            >
                <Comment :commentProps="c" :replies="c.replies" :post-id="id" @refresh="getDataPostById" />
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
import { onMounted, onBeforeUnmount, ref, computed, inject, watch, nextTick } from 'vue';
import { getPostById } from '@/apis/post';
import { createComment } from '@/apis/comment';
import { likePostApi, unLikePostApi, deletePost } from '@/apis/post';
import { checkBookmark, toggleBookmark } from '@/apis/bookmark';
import { sendReport } from '@/apis/report';
import { getUserInfo, getAllUsers } from '@/apis/user';
import { markReadByTarget } from '@/apis/notification';
import { useRouter } from 'vue-router';
import { calculateTimeDifference } from '@/js/helper';
import Comment from '../comment/Comment.vue';
import { DxTextBox, DxButton, DxPopup } from 'devextreme-vue';
import BaseAvatar from '../BaseAvatar.vue';
import EmojiPicker from '@/components/EmojiPicker.vue';
import ReactionBar from '@/components/ReactionBar.vue';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { activePostId, bumpNotifRefresh } from '@/storages/appState';
import EditPost from '@/components/Post/EditPost.vue';
import { IMAGE_BASE, SOCKET_URL } from '@/config';
import { io } from 'socket.io-client';

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
const bookmarked = ref(false);

/* ---------- @nhắc tên (hiển thị @Tên, gửi kèm id ẩn) ---------- */
const allUsers = ref([]);
const mentionOpen = ref(false);
const mentionResults = ref([]);
const pickedMentions = ref([]);           // [{ name, id }] đã chọn
const MENTION_TAIL = /@([^\s@[\]]{0,30})$/;

const loadUsers = async () => {
    try {
        const res = await getAllUsers();
        allUsers.value = (res?.data?.data || []).filter((u) => u.userId !== getItemLocal(LOCALKEYS.USER_ID));
    } catch (e) { allUsers.value = []; }
};

const detectMention = (val) => {
    const mm = (val || '').match(MENTION_TAIL);
    if (!mm) { mentionOpen.value = false; return; }
    const q = mm[1].toLowerCase();
    mentionResults.value = allUsers.value
        .filter((u) => (u.fullName || '').toLowerCase().includes(q))
        .slice(0, 6);
    mentionOpen.value = mentionResults.value.length > 0;
};

// DxTextBox v-model chỉ đồng bộ khi blur -> đọc trực tiếp từ sự kiện input
const onCommentType = (e) => {
    let v = e?.event?.target?.value;
    if (v == null && e?.component?.option) v = e.component.option('text');
    if (v != null) contentPost.value = v;
};
const onCommentBlur = () => { setTimeout(() => { mentionOpen.value = false; }, 120); };

watch(contentPost, detectMention);

// đổi ?comment= khi vẫn đang ở trang bài viết -> cuộn lại
watch(() => route.currentRoute.value.query.comment, (c) => { if (c) scrollToComment(); });

const pickMention = (u) => {
    contentPost.value = (contentPost.value || '').replace(MENTION_TAIL, `@${u.fullName} `);
    if (!pickedMentions.value.some((p) => p.id === u.userId)) {
        pickedMentions.value.push({ name: u.fullName, id: u.userId });
    }
    mentionOpen.value = false;
};

// đổi "@Tên" -> "@[Tên](id)" cho những người đã chọn, ngay trước khi gửi
const resolveMentions = (text) => {
    let out = text || '';
    for (const { name, id } of pickedMentions.value) {
        const tag = '@' + name;
        if (out.includes(tag)) out = out.split(tag).join(`@[${name}](${id})`);
    }
    return out;
};

const getDataPostById = async() => {
    const data =  await getPostById(id.value);
    post.value = data?.data?.data;
    linkPostImg.value = post.value?.postImg ? IMAGE_BASE + post.value.postImg : '';
}

// gom bình luận thành cây 1 cấp: gốc (mới -> cũ), trả lời (cũ -> mới)
const threadedComments = computed(() => {
    const all = post.value?.comments || [];
    const ids = new Set(all.map((c) => c.commentId));
    const children = {};
    const roots = [];
    for (const c of all) {
        if (c.parentId && ids.has(c.parentId)) (children[c.parentId] ||= []).push(c);
        else roots.push(c);
    }
    const asc = (a, b) => new Date(a.commentAt) - new Date(b.commentAt);
    return roots.map((r) => ({ ...r, replies: (children[r.commentId] || []).slice().sort(asc) }));
});

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
        const payload = { content: resolveMentions(contentPost.value) };
        if (commentImg.value) payload.commentImg = commentImg.value;
        await createComment(id.value, payload);
        contentPost.value = '';
        pickedMentions.value = [];
        mentionOpen.value = false;
        clearCommentImg();
        await getDataPostById();
    } catch (error) {
        showDialog?.('Thông báo', error?.description || 'Gửi bình luận thất bại');
    }
}

const reactPost = async (type) => {
    try {
        await likePostApi(id.value, type);
        await getDataPostById();
    } catch (error) {
        showDialog?.('Thông báo', error?.description || 'Thao tác thất bại');
    }
}
const unreactPost = async () => {
    try {
        await unLikePostApi(id.value);
        await getDataPostById();
    } catch (error) {
        showDialog?.('Thông báo', error?.description || 'Thao tác thất bại');
    }
}

const reportPost = () => {
    openConfirm?.('Báo cáo bài viết', 'Gửi báo cáo bài viết này tới quản trị viên?', async () => {
        try {
            await sendReport('POST', id.value);
            toast?.('Đã gửi báo cáo');
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Báo cáo thất bại');
        }
    }, { danger: true, confirmText: 'Báo cáo' });
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

const loadBookmark = async () => {
    try {
        bookmarked.value = !!(await checkBookmark(id.value))?.data?.data?.bookmarked;
    } catch (e) { bookmarked.value = false; }
}
const toggleBookmarkBtn = async () => {
    try {
        const res = await toggleBookmark(id.value);
        bookmarked.value = !!res?.data?.data?.bookmarked;
        toast?.(bookmarked.value ? 'Đã lưu bài viết' : 'Đã bỏ lưu');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thao tác thất bại');
    }
}

const markPostNotifsRead = async () => {
    try {
        await markReadByTarget(id.value);
        bumpNotifRefresh();
    } catch (e) { /* ignore */ }
}

// cuộn tới đúng bình luận khi mở từ thông báo (?comment=<id>)
const scrollToComment = async () => {
    const cid = route.currentRoute.value.query.comment;
    if (!cid) return;
    await nextTick();
    let tries = 0;
    const tick = () => {
        const el = document.getElementById('comment-' + cid);
        if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            el.classList.add('comment-flash');
            setTimeout(() => el.classList.remove('comment-flash'), 2200);
        } else if (tries++ < 20) {
            setTimeout(tick, 150);
        }
    };
    tick();
}

/* ---------- realtime: bình luận từ người khác ---------- */
let postSocket = null;
let refetchTimer = null;
const connectPostSocket = () => {
    const token = getItemLocal(LOCALKEYS.ACCESS_TOKEN);
    if (!token) return;
    try {
        postSocket = io(SOCKET_URL, { transports: ['websocket'], query: { token, postID: id.value } });
        postSocket.on('post_comments_changed', (p) => {
            if (!p || p.postId !== id.value) return;
            clearTimeout(refetchTimer);
            refetchTimer = setTimeout(getDataPostById, 300);
        });
    } catch (e) { /* ignore */ }
}
const teardownPostSocket = () => {
    clearTimeout(refetchTimer);
    if (postSocket) { postSocket.removeAllListeners(); postSocket.disconnect(); postSocket = null; }
}

onMounted(async() => {
    activePostId.value = id.value;
    loadUsers();
    await getDataPostById();
    await getDataUser();
    loadBookmark();
    // đang xem bài này -> đánh dấu đã đọc các thông báo bình luận / thích của bài
    markPostNotifsRead();
    connectPostSocket();
    scrollToComment();
})

onBeforeUnmount(() => {
    activePostId.value = null;
    teardownPostSocket();
})

</script>