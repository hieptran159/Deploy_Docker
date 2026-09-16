<template>
    <div class="page">
        <div>
            <div class="flex items-center gap-2">
                <button class="link text-sm" @click="router.back()">← Quay lại</button>
            </div>
            <!-- Cùng măng-sét với bảng tin: hai màn hình phải đọc như một cuốn sổ -->
            <header class="masthead mt-1">
                <h1 class="masthead__name">#{{ tag }}</h1>
                <div class="masthead__tools">
                    <span class="masthead__date tnum">{{ total ? `${total} bài viết` : '' }}</span>
                </div>
            </header>
            <div class="flex items-center gap-3 flex-wrap mt-3">
                <button
                    v-if="isLogin"
                    type="button"
                    class="sign-btn"
                    :class="{ 'sign-btn--outline': following }"
                    :disabled="busy"
                    @click="toggleFollow"
                >{{ following ? 'Đang theo dõi' : 'Theo dõi' }}</button>
            </div>

            <div v-if="loading && !posts.length" class="state">Đang tải…</div>
            <div v-else-if="!posts.length" class="state">Chưa có bài viết nào với hashtag này</div>

            <div class="ledger">
                <template v-for="(post, i) in posts" :key="post.postId">
                    <div v-if="dayKey(post.postedAt) !== dayKey(posts[i - 1]?.postedAt)" class="ledger__day">
                        {{ dayLabel(post.postedAt) }}
                    </div>
                    <Post :post="post" @refresh="() => load(true)" />
                </template>
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
import { inject, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import Post from '@/components/Post/Post.vue';
import { getPostsByTag, getFollowedTags, followTag, unfollowTag } from '@/apis/post';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { dayKey, dayLabel } from '@/js/helper';

const router = useRouter();
const tag = ref(router.currentRoute.value.params.tag || '');
const posts = ref([]);
const page = ref(0);
const total = ref(0);
const totalPages = ref(1);
const loading = ref(false);

/* ---------- theo dõi hashtag ---------- */
const isLogin = !!getItemLocal(LOCALKEYS.ACCESS_TOKEN);
const showDialog = inject('openDialogError', null);
const following = ref(false);
const busy = ref(false);

// So khớp bằng tag ĐÃ CHUẨN HOÁ: backend luôn trả về dạng thường, còn trên URL
// người dùng có thể gõ hoa.
const normTag = () => (tag.value || '').replace(/^#/, '').trim().toLowerCase();

const loadFollowState = async () => {
    if (!isLogin) return;
    try {
        following.value = ((await getFollowedTags())?.data?.data || []).includes(normTag());
    } catch (e) { following.value = false; }
};

const toggleFollow = async () => {
    busy.value = true;
    try {
        const fn = following.value ? unfollowTag : followTag;
        const tags = (await fn(normTag()))?.data?.data || [];
        following.value = tags.includes(normTag());
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Không đổi được trạng thái theo dõi');
    } finally {
        busy.value = false;
    }
};

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
    (t) => { if (t) { tag.value = t; load(true); loadFollowState(); } },
    { immediate: true }
);
</script>
