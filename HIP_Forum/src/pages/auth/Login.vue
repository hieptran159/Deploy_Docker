<template>
    <div class="form-login flex justify-center">
        <div class="w-[60%] flex flex-col bg-orange-400 items-center rounded py-4">
            <span class="text-[32px] mb-4">
                Đăng nhập
            </span>
            <span class="text-white mb-4">
                Chào mừng bạn đến với diễn đàn, vui lòng đăng nhập để tiếp tục
            </span>
            <div class="w-[40%] mb-4">
                <DxForm
                    :formData="formData"
                >
                </DxForm>
            </div>
            <DxButton
                @click="loginHandler"
                type="default"
            >
                Đăng nhập
            </DxButton>
            <i class="mt-4">
                Nếu chưa có tài khoản vui lòng đăng kí
            </i>
        </div>
    </div>
</template>

<script setup>
import DxForm from 'devextreme-vue/form';
import DxButton from 'devextreme-vue/button';
import { login } from '../../apis/auth';
import { getUserInfo } from '@/apis/user';
import { inject, ref } from 'vue';
import { LOCALKEYS, setItemLocal } from '../../storages/localStorage';
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
        setItemLocal(LOCALKEYS.ACCESS_TOKEN, data.data.data.accessToken);
        setItemLocal(LOCALKEYS.USER_ID, data.data.data.userId);
        await getDataUser(data.data.data.userId);
        route.push('/');
    }   
    catch{
        showDialog("Đăng nhập thất bại", "Tài khoản hoặc mật khẩu không chính xác!")
    }
}

const getDataUser = async(id) => {
    try {
        const data = await getUserInfo(id);
        setItemLocal(LOCALKEYS.USER_NAME, data?.data?.data?.fullName);
        setItemLocal(LOCALKEYS.LINK_AVT, "http://localhost:8081/images/" + data?.data?.data?.avtUrl);
    } catch (error) {
        console.error(error);
    }
}

</script>