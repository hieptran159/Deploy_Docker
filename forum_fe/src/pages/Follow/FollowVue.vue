<template>
    <div class="content flex justify-center flex-wrap">
        <div class="w-[80%] border p-5 rounded mb-5">
            <span class="text-[32px] text-orange-400 font-bold mb-5">
                Danh sách người dùng bạn đang theo dõi
            </span>
            <div v-if="listFollow.length === 0" class="text-gray-500 mt-3">
                Bạn chưa theo dõi ai
            </div>
            <div v-for="item in listFollow" :key="item.id" class="flex items-center m-5">
                <BaseAvatar
                    :userCreatedPost="item.name"
                    :userId="item.id"
                    :isShow="false"
                />
                <div
                    class="text-black font-bold text-[18px] ml-5 w-[25%] cursor-pointer hover:underline"
                    @click="() => route.push('/user/' + item.id)"
                >
                    {{ item.name }}
                </div>
                <DxButton
                    type="success"
                    icon="message"
                    @click="() => messageUser(item)"
                >
                    Nhắn tin
                </DxButton>
                <DxButton
                    class="ml-2"
                    type="danger"
                    @click="() => unFollow(item)"
                >
                    Bỏ theo dõi
                </DxButton>
            </div>
        </div>
    </div>
</template>

<script setup>
import { getAllFollow, unFollowApi } from '@/apis/follow';
import { getUserInfo } from "@/apis/user";
import { openDirectConversation } from '@/apis/chat';
import { onMounted, ref, inject } from 'vue';
import { useRouter } from 'vue-router';
import { getItemLocal, LOCALKEYS } from '@/storages/localStorage';
import BaseAvatar from '@/components/BaseAvatar.vue';
import { DxButton } from 'devextreme-vue';

const route = useRouter();
const showDialog = inject('openDialogError');

const listFollow = ref([]);

const getListFolloww = async() => {
    listFollow.value = [];
    try {
        const data = await getAllFollow(getItemLocal(LOCALKEYS.USER_ID));
        await getDataUser(data?.data?.data?.userId || []);
    } catch (error) {
        console.log(error);
    }
}

const getDataUser = async(ids) => {
    for (const id of ids) {
        try {
            const res = await getUserInfo(id);
            listFollow.value.push({ id, name: res?.data?.data?.fullName || id });
        } catch (e) {
            listFollow.value.push({ id, name: id });
        }
    }
}

const messageUser = async (item) => {
    try {
        const res = await openDirectConversation(item.id);
        const conv = res?.data?.data;
        if (conv?.conversationId) {
            route.push({ path: '/chat', query: { c: conv.conversationId, name: item.name } });
        } else {
            showDialog?.('Thông báo', 'Không mở được cuộc trò chuyện');
        }
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Không mở được cuộc trò chuyện');
    }
}

const unFollow = async (item) => {
    try {
        await unFollowApi(item.id);
        listFollow.value = listFollow.value.filter((u) => u.id !== item.id);
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Bỏ theo dõi thất bại');
    }
}

onMounted(async()=>{
    await getListFolloww();
})

</script>
