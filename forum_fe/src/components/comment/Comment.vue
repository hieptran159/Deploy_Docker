<template>
    <div class="flex">
        <div class="w-[10%] h-[60px] p-2 flex items-center justify-center flex-col">
            <BaseAvatar
                    :linkAvt ="linkAvt"
                    :userCreatedPost="userComments"
                    :userId="comment?.userComments"
                />
        </div>
        <div class="flex flex-col w-[90%] mr-5">
            <span class="text-black font-bold text-[18px]">
                {{ userComments }}
            </span>
            <span>
                {{ comment.content }}
                <div class="flex">
                    <span class="text-red-500 ml-auto">
                        <b>{{ comment?.commentLikes }}</b> lượt yêu thích
                    </span>
                </div>
            </span>
            <div class="flex mt-1">
                    <DxButton
                        v-if="comment?.userComments == getItemLocal(LOCALKEYS.USER_ID)"
                        icon = "edit"
                        type = "default"
                        @click="() => {isShowEditComment = true}"
                    >
                        Chỉnh sửa
                    </DxButton>
                    <DxButton
                        class= "ml-auto"
                        icon = "like"
                        type = "danger"
                        @click="likePost"
                    >
                        Yêu thích
                    </DxButton>

                    <DxButton
                        class= "ml-2"
                        icon = "like"
                        type = "default"
                        @click="unLikePost"
                    >
                        Bỏ yêu thích
                    </DxButton>
                </div>
        </div>

        <DxPopup
            title="Chỉnh sửa bình luận"
            v-model:visible="isShowEditComment"
            width="700px"
            height="300px"
        >
            <EditComments
                :commentId="comment?.commentId"
                :content="comment?.content"
                @close="() => {isShowEditComment = false; emits('refresh')}"
                @update-fail="() => {isShowEditComment = false}"
            />
        </DxPopup>
    </div>
</template>

<script setup>
import { onMounted, ref, inject, defineProps, defineEmits } from 'vue';
import {getUserInfo} from '../../apis/user';
import { DxButton, DxPopup } from 'devextreme-vue';
import { likeCommentApi, unLikeCommentApi } from '@/apis/comment';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import BaseAvatar from '../BaseAvatar.vue';
import EditComments from './EditComments.vue';

const props = defineProps({
    commentProps: {
        type: Object
    }
})

const emits = defineEmits(['refresh']);

const comment = ref(props.commentProps);

const userComments = ref();
const linkAvt = ref();
const imageLoaded = ref(false);
const isShowEditComment = ref(false);
const showDialog = inject("openDialogError");

const getDataUser = async() => {
    const data = await getUserInfo(comment.value?.userComments);
    userComments.value = data?.data?.data?.fullName;
    linkAvt.value = "http://hp11.hipe.id.vn:8081/images/" + data?.data?.data?.avtUrl;
}

const handleImageLoad = () => {
    imageLoaded.value = true;
}

const likePost = async() => {
    try {
        await likeCommentApi(comment.value.commentId);
        emits('refresh');
    } catch (error) {
        console.log(error);
    }
}

const unLikePost = async() => {
    try {
        await unLikeCommentApi(comment.value.commentId);
        emits('refresh');
    } catch (error) {
        console.log(error);
    }
}

onMounted(async()=>{
    await getDataUser();
})

</script>
