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
                style="height: 120px; border-top-left-radius: 0; border-top-right-radius: 0"
                class="field"
                v-model="data.body"
            ></textarea>
        </div>
        <div>
            <label class="text-sm muted">Ảnh đính kèm (để trống nếu giữ nguyên)</label>
            <input type="file" accept="image/*" multiple @change="onFile" />
            <p v-if="data.postImgs.length" class="muted text-xs mt-1">
                Đã chọn {{ data.postImgs.length }} ảnh — sẽ THAY toàn bộ ảnh cũ
            </p>
        </div>
        <div class="flex items-center gap-2">
            <label class="text-sm muted">Ai xem được</label>
            <select v-model="data.visibility" class="text-sm border rounded-lg px-2 py-1 bg-[var(--surface)]">
                <option value="public">🌐 Mọi người</option>
                <option value="friends">👥 Chỉ bạn bè</option>
                <option value="private">🔒 Chỉ mình tôi</option>
            </select>
        </div>
        <div v-if="tags.length" class="flex flex-wrap items-center gap-1.5 text-xs">
            <span class="muted">Hashtag:</span>
            <span v-for="t in tags" :key="t" class="tag-chip tag-chip--sm">{{ t }}</span>
        </div>
        <div class="flex justify-end">
            <button type="button" class="sign-btn" @click="submitEdit">Cập nhật</button>
        </div>
    </div>
</template>

<script setup>
import { updatePost, getCategories } from '@/apis/post';
import { ref, computed, watch, onMounted } from 'vue';
import { extractHashtags } from '@/js/helper';
import MarkdownToolbar from '@/components/Post/MarkdownToolbar.vue';

const emits = defineEmits(['close', 'post-fail']);
const props = defineProps({
    postId: {},
    title: {},
    body: {},
    visibility: { default: 'public' },
    categoryId: { default: '' },
});

const bodyEl = ref(null);
const data = ref({
    title: props.title ?? '',
    body: props.body ?? '',
    postImgs: [],
    visibility: props.visibility || 'public',
    categoryId: props.categoryId || '',
});

const categories = ref([]);
onMounted(async () => {
    try { categories.value = (await getCategories())?.data?.data || []; }
    catch (e) { /* dropdown rỗng vẫn sửa được, không cần báo lỗi */ }
});

// Đồng bộ lại khi props tới sau (popup mount trước lúc bài viết tải xong / đổi bài khác)
watch(
    () => [props.postId, props.title, props.body, props.visibility, props.categoryId],
    () => {
        data.value.title = props.title ?? '';
        data.value.body = props.body ?? '';
        data.value.visibility = props.visibility || 'public';
        data.value.categoryId = props.categoryId || '';
        data.value.postImgs = [];
    },
    { immediate: true }
);

const tags = computed(() => extractHashtags(data.value.title, data.value.body));

const MAX_IMAGES = 8;
const onFile = (e) => {
    data.value.postImgs = Array.from(e.target.files || []).slice(0, MAX_IMAGES);
}

const submitEdit = async () => {
    try {
        const payload = {
            title: data.value.title,
            body: data.value.body,
            visibility: data.value.visibility,
            // Luôn gửi (kể cả rỗng): backend phân biệt "trường vắng mặt" (giữ nguyên)
            // với "chuỗi rỗng" (bỏ chuyên mục) — form sửa bài luôn biết rõ lựa chọn
            // hiện tại nên luôn gửi thẳng giá trị đó, không cần phân biệt gì thêm.
            categoryId: data.value.categoryId || '',
        };
        // Có chọn ảnh mới = THAY toàn bộ bộ ảnh cũ; không chọn thì giữ nguyên.
        if (data.value.postImgs.length) payload.postImgs = data.value.postImgs;
        await updatePost(props.postId, payload);
        emits("close");
    } catch {
        emits("post-fail");
    }
}
</script>
