<template>
    <div class="content flex justify-center flex-wrap">
        <div class="w-[80%] border p-5 rounded">
            <div class="flex mb-3">
                <span class="text-[32px] text-orange-400 font-bold">
                    Tìm người dùng
                </span>
                <div class="ml-auto">
                    <DxTextBox
                        v-model="searchText"
                        @valueChanged="handleSearch"
                        placeholder="Nhập tên hoặc email"
                        :showClearButton="true"
                    />
                </div>
            </div>

            <div v-if="users.length === 0" class="text-gray-500">
                Không có người dùng nào
            </div>

            <div v-for="user in users" :key="user.userId" class="flex items-center m-4">
                <BaseAvatar
                    :linkAvt="'http://localhost:8081/images/' + user.avtUrl"
                    :userCreatedPost="user.fullName"
                    :userId="user.userId"
                />
                <div class="text-black font-bold text-[18px] ml-5 w-[30%]">
                    {{ user.fullName }}
                </div>
                <div class="text-gray-600 mr-5">{{ user.followers }} người theo dõi</div>
                <div class="text-gray-600">{{ user.posts }} bài viết</div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { DxTextBox } from 'devextreme-vue';
import { ref } from 'vue';
import { searchUserApi } from '@/apis/user';
import BaseAvatar from '@/components/BaseAvatar.vue';

const users = ref([]);
const searchText = ref("");

const handleSearch = async () => {
    try {
        const data = await searchUserApi(searchText.value);
        users.value = data?.data?.data || [];
    } catch (error) {
        console.log(error);
        users.value = [];
    }
}
</script>
