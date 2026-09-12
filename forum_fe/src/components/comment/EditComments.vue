<template>
    <div class="flex flex-col gap-3 py-3">
        <div>
            <label class="text-sm muted">Nội dung bình luận</label>
            <textarea style="height: 100px" class="field" v-model="data.content"></textarea>
        </div>
        <div>
            <label class="text-sm muted">Thay ảnh đính kèm (để trống nếu giữ nguyên)</label>
            <input type="file" accept="image/*" @change="onFile" />
        </div>
        <div class="flex justify-end">
            <button type="button" class="sign-btn" @click="submitEdit">Cập nhật</button>
        </div>
    </div>
</template>

<script setup>
import { updateComment } from '@/apis/comment';
import { ref } from 'vue';

const emits = defineEmits(['close', 'update-fail']);
const props = defineProps({
    commentId: {},
    content: {}
});

const data = ref({
    content: props.content,
    commentImg: null,
});

const onFile = (e) => { data.value.commentImg = e.target.files[0] || null; };

const submitEdit = async () => {
    try {
        const payload = { content: data.value.content || '' };
        if (data.value.commentImg) payload.commentImg = data.value.commentImg;
        await updateComment(props.commentId, payload);
        emits("close");
    } catch {
        emits("update-fail");
    }
};
</script>
