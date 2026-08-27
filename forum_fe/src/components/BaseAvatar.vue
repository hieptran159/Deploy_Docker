<template>
    <div class="base-avatar relative cursor-pointer" @click="() => { isFollow = !isFollow }">
        <img
            v-show="showImage"
            :src="props.linkAvt"
            class="size-9 rounded-full object-cover bg-gray-100"
            @error="errored = true"
            @load="errored = false"
        />
        <div v-if="!showImage" class="avatar-fallback size-9 text-base">
            <span>{{ firstLetter }}</span>
        </div>

        <div
            class="absolute top-[38px] left-0 z-20 flex flex-col gap-1 w-[110px] p-1 bg-white border rounded-lg shadow"
            v-if="isFollow && userName != props.userCreatedPost && isShow"
        >
            <DxButton type="default" text="Theo dõi" @click="follow" />
            <DxButton type="danger" text="Bỏ theo dõi" @click="unFollow" />
        </div>
    </div>
</template>

<script setup>
import { ref, computed, watch, inject } from 'vue';
import { DxButton } from 'devextreme-vue';
import { getItemLocal, LOCALKEYS } from '@/storages/localStorage';
import { followApi, unFollowApi } from '@/apis/follow';

const props = defineProps({
    linkAvt: {},
    userCreatedPost: {},
    userId: {},
    isShow: { type: Boolean, default: true }
});

const showDialog = inject("openDialogError");
const isFollow = ref(false);
const userName = ref(getItemLocal(LOCALKEYS.USER_NAME));
const errored = ref(false);

// URL hợp lệ = có chuỗi và không kết thúc bằng "/" (tức là có tên file)
const hasValidUrl = computed(() => {
    const u = props.linkAvt;
    return typeof u === 'string' && u.trim() !== '' && !u.endsWith('/') && !u.endsWith('null') && !u.endsWith('undefined');
});
const showImage = computed(() => hasValidUrl.value && !errored.value);
const firstLetter = computed(() => (props.userCreatedPost || '?').toString().charAt(0));

// đổi ảnh -> thử lại
watch(() => props.linkAvt, () => { errored.value = false; });

const follow = async () => {
    try {
        await followApi(props.userId);
        showDialog("Thông báo", "Theo dõi thành công");
    } catch (error) {
        showDialog("Thông báo", "Bạn đã theo dõi người dùng này rồi!");
    }
}

const unFollow = async () => {
    try {
        await unFollowApi(props.userId);
        showDialog("Thông báo", "Bỏ theo dõi thành công");
    } catch (error) {
        showDialog("Thông báo", "Bạn chưa theo dõi người dùng này!");
    }
}
</script>
