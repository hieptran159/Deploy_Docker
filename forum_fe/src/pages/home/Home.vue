<template>
    <div class="page">
        <!-- Măng-sét: tên sổ + ngày hôm nay + tổng số mục đã ghi.
             KHÔNG phải khối "con số to + nhãn nhỏ" — đó là hero mặc định mà trang
             nào cũng có. Ở đây số nằm trong câu, đọc như một dòng ghi chú. -->
        <header class="masthead">
            <h1 class="masthead__name">Bảng tin</h1>
            <div class="masthead__tools">
                <input
                    type="search"
                    class="field field--sm"
                    style="width: 190px"
                    v-model="searchText"
                    @keyup.enter="handleSearch"
                    @input="onSearchChanged"
                    placeholder="Tìm trong sổ…"
                />
                <span class="masthead__date tnum">{{ todayStamp }}</span>
            </div>
        </header>
        <p class="masthead__note" style="margin-top: 0">
            {{ isLogin ? 'Đọc, chia sẻ lại, hoặc mở một chủ đề của riêng bạn.'
                : 'Bạn đang xem với tư cách khách. Đăng nhập để viết bài và bình luận.' }}
            Đã ghi <b class="tnum">{{ shownTotal.toLocaleString('vi-VN') }}</b> mục.
        </p>

        <!-- Hashtag đang chạy: một dòng chữ, không phải đám mây viên thuốc -->
        <div v-if="trendingTags.length" class="tag-list">
            <button
                v-for="t in trendingTags"
                :key="t.tag"
                class="tag-chip"
                @click="router.push('/tag/' + encodeURIComponent(t.tag))"
            >{{ t.tag }} <span class="tag-chip__count">{{ t.count }}</span></button>
        </div>

        <div v-if="isLogin" class="card flex items-center gap-3 flex-wrap sm:flex-nowrap">
            <BaseAvatar
                :link-avt="getItemLocal(LOCALKEYS.LINK_AVT)"
                :user-created-post="getItemLocal(LOCALKEYS.USER_NAME)"
                :is-show="false"
            />
            <button
                class="flex-1 min-w-[160px] text-left px-4 py-3 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-600 transition truncate"
                @click="ishowCreatePost = true"
            >
                {{ getItemLocal(LOCALKEYS.USER_NAME) }} ơi, bạn đang nghĩ gì?
            </button>
            <button class="sign-btn flex-none" @click="ishowCreatePost = true">
                <AppIcon name="edit" :size="16" /> Viết bài
            </button>
            <AppModal
                title="Tạo bài viết mới"
                v-model:open="ishowCreatePost"
                :width="700"
            >
                <CreatePost
                    @close="() => { ishowCreatePost = false; getListPost(); loadTrending() }"
                    @post-fail="(msg) => showDialog('Đăng bài thất bại', msg || 'Đã có lỗi xảy ra.')"
                />
            </AppModal>
        </div>

        <div>
            <div class="flex items-center gap-3 mb-2 flex-wrap">
                <div v-if="isLogin" class="seg">
                    <button class="seg__btn" :class="{ 'is-on': feedMode === 'all' }" @click="setFeedMode('all')">Tất cả</button>
                    <button class="seg__btn" :class="{ 'is-on': feedMode === 'friends' }" @click="setFeedMode('friends')">Bạn bè</button>
                    <button class="seg__btn" :class="{ 'is-on': feedMode === 'tags' }" @click="setFeedMode('tags')">Hashtag</button>
                </div>
                <span class="flex-1"></span>
                <button v-if="isLogin" class="icon-btn" title="Bài đã lưu" @click="router.push('/saved')">
                    <AppIcon name="bookmark" :size="18" />
                </button>
                <button v-if="isLogin" class="icon-btn" title="Bản nháp" @click="router.push('/drafts')">
                    <AppIcon name="file-text" :size="18" />
                </button>
            </div>

            <div v-if="searchMode" class="mb-3 text-sm muted flex items-center gap-2">
                <span>Kết quả cho "<b>{{ activeQuery }}</b>"</span>
                <button class="link" @click="clearSearch">Xoá tìm kiếm</button>
            </div>

            <div v-if="loading && !posts.length" class="state">Đang tải…</div>
            <div v-else-if="!posts.length" class="state">
                <template v-if="searchMode">Không có bài nào khớp với từ khoá này. Thử từ ngắn hơn hoặc một hashtag.</template>
                <template v-else-if="feedMode === 'friends'">Bạn bè của bạn chưa đăng hay chia sẻ gì. Chuyển sang Tất cả để xem toàn diễn đàn.</template>
                <template v-else-if="feedMode === 'tags' && !followedTags.length">
                    Bạn chưa theo dõi hashtag nào. Mở một hashtag rồi bấm “Theo dõi”.
                </template>
                <template v-else-if="feedMode === 'tags'">
                    Chưa có bài mới nào dưới {{ followedTags.length }} hashtag bạn theo dõi.
                </template>
                <template v-else>Chưa có bài viết nào. Viết bài đầu tiên đi.</template>
            </div>

            <div class="ledger">
                <template v-for="(post, i) in posts" :key="post.postId">
                    <!-- Sang ngày mới thì kẻ một dòng, như sang trang sổ -->
                    <div v-if="dayKey(post.postedAt) !== dayKey(posts[i - 1]?.postedAt)" class="ledger__day">
                        {{ dayLabel(post.postedAt) }}
                    </div>
                    <Post :post="post" @refresh="getListPost" />
                </template>
            </div>

            <div v-if="searchMode" class="text-center mt-4">
                <button v-if="searchHasMore" class="link text-sm" :disabled="loading" @click="loadMoreSearch">
                    {{ loading ? 'Đang tải…' : 'Xem thêm kết quả' }}
                </button>
                <span v-else-if="posts.length" class="muted text-xs">Đã hết kết quả</span>
            </div>

            <div v-else class="flex items-center justify-center gap-2 mt-4 flex-wrap">
                <button class="icon-btn" title="Trang trước" :disabled="currentPage <= 1" @click="currentPageChange(-1)">
                    <AppIcon name="chevron-left" :size="18" />
                </button>
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
                <button class="icon-btn" title="Trang sau" :disabled="currentPage >= totalPages" @click="currentPageChange(1)">
                    <AppIcon name="chevron-right" :size="18" />
                </button>
                <button type="button" class="sign-btn sign-btn--quiet" @click="doGoto">Đi tới</button>
            </div>
        </div>
    </div>
