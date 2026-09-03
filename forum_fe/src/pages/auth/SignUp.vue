<template>
    <div class="form-login">
        <!-- Bước 1: nhập thông tin -->
        <div v-if="step === 'form'" class="flex flex-col gap-2">
            <div class="section-title">Đăng ký thành viên</div>
            <p class="muted text-sm text-center -mt-2 mb-1">Nhập đầy đủ thông tin để tạo tài khoản</p>
            <div class="form-sign-up flex flex-col">
                <div>
                    <label class="text-sm muted">Họ và tên</label>
                    <DxTextBox v-model="dataForm.fullName"/>
                </div>
                <div>
                    <label class="text-sm muted">Email</label>
                    <DxTextBox v-model="dataForm.email"/>
                </div>
                <div>
                    <label class="text-sm muted">Mật khẩu (tối thiểu 8 ký tự)</label>
                    <DxTextBox v-model="dataForm.password" mode="password"/>
                </div>
                <div>
                    <label class="text-sm muted">Ngày sinh</label>
                    <DxDateBox v-model="dataForm.birthday" type="date" display-format="yyyy-MM-dd" width="100%"/>
                </div>
                <div>
                    <label class="text-sm muted">Ảnh đại diện (tuỳ chọn)</label>
                    <input type="file" accept="image/*" @change="handleFileChange">
                </div>
                <DxButton width="100%" text="Đăng ký" type="default" @click="handleSignUp"/>
                <div class="text-center text-sm muted mt-1">
                    Đã có tài khoản?
                    <span class="link" @click="() => route.push('/login')">Đăng nhập</span>
                </div>
            </div>
        </div>

        <!-- Bước 2: xác thực email -->
        <div v-else class="flex flex-col gap-3">
            <div class="section-title">Xác thực email</div>
            <p class="muted text-sm text-center -mt-2">
                Mã xác thực đã được gửi tới <b>{{ pendingEmail }}</b>. Nhập mã 6 ký tự để hoàn tất.
            </p>
            <div>
                <label class="text-sm muted">Mã xác thực</label>
                <DxTextBox v-model="code" @enter-key="handleVerify" />
            </div>
            <DxButton width="100%" text="Xác thực & đăng nhập" type="default" @click="handleVerify" />
            <div class="text-sm text-center">
                <span class="link" @click="handleResend">Gửi lại mã</span>
                <span class="muted"> · </span>
                <span class="link" @click="() => route.push('/login')">Về đăng nhập</span>
            </div>
        </div>
    </div>
</template>

<script setup>
import { DxButton, DxTextBox, DxDateBox } from 'devextreme-vue';
import { useRouter } from 'vue-router';
import { inject, onMounted, ref } from 'vue';
import { signup, verifyEmail, resendVerify } from "@/apis/auth";
import { getUserInfo } from '@/apis/user';
import { LOCALKEYS, setItemLocal } from '@/storages/localStorage';
import { IMAGE_BASE } from '@/config';

const route = useRouter();
const showDialog = inject("openDialogError");
const toast = inject("toast");

const step = ref('form');           // 'form' | 'verify'
const pendingEmail = ref('');
const code = ref('');

const dataForm = ref({
    fullName: "",
    email: "",
    password: "",
    birthday: null,
    avatar: null,
});

const formatDate = (value) => {
    if (!value) return "";
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return "";
    const pad = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

function handleFileChange(event) {
    dataForm.value.avatar = event.target.files[0] || null;
}

const handleSignUp = async () => {
    const { fullName, email, password, birthday, avatar } = dataForm.value;
    if (!fullName || !email || !password || !birthday) {
        showDialog("Thông báo", "Vui lòng nhập đầy đủ thông tin");
        return;
    }
    if (password.length < 8) {
        showDialog("Thông báo", "Mật khẩu phải có tối thiểu 8 ký tự");
        return;
    }
    try {
        const payload = { fullName, email, password, birthday: formatDate(birthday) };
        if (avatar) payload.avatar = avatar;
        await signup(payload);
        pendingEmail.value = email;
        step.value = 'verify';
    } catch (error) {
        showDialog("Thông báo", error?.description || "Đăng ký thất bại");
    }
};

const handleVerify = async () => {
    if (!code.value.trim()) { showDialog("Thông báo", "Nhập mã xác thực"); return; }
    try {
        const res = await verifyEmail(pendingEmail.value, code.value.trim());
        const d = res?.data?.data || {};
        setItemLocal(LOCALKEYS.ACCESS_TOKEN, d.accessToken);
        if (d.refreshToken) setItemLocal(LOCALKEYS.REFRESH_TOKEN, d.refreshToken);
        setItemLocal(LOCALKEYS.USER_ID, d.userId);
        setItemLocal(LOCALKEYS.IS_ADMIN, String(d.isAdmin) === '1');
        try {
            const u = await getUserInfo(d.userId);
            setItemLocal(LOCALKEYS.USER_NAME, u?.data?.data?.fullName);
            setItemLocal(LOCALKEYS.LINK_AVT, IMAGE_BASE + u?.data?.data?.avtUrl);
        } catch (e) { /* ignore */ }
        route.push('/');
    } catch (error) {
        showDialog("Thông báo", error?.description || "Xác thực thất bại");
    }
};

const handleResend = async () => {
    try {
        await resendVerify(pendingEmail.value);
        toast?.('Đã gửi lại mã xác thực');
    } catch (error) {
        showDialog("Thông báo", error?.description || "Gửi lại mã thất bại");
    }
};

onMounted(() => {
    const v = route.currentRoute.value.query.verify;
    if (v) { pendingEmail.value = String(v); step.value = 'verify'; }
});
</script>

<style scoped>
.form-sign-up > div {
    margin-bottom: 10px;
}
.form-sign-up label {
    display: block;
    margin-bottom: 2px;
}
</style>
