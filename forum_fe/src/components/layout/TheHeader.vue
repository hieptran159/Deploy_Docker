<template>
    <header class="app-header">
        <div class="app-header__inner">
            <span class="app-brand" @click="() => route.push('/')">HIP&nbsp;Forum</span>

            <div v-if="isLogin" class="flex-1 min-w-0">
                <DxTabs
                    :selected-index="0"
                    :dataSource="options"
                    styling-mode="secondary"
                    @item-click="selectChange"
                />
            </div>
            <div v-else class="flex-1"></div>

            <DxButton
                :icon="isDark ? 'sun' : 'moon'"
                hint="Đổi giao diện sáng/tối"
                stylingMode="text"
                @click="toggleTheme"
            />

            <div v-if="isLogin" class="row-actions">
                <div
                    class="flex items-center gap-2 cursor-pointer px-2 py-1 rounded-lg hover:bg-gray-100"
                    @click="() => route.push('/profile/edit')"
                >
                    <BaseAvatar
                        :link-avt="getItemLocal(LOCALKEYS.LINK_AVT)"
                        :user-created-post="getItemLocal(LOCALKEYS.USER_NAME)"
                        :is-show="false"
                    />
                    <span class="font-semibold text-[15px] hidden sm:inline">
                        {{ getItemLocal(LOCALKEYS.USER_NAME) }}
                    </span>
                </div>
                <DxButton icon="runner" hint="Đăng xuất" type="normal" stylingMode="text" @click="logout" />
            </div>

            <div v-else class="row-actions">
                <DxButton type="success" stylingMode="contained" @click="signUp">Đăng ký</DxButton>
                <DxButton type="default" stylingMode="contained" @click="() => route.push('/login')">Đăng nhập</DxButton>
            </div>
        </div>
    </header>
</template>

<script setup>
import DxTabs from 'devextreme-vue/tabs';
import DxButton from 'devextreme-vue/button';
import { ref, computed, watch, onMounted } from 'vue';
import { LOCALKEYS, getItemLocal, delItemLocal, setItemLocal } from '@/storages/localStorage';
import { useRouter } from 'vue-router';
import { logout as logoutApi } from '@/apis/auth';
import { checkIsAdmin } from '@/apis/admin';
import BaseAvatar from '../BaseAvatar.vue';

const  route = useRouter();

const isDark = ref(document.documentElement.dataset.theme === 'dark');
const toggleTheme = () => {
    isDark.value = !isDark.value;
    if (isDark.value) {
        document.documentElement.dataset.theme = 'dark';
        try { localStorage.setItem('theme', 'dark'); } catch (e) { /* ignore */ }
    } else {
        delete document.documentElement.dataset.theme;
        try { localStorage.setItem('theme', 'light'); } catch (e) { /* ignore */ }
    }
}

const isAdmin = ref(getItemLocal(LOCALKEYS.IS_ADMIN) === true);

const options = computed(() => {
    const base = [
        { id: 0, text: "Trang chủ", icon: "home" },
        { id: 1, text: "Nhắn tin", icon: "textdocument" },
        { id: 2, text: "Bạn bè", icon: "group" },
        { id: 3, text: "Tìm người dùng", icon: "search" },
    ];
    if (isAdmin.value) {
        base.push({ id: 4, text: "Quản trị", icon: "preferences" });
    }
    return base;
})

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
    delItemLocal(LOCALKEYS.IS_ADMIN);
    isAdmin.value = false;
    route.push('/login');
}

const refreshAdminFlag = async () => {
    if (!getItemLocal(LOCALKEYS.ACCESS_TOKEN)) {
        isAdmin.value = false;
        return;
    }
    const stored = getItemLocal(LOCALKEYS.IS_ADMIN);
    if (stored === true || stored === false) {
        isAdmin.value = stored;
        return;
    }
    const ok = await checkIsAdmin();
    setItemLocal(LOCALKEYS.IS_ADMIN, ok);
    isAdmin.value = ok;
}

onMounted(refreshAdminFlag);

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
    isAdmin.value = getItemLocal(LOCALKEYS.IS_ADMIN) === true;
})

</script>