<template>
    <div class="form-login">
        <div class="flex flex-col items-stretch gap-3">
            <div class="section-title">Đăng nhập</div>
            <p class="muted text-sm text-center -mt-2">Chào mừng bạn đến với diễn đàn</p>
            <div class="form-login-fields">
                <div>
                    <label class="text-sm muted">Email</label>
                    <DxTextBox v-model="formData.email" @enter-key="loginHandler"/>
                </div>
                <div>
                    <label class="text-sm muted">Mật khẩu</label>
                    <DxTextBox
                        v-model="formData.password"
                        mode="password"
                        @enter-key="loginHandler"
                    />
                </div>
            </div>
            <DxButton width="100%" text="Đăng nhập" type="default" @click="loginHandler" />
            <div class="text-sm text-center muted">
                Chưa có tài khoản?
                <span class="link" @click="() => route.push('/signup')">Đăng ký</span>
            </div>
            <div class="text-sm text-center">
                <span class="link" @click="() => route.push('/forgot-password')">Quên mật khẩu?</span>
            </div>
        </div>
    </div>
</template>

<script setup>
import { DxTextBox } from 'devextreme-vue';
import DxButton from 'devextreme-vue/button';
import { login } from '../../apis/auth';
import { getUserInfo } from '@/apis/user';
import { inject, ref } from 'vue';
import { LOCALKEYS, setItemLocal } from '../../storages/localStorage';
import { IMAGE_BASE } from '@/config';
import { useRouter } from 'vue-router';

const  route = useRouter();
const showDialog = inject("openDialogError");

const formData = ref({
    "email": "",
    "password": "",
})

const loginHandler = async() => {
    try{
        const data = await login(formData.value);
        const d = data.data.data;
        setItemLocal(LOCALKEYS.ACCESS_TOKEN, d.accessToken);
        setItemLocal(LOCALKEYS.USER_ID, d.userId);
        setItemLocal(LOCALKEYS.IS_ADMIN, String(d.isAdmin) === '1');
        await getDataUser(d.userId);
        route.push('/');
    }
    catch (e) {
        const msg = e?.description || '';
        if (msg.includes('xác thực')) {
            route.push({ path: '/signup', query: { verify: formData.value.email } });
            return;
        }
        showDialog("Đăng nhập thất bại", msg || "Tài khoản hoặc mật khẩu không chính xác!");
    }
}

const getDataUser = async(id) => {
    try {
        const data = await getUserInfo(id);
        setItemLocal(LOCALKEYS.USER_NAME, data?.data?.data?.fullName);
        setItemLocal(LOCALKEYS.LINK_AVT, IMAGE_BASE + data?.data?.data?.avtUrl);
    } catch (error) {
        console.error(error);
    }
}

</script>

<style scoped>
.form-login-fields > div {
    margin-bottom: 10px;
}
.form-login-fields label {
    display: block;
    margin-bottom: 2px;
}
</style>