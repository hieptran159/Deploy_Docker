<template>
    <div class="header w-[100%] flex sticky mb-5">
        <div class="w-[40%] ml-[10%]">
            <DxTabs
            :selected-index="0"
            :dataSource="options"
            @item-click="selectChange"
            >
            </DxTabs>
        </div>
        <div class="ml-auto mr-[10%] flex items-center">
            <div v-if="isLogin">
                <BaseAvatar
                :link-avt="getItemLocal(LOCALKEYS.LINK_AVT)"
                :user-created-post="getItemLocal(LOCALKEYS.USER_NAME)"
                />
            </div>
            <div class="text-black font-bold text-[18px] text-center mx-2" >
                {{ getItemLocal(LOCALKEYS.USER_NAME) }}
            </div>
            <DxButton
                v-if="isLogin"
                type="danger"
                @click="logout"
            >
                Đăng xuất
            </DxButton>
            <DxButton
                v-if="!isLogin"
                class="mr-2"
                type="success"
                @click="signUp"
            >
                Đăng kí
            </DxButton>
            <DxButton
                v-if="!isLogin"
                type="default"
                @click="() => {route.push('/login')}"
            >
                Đăng nhập
            </DxButton>
        </div>
    </div>
</template>

<script setup>
import DxTabs from 'devextreme-vue/tabs';
import DxButton from 'devextreme-vue/button';
import { ref, watch } from 'vue';
import { LOCALKEYS, getItemLocal, delItemLocal } from '@/storages/localStorage';
import { useRouter } from 'vue-router';
import BaseAvatar from '../BaseAvatar.vue';

const  route = useRouter();

const options = ref([
    {
        id: 0,
        text: "Trang chủ",
        icon: "home"
    },
    {
        id: 1,
        text: "Nhắn tin",
        icon: "textdocument"
    },
    {
        id: 2,
        text: "Bạn bè",
        icon: "group"
    },
])

const isLogin = ref(
    getItemLocal(LOCALKEYS.ACCESS_TOKEN) != null
)

const logout = () => {
    delItemLocal(LOCALKEYS.ACCESS_TOKEN);
    delItemLocal(LOCALKEYS.USER_ID);
    delItemLocal(LOCALKEYS.LINK_AVT);
    delItemLocal(LOCALKEYS.USER_NAME);
    route.push('/login');
}

const selectChange = (e)=> {
    const id = e.itemData.id;
    if(id == 0){
        route.push('/');
    }
    else if(id == 1){
        route.push('/chat');
    }
    else {
        route.push('/follow');
    }
}

const signUp = () => {
    route.push("/signup")
}

watch(route.currentRoute, ()=>{
    isLogin.value = getItemLocal(LOCALKEYS.ACCESS_TOKEN) != null;
})

</script>