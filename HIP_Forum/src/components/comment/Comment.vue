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
    </div>
</template>

<script setup>
import { onMounted, ref, defineProps } from 'vue';
import {getUserInfo} from '../../apis/user';
import { DxButton } from 'devextreme-vue';
import { likeCommentApi, unLikeCommentApi } from '@/apis/comment';
import BaseAvatar from '../BaseAvatar.vue';

const props = defineProps({
    commentProps: {
        type: Object
    }
})

// const comment = ref({
//     "userComments": "b2bb132c-bc70-43c1-8c88-10756aafff4e",
//     "commentId": "953f739d-c943-492f-b923-574dde7b8d0c",
//     "content": "Wow, that exciting!",
//     "commentImg": null,
//     "commentAt": "2023-11-06 11:19:19.0",
//     "commentLikes": 0,
//     "userLikes": []
// })

const comment = ref(props.commentProps);

const userComments = ref();
const linkAvt = ref();
const imageLoaded = ref(false);

const getDataUser = async() => {
    const data = await getUserInfo(comment.value?.userComments);
    userComments.value = data?.data?.data?.fullName;
    linkAvt.value = "http://localhost:8081/images/" + data?.data?.data?.avtUrl;
}

const handleImageLoad = () => {
    imageLoaded.value = true;
}

const likePost = async() => {
    try {
        await likeCommentApi(comment.value.commentId);
    } catch (error) {
        console.log(error);
    }
}

const unLikePost = async() => {
    try {
        await unLikeCommentApi(comment.value.commentId);
    } catch (error) {
        console.log(error);
    }
}

onMounted(async()=>{
    await getDataUser();
})

</script>