<template>
    <div class="base-avatar" :class="{ 'cursor-pointer': userId }" @click="goProfile">
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
    </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRouter } from 'vue-router';

const props = defineProps({
    linkAvt: {},
    userCreatedPost: {},
    userId: {},
    // giữ lại prop để không phải sửa mọi nơi đang truyền :is-show
    isShow: { type: Boolean, default: true },
});

const router = useRouter();
const errored = ref(false);

const hasValidUrl = computed(() => {
    const u = props.linkAvt;
    return typeof u === 'string' && u.trim() !== '' && !u.endsWith('/') && !u.endsWith('null') && !u.endsWith('undefined');
});
const showImage = computed(() => hasValidUrl.value && !errored.value);
const firstLetter = computed(() => (props.userCreatedPost || '?').toString().charAt(0));

watch(() => props.linkAvt, () => { errored.value = false; });

const goProfile = (e) => {
    if (!props.userId) return;
    e?.stopPropagation?.();
    router.push(`/user/${props.userId}`);
};
</script>
