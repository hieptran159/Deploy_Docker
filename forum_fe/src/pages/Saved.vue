<template>
    <div class="page">
        <div>
            <header class="masthead">
                <h1 class="masthead__name">Đã lưu</h1>
                <div class="masthead__tools">
                    <span class="masthead__date tnum">{{ posts.length }} mục</span>
                </div>
            </header>
            <div v-if="loading" class="state">Đang tải…</div>
            <div v-else-if="!posts.length" class="state">Bạn chưa lưu bài viết nào</div>
            <!-- Không kẻ dòng ngày ở đây: danh sách xếp theo lúc LƯU chứ không theo
                 ngày đăng, nên mốc ngày sẽ nhảy loạn và thành nhiễu chứ không phải tin. -->
            <div class="ledger">
                <Post v-for="post in posts" :key="post.postId" :post="post" @refresh="load" />
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
