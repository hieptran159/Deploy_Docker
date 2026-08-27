<template>
    <div class="page">
        <div class="card">
            <div class="flex gap-4">
                <div class="flex flex-col items-center gap-1 flex-none w-16">
                    <BaseAvatar
                        :linkAvt="linkAvt"
                        :userCreatedPost="userCreatedPost"
                        :userId="post?.userCreatedPost"
                        :is-show="false"
                    />
                    <div
                        class="text-xs font-semibold text-center cursor-pointer hover:underline truncate w-full"
                        @click="goAuthor"
                    >
                        {{ userCreatedPost }}
                    </div>
                </div>

                <div class="min-w-0 flex-1">
                    <div class="flex items-start gap-2">
                        <div class="font-bold text-xl text-[var(--accent)] flex-1">{{ post?.title }}</div>
                        <div
                            class="relative flex-none"
                            v-if="post?.userCreatedPost == getItemLocal(LOCALKEYS.USER_ID)"
                        >
                            <DxButton icon="overflow" stylingMode="text" @click="isShowSetting = !isShowSetting" />
                            <div class="absolute right-0 z-20 w-32 flex flex-col bg-white border rounded-lg shadow p-1" v-if="isShowSetting">
                                <DxButton icon="edit" type="default" stylingMode="text" text="Chỉnh sửa" @click="ishowEditPost = true" />
                                <DxButton icon="trash" type="danger" stylingMode="text" text="Xoá" @click="handleDeletePost" />
                            </div>
                        </div>
                    </div>

                    <div class="text-sm muted mt-0.5">
                        {{ calculateTimeDifference(post?.postedAt) }} trước · {{ post?.likesQuantity ?? 0 }} lượt yêu thích
                    </div>

                    <div class="my-3 whitespace-pre-wrap">{{ post?.body }}</div>

                    <img :src="linkPostImg" v-if="post?.postImg" class="max-w-md rounded-xl border" />

                    <div class="row-actions mt-3">
                        <DxButton icon="like" type="danger" text="Yêu thích" @click="likePost" />
                        <DxButton icon="like" type="normal" stylingMode="outlined" text="Bỏ yêu thích" @click="unLikePost" />
                    </div>
                </div>
            </div>
        </div>

        <div class="card">
            <div class="section-title">Bình luận ({{ post?.comments?.length ?? 0 }})</div>

            <div class="flex items-center gap-3 pb-3 border-b">
                <BaseAvatar
                    :linkAvt="getItemLocal(LOCALKEYS.LINK_AVT)"
                    :userCreatedPost="getItemLocal(LOCALKEYS.USER_NAME)"
                    :is-show="false"
                />
                <DxTextBox class="flex-1" placeholder="Viết bình luận…" v-model="contentPost" @enter-key="commentPost" />
                <DxButton type="default" text="Gửi" @click="commentPost" />
            </div>

            <div v-if="!post?.comments?.length" class="state">Chưa có bình luận</div>
            <div
                class="py-3 border-b last:border-b-0"
                v-for="comment in post?.comments"
                :key="comment?.commentId"
            >
                <Comment :commentProps="comment" @refresh="getDataPostById" />
            </div>
        </div>

        <DxPopup
            title="Chỉnh sửa bài viết"
            v-model:visible="ishowEditPost"
            :width="700"
            :height="420"
            :hide-on-outside-click="true"
        >
            <EditPost
                :postId="id"
                :body="post?.body"
                :title="post?.title"
                @close="() => { ishowEditPost = false; getDataPostById() }"
                @post-fail="showDialog('Cập nhật bài viết thất bại')"
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
import { IMAGE_BASE } from '@/config';

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
    linkPostImg.value = IMAGE_BASE + post.value.postImg;
}

const getDataUser = async() => {
    try {
        const data = await getUserInfo(post.value?.userCreatedPost);
        userCreatedPost.value = data?.data?.data?.fullName;
        linkAvt.value = IMAGE_BASE + data?.data?.data?.avtUrl;
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

const goAuthor = () => {
    if (post.value?.userCreatedPost) route.push(`/user/${post.value.userCreatedPost}`);
}

onMounted(async() => {
    await getDataPostById();
    await getDataUser();
})

</script>