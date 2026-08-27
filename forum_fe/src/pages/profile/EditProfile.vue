<template>
    <div class="page">
        <div class="card">
            <div class="section-title">Xác thực</div>
            <p class="muted text-sm mb-2">Nhập mật khẩu hiện tại — bắt buộc cho mọi thay đổi bên dưới.</p>
            <DxTextBox v-model="currentPassword" mode="password" placeholder="Mật khẩu hiện tại" />
        </div>

        <div class="card">
            <div class="section-title">Đổi email</div>
            <div class="flex flex-col sm:flex-row gap-2">
                <DxTextBox v-model="newEmail" class="flex-1" placeholder="Email mới" />
                <DxButton type="default" text="Cập nhật email" @click="updateEmail" />
            </div>
        </div>

        <div class="card">
            <div class="section-title">Đổi mật khẩu</div>
            <div class="flex flex-col sm:flex-row gap-2">
                <DxTextBox v-model="newPassword" mode="password" class="flex-1" placeholder="Mật khẩu mới (tối thiểu 5 ký tự)" />
                <DxButton type="default" text="Đổi mật khẩu" @click="updatePassword" />
            </div>
        </div>

        <div class="card">
            <div class="section-title">Đổi ảnh đại diện</div>
            <div class="flex flex-col sm:flex-row sm:items-center gap-2">
                <input type="file" accept="image/*" class="flex-1" @change="handleFileChange" />
                <DxButton type="default" text="Cập nhật ảnh" @click="updateAvatar" />
            </div>
        </div>
    </div>
</template>

<script setup>
import { DxButton, DxTextBox } from 'devextreme-vue';
import { useRouter } from 'vue-router';
import { inject, ref } from 'vue';
import { editUser, getUserInfo } from '@/apis/user';
import { LOCALKEYS, getItemLocal, setItemLocal } from '@/storages/localStorage';
import { IMAGE_BASE } from '@/config';

const route = useRouter();
const showDialog = inject("openDialogError");

const currentPassword = ref("");
const newEmail = ref("");
const newPassword = ref("");
const avatarFile = ref(null);

function handleFileChange(event) {
    avatarFile.value = event.target.files[0] || null;
}

const requirePassword = () => {
    if (!currentPassword.value) {
        showDialog("Thông báo", "Vui lòng nhập mật khẩu hiện tại");
        return false;
    }
    return true;
}

const refreshLocalUser = async () => {
    try {
        const data = await getUserInfo(getItemLocal(LOCALKEYS.USER_ID));
        setItemLocal(LOCALKEYS.USER_NAME, data?.data?.data?.fullName);
        setItemLocal(LOCALKEYS.LINK_AVT, IMAGE_BASE + data?.data?.data?.avtUrl);
    } catch (error) {
        console.log(error);
    }
}

const updateEmail = async () => {
    if (!requirePassword()) return;
    if (!newEmail.value) { showDialog("Thông báo", "Nhập email mới"); return; }
    try {
        await editUser({ password: currentPassword.value, email: newEmail.value });
        await refreshLocalUser();
        showDialog("Thông báo", "Cập nhật email thành công");
        newEmail.value = "";
    } catch (e) {
        showDialog("Thông báo", e?.description || "Cập nhật email thất bại");
    }
}

const updatePassword = async () => {
    if (!requirePassword()) return;
    if (!newPassword.value || newPassword.value.length < 5) {
        showDialog("Thông báo", "Mật khẩu mới phải có tối thiểu 5 ký tự");
        return;
    }
    try {
        await editUser({ password: currentPassword.value, newPassword: newPassword.value });
        showDialog("Thông báo", "Đổi mật khẩu thành công, vui lòng đăng nhập lại", () => route.push('/login'));
        newPassword.value = "";
        currentPassword.value = "";
    } catch (e) {
        showDialog("Thông báo", e?.description || "Đổi mật khẩu thất bại");
    }
}

const updateAvatar = async () => {
    if (!requirePassword()) return;
    if (!avatarFile.value) { showDialog("Thông báo", "Chọn ảnh đại diện mới"); return; }
    try {
        await editUser({ password: currentPassword.value, avatar: avatarFile.value });
        await refreshLocalUser();
        showDialog("Thông báo", "Cập nhật ảnh đại diện thành công");
        avatarFile.value = null;
    } catch (e) {
        showDialog("Thông báo", e?.description || "Cập nhật ảnh thất bại");
    }
}
</script>
