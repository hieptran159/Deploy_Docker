<template>
    <div class="page">
        <div class="card">
            <div class="flex items-center gap-2">
                <button class="link text-sm" @click="router.back()">← Quay lại</button>
            </div>
            <div class="section-title mt-1">
                #{{ tag }}
                <span class="muted font-normal text-sm">{{ total ? `· ${total} bài viết` : '' }}</span>
            </div>

            <div v-if="loading && !posts.length" class="state">Đang tải…</div>
            <div v-else-if="!posts.length" class="state">Chưa có bài viết nào với hashtag này</div>

            <div v-for="post in posts" :key="post.postId" class="border-b last:border-b-0">
                <Post :post="post" @refresh="() => load(true)" />
            </div>

            <div v-if="page < totalPages" class="pt-3 text-center">
                <button class="link text-sm" :disabled="loading" @click="loadMore">
                    {{ loading ? 'Đang tải…' : 'Xem thêm' }}
                </button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import Post from '@/components/Post/Post.vue';
import { getPostsByTag } from '@/apis/post';

const router = useRouter();
const tag = ref(router.currentRoute.value.params.tag || '');
const posts = ref([]);
const page = ref(0);
const total = ref(0);
const totalPages = ref(1);
const loading = ref(false);

const load = async (reset = false) => {
    if (reset) { page.value = 0; posts.value = []; total.value = 0; totalPages.value = 1; }
    loading.value = true;
    try {
        const d = (await getPostsByTag(tag.value, page.value, 10))?.data?.data || {};
        const batch = d.items || [];
        posts.value = reset ? batch : [...posts.value, ...batch];
        total.value = d.total ?? posts.value.length;
        totalPages.value = d.totalPages ?? 1;
        if (batch.length) page.value += 1; else totalPages.value = page.value;
    } catch (e) {
        if (reset) posts.value = [];
    } finally {
        loading.value = false;
    }
};

const loadMore = () => load(false);

watch(
    () => router.currentRoute.value.params.tag,
    (t) => { if (t) { tag.value = t; load(true); } },
    { immediate: true }
);
</script>
