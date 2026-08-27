<template>
    <div class="page">
        <div class="card">
            <div class="flex items-center justify-between mb-1">
                <div class="section-title mb-0">Thông báo</div>
                <button
                    v-if="items.some((n) => !n.read)"
                    class="link text-sm"
                    @click="readAll"
                >Đánh dấu tất cả đã đọc</button>
            </div>

            <div v-if="loading && !items.length" class="state">Đang tải…</div>
            <div v-else-if="!items.length" class="state">Chưa có thông báo nào</div>

            <button
                v-for="n in items"
                :key="n.notificationId"
                class="w-full text-left flex gap-3 px-1 py-3 border-b last:border-b-0 hover:bg-gray-50 rounded-lg"
                :class="{ 'bg-[var(--brand-soft)]': !n.read }"
                @click="open(n)"
            >
                <img
                    v-if="avatarUrlOf(n)"
                    :src="avatarUrlOf(n)"
                    class="size-9 rounded-full object-cover flex-none bg-gray-100"
                    @error="(e) => (e.target.style.display = 'none')"
                />
                <div v-else class="avatar-fallback size-9 text-sm flex-none">
                    {{ (n.actorName || '?')[0] }}
                </div>
                <div class="min-w-0 flex-1">
                    <div class="text-sm">{{ n.message }}</div>
                    <div class="text-xs muted">{{ timeAgo(n.createdAt) }}</div>
                </div>
                <span v-if="!n.read" class="size-2 rounded-full bg-[var(--brand)] flex-none mt-2"></span>
            </button>

            <div v-if="items.length" class="pt-3 text-center">
                <button
                    v-if="hasMore"
                    class="link text-sm"
                    :disabled="loading"
                    @click="loadMore"
                >{{ loading ? 'Đang tải…' : 'Xem thêm' }}</button>
                <span v-else class="muted text-xs">Đã hết</span>
            </div>
        </div>
    </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getNotificationsPaged, markAllRead, markRead } from '@/apis/notification';
import { notifRoute } from '@/js/notifTarget';
import { timeAgo } from '@/js/helper';
import { IMAGE_BASE } from '@/config';
import { bumpNotifRefresh } from '@/storages/appState';

const PAGE_SIZE = 20;
const route = useRouter();

const items = ref([]);
const page = ref(0);
const loading = ref(false);
const hasMore = ref(true);

const avatarUrlOf = (n) => {
    const a = n?.actorAvatar;
    if (!a || String(a).includes('null') || String(a).includes('undefined')) return '';
    return IMAGE_BASE + a;
};

const load = async (reset = false) => {
    if (loading.value) return;
    loading.value = true;
    if (reset) {
        page.value = 0;
        hasMore.value = true;
    }
    try {
        const res = await getNotificationsPaged(page.value, PAGE_SIZE);
        const batch = res?.data?.data || [];
        items.value = reset ? batch : [...items.value, ...batch];
        hasMore.value = batch.length === PAGE_SIZE;
        page.value += 1;
    } catch (e) {
        if (reset) items.value = [];
        hasMore.value = false;
    } finally {
        loading.value = false;
    }
};

const loadMore = () => load(false);

const open = async (n) => {
    if (!n.read) {
        n.read = true;
        try {
            await markRead(n.notificationId);
            bumpNotifRefresh();
        } catch (e) {
            /* ignore */
        }
    }
    const loc = notifRoute(n);
    if (loc) route.push(loc);
};

const readAll = async () => {
    try {
        await markAllRead();
        bumpNotifRefresh();
    } catch (e) {
        /* ignore */
    }
    items.value = items.value.map((n) => ({ ...n, read: true }));
};

onMounted(() => load(true));
</script>
