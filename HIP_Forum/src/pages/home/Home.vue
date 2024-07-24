<template>
    <div class="content flex justify-center flex-wrap">
        <div class="w-[80%] border p-5 rounded mb-5 flex">
            <div class="flex flex-col items-center justify-center">
                <BaseAvatar
                :link-avt="getItemLocal(LOCALKEYS.LINK_AVT)"
                :user-created-post="getItemLocal(LOCALKEYS.USER_NAME)"
                />
                <div class="text-black font-bold text-[18px] text-center mx-2" >
                    {{ getItemLocal(LOCALKEYS.USER_NAME) }}
                </div>
            </div>
            <div class="border flex items-center w-[50%] rounded ml-5 px-5 cursor-pointer" @click="() => {ishowCreatePost = true}">
                <span class="font-bold"
                >
                    Tạo bài viết mới ...
                </span>
                <DxPopup
                    title="Tạo bài viết mới"
                    v-model:visible="ishowCreatePost"
                    width="700px"
                    height="400px"
                >
                    <CreatePost
                        @close="() => {ishowCreatePost = false; getListPost()}"
                        @post-fail="showDialog('Đăng bài thất bại')"
                    />
                </DxPopup>
            </div>
        </div>
        <div class="w-[80%] border p-5 rounded">
            <div class="flex">
                <div class="ml-auto">
                    <dx-text-box 
                    v-model="searchText" 
                    @valueChanged="handleSearch" 
                    placeholder="Nhấn Enter để tìm kiếm"
                    showClearButton="true"
                    :icon="'search'"
                    ></dx-text-box>
                </div>
            </div>
            <span class="text-[32px] text-orange-400 font-bold mb-5">
                Danh sách các bài đăng
            </span>
            <div v-for="post in posts" :key="post.postId" class="mb-1">
                <Post :post="post"/>
            </div>
            <div class="paging ml-[80%] flex">
                <DxButton
                    type="default"
                    @click="currentPageChange(-1)"
                >
                    <
                </DxButton>
                <DxButton>
                    {{ currentPage }}
                </DxButton>
                <DxButton
                    type="default"
                    @click="currentPageChange(1)"
                >
                    >
                </DxButton>
            </div>
        </div>
    </div>
</template>

<script setup>
import Post from '../../components/Post/Post.vue';
import { DxButton, DxPopup, DxTextBox } from 'devextreme-vue';
import { getListPostApi, searchPost } from '@/apis/post';
import { inject, onMounted, ref } from 'vue';
import BaseAvatar from '@/components/BaseAvatar.vue';
import {getItemLocal, LOCALKEYS} from '../../storages/localStorage';
import CreatePost from '../../components/Post/CreatePost.vue'
const posts = ref([]);
const currentPage = ref(1);
const ishowCreatePost = ref(false);

const showDialog = inject("openDialogError");
const searchText = ref("");

const getListPost = async() => {
    const data = await getListPostApi(currentPage.value);
    posts.value = data?.data?.data;
}

const currentPageChange = (i) => {
    currentPage.value += i;
    currentPage.value = Math.max(currentPage.value, 1);
    getListPost();
}

const handleSearch = async() =>{
    const data = await searchPost(searchText.value);
    posts.value = data?.data?.data;
}

onMounted(()=>{
    getListPost();
})

</script>

<style>
.paging .dx-button{
    width: 36px;
    height: 36px;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-right: 10px ;
}
</style>