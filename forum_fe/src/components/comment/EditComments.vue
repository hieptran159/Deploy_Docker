<template>
    <div class="flex flex-col justify-center py-5">
        <div class="mb-4">
            <span><b>Nội dung bình luận:</b></span>
            <DxTextArea v-model="data.body"></DxTextArea>
        </div>
        <div class="w-30% flex justify-center">
            <DxButton
                type="default"
                @click="updateComment"
            >
                Cập nhật
            </DxButton>
        </div>
    </div>
</template>

<script setup>
import { DxTextArea, DxButton } from 'devextreme-vue';
import { updateComment } from '@/apis/comment';
import { ref, defineEmits, defineProps } from 'vue';

const emits = defineEmits();
const props = defineProps({
    body: {}
});

const data = ref({
    body: props.body
});

const updateComment = async () => {
    try {
        await updateComment(data.value);
        emits("close");
    } catch {
        emits("update-fail");
    }
};
</script>
