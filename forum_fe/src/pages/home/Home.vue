<template>
    <div class="page">
        <div class="card flex items-center gap-4">
            <BaseAvatar
                :link-avt="getItemLocal(LOCALKEYS.LINK_AVT)"
                :user-created-post="getItemLocal(LOCALKEYS.USER_NAME)"
                :is-show="false"
            />
            <button
                class="flex-1 text-left px-4 py-3 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-600 transition"
                @click="ishowCreatePost = true"
            >
                {{ getItemLocal(LOCALKEYS.USER_NAME) }} ơi, bạn đang nghĩ gì?
            </button>
            <DxButton icon="edit" text="Viết bài" type="default" @click="ishowCreatePost = true" />
            <DxPopup
                title="Tạo bài viết mới"
                v-model:visible="ishowCreatePost"
                :width="700"
                :height="420"
                :hide-on-outside-click="true"
            >
                <CreatePost
                    @close="() => { ishowCreatePost = false; getListPost() }"
                    @post-fail="showDialog('Đăng bài thất bại')"
                />
            </DxPopup>
        </div>

        <div class="card">
            <div class="flex items-center gap-3 mb-3">
                <span class="section-title mb-0 flex-1">Bài đăng mới nhất</span>
                <DxTextBox
                    v-model="searchText"
                    @enter-key="handleSearch"
                    @value-changed="onSearchChanged"
                    placeholder="Tìm bài viết…"
                    :show-clear-button="true"
                    width="220"
                />
            </div>

            <div v-if="loading" class="state">Đang tải…</div>
            <div v-else-if="!posts.length" class="state">Chưa có bài viết nào</div>

            <div v-for="post in posts" :key="post.postId" class="border-b last:border-b-0">
                <Post :post="post" />
            </div>

            <div class="flex items-center justify-center gap-2 mt-4">
                <DxButton icon="chevronleft" :disabled="currentPage <= 1" @click="currentPageChange(-1)" />
                <span class="muted text-sm px-2">Trang {{ currentPage }}</span>
                <DxButton icon="chevronright" :disabled="!posts.length" @click="currentPageChange(1)" />
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
import { getItemLocal, LOCALKEYS } from '../../storages/localStorage';
import CreatePost from '../../components/Post/CreatePost.vue';

const posts = ref([]);
const currentPage = ref(1);
const ishowCreatePost = ref(false);
const loading = ref(false);

const showDialog = inject("openDialogError");
const searchText = ref("");

const getListPost = async () => {
    loading.value = true;
    try {
        const data = await getListPostApi(currentPage.value);
        posts.value = data?.data?.data || [];
    } catch (e) {
        posts.value = [];
    } finally {
        loading.value = false;
    }
}

const currentPageChange = (i) => {
    currentPage.value = Math.max(currentPage.value + i, 1);
    getListPost();
}

const onSearchChanged = (e) => {
    if (!e?.value) {
        currentPage.value = 1;
        getListPost();
    }
}

const handleSearch = async () => {
    if (!searchText.value) { getListPost(); return; }
    loading.value = true;
    try {
        const data = await searchPost(searchText.value);
        posts.value = data?.data?.data || [];
    } catch (e) {
        posts.value = [];
    } finally {
        loading.value = false;
    }
}

onMounted(getListPost);
</script>
