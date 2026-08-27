<template>
    <div class="form-login flex justify-center">
        <div class="w-[60%] flex flex-col bg-orange-400 items-center rounded py-4">
            <span class="text-[32px] mb-4">
                Chỉnh sửa hồ sơ
            </span>
            <span class="text-white mb-4">
                Nhập mật khẩu hiện tại để xác nhận thay đổi
            </span>
            <div class="form-edit-profile flex flex-col w-[40%]">
                <div>
                    <span>Mật khẩu hiện tại (bắt buộc):</span>
                    <DxTextBox v-model="dataForm.password" mode="password"/>
                </div>
                <div>
                    <span>Email mới:</span>
                    <DxTextBox v-model="dataForm.email"/>
                </div>
                <div>
                    <span>Mật khẩu mới:</span>
                    <DxTextBox v-model="dataForm.newPassword" mode="password"/>
                </div>
                <div>
                    <span>Avatar mới:</span>
                    <input type="file" accept="image/*" @change="handleFileChange">
                </div>
                <div class="flex justify-center">
                    <DxButton
                        type="default"
                        @click="handleSubmit"
                    >
                        Cập nhật
                    </DxButton>
                </div>
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

const route = useRouter();
const showDialog = inject("openDialogError");

const dataForm = ref({
    password: null,
    email: null,
    newPassword: null,
    avatar: null,
});

function handleFileChange(event) {
    dataForm.value.avatar = event.target.files[0] || null;
}

const handleSubmit = async () => {
    if (!dataForm.value.password) {
        showDialog("Thông báo", "Vui lòng nhập mật khẩu hiện tại");
        return;
    }
    try {
        await editUser(dataForm.value);
        await refreshLocalUser();
        showDialog("Thông báo", "Cập nhật hồ sơ thành công");
        route.push('/');
    } catch (error) {
        showDialog("Thông báo", error?.description || "Cập nhật hồ sơ thất bại");
    }
}

const refreshLocalUser = async () => {
    try {
        const data = await getUserInfo(getItemLocal(LOCALKEYS.USER_ID));
        setItemLocal(LOCALKEYS.USER_NAME, data?.data?.data?.fullName);
        setItemLocal(LOCALKEYS.LINK_AVT, "http://localhost:8081/images/" + data?.data?.data?.avtUrl);
    } catch (error) {
        console.log(error);
    }
}
</script>

<style scoped>
.form-edit-profile>div{
    margin-bottom: 8px;
}
</style>
