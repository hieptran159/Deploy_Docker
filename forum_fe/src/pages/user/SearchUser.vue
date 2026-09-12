<template>
    <div class="page page--wide">
        <div class="card">
            <div class="flex items-center gap-3 mb-4">
                <span class="section-title mb-0 flex-1">Tìm người dùng</span>
                <input
                    type="search"
                    style="width: 240px"
                    class="field"
                    v-model="searchText"
                    @input="onType"
                    @keyup.enter="() => load(true)"
                    placeholder="Tên hoặc email…"
                />
                <button class="sign-btn flex-none" @click="() => load(true)">
                    <AppIcon name="search" :size="16" /> Tìm
                </button>
            </div>

            <div v-if="loading" class="state">Đang tải…</div>
            <div v-else-if="users.length === 0" class="state">Không có người dùng nào</div>
            <div v-else-if="total" class="muted text-sm mb-2">{{ total }} người</div>

            <div
                v-for="user in users"
                :key="user.userId"
                class="flex items-center gap-4 py-3 border-b last:border-b-0"
            >
                <BaseAvatar
                    :linkAvt="IMAGE_BASE + user.avtUrl"
                    :userCreatedPost="user.fullName"
                    :userId="user.userId"
                />
                <div
                    class="font-semibold text-[15px] flex-1 min-w-0 truncate cursor-pointer hover:underline"
                    @click="() => route.push('/user/' + user.userId)"
                >
                    {{ user.fullName }}
                </div>
                <div class="muted text-sm flex-none">{{ user.followers ?? 0 }} theo dõi</div>
                <div class="muted text-sm flex-none">{{ user.posts ?? 0 }} bài</div>
            </div>

            <div v-if="users.length < total" class="pt-3 text-center">
                <button class="link text-sm" :disabled="loading" @click="loadMore">
                    {{ loading ? 'Đang tải…' : `Xem thêm (${total - users.length})` }}
                </button>
            </div>
        </div>
    </div>
</template>

<script setup>
import AppIcon from '@/components/AppIcon.vue';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { searchUserApi } from '@/apis/user';
import { IMAGE_BASE } from '@/config';
import BaseAvatar from '@/components/BaseAvatar.vue';

const route = useRouter();
const users = ref([]);
const searchText = ref("");
const loading = ref(false);
const total = ref(0);
const page = ref(0);

const PAGE = 20;

/* Lọc và phân trang PHÍA SERVER. Trước đây trang này tải toàn bộ người dùng về
   (190 KB, 1,5s cho 43 người — và tăng tuyến tính theo số thành viên) rồi mới cắt
   10 dòng ở client. */
const load = async (reset) => {
    if (reset) { page.value = 0; users.value = []; }
    loading.value = true;
    try {
        const d = (await searchUserApi(searchText.value || "", page.value, PAGE))?.data?.data || {};
        const batch = d.items || [];
        users.value = page.value === 0 ? batch : [...users.value, ...batch];
        total.value = d.total ?? users.value.length;
    } catch (error) {
        if (page.value === 0) users.value = [];
    } finally {
        loading.value = false;
    }
}

const loadMore = () => { page.value += 1; load(false); };

// Gõ tới đâu tìm tới đó, hoãn 250 ms để không bắn một request mỗi ký tự
let timer = null;
const onType = () => {
    clearTimeout(timer);
    timer = setTimeout(() => load(true), 250);
};

onMounted(() => load(true));
</script>
