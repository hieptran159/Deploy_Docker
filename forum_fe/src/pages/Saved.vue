<template>
    <div class="page">
        <div class="card">
            <div class="section-title">Bài viết đã lưu ({{ posts.length }})</div>
            <div v-if="loading" class="state">Đang tải…</div>
            <div v-else-if="!posts.length" class="state">Bạn chưa lưu bài viết nào</div>
            <div v-for="post in posts" :key="post.postId" class="border-b last:border-b-0">
                <Post :post="post" @refresh="load" />
            </div>
        </div>
    </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import Post from '@/components/Post/Post.vue';
import { getMyBookmarks } from '@/apis/bookmark';
import { getPostById } from '@/apis/post';

const posts = ref([]);
const loading = ref(false);

const load = async () => {
    loading.value = true;
    try {
        const ids = (await getMyBookmarks())?.data?.data?.postId || [];
        const results = await Promise.allSettled(ids.map((id) => getPostById(id)));
        posts.value = results
            .filter((r) => r.status === 'fulfilled')
            .map((r) => r.value?.data?.data)
            .filter(Boolean);
    } catch (e) {
        posts.value = [];
    } finally {
        loading.value = false;
    }
};

onMounted(load);
</script>
