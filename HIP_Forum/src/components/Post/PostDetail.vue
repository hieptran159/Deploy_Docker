<template>
    <div class="post-detail w-[100%] flex justify-center items-center flex-col">
        <div class="w-[80%] border py-5 rounded flex">
            <div class="w-[10%] h-[120px] flex flex-col justify-center items-center p-2">
                <BaseAvatar
                    :linkAvt ="linkAvt"
                    :userCreatedPost="userCreatedPost"
                    :userId="post?.userCreatedPost"
                />
                <div class="text-black font-bold text-[18px] mt-1 text-center" >
                    {{ userCreatedPost }}
                </div>
            </div>
            <div class="w-[90%] bg-orange-200 p-2 rounded mr-5">
                <span
                    class="font-bold text-lg text-[#2577b1] cursor-pointer"
                >
                    {{ post?.title }}
                </span>

                <span class="flex mb-1">
                    <i class="ml-2 text-yellow-500">
                        {{ calculateTimeDifference(post?.postedAt) }} trước
                    </i>
                    <span class="text-red-500 ml-auto">
                        <b>{{ post?.likesQuantity }}</b> lượt yêu thích
                    </span>
                    <div class="setting relative ml-3" 
                    @click="() => {isShowSetting = !isShowSetting}"
                    v-if="post?.userCreatedPost == getItemLocal(LOCALKEYS.USER_ID)">
                        <DxButton
                            icon = "info"
                        />
                        <div class="absolute w-[100px] flex flex-col" v-if="isShowSetting">
                            <DxButton
                                icon = "background"
                                type = "default"
                                @click="()=>{ishowEditPost = true}"
                            >
                                Chỉnh sửa
                            </DxButton>
                            <DxButton
                                type="danger"
                                @click="handleDeletePost"
                            >
                                Xoá
                            </DxButton>
                        </div>
                    </div> 
                </span>

                <div class="my-2">
                    {{ post?.body }}
                </div>

                <img :src="linkPostImg" width="400" v-if="post?.postImg"/>

                <div class="flex">
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
        <div class="w-[80%] border py-5 rounded mt-3" v-for="comment in post?.comments" :key="comment?.commentId">
            <Comment :commentProps="comment"/>
        </div>
        <div class="w-[80%] border p-5 rounded my-3 flex">
            <BaseAvatar
                    :linkAvt ="getItemLocal(LOCALKEYS.LINK_AVT)"
                    :userCreatedPost="getItemLocal(LOCALKEYS.USER_NAME)"
            />
            <div class="w-[80%] mx-4">
                <DxTextBox
                placeholder="Viết câu trả lời ..."
                v-model="contentPost"
                />
            </div>

            <DxButton
                type="default"
                @click="commentPost"
            >
                Gửi
            </DxButton>
        </div>

        <DxPopup
            title="Chỉnh sửa bài viết"
            v-model:visible="ishowEditPost"
            width="700px"
            height="400px"
            >
            <EditPost
                :body="post?.body"
                :title="post?.title"
                @close="() => {ishowEditPost = false; getListPost()}"
                @post-fail="showDialog('Đăng bài thất bại')"
                />
            </DxPopup>
    </div>
</template>

<script setup>
import { onMounted, ref, inject } from 'vue';
import { getPostById } from '@/apis/post';
import { createComment } from '@/apis/comment';
import { likePostApi, unLikePostApi, deletePost } from '@/apis/post';
import { getUserInfo } from '@/apis/user';
import { useRouter } from 'vue-router';
import { calculateTimeDifference } from '@/js/helper';
import Comment from '../comment/Comment.vue';
import { DxTextBox, DxButton, DxPopup } from 'devextreme-vue';
import BaseAvatar from '../BaseAvatar.vue';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import EditPost from '@/components/Post/EditPost.vue';

const  route = useRouter();

const id = ref(route.currentRoute._value.params.id);
const post = ref();
const linkPostImg = ref();
const contentPost = ref();
const userCreatedPost = ref();
const linkAvt = ref();
const showDialog = inject("openDialogError");
const isShowSetting = ref(false);
const ishowEditPost = ref(false);

const getDataPostById = async() => {
    const data =  await getPostById(id.value);
    post.value = data?.data?.data;
    linkPostImg.value = "http://localhost:8081/images/" + post.value.postImg;
}

const getDataUser = async() => {
    try {
        const data = await getUserInfo(post.value?.userCreatedPost);
        userCreatedPost.value = data?.data?.data?.fullName;
        linkAvt.value = "http://localhost:8081/images/" + data?.data?.data?.avtUrl;
    } catch (error) {
        console.error(error);
    }
}

const commentPost = async()=> {
    try {
        await createComment(id.value, {
        content: contentPost.value,
        commentImg: null
        })
        await getDataPostById();
    } catch (error) {
        console.log(error);
    }
    
}

const likePost = async() => {
    try {
        await likePostApi(id.value);
        await getDataPostById();
        showDialog("Thông báo", "Like bài viết thành công");
    } catch (error) {
        console.log(error);
        showDialog("Thông báo", "Bạn đã yêu thích bài viết này!");
    }
}

const unLikePost = async() => {
    try {
        await unLikePostApi(id.value);
        await getDataPostById();
    } catch (error) {
        console.log(error);
    }
}

const handleDeletePost = async() => {
    try {
        await deletePost(id.value);
        route.push('/');
    } catch (error) {
        console.log(error);
    }
}

onMounted(async() => {
    await getDataPostById();
    await getDataUser();
})

</script>