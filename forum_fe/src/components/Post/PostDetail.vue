<template>
    <div class="page">
        <div class="card">
            <!-- dòng tác giả -->
            <div class="flex items-center gap-2">
                <BaseAvatar
                    :linkAvt="linkAvt"
                    :userCreatedPost="userCreatedPost"
                    :userId="post?.userCreatedPost"
                    :is-show="false"
                />
                <div class="text-sm muted min-w-0 flex-1 truncate">
                    <span class="link font-medium text-[var(--text)]" @click="goAuthor">{{ userCreatedPost || '—' }}</span>
                    · {{ calculateTimeDifference(post?.postedAt) }} trước
                    <span v-if="post?.editedAt">· đã chỉnh sửa</span>
                    <span v-if="post?.visibility === 'friends'" class="ml-1 px-1.5 py-0.5 rounded bg-gray-100 text-[11px] font-semibold">👥 Chỉ bạn bè</span>
                    <span v-else-if="post?.visibility === 'private'" class="ml-1 px-1.5 py-0.5 rounded bg-gray-100 text-[11px] font-semibold">🔒 Chỉ mình tôi</span>
                </div>
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
                    v-else-if="post?.userCreatedPost && isLogin"
                    icon="warning" type="danger" stylingMode="text" hint="Báo cáo bài viết"
                    @click="reportPost"
                />
            </div>

            <!-- tiêu đề -->
            <div class="font-bold text-3xl leading-snug text-[var(--accent)] mt-2">{{ post?.title }}</div>

            <!-- nội dung -->
            <div class="my-3 whitespace-pre-wrap">{{ post?.body }}</div>

            <div v-if="post?.hashtags?.length" class="flex flex-wrap gap-1.5 mb-3">
                <button
                    v-for="t in post.hashtags"
                    :key="t"
                    class="text-xs font-semibold text-[var(--brand)] bg-[var(--brand-soft)] rounded-full px-2 py-0.5 hover:underline"
                    @click="route.push('/tag/' + encodeURIComponent(t))"
                >#{{ t }}</button>
            </div>

            <img :src="linkPostImg" v-if="post?.postImg" class="max-w-md rounded-xl border cursor-zoom-in" @click="openLightbox(linkPostImg)" />

            <div class="row-actions mt-3 items-center">
                <ReactionBar
                    :my-reaction="post?.myReaction"
                    :counts="post?.reactionCounts || {}"
                    @react="reactPost"
                    @unreact="unreactPost"
                />
                <button
                    v-if="isLogin"
                    type="button"
                    class="act-pill"
                    :class="{ 'is-on': bookmarked }"
                    :title="bookmarked ? 'Bỏ lưu' : 'Lưu bài viết'"
                    @click="toggleBookmarkBtn"
                >
                    <svg viewBox="0 0 24 24" :fill="bookmarked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
                    </svg>
                    {{ bookmarked ? 'Đã lưu' : 'Lưu' }}
                </button>
                <button
                    v-if="isLogin && post?.userCreatedPost !== getItemLocal(LOCALKEYS.USER_ID)"
                    type="button"
                    class="act-pill"
                    :class="{ 'is-on': post?.reposted }"
                    :title="post?.reposted ? 'Bỏ chia sẻ' : 'Chia sẻ'"
                    @click="toggleRepost"
                >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="17 1 21 5 17 9" /><path d="M3 11V9a4 4 0 0 1 4-4h14" />
                        <polyline points="7 23 3 19 7 15" /><path d="M21 13v2a4 4 0 0 1-4 4H3" />
                    </svg>
                    {{ (post?.reposted ? 'Đã chia sẻ' : 'Chia sẻ') + (post?.repostCount ? ` (${post.repostCount})` : '') }}
                </button>
            </div>
        </div>

        <div class="card">
            <div class="section-title">Bình luận ({{ post?.commentsQuantity ?? commentRootTotal }})</div>

            <div v-if="!isLogin" class="pb-3 border-b text-sm muted">
                <span class="link" @click="route.push('/login')">Đăng nhập</span> để bình luận và tương tác.
            </div>

            <div v-else class="pb-3 border-b">
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

            <div v-if="!threadedComments.length" class="state">Chưa có bình luận</div>
            <div
                class="py-3 border-b last:border-b-0"
                v-for="c in threadedComments"
                :key="c.commentId"
            >
                <Comment :commentProps="c" :replies="c.replies" :post-id="id" @refresh="() => loadComments(true)" />
            </div>
            <div v-if="commentPage < commentTotalPages" class="pt-3 text-center">
                <button class="link text-sm" @click="() => loadComments(false)">
                    Xem thêm bình luận cũ hơn ({{ commentRootTotal - loadedRootCount }})
                </button>
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
                :visibility="post?.visibility || 'public'"
                @close="() => { ishowEditPost = false; getDataPostById() }"
                @post-fail="showDialog('Cập nhật bài viết thất bại')"
            />
        </DxPopup>
    </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, computed, inject, watch, nextTick } from 'vue';
