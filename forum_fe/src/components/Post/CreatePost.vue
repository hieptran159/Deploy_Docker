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
            <label class="text-sm muted">Ảnh đính kèm (tuỳ chọn)</label>
            <input type="file" accept="image/*" @change="onFile" />
        </div>
        <div class="flex justify-end">
            <DxButton type="default" text="Đăng bài" @click="postNew" />
        </div>
    </div>
</template>

<script setup>
import { DxTextBox, DxTextArea, DxButton } from 'devextreme-vue';
import { createdPost } from '@/apis/post';
import { ref } from 'vue';

const emits = defineEmits(['close', 'post-fail']);

const data = ref({
    title: "",
    body: "",
    postImg: null,
});

const onFile = (e) => {
    data.value.postImg = e.target.files[0] || null;
}

const postNew = async () => {
    try {
        await createdPost(data.value);
        emits("close");
    } catch {
        emits("post-fail");
    }
}
</script>
