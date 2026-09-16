<template>
    <div class="flex flex-col gap-3 py-3">
        <div>
            <label class="text-sm muted">Tiêu đề</label>
            <input type="text" class="field" v-model="data.title" />
        </div>
        <div class="flex items-center gap-2">
            <label class="text-sm muted flex-none">Chuyên mục</label>
            <select v-model="data.categoryId" class="field flex-1" style="padding: 6px 10px">
                <option value="">— Không chọn —</option>
                <option v-for="c in categories" :key="c.categoryId" :value="c.categoryId">{{ c.name }}</option>
            </select>
        </div>
        <div>
            <label class="text-sm muted">Nội dung</label>
            <MarkdownToolbar :textarea="bodyEl" />
            <textarea
                ref="bodyEl"
                style="height: 140px; border-top-left-radius: 0; border-top-right-radius: 0"
                class="field"
                v-model="data.body"
            ></textarea>
            <p class="muted text-xs mt-1">Hỗ trợ markdown: **đậm**, _nghiêng_, &gt; trích dẫn, `code`, [chữ](link).</p>
        </div>
        <div>
            <label class="text-sm muted">Ảnh đính kèm (tuỳ chọn, tối đa 8)</label>
            <input type="file" accept="image/*" multiple @change="onFile" />
            <p v-if="data.postImgs.length" class="muted text-xs mt-1">Đã chọn {{ data.postImgs.length }} ảnh</p>
        </div>
        <div class="flex items-center gap-2">
            <label class="text-sm muted">Ai xem được</label>
            <select v-model="data.visibility" class="text-sm border rounded-lg px-2 py-1 bg-[var(--surface)]">
                <option value="public">🌐 Mọi người</option>
                <option value="friends">👥 Chỉ bạn bè</option>
                <option value="private">🔒 Chỉ mình tôi</option>
            </select>
        </div>
        <div>
            <div class="flex items-center gap-2">
                <label class="text-sm muted flex-1">Bình chọn (tuỳ chọn)</label>
                <button v-if="!pollOn" type="button" class="sign-btn sign-btn--quiet" @click="openPoll">
                    <AppIcon name="check" :size="16" /> Thêm bình chọn
                </button>
                <button v-else type="button" class="sign-btn sign-btn--quiet sign-btn--quiet-danger" @click="pollOn = false">
                    Bỏ bình chọn
                </button>
            </div>
            <div v-if="pollOn" class="flex flex-col gap-2 mt-2">
                <p class="muted text-xs">Câu hỏi chính là tiêu đề bài viết. Mỗi người bỏ được một phiếu.</p>
                <div v-for="(o, i) in pollOptions" :key="i" class="flex items-center gap-2">
                    <input type="text" class="field flex-1" :placeholder="`Phương án ${i + 1}`"
                           maxlength="200" v-model="pollOptions[i]" />
                    <button type="button" class="icon-btn icon-btn--danger" title="Xoá phương án"
                            :disabled="pollOptions.length <= 2" @click="pollOptions.splice(i, 1)">
                        <AppIcon name="trash" :size="16" />
                    </button>
                </div>
                <button v-if="pollOptions.length < 10" type="button" class="sign-btn sign-btn--outline self-start"
                        @click="pollOptions.push('')">
                    <AppIcon name="plus" :size="16" /> Thêm phương án
                </button>
            </div>
        </div>

        <div v-if="tags.length" class="flex flex-wrap items-center gap-1.5 text-xs">
            <span class="muted">Hashtag:</span>
            <span v-for="t in tags" :key="t" class="tag-chip tag-chip--sm">{{ t }}</span>
        </div>
        <p v-if="willBePending" class="text-xs muted">
            Đây là bài đầu tiên của bạn — bài sẽ hiện sau khi được quản trị viên duyệt.
        </p>
        <div class="flex justify-end gap-2">
            <button type="button" class="sign-btn sign-btn--outline" :disabled="busy" @click="() => submit(true)">Lưu nháp</button>
            <button type="button" class="sign-btn" :disabled="busy" @click="() => submit(false)">Đăng bài</button>
        </div>
    </div>
