<template>
    <div class="form-login">
        <div v-if="stage === 'creds'" class="flex flex-col items-stretch gap-3">
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
            <label class="flex items-center gap-2 text-sm muted cursor-pointer select-none">
                <input type="checkbox" v-model="remember" class="cursor-pointer" />
                Ghi nhớ đăng nhập
            </label>
            <DxButton width="100%" text="Đăng nhập" type="default" :disabled="busy" @click="loginHandler" />
            <div class="text-sm text-center muted">
                Chưa có tài khoản?
                <span class="link" @click="() => route.push('/signup')">Đăng ký</span>
            </div>
            <div class="text-sm text-center">
                <span class="link" @click="() => route.push('/forgot-password')">Quên mật khẩu?</span>
            </div>
        </div>

        <div v-else class="flex flex-col items-stretch gap-3">
            <div class="section-title">Xác thực 2 bước</div>
            <p class="muted text-sm text-center -mt-2">
                Nhập mã 6 ký tự vừa gửi tới <b>{{ twofaEmail }}</b>
            </p>
            <div class="form-login-fields">
                <div>
                    <label class="text-sm muted">Mã xác thực</label>
                    <DxTextBox v-model="code" @enter-key="verifyHandler" />
                </div>
            </div>
            <DxButton width="100%" text="Xác nhận" type="default" :disabled="busy" @click="verifyHandler" />
            <div class="text-sm text-center">
                <span class="link" @click="backToCreds">← Đăng nhập lại</span>
            </div>
        </div>
    </div>
</template>

<script setup>
import { DxTextBox } from 'devextreme-vue';
import DxButton from 'devextreme-vue/button';
import { login, verifyTwoFactor } from '../../apis/auth';
import { getUserInfo } from '@/apis/user';
import { inject, ref } from 'vue';
import { LOCALKEYS, setItemLocal, setAuthPersistence } from '../../storages/localStorage';
import { IMAGE_BASE } from '@/config';
import { useRouter } from 'vue-router';

const  route = useRouter();
const showDialog = inject("openDialogError");

const formData = ref({
    "email": "",
    "password": "",
})
const stage = ref('creds');   // 'creds' | '2fa'
const twofaEmail = ref('');
const code = ref('');
const busy = ref(false);
const remember = ref(false);  // bỏ tick = phiên tạm (đóng trình duyệt / ngồi im 30' là hết)

const finishLogin = async (d) => {
    setAuthPersistence(remember.value);   // phải đặt TRƯỚC khi lưu token
    setItemLocal(LOCALKEYS.ACCESS_TOKEN, d.accessToken);
    if (d.refreshToken) setItemLocal(LOCALKEYS.REFRESH_TOKEN, d.refreshToken);
    setItemLocal(LOCALKEYS.USER_ID, d.userId);
    setItemLocal(LOCALKEYS.IS_ADMIN, String(d.isAdmin) === '1');
    await getDataUser(d.userId);
    route.push('/');
}

const loginHandler = async() => {
    if (busy.value) return;
    busy.value = true;
    try{
        const data = await login(formData.value, remember.value);
        const d = data.data.data;
        if (d.twoFactorRequired === '1' || d.twoFactorRequired === true) {
            twofaEmail.value = d.email || formData.value.email;
            code.value = '';
            stage.value = '2fa';
            return;
        }
        await finishLogin(d);
    }
    catch (e) {
        const msg = e?.description || '';
        if (msg.includes('xác thực') && !msg.includes('2 bước')) {
            route.push({ path: '/signup', query: { verify: formData.value.email } });
            return;
        }
        showDialog("Đăng nhập thất bại", msg || "Tài khoản hoặc mật khẩu không chính xác!");
    } finally {
        busy.value = false;
    }
}

const verifyHandler = async () => {
    if (busy.value) return;
    if (!code.value.trim()) { showDialog("Thông báo", "Nhập mã xác thực"); return; }
    busy.value = true;
    try {
        const data = await verifyTwoFactor(twofaEmail.value, code.value.trim(), remember.value);
        await finishLogin(data.data.data);
    } catch (e) {
        showDialog("Xác thực thất bại", e?.description || "Mã không đúng hoặc đã hết hạn");
    } finally {
        busy.value = false;
    }
}

const backToCreds = () => {
    stage.value = 'creds';
    code.value = '';
    formData.value.password = '';
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