</template>

<script setup>
import Post from '../../components/Post/Post.vue';
import AppModal from '@/components/ui/AppModal.vue';
import { getFollowedTagsFeed, getListPostApi, searchPost, getFeedPages, getFriendsFeed, getFriendsFeedPages, getTrendingHashtags } from '@/apis/post';
import { dayKey, dayLabel, dayStamp } from '@/js/helper';
import { computed, inject, nextTick, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import BaseAvatar from '@/components/BaseAvatar.vue';
import { getItemLocal, LOCALKEYS } from '../../storages/localStorage';
import CreatePost from '../../components/Post/CreatePost.vue';
import AppIcon from '@/components/AppIcon.vue';

const router = useRouter();
const isLogin = computed(() => getItemLocal(LOCALKEYS.ACCESS_TOKEN) != null);
const posts = ref([]);
const currentPage = ref(pageFromQuery());
const totalPages = ref(1);
const gotoPage = ref(currentPage.value);
const ishowCreatePost = ref(false);
const loading = ref(false);
const feedMode = ref('all'); // 'all' | 'friends' | 'tags'
const followedTags = ref([]);   // để câu thông báo rỗng nói đúng chuyện gì đang xảy ra
const trendingTags = ref([]);

const loadTrending = async () => {
    try {
        trendingTags.value = (await getTrendingHashtags(12))?.data?.data || [];
    } catch (e) { trendingTags.value = []; }
}

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
    // Feed hashtag trả totalPages ngay trong chính phản hồi -> không có endpoint /pages riêng
    if (feedMode.value === 'tags') return;
    try {
        const fn = feedMode.value === 'friends' ? getFriendsFeedPages : getFeedPages;
        const info = (await fn())?.data?.data || {};
        totalPages.value = info.totalPages || 1;
        countTo(info.total || 0);
    } catch (e) { totalPages.value = 1; }
}

/* Chuyển động duy nhất không do người dùng kích hoạt trên trang này: con số trên
   tấm biển đếm tăng một lần khi tải xong. Ai bật "giảm chuyển động" thì nhảy thẳng
   tới số cuối. */
// Ngày hôm nay ở măng-sét — đọc một lần khi mở trang, không cần phản ứng gì
const todayStamp = dayStamp(new Date());

const shownTotal = ref(0);
const countTo = (target) => {
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;
    if (reduce || target <= 0) { shownTotal.value = target; return; }
    const start = performance.now();
    const dur = 700;
    const step = (now) => {
        const p = Math.min((now - start) / dur, 1);
        shownTotal.value = Math.round(target * (1 - Math.pow(1 - p, 3)));
        if (p < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
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
        if (feedMode.value === 'tags' && isLogin.value) {
            // page của endpoint này bắt đầu từ 0, còn currentPage của trang bắt đầu từ 1
            const d = (await getFollowedTagsFeed(currentPage.value - 1))?.data?.data || {};
            posts.value = d.items || [];
            followedTags.value = d.tags || [];
            totalPages.value = d.totalPages || 1;
            return;
        }
        const fn = (feedMode.value === 'friends' && isLogin.value) ? getFriendsFeed : getListPostApi;
        const data = await fn(currentPage.value);
        posts.value = data?.data?.data || [];
    } catch (e) {
        posts.value = [];
    } finally {
        loading.value = false;
        // Báo router: danh sách đã render xong -> có thể khôi phục vị trí cuộn (back/forward)
        await nextTick();
        window.dispatchEvent(new Event('page:ready'));
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

// input[type=search]: bấm nút ✕ của trình duyệt cũng bắn @input -> ô rỗng thì bỏ tìm kiếm
const onSearchChanged = () => {
    if (!searchText.value) clearSearch();
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

onMounted(() => { getListPost(); loadPageInfo(); loadTrending(); });
</script>
