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
            </select>
        </div>
        <div class="flex justify-end">
            <DxButton type="default" text="Cập nhật" @click="submitEdit" />
        </div>
    </div>
</template>

<script setup>
import { DxTextBox, DxTextArea, DxButton } from 'devextreme-vue';
import { updatePost } from '@/apis/post';
import { ref } from 'vue';

const emits = defineEmits(['close', 'post-fail']);
const props = defineProps({
    postId: {},
    title: {},
    body: {},
    visibility: { default: 'public' },
});

const data = ref({
    title: props.title,
    body: props.body,
    postImg: null,
    visibility: props.visibility || 'public',
});

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
