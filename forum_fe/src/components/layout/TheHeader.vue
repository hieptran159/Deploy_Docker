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
            <div
                v-if="isLogin"
                class="text-black font-bold text-[18px] text-center mx-2 cursor-pointer"
                @click="() => {route.push('/profile/edit')}"
            >
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
import { logout as logoutApi } from '@/apis/auth';
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
    {
        id: 3,
        text: "Tìm người dùng",
        icon: "search"
    },
    {
        id: 4,
        text: "Quản trị",
        icon: "preferences"
    },
])

const isLogin = ref(
    getItemLocal(LOCALKEYS.ACCESS_TOKEN) != null
)

const logout = async () => {
    try {
        await logoutApi();
    } catch (error) {
        console.log(error);
    }
    delItemLocal(LOCALKEYS.ACCESS_TOKEN);
    delItemLocal(LOCALKEYS.USER_ID);
    delItemLocal(LOCALKEYS.LINK_AVT);
    delItemLocal(LOCALKEYS.USER_NAME);
    route.push('/login');
}

const routeById = {
    0: '/',
    1: '/chat',
    2: '/follow',
    3: '/users',
    4: '/admin',
}

const selectChange = (e)=> {
    route.push(routeById[e.itemData.id] || '/');
}

const signUp = () => {
    route.push("/signup")
}

watch(route.currentRoute, ()=>{
    isLogin.value = getItemLocal(LOCALKEYS.ACCESS_TOKEN) != null;
})

</script>