<template>
    <div class="content flex justify-center flex-wrap">
        <div class="w-[80%] border p-5 rounded">
            <div class="flex items-center mb-3">
                <span class="text-[32px] text-orange-400 font-bold">
                    Tìm người dùng
                </span>
                <div class="ml-auto flex items-center">
                    <DxTextBox
                        :value="searchText"
                        @value-changed="onInput"
                        @enter-key="doSearch"
                        placeholder="Nhập tên hoặc email rồi Enter"
                        :showClearButton="true"
                        width="260"
                    />
                    <DxButton
                        class="ml-2"
                        icon="search"
                        type="default"
                        @click="doSearch"
                    >
                        Tìm
                    </DxButton>
                </div>
            </div>

            <div v-if="loading" class="text-gray-500">Đang tải...</div>
            <div v-else-if="users.length === 0" class="text-gray-500">
                Không có người dùng nào
            </div>

            <div v-for="user in users" :key="user.userId" class="flex items-center m-4">
                <BaseAvatar
                    :linkAvt="'http://localhost:8081/images/' + user.avtUrl"
                    :userCreatedPost="user.fullName"
                    :userId="user.userId"
                />
                <div
                    class="text-black font-bold text-[18px] ml-5 w-[30%] cursor-pointer hover:underline"
                    @click="() => route.push('/user/' + user.userId)"
                >
                    {{ user.fullName }}
                </div>
                <div class="text-gray-600 mr-5">{{ user.followers }} người theo dõi</div>
                <div class="text-gray-600">{{ user.posts }} bài viết</div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { DxTextBox, DxButton } from 'devextreme-vue';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { searchUserApi } from '@/apis/user';
import BaseAvatar from '@/components/BaseAvatar.vue';

const route = useRouter();
const users = ref([]);
const searchText = ref("");
const loading = ref(false);

const onInput = (e) => {
    searchText.value = e?.value ?? "";
}

const doSearch = async () => {
    loading.value = true;
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
