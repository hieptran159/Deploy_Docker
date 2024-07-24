<template>
    <div class="base-avatar relative cursor-pointer" @click="() => {isFollow = !isFollow}">
        <img v-if="imageLoaded" :src="linkAvt" @load="handleImageLoad" width="36px" />
        <div v-if="!imageLoaded" class="size-[36px] rounded flex bg-red-600 items-center justify-center font-bold text-lg text-white">
            <span v-if="userCreatedPost && userCreatedPost?.length > 0">
                    {{ userCreatedPost[0] }}
            </span>
        </div>
        <div class="absolute top-[36px] left-0 flex flex-col w-[100px] p-1" v-if="isFollow && userName!= userCreatedPost && isShow">
            <DxButton
                type="default"
                @click="follow"
            >
                Theo dõi
            </DxButton>
            <DxButton
                type="danger"
                @click="unFollow"
            >
                Bỏ theo dõi
            </DxButton>
        </div>
    </div>
</template>

<script setup>
import {ref, inject} from 'vue';
import { DxButton } from 'devextreme-vue';
import { getItemLocal, LOCALKEYS } from '@/storages/localStorage';
import { followApi, unFollowApi } from '@/apis/follow';

const props = defineProps({
    linkAvt: {},
    userCreatedPost: { },
    userId: {},
    isShow: {type: Boolean, default:true}
});

const showDialog = inject("openDialogError");
const isFollow = ref(false);
const userName = ref(getItemLocal(LOCALKEYS.USER_NAME));

const imageLoaded = ref(false);
const follow = async() => {
    try {
        await followApi(props.userId);
        showDialog("Thông báo", "Theo dõi thành công");
    } catch (error) {
        showDialog("Thông báo", "Bạn đã theo dõi người dùng này rồi!");
    }
}

const unFollow = async() => {
    try {
        await unFollowApi(props.userId);
        showDialog("Thông báo", "Bỏ theo dõi thành công");
    } catch (error) {
        showDialog("Thông báo", "Bạn chưa theo dõi người dùng này!");
    }
}

const handleImageLoad = () => {
    imageLoaded.value = true;
}

</script>