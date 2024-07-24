<template>
    <div class="content flex justify-center flex-wrap">
        <div class="w-[80%] border p-5 rounded mb-5">
            <span class="text-[32px] text-orange-400 font-bold mb-5">
                Danh sách người dùng bạn đang theo dõi
            </span>
            <div v-for="item in listFollow" :key="item.id" class="flex m-5">
                <BaseAvatar
                    :userCreatedPost="item.name"
                    :userId="item.id"
                    :isShow="false"
                />
                <div class="text-black font-bold text-[18px] ml-5 mt-1 w-[20%]" >
                    {{ item.name }}
                </div>
                <DxButton
                    type="danger"
                    @click="unFollow"
                >
                    Bỏ theo dõi
                </DxButton>
            </div>
        </div>
    </div>
</template>

<script setup>
import { getAllFollow } from '@/apis/follow';
import { getUserInfo } from "@/apis/user"
import {onMounted, ref} from 'vue';
import { getItemLocal, LOCALKEYS } from '@/storages/localStorage';
import BaseAvatar from '@/components/BaseAvatar.vue';
import { DxButton } from 'devextreme-vue';

const listFollow = ref([]);

const getListFolloww = async() => {
    try {
        const data = await getAllFollow(getItemLocal(LOCALKEYS.USER_ID));
        await getDataUser(data?.data.data.userId);
        console.log(listFollow.value);
    } catch (error) {
        
    }
}

const getDataUser = async(data) => {
    data.forEach(async(element) => {
        const data = await getUserInfo(element);
        listFollow.value.push({"id": element, "name": data?.data?.data?.fullName});
    });
}


onMounted(async()=>{
    await getListFolloww();
})

</script>