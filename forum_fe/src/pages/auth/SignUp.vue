<template>
    <div class="form-login">
        <div class="flex flex-col gap-2">
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
                    <label class="text-sm muted">Mật khẩu (tối thiểu 5 ký tự)</label>
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
    </div>
</template>

<script setup>
import { DxButton, DxTextBox, DxDateBox } from 'devextreme-vue';
import { useRouter } from 'vue-router';
import { inject, ref } from 'vue';
import { signup } from "@/apis/auth";

const route = useRouter();
const showDialog = inject("openDialogError");

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
    if (password.length < 5) {
        showDialog("Thông báo", "Mật khẩu phải có tối thiểu 5 ký tự");
        return;
    }
    try {
        const payload = { fullName, email, password, birthday: formatDate(birthday) };
        if (avatar) payload.avatar = avatar;
        await signup(payload);
        showDialog("Thông báo", "Đăng ký thành công, hãy đăng nhập");
        route.push("/login");
    } catch (error) {
        showDialog("Thông báo", error?.description || "Đăng ký thất bại");
    }
};
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
