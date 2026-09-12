<template>
    <div class="form-login">
        <div class="flex flex-col gap-3">
            <div class="section-title">Quên mật khẩu</div>

            <div v-if="step === 1" class="flex flex-col gap-2">
                <p class="muted text-sm -mt-3 mb-1">Nhập email đã đăng ký để nhận mã OTP</p>
                <div>
                    <label class="text-sm muted">Email</label>
                    <input type="text" class="field" v-model="email" @keyup.enter="requestOtp" />
                </div>
                <button type="button" class="sign-btn sign-btn--block" @click="requestOtp">Gửi mã OTP</button>
            </div>

            <div v-else class="flex flex-col gap-2">
                <p class="muted text-sm -mt-3 mb-1">Đã gửi OTP tới email. Nhập OTP và mật khẩu mới.</p>
                <div>
                    <label class="text-sm muted">Mã OTP</label>
                    <input type="text" class="field" v-model="token" />
                </div>
                <div>
                    <label class="text-sm muted">Mật khẩu mới (tối thiểu 8 ký tự)</label>
                    <input type="password" class="field" v-model="newPassword" @keyup.enter="submitReset" />
                </div>
                <button type="button" class="sign-btn sign-btn--block" @click="submitReset">Đặt lại mật khẩu</button>
            </div>

            <div class="text-sm">
                <span class="link" @click="() => route.push('/login')">Quay lại đăng nhập</span>
            </div>
        </div>
    </div>
</template>

<script setup>
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
