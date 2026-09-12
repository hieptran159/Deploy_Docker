<template>
    <div class="flex flex-col gap-3 py-3">
        <div>
            <label class="text-sm muted">Tiêu đề</label>
            <input type="text" class="field" v-model="data.title" />
        </div>
        <div>
            <label class="text-sm muted">Nội dung</label>
            <textarea style="height: 120px" class="field" v-model="data.body"></textarea>
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
            <span v-for="t in tags" :key="t" class="tag-chip tag-chip--sm">#{{ t }}</span>
        </div>
        <div class="flex justify-end">
            <button type="button" class="sign-btn" @click="submitEdit">Cập nhật</button>
        </div>
    </div>
</template>

<script setup>
import { updatePost } from '@/apis/post';
import { ref, computed, watch } from 'vue';
import { extractHashtags } from '@/js/helper';

const emits = defineEmits(['close', 'post-fail']);
const props = defineProps({
    postId: {},
    title: {},
    body: {},
    visibility: { default: 'public' },
});

const data = ref({
    title: props.title ?? '',
    body: props.body ?? '',
    postImgs: [],
    visibility: props.visibility || 'public',
});

// Đồng bộ lại khi props tới sau (popup mount trước lúc bài viết tải xong / đổi bài khác)
watch(
    () => [props.postId, props.title, props.body, props.visibility],
    () => {
        data.value.title = props.title ?? '';
        data.value.body = props.body ?? '';
        data.value.visibility = props.visibility || 'public';
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
        const payload = { title: data.value.title, body: data.value.body, visibility: data.value.visibility };
        // Có chọn ảnh mới = THAY toàn bộ bộ ảnh cũ; không chọn thì giữ nguyên.
        if (data.value.postImgs.length) payload.postImgs = data.value.postImgs;
        await updatePost(props.postId, payload);
        emits("close");
    } catch {
        emits("post-fail");
    }
}
</script>
