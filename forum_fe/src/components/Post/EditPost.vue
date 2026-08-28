<template>
    <div class="flex flex-col gap-3 py-3">
        <div>
            <label class="text-sm muted">Tiêu đề</label>
            <DxTextBox v-model="data.title" />
        </div>
        <div>
            <label class="text-sm muted">Nội dung</label>
            <DxTextArea v-model="data.body" :height="120" />
        </div>
        <div>
            <label class="text-sm muted">Ảnh đính kèm (để trống nếu giữ nguyên)</label>
            <input type="file" accept="image/*" @change="onFile" />
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
            <span v-for="t in tags" :key="t" class="font-semibold text-[var(--brand)] bg-[var(--brand-soft)] rounded-full px-2 py-0.5">#{{ t }}</span>
        </div>
        <div class="flex justify-end">
            <DxButton type="default" text="Cập nhật" @click="submitEdit" />
        </div>
    </div>
</template>

<script setup>
import { DxTextBox, DxTextArea, DxButton } from 'devextreme-vue';
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
    postImg: null,
    visibility: props.visibility || 'public',
});

// Đồng bộ lại khi props tới sau (popup mount trước lúc bài viết tải xong / đổi bài khác)
watch(
    () => [props.postId, props.title, props.body, props.visibility],
    () => {
        data.value.title = props.title ?? '';
        data.value.body = props.body ?? '';
        data.value.visibility = props.visibility || 'public';
        data.value.postImg = null;
    }
);

const tags = computed(() => extractHashtags(data.value.title, data.value.body));

const onFile = (e) => {
    data.value.postImg = e.target.files[0] || null;
}

const submitEdit = async () => {
    try {
        const payload = { title: data.value.title, body: data.value.body, visibility: data.value.visibility };
        if (data.value.postImg) payload.postImg = data.value.postImg;
        await updatePost(props.postId, payload);
        emits("close");
    } catch {
        emits("post-fail");
    }
}
</script>
