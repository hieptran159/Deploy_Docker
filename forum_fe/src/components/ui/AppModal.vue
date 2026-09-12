<template>
    <dialog ref="el" class="modal" :style="{ width: px(width) }" @close="emit('update:open', false)" @click="onBackdrop">
        <div class="modal__bar">
            <span class="modal__title">{{ title }}</span>
            <button type="button" class="modal__x" aria-label="Đóng" @click="emit('update:open', false)">
                <AppIcon name="x" :size="18" />
            </button>
        </div>
        <div class="modal__body">
            <slot />
        </div>
    </dialog>
</template>

<script setup>
/**
 * Hộp thoại dựng trên <dialog> thuần, thay DxPopup.
 *
 * Native lo sẵn những thứ DxPopup phải tự dựng: Esc để đóng, bẫy focus, ::backdrop,
 * và xếp lớp ở top-layer (không cần z-index, không bị cha overflow:hidden cắt).
 *
 * Không có prop height: hộp thoại cao theo nội dung, chặn trên 90vh rồi phần thân
 * tự cuộn. DxPopup trước đây cao cố định nên form dài bị cắt (popup "Tạo bài viết"
 * 420px là ví dụ).
 */
import { onMounted, ref, watch } from 'vue';
import AppIcon from '@/components/AppIcon.vue';

const props = defineProps({
    open: { type: Boolean, default: false },
    title: { type: String, default: '' },
    width: { type: [Number, String], default: 600 },
});
const emit = defineEmits(['update:open']);

const el = ref(null);
const px = (v) => (typeof v === 'number' ? `${v}px` : v);
const sync = () => {
    const d = el.value;
    if (!d) return;
    // showModal() trên dialog đang mở sẽ ném lỗi -> kiểm tra d.open trước.
    if (props.open && !d.open) d.showModal();
    else if (!props.open && d.open) d.close();
};
onMounted(sync);
watch(() => props.open, sync);

// Bấm ra ngoài: khi click trúng vùng backdrop, target chính là <dialog> (thân
// thật nằm trong .modal__bar/.modal__body, và .modal có padding: 0).
const onBackdrop = (e) => {
    if (e.target === el.value) emit('update:open', false);
};
</script>
