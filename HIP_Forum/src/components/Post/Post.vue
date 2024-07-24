<template>
    <div class="post-item flex p-1 bg-[#fefefe]">
        <img v-if="imageLoaded" :src="linkAvt" @load="handleImageLoad" width="36px" />
        <div v-if="!imageLoaded" class="size-[36px] rounded flex bg-red-600 items-center justify-center font-bold text-lg text-white mt-1">
            {{ userCreatedPost[0] }}
        </div>
        <div class="flex flex-col w-[70%]">
            <div class="flex flex-col ml-3">
                <span class="font-bold text-lg text-[#2577b1] whitespace-nowrap truncate cursor-pointer" @click="viewDetail">{{ post?.title }}</span>
                <span class="mt-1">
                    <span class="text-[#1e5b7e]">bởi {{ userCreatedPost }} </span>
                    <i class="ml-2 text-yellow-400">{{ calculateTimeDifference(post?.postedAt) }} trước</i> 
                </span>
            </div>
        </div>
        <div class="flex flex-col ml-[10%]">
            <span class="text-red-500">
                <b>{{ post?.likesQuantity }}</b> lượt yêu thích
            </span>
            <span class="text-yellow-400">
                <b>{{ post?.commentsQuantity }}</b> bình luận
            </span>
        </div>
    </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { getUserInfo } from '../../apis/user';
import { calculateTimeDifference } from '../../js/helper';

import { useRouter } from 'vue-router';

const  route = useRouter();

const props = defineProps({
    post: {
        type: Object
    }
});

const linkAvt = ref();  
const post = computed(()=> props.post)
const userCreatedPost = ref("");
const imageLoaded = ref(false);

const getDataUser = async() => {
    try {
        const data = await getUserInfo(post.value?.userCreatedPost);
        userCreatedPost.value = data?.data?.data?.fullName;
        linkAvt.value = "http://localhost:8081/images/" + data?.data?.data?.avtUrl;
    } catch (error) {
        console.error(error);
    }
}

const viewDetail = () => {
    route.push(`/post/${post?.value?.postId}`);
}

const handleImageLoad = () => {
    imageLoaded.value = true;
}

onMounted(async()=>{
    await getDataUser();
})

</script>
