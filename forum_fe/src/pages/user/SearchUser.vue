<template>
    <div class="page">
        <div class="card">
            <div class="flex items-center gap-3 mb-4">
                <span class="section-title mb-0 flex-1">Tìm người dùng</span>
                <DxTextBox
                    :value="searchText"
                    @value-changed="onInput"
                    @enter-key="doSearch"
                    placeholder="Tên hoặc email…"
                    :show-clear-button="true"
                    width="240"
                />
                <DxButton icon="search" type="default" text="Tìm" @click="doSearch" />
            </div>

            <div v-if="loading" class="state">Đang tải…</div>
            <div v-else-if="users.length === 0" class="state">Không có người dùng nào</div>

            <div
                v-for="user in visibleUsers"
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

            <div v-if="!loading && shown < users.length" class="pt-3 text-center">
                <button class="link text-sm" @click="shown += PAGE">
                    Xem thêm ({{ users.length - shown }})
                </button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { DxTextBox, DxButton } from 'devextreme-vue';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { searchUserApi } from '@/apis/user';
import { IMAGE_BASE } from '@/config';
import BaseAvatar from '@/components/BaseAvatar.vue';

const route = useRouter();
const users = ref([]);
const searchText = ref("");
const loading = ref(false);

const PAGE = 20;
const shown = ref(PAGE);
const visibleUsers = computed(() => users.value.slice(0, shown.value));

const onInput = (e) => {
    searchText.value = e?.value ?? "";
}

const doSearch = async () => {
    loading.value = true;
    shown.value = PAGE;
    try {
        const data = await searchUserApi(searchText.value || "");
        users.value = data?.data?.data || [];
    } catch (error) {
        console.log(error);
        users.value = [];
    } finally {
        loading.value = false;
    }
}

onMounted(doSearch);
</script>
