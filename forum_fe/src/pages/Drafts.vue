<template>
    <div class="page">
        <div class="card">
            <div class="section-title">Bản nháp ({{ drafts.length }})</div>
            <div v-if="loading" class="state">Đang tải…</div>
            <div v-else-if="!drafts.length" class="state">Bạn chưa có bản nháp nào</div>

            <div
                v-for="d in drafts"
                :key="d.postId"
                class="py-3 border-b last:border-b-0"
            >
                <div class="font-semibold text-[#2577b1]">{{ d.title || '(chưa có tiêu đề)' }}</div>
                <div class="muted text-sm line-clamp-2 whitespace-pre-wrap">{{ d.body || '(chưa có nội dung)' }}</div>
                <div class="text-xs muted mt-1">Sửa lần cuối: {{ timeAgo(d.postedAt) }}</div>
                <div class="flex gap-2 mt-2">
                    <DxButton type="default" text="Đăng" @click="() => doPublish(d)" />
                    <DxButton stylingMode="outlined" text="Sửa" @click="() => openEdit(d)" />
                    <DxButton type="danger" stylingMode="text" text="Xoá" @click="() => confirmDelete(d)" />
                </div>
            </div>
        </div>

        <DxPopup
            title="Sửa bản nháp"
            v-model:visible="editing"
            :width="700"
            :height="420"
            :hide-on-outside-click="true"
        >
            <EditPost
                v-if="editTarget"
                :postId="editTarget.postId"
                :title="editTarget.title"
                :body="editTarget.body"
                @close="() => { editing = false; load(); }"
                @post-fail="() => { editing = false; showDialog?.('Thông báo', 'Cập nhật thất bại'); }"
            />
        </DxPopup>
    </div>
</template>

<script setup>
import { onMounted, ref, inject } from 'vue';
import { DxButton, DxPopup } from 'devextreme-vue';
import { getDrafts, publishPost, deletePost } from '@/apis/post';
import { timeAgo } from '@/js/helper';
import EditPost from '@/components/Post/EditPost.vue';

const showDialog = inject('openDialogError', null);
const openConfirm = inject('openConfirm', null);
const toast = inject('toast', null);

const drafts = ref([]);
const loading = ref(false);
const editing = ref(false);
const editTarget = ref(null);

const load = async () => {
    loading.value = true;
    try {
        drafts.value = (await getDrafts())?.data?.data || [];
    } catch (e) {
        drafts.value = [];
    } finally {
        loading.value = false;
    }
};

const doPublish = async (d) => {
    try {
        await publishPost(d.postId);
        toast?.('Đã đăng bài');
        drafts.value = drafts.value.filter((x) => x.postId !== d.postId);
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
            toast?.('Đã xoá bản nháp');
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Xoá thất bại');
        }
    }, { danger: true, confirmText: 'Xoá' });
};

onMounted(load);
</script>