</template>

<script setup>
import AppIcon from '@/components/AppIcon.vue';
import MarkdownToolbar from '@/components/Post/MarkdownToolbar.vue';
import { createdPost, getCategories, getPostsByUser } from '@/apis/post';
import { ref, computed, inject, onMounted } from 'vue';
import { extractHashtags } from '@/js/helper';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';

const emits = defineEmits(['close', 'post-fail']);
const showDialog = inject('openDialogError', null);
const toast = inject('toast', null);

const busy = ref(false);
const bodyEl = ref(null);
const data = ref({
    title: "",
    body: "",
    postImgs: [],
    visibility: "public",
    categoryId: "",
});

const categories = ref([]);
onMounted(async () => {
    try { categories.value = (await getCategories())?.data?.data || []; }
    catch (e) { /* dropdown rỗng vẫn đăng bài được bình thường, không cần báo lỗi */ }
});

// Gợi ý riêng cho tài khoản CHƯA từng có bài lọt duyệt — hỏi thẳng API
// (GET /post/by-user trả đúng "total" theo PUBLISHED, khớp luật firstPostStatus
// ở backend) thay vì đoán bằng localStorage: một cờ lưu máy sẽ nói SAI ngay khi
// người dùng lâu năm đổi máy hoặc xoá cache trình duyệt, làm họ tưởng nhầm bài
// của mình phải chờ duyệt. Chỉ là gợi ý hiển thị — quyết định thật vẫn ở backend,
// sai một nhịp ở đây cũng không hỏng gì, chỉ là dòng chữ không khớp.
const willBePending = ref(false);
onMounted(async () => {
    try {
        const myId = getItemLocal(LOCALKEYS.USER_ID);
        const total = (await getPostsByUser(myId, 0, 1))?.data?.data?.total ?? 0;
        willBePending.value = total === 0;
    } catch (e) { /* không đoán được thì thôi, không chặn đăng bài vì việc này */ }
});

const tags = computed(() => extractHashtags(data.value.title, data.value.body));

/* ---------- bình chọn ---------- */
const pollOn = ref(false);
const pollOptions = ref([]);
const openPoll = () => {
    pollOn.value = true;
    // Hai ô trống ngay từ đầu: bình chọn cần tối thiểu 2 phương án, đừng bắt
    // người dùng bấm thêm hai lần mới hiểu điều đó.
    if (!pollOptions.value.length) pollOptions.value = ['', ''];
};
// Backend cũng lọc lại, nhưng chặn sớm ở đây để báo lỗi ngay thay vì đăng ra một
// bài có bình chọn hụt.
const validOptions = computed(() => pollOptions.value.map((o) => o.trim()).filter(Boolean));

const MAX_IMAGES = 8;
const onFile = (e) => {
    // Cắt ở 8 ngay tại đây để người dùng thấy con số thật, thay vì để backend
    // lặng lẽ bỏ bớt sau khi đã tải lên.
    data.value.postImgs = Array.from(e.target.files || []).slice(0, MAX_IMAGES);
}

const submit = async (draft) => {
    if (draft && !data.value.title?.trim() && !data.value.body?.trim()) {
        showDialog?.('Thông báo', 'Bản nháp cần ít nhất tiêu đề hoặc nội dung');
        return;
    }
    if (pollOn.value && validOptions.value.length < 2) {
        showDialog?.('Thông báo', 'Bình chọn cần ít nhất 2 phương án');
        return;
    }
    busy.value = true;
    try {
        const payload = { ...data.value };
        if (!payload.categoryId) delete payload.categoryId;
        if (pollOn.value) payload.pollOptions = validOptions.value;
        if (draft) payload.draft = 'true';
        await createdPost(payload);
        if (draft) toast?.('Đã lưu bản nháp');
        emits("close");
    } catch (e) {
        if (draft) showDialog?.('Thông báo', e?.description || 'Lưu nháp thất bại');
        else emits("post-fail", e?.description);
    } finally {
        busy.value = false;
    }
}
</script>
