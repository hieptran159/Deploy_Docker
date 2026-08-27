<template>
    <div class="flex flex-col justify-center py-5">
        <div class="mb-4">
            <span><b>Tiêu đề:</b></span>
            <DxTextBox v-model="data.title"></DxTextBox>
        </div>
        <div class="mb-4">
            <span><b>Nội dung:</b></span>
            <DxTextArea v-model="data.body"></DxTextArea>
        </div>
        <div class="mb-4">
            <span class="mr-2"><b>Ảnh đính kèm:</b></span>
            <input type="file" />
        </div>
        <div class="w-30% flex justify-center">
            <DxButton
            type = "default"
            @click="submitEdit"
            >
                Cập nhật
            </DxButton>
        </div>
    </div>
</template>

<script setup>
import { DxTextBox, DxTextArea, DxButton } from 'devextreme-vue';
import { updatePost } from '@/apis/post';
import { ref, defineEmits, defineProps } from 'vue';

const emits = defineEmits();
const props = defineProps({
    postId: {},
    title: {},
    body: {}
})

const data = ref({
    title : props.title,
    body: props.body
})

const submitEdit = async() => {
    try{
        await updatePost(props.postId, data.value);
        emits("close");
    }catch{
        emits("post-fail");
    }
}

</script>