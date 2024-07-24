<template>
    <div class="form-login flex justify-center">
        <div class="w-[60%] flex flex-col bg-orange-400 items-center rounded py-4">
            <span class="text-[32px] mb-4">
                Đăng ký thành viên
            </span>
            <span class="text-white mb-4">
                Vui lòng nhập đầy đủ thông tin để đăng ký
            </span>
            <div class="form-sign-up felx flex-col w-[40%]">
                <div>
                    <span>Họ và tên:</span>
                    <DxTextBox v-model="dataForm.fullName"/>
                </div>
                <div>
                    <span>Email:</span>
                    <DxTextBox v-model="dataForm.email"/>
                </div>
                <div>
                    <span>Mật khẩu:</span>
                    <DxTextBox v-model="dataForm.password"/>
                </div>
                <div>
                    <span>Ngày sinh:</span>
                    <DxDateBox v-model="dataForm.birthday"/>
                </div>
                <div>
                    <span>Avatar:</span>
                    <input type="file" @change="handleFileChange">
                </div>
                <div class="flex justify-center">
                    <DxButton
                        type="default"
                        @click="handleSignUp"
                    >
                        Đăng ký
                    </DxButton>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import {DxButton, DxTextBox, DxDateBox} from 'devextreme-vue';
import { useRouter } from 'vue-router';
import {inject, ref} from 'vue';
import {signup} from "@/apis/auth";

const route = useRouter();
const showDialog = inject("openDialogError");

const dataForm = ref({
    fullName: null,
    email: null,
    password: null,
    birthday: null,
    avatar: null,
})

const handleSignUp = async() => {
    try {
        await signup(dataForm.value);
    } catch (error) {
        
    }
}

function handleFileChange(event) {
      const file = event.target.files[0];
      if (file) {
        fileToBase64(file, base64String => {
          dataForm.value.avatar = base64String;
        });
      }
    }
function fileToBase64(file, callback) {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = function () {
        // Đọc dữ liệu base64 string từ FileReader
        const base64String = reader.result.split(',')[1];
        // Gọi callback với base64 string như là tham số
        callback(base64String);
      };
      reader.onerror = function (error) {
        console.error('Error occurred while reading the file.', error);
      };
    }

</script>

<style scoped>
.form-sign-up>div{
    margin-bottom: 8px;
}

</style>