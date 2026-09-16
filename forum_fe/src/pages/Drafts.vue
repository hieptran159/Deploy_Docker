<template>
    <div class="page">
        <!-- Bài chờ duyệt: không có ở feed, "Bài viết của tôi" hay bản nháp — không
             có khối này thì bài "biến mất" ngay sau khi đăng, không dấu vết. Chỉ đọc,
             sửa/xoá thì vào thẳng bài (đường /post/{id} đã cho tác giả xem bài pending). -->
        <div v-if="pending.length" class="card">
            <div class="section-title">Đang chờ duyệt ({{ pending.length }})</div>
            <p class="muted text-sm mb-2">Quản trị viên duyệt xong, bài sẽ tự hiện trên bảng tin.</p>
            <div v-for="p in pending" :key="p.postId" class="py-2 border-b last:border-b-0">
                <router-link :to="`/post/${p.postId}`" class="link font-semibold">{{ p.title || '(chưa có tiêu đề)' }}</router-link>
                <div class="text-xs muted mt-1">Đăng {{ timeAgo(p.postedAt) }}</div>
            </div>
        </div>

        <div class="card">
            <div class="flex flex-wrap items-center gap-2 mb-1">
                <div class="section-title flex-1">Bản nháp ({{ total }})</div>
                <template v-if="total > 0">
                    <button type="button" class="sign-btn sign-btn--outline" :disabled="busy" @click="confirmPublishAll">Đăng tất cả</button>
                    <button type="button" class="sign-btn sign-btn--outline sign-btn--outline-danger" :disabled="busy" @click="confirmDeleteAll">Xoá tất cả</button>
                </template>
            </div>

            <div v-if="loading && !drafts.length" class="state">Đang tải…</div>
            <div v-else-if="!drafts.length" class="state">Bạn chưa có bản nháp nào</div>

            <div
                v-for="d in drafts"
                :key="d.postId"
                class="py-3 border-b last:border-b-0"
            >
                <div class="post-title post-title--sm !mt-0">{{ d.title || '(chưa có tiêu đề)' }}</div>
                <div class="muted text-sm line-clamp-2 whitespace-pre-wrap">{{ d.body || '(chưa có nội dung)' }}</div>
                <div class="text-xs muted mt-1">Sửa lần cuối: {{ timeAgo(d.postedAt) }}</div>
                <div class="flex gap-2 mt-2">
                    <button type="button" class="sign-btn" @click="() => doPublish(d)">Đăng</button>
                    <button type="button" class="sign-btn sign-btn--outline" @click="() => openEdit(d)">Sửa</button>
                    <button type="button" class="sign-btn sign-btn--quiet sign-btn--quiet-danger" @click="() => confirmDelete(d)">Xoá</button>
                </div>
            </div>

            <div v-if="drafts.length < total" class="pt-3 text-center">
                <button class="link text-sm" :disabled="loading" @click="loadMore">
                    {{ loading ? 'Đang tải…' : `Xem thêm (${total - drafts.length})` }}
                </button>
            </div>
        </div>

        <AppModal
            v-if="editing && editTarget"
            title="Sửa bản nháp"
            v-model:open="editing"
            :width="700"
        >
            <EditPost
                :postId="editTarget.postId"
                :title="editTarget.title"
                :body="editTarget.body"
                :visibility="editTarget.visibility"
                @close="() => { editing = false; editTarget = null; reload(); }"
                @post-fail="() => { editing = false; editTarget = null; showDialog?.('Thông báo', 'Cập nhật thất bại'); }"
            />
        </AppModal>
    </div>
</template>

<script setup>
import { onMounted, ref, inject } from 'vue';
import AppModal from '@/components/ui/AppModal.vue';
import { getDrafts, publishPost, deletePost, publishAllDrafts, deleteAllDrafts, getMyPendingPosts } from '@/apis/post';
import { timeAgo } from '@/js/helper';
import EditPost from '@/components/Post/EditPost.vue';

const PAGE_SIZE = 20;

const showDialog = inject('openDialogError', null);
const openConfirm = inject('openConfirm', null);
const toast = inject('toast', null);

const drafts = ref([]);
const pending = ref([]);
const total = ref(0);
const page = ref(0);
const loading = ref(false);
const busy = ref(false);
const editing = ref(false);
const editTarget = ref(null);

const fetchPage = async (p) => {
    loading.value = true;
    try {
        const d = (await getDrafts(p, PAGE_SIZE))?.data?.data || {};
        const items = d.items || [];
        drafts.value = p === 0 ? items : [...drafts.value, ...items];
        total.value = d.total ?? drafts.value.length;
        page.value = p;
    } catch (e) {
        if (p === 0) { drafts.value = []; total.value = 0; }
    } finally {
        loading.value = false;
    }
};

const reload = () => fetchPage(0);
const loadMore = () => fetchPage(page.value + 1);

const doPublish = async (d) => {
    try {
        await publishPost(d.postId);
        toast?.('Đã đăng bài');
        drafts.value = drafts.value.filter((x) => x.postId !== d.postId);
        total.value = Math.max(0, total.value - 1);
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Đăng bài thất bại');
    }
};

const openEdit = (d) => {
    editTarget.value = d;
    editing.value = true;
};

const confirmDelete = (d) => {
    openConfirm?.('Xoá bản nháp', 'Xoá bản nháp này?', async () => {
        try {
            await deletePost(d.postId);
            drafts.value = drafts.value.filter((x) => x.postId !== d.postId);
            total.value = Math.max(0, total.value - 1);
            toast?.('Đã xoá bản nháp');
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Xoá thất bại');
        }
    }, { danger: true, confirmText: 'Xoá' });
};

const confirmPublishAll = () => {
    openConfirm?.('Đăng tất cả bản nháp', `Đăng toàn bộ ${total.value} bản nháp? (bỏ qua bài thiếu tiêu đề hoặc nội dung)`, async () => {
        busy.value = true;
        try {
            const r = (await publishAllDrafts())?.data?.data || {};
            toast?.(`Đã đăng ${r.published || 0} bài` + (r.skipped ? `, bỏ qua ${r.skipped}` : ''));
            await reload();
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Đăng tất cả thất bại');
        } finally {
            busy.value = false;
        }
    }, { confirmText: 'Đăng tất cả' });
};

const confirmDeleteAll = () => {
    openConfirm?.('Xoá tất cả bản nháp', `Xoá vĩnh viễn toàn bộ ${total.value} bản nháp? Không thể hoàn tác.`, async () => {
        busy.value = true;
        try {
            const r = (await deleteAllDrafts())?.data?.data || {};
            toast?.(`Đã xoá ${r.deleted || 0} bản nháp`);
            await reload();
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Xoá tất cả thất bại');
        } finally {
            busy.value = false;
        }
    }, { danger: true, confirmText: 'Xoá tất cả' });
};

onMounted(async () => {
    reload();
    try { pending.value = (await getMyPendingPosts())?.data?.data || []; }
    catch (e) { /* không tải được thì chỉ ẩn khối này, không chặn xem bản nháp */ }
});
</script>
