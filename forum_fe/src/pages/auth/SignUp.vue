<template>
    <div class="form-login flex justify-center">
        <div class="w-[60%] flex flex-col bg-orange-400 items-center rounded py-4">
            <span class="text-[32px] mb-4">
                Đăng ký thành viên
            </span>
            <span class="text-white mb-4">
                Vui lòng nhập đầy đủ thông tin để đăng ký
            </span>
            <div class="form-sign-up flex flex-col w-[40%]">
                <div>
                    <span>Họ và tên:</span>
                    <DxTextBox v-model="dataForm.fullName"/>
                </div>
                <div>
                    <span>Email:</span>
                    <DxTextBox v-model="dataForm.email"/>
                </div>
                <div>
                    <span>Mật khẩu (tối thiểu 5 ký tự):</span>
                    <DxTextBox v-model="dataForm.password" mode="password"/>
                </div>
                <div>
                    <span>Ngày sinh:</span>
                    <DxDateBox v-model="dataForm.birthday" type="date" display-format="yyyy-MM-dd"/>
                </div>
                <div>
                    <span>Avatar:</span>
                    <input type="file" accept="image/*" @change="handleFileChange">
                </div>
                <div class="flex justify-center">
                    <DxButton
                        type="default"
                        @click="handleSignUp"
                    >
                        Đăng ký
                    </DxButton>
                </div>
                <div class="text-center mt-2">
                    <i class="cursor-pointer underline" @click="() => route.push('/login')">
                        Đã có tài khoản? Đăng nhập
                    </i>
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
    if (!avatar) {
        showDialog("Thông báo", "Vui lòng chọn ảnh đại diện");
        return;
    }
    try {
        await signup({
            fullName,
            email,
            password,
            birthday: formatDate(birthday),
            avatar,
        });
        showDialog("Thông báo", "Đăng ký thành công, hãy đăng nhập");
        route.push("/login");
    } catch (error) {
        showDialog("Thông báo", error?.description || "Đăng ký thất bại");
    }
};
</script>

<style scoped>
.form-sign-up>div{
    margin-bottom: 8px;
}
</style>