import { getPostById } from '@/apis/post';
import { createComment, getCommentsPage } from '@/apis/comment';
import { likePostApi, unLikePostApi, deletePost, repostPost, unrepostPost } from '@/apis/post';
import { checkBookmark, toggleBookmark } from '@/apis/bookmark';
import { getAllUsers } from '@/apis/user';
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

const id = ref(route.currentRoute.value.params.id);
const post = ref();
const linkPostImg = ref();
const contentPost = ref();
const isLogin = computed(() => getItemLocal(LOCALKEYS.ACCESS_TOKEN) != null);
// Tên + avatar tác giả lấy thẳng từ DTO (không gọi /user/{id})
const userCreatedPost = computed(() => post.value?.authorName || '');
const linkAvt = computed(() => (post.value?.authorAvatar ? IMAGE_BASE + post.value.authorAvatar : ''));
const needLogin = () => {
    if (isLogin.value) return false;
    route.push('/login');
    return true;
};
const showDialog = inject("openDialogError");
const openConfirm = inject("openConfirm");
const openReport = inject("openReport");
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
    if (!isLogin.value) return;
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

/* ---------- bình luận theo trang ---------- */
const COMMENT_PAGE_SIZE = 20;
const commentItems = ref([]);           // danh sách phẳng đã tải (nhiều trang cộng dồn)
const commentPage = ref(0);
const commentTotalPages = ref(1);
const commentRootTotal = ref(0);        // tổng số bình luận gốc
const loadedRootCount = computed(() => commentItems.value.filter((c) => !c.parentId).length);

const loadComments = async (reset) => {
    const next = reset ? 1 : commentPage.value + 1;
    try {
        const res = await getCommentsPage(id.value, next, COMMENT_PAGE_SIZE);
        const d = res?.data?.data || {};
        const items = d.items || [];
        commentItems.value = reset ? items : [...commentItems.value, ...items];
        commentPage.value = d.page || next;
        commentTotalPages.value = d.totalPages || 1;
        commentRootTotal.value = d.total || 0;
    } catch (e) {
        if (reset) commentItems.value = [];
    }
}

// gom bình luận thành cây 1 cấp: gốc (mới -> cũ), trả lời (cũ -> mới)
const threadedComments = computed(() => {
    const all = commentItems.value;
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
    if (needLogin()) return;
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
        await loadComments(true);
    } catch (error) {
        showDialog?.('Thông báo', error?.description || 'Gửi bình luận thất bại');
    }
}

const reactPost = async (type) => {
    if (needLogin()) return;
    try {
        await likePostApi(id.value, type);
        await getDataPostById();
    } catch (error) {
        showDialog?.('Thông báo', error?.description || 'Thao tác thất bại');
    }
}
const unreactPost = async () => {
    if (needLogin()) return;
    try {
        await unLikePostApi(id.value);
        await getDataPostById();
    } catch (error) {
        showDialog?.('Thông báo', error?.description || 'Thao tác thất bại');
    }
}

const reportPost = () => {
    openReport?.('POST', id.value, 'bài viết');
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
    if (!isLogin.value) return;
    try {
        bookmarked.value = !!(await checkBookmark(id.value))?.data?.data?.bookmarked;
    } catch (e) { bookmarked.value = false; }
}
const toggleBookmarkBtn = async () => {
    if (needLogin()) return;
    try {
        const res = await toggleBookmark(id.value);
        bookmarked.value = !!res?.data?.data?.bookmarked;
        toast?.(bookmarked.value ? 'Đã lưu bài viết' : 'Đã bỏ lưu');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thao tác thất bại');
    }
}

const toggleRepost = async () => {
    if (needLogin()) return;
    const wasReposted = !!post.value?.reposted;
    try {
        if (wasReposted) await unrepostPost(id.value);
        else await repostPost(id.value);
        await getDataPostById();
        toast?.(wasReposted ? 'Đã bỏ chia sẻ' : 'Đã chia sẻ bài viết');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thao tác thất bại');
    }
}

const markPostNotifsRead = async () => {
    if (!isLogin.value) return;
    try {
        await markReadByTarget(id.value);
        bumpNotifRefresh();
    } catch (e) { /* ignore */ }
}

// cuộn tới đúng bình luận khi mở từ thông báo (?comment=<id>)
const scrollToComment = async () => {
    const cid = route.currentRoute.value.query.comment;
    if (!cid) return;
    // nạp thêm trang cho tới khi có bình luận đó (hoặc hết trang)
    let guard = 0;
    while (!commentItems.value.some((c) => c.commentId === cid)
        && commentPage.value < commentTotalPages.value && guard++ < 30) {
        await loadComments(false);
    }
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
            refetchTimer = setTimeout(() => { getDataPostById(); loadComments(true); }, 300);
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
    loadBookmark();
    // bài + bình luận độc lập nhau -> tải song song
    await Promise.all([getDataPostById(), loadComments(true)]);
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