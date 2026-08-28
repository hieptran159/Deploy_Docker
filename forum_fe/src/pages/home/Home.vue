<template>
    <div class="page">
        <div v-if="isLogin" class="card flex items-center gap-4">
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

        <div v-else class="card text-sm muted">
            Bạn đang xem với tư cách khách. Đăng nhập để viết bài, bình luận và tương tác.
        </div>

        <div class="card">
            <div v-if="isLogin" class="flex gap-1 mb-3">
                <button
                    class="px-3 py-1.5 rounded-lg text-sm font-semibold"
                    :class="feedMode === 'all' ? 'bg-[var(--brand-soft)] text-[var(--brand)]' : 'muted'"
                    @click="setFeedMode('all')"
                >Tất cả</button>
                <button
                    class="px-3 py-1.5 rounded-lg text-sm font-semibold"
                    :class="feedMode === 'friends' ? 'bg-[var(--brand-soft)] text-[var(--brand)]' : 'muted'"
                    @click="setFeedMode('friends')"
                >Bạn bè</button>
            </div>
            <div class="flex items-center gap-2 mb-3">
                <span class="section-title mb-0 flex-1">{{ feedMode === 'friends' ? 'Từ bạn bè' : 'Bài đăng mới nhất' }}</span>
                <DxButton
                    v-if="isLogin"
                    icon="bookmark"
                    hint="Đã lưu"
                    stylingMode="text"
                    @click="router.push('/saved')"
                />
                <DxButton
                    v-if="isLogin"
                    icon="doc"
                    hint="Bản nháp"
                    stylingMode="text"
                    @click="router.push('/drafts')"
                />
                <DxTextBox
                    v-model="searchText"
                    @enter-key="handleSearch"
                    @value-changed="onSearchChanged"
                    placeholder="Tìm bài viết…"
                    :show-clear-button="true"
                    width="200"
                />
            </div>

            <div v-if="searchMode" class="mb-3 text-sm muted flex items-center gap-2">
                <span>Kết quả cho "<b>{{ activeQuery }}</b>"</span>
                <button class="link" @click="clearSearch">Xoá tìm kiếm</button>
            </div>

            <div v-if="loading && !posts.length" class="state">Đang tải…</div>
            <div v-else-if="!posts.length" class="state">
                {{ searchMode ? 'Không tìm thấy bài viết nào'
                    : (feedMode === 'friends' ? 'Bạn bè của bạn chưa đăng hay chia sẻ gì' : 'Chưa có bài viết nào') }}
            </div>

            <div v-for="post in posts" :key="post.postId" class="border-b last:border-b-0">
                <Post :post="post" />
            </div>

            <div v-if="searchMode" class="text-center mt-4">
                <button v-if="searchHasMore" class="link text-sm" :disabled="loading" @click="loadMoreSearch">
                    {{ loading ? 'Đang tải…' : 'Xem thêm kết quả' }}
                </button>
                <span v-else-if="posts.length" class="muted text-xs">Đã hết kết quả</span>
            </div>

            <div v-else class="flex items-center justify-center gap-2 mt-4 flex-wrap">
                <DxButton icon="chevronleft" :disabled="currentPage <= 1" @click="currentPageChange(-1)" />
                <span class="muted text-sm">Trang</span>
                <input
                    type="number"
                    min="1"
                    :max="totalPages"
                    v-model.number="gotoPage"
                    class="w-14 text-center text-sm rounded-lg border px-2 py-1 bg-[var(--surface)]"
                    @keyup.enter="doGoto"
                />
                <span class="muted text-sm">/ {{ totalPages }}</span>
                <DxButton icon="chevronright" :disabled="currentPage >= totalPages" @click="currentPageChange(1)" />
                <DxButton text="Đi tới" stylingMode="text" @click="doGoto" />
            </div>
        </div>
    </div>
</template>

