<template>
    <div class="form-login flex justify-center">
        <div class="w-[60%] flex flex-col bg-orange-400 items-center rounded py-4">
            <span class="text-[32px] mb-4">
                Quên mật khẩu
            </span>

            <div v-if="step === 1" class="flex flex-col w-[40%]">
                <span class="text-white mb-4">
                    Nhập email đã đăng ký để nhận mã OTP
                </span>
                <div class="mb-2">
                    <span>Email:</span>
                    <DxTextBox v-model="email"/>
                </div>
                <div class="flex justify-center">
                    <DxButton type="default" @click="requestOtp">
                        Gửi mã OTP
                    </DxButton>
                </div>
            </div>

            <div v-else class="flex flex-col w-[40%]">
                <span class="text-white mb-4">
                    Đã gửi OTP tới email. Nhập OTP và mật khẩu mới.
                </span>
                <div class="mb-2">
                    <span>Mã OTP:</span>
                    <DxTextBox v-model="token"/>
                </div>
                <div class="mb-2">
                    <span>Mật khẩu mới (tối thiểu 5 ký tự):</span>
                    <DxTextBox v-model="newPassword" mode="password"/>
                </div>
                <div class="flex justify-center">
                    <DxButton type="default" @click="submitReset">
                        Đặt lại mật khẩu
                    </DxButton>
                </div>
            </div>

            <i class="mt-4 cursor-pointer" @click="() => route.push('/login')">
                Quay lại đăng nhập
            </i>
        </div>
    </div>
</template>

<script setup>
import { DxButton, DxTextBox } from 'devextreme-vue';
import { useRouter } from 'vue-router';
import { inject, ref } from 'vue';
import { requestResetToken, resetPassword } from '@/apis/auth';

const route = useRouter();
const showDialog = inject("openDialogError");

const step = ref(1);
const email = ref("");
const token = ref("");
const newPassword = ref("");

const requestOtp = async () => {
    if (!email.value) {
        showDialog("Thông báo", "Vui lòng nhập email");
        return;
    }
    try {
        await requestResetToken(email.value);
        showDialog("Thông báo", "Đã gửi mã OTP, vui lòng kiểm tra email");
        step.value = 2;
    } catch (error) {
        showDialog("Thông báo", error?.description || "Không gửi được OTP");
    }
}

const submitReset = async () => {
    if (!token.value || !newPassword.value) {
        showDialog("Thông báo", "Vui lòng nhập đủ OTP và mật khẩu mới");
        return;
    }
    try {
        const res = await resetPassword(token.value, newPassword.value);
        if (res?.data?.statusCode && res.data.statusCode !== 200) {
            showDialog("Thông báo", res.data.description || "OTP không hợp lệ hoặc đã hết hạn");
            return;
        }
        showDialog("Thông báo", "Đổi mật khẩu thành công, hãy đăng nhập lại");
        route.push('/login');
    } catch (error) {
        showDialog("Thông báo", error?.description || "Đặt lại mật khẩu thất bại");
    }
}
</script>