<script setup>
import Post from '../../components/Post/Post.vue';
import { DxButton, DxPopup, DxTextBox } from 'devextreme-vue';
import { getListPostApi, searchPost, getFeedPages, getFriendsFeed, getFriendsFeedPages } from '@/apis/post';
import { computed, inject, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import BaseAvatar from '@/components/BaseAvatar.vue';
import { getItemLocal, LOCALKEYS } from '../../storages/localStorage';
import CreatePost from '../../components/Post/CreatePost.vue';

const router = useRouter();
const isLogin = computed(() => getItemLocal(LOCALKEYS.ACCESS_TOKEN) != null);
const posts = ref([]);
const currentPage = ref(pageFromQuery());
const totalPages = ref(1);
const gotoPage = ref(currentPage.value);
const ishowCreatePost = ref(false);
const loading = ref(false);
const feedMode = ref(isLogin.value ? 'all' : 'all'); // 'all' | 'friends'

const setFeedMode = (m) => {
    if (feedMode.value === m) return;
    feedMode.value = m;
    if (currentPage.value !== 1) {
        router.push({ query: { ...router.currentRoute.value.query, page: 1 } });
    } else {
        getListPost();
    }
    loadPageInfo();
}

const loadPageInfo = async () => {
    try {
        const fn = feedMode.value === 'friends' ? getFriendsFeedPages : getFeedPages;
        totalPages.value = (await fn())?.data?.data?.totalPages || 1;
    } catch (e) { totalPages.value = 1; }
}

const doGoto = () => {
    let p = parseInt(gotoPage.value, 10);
    if (!Number.isFinite(p)) return;
    p = Math.min(Math.max(p, 1), totalPages.value);
    gotoPage.value = p;
    if (p !== currentPage.value) router.push({ query: { ...router.currentRoute.value.query, page: p } });
}

const showDialog = inject("openDialogError");
const searchText = ref("");

const SEARCH_SIZE = 10;
const searchMode = ref(false);
const activeQuery = ref("");
const searchPageNum = ref(0);
const searchHasMore = ref(false);

function pageFromQuery() {
    const p = parseInt(router.currentRoute.value.query.page, 10);
    return Number.isFinite(p) && p > 0 ? p : 1;
}

const getListPost = async () => {
    loading.value = true;
    try {
        const fn = (feedMode.value === 'friends' && isLogin.value) ? getFriendsFeed : getListPostApi;
        const data = await fn(currentPage.value);
        posts.value = data?.data?.data || [];
    } catch (e) {
        posts.value = [];
    } finally {
        loading.value = false;
    }
}

const currentPageChange = (i) => {
    const next = Math.max(currentPage.value + i, 1);
    if (next === currentPage.value) return;
    // ghi số trang vào URL -> nút back của trình duyệt quay lại đúng trang trước đó
    router.push({ query: { ...router.currentRoute.value.query, page: next } });
}

// đồng bộ khi URL đổi (bấm next/prev, hoặc back/forward của trình duyệt)
watch(() => router.currentRoute.value.query.page, () => {
    const p = pageFromQuery();
    if (p !== currentPage.value) {
        currentPage.value = p;
        gotoPage.value = p;
        getListPost();
    }
});

const onSearchChanged = (e) => {
    if (!e?.value) clearSearch();
}

const clearSearch = () => {
    searchText.value = "";
    searchMode.value = false;
    activeQuery.value = "";
    searchHasMore.value = false;
    getListPost();
}

const runSearch = async (append = false) => {
    loading.value = true;
    try {
        const data = await searchPost(activeQuery.value, searchPageNum.value, SEARCH_SIZE);
        const batch = data?.data?.data || [];
        posts.value = append ? [...posts.value, ...batch] : batch;
        searchHasMore.value = batch.length === SEARCH_SIZE;
        searchPageNum.value += 1;
    } catch (e) {
        if (!append) posts.value = [];
        searchHasMore.value = false;
    } finally {
        loading.value = false;
    }
}

const handleSearch = () => {
    const q = (searchText.value || "").trim();
    if (!q) { clearSearch(); return; }
    searchMode.value = true;
    activeQuery.value = q;
    searchPageNum.value = 0;
    posts.value = [];
    runSearch(false);
}

const loadMoreSearch = () => runSearch(true);

onMounted(() => { getListPost(); loadPageInfo(); });
</script>
