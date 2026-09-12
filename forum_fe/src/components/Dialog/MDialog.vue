<template>
	<dialog ref="el" class="m-dialog" @close="emit('close')" @cancel.prevent="emit('close')">
		<div class="m-dialog__header">
			<div class="m-dialog__title">{{ title }}</div>
			<div class="m-dialog__closeBtn">
				<button class="icon-btn" title="Đóng" @click="emit('close')">
					<AppIcon name="x" :size="18" />
				</button></div>
		</div>
		<div class="m-dialog__body">
			<span style="white-space: pre-line">{{ content }}</span>
		</div>
		<div class="m-dialog__footer">
			<button
				v-for="btn of buttons"
				:key="btn.text"
				ref="buttonRefs"
				type="button"
				class="sign-btn"
				:class="btnClass(btn.type)"
				@click="btn.onClick">{{ btn.text }}</button>
		</div>
	</dialog>
</template>

<script setup>
/**
 * Hộp thoại thông báo / xác nhận.
 *
 * Phải là <dialog> mở bằng showModal(), KHÔNG phải div với z-index: AppModal cũng
 * là <dialog> modal, mà phần tử ở "top layer" nằm trên MỌI z-index. Khi còn là div
 * z-index:10000, thông báo lỗi bật lên trong lúc popup "Tạo bài viết" đang mở sẽ
 * nằm KHUẤT SAU popup đó — người dùng không thấy gì cả.
 *
 * Hai phần tử cùng ở top layer thì cái mở SAU nằm trên, nên hộp thoại này luôn
 * đứng trước popup đã mở từ trước.
 */
import AppIcon from '@/components/AppIcon.vue';
import { ref, onMounted, onBeforeUnmount } from 'vue';
const props = defineProps({
	title: {
		type: String,
		default: 'Thông báo',
	},
	content: {
		type: String,
		default: '',
	},
	buttons: {
		type: Array,
		default: () => [
			{
				text: 'Hủy',
				onClick: () => {},
				type: 'normal',
			},
		],
	},
});
const emit = defineEmits(['close']);
const el = ref(null);
const buttonRefs = ref();

/** type của nút (default/normal/danger) -> biến thể .sign-btn */
const btnClass = (t) =>
	t === 'danger' ? 'sign-btn--danger' : t === 'normal' ? 'sign-btn--outline' : '';

onMounted(() => {
	el.value?.showModal();
	// Focus nút cuối (thường là nút xác nhận) để bấm Enter là xong
	buttonRefs.value?.[buttonRefs.value.length - 1]?.focus();
});

// Component bị v-if gỡ đi -> đóng dialog để trình duyệt nhả top layer ra
onBeforeUnmount(() => {
	if (el.value?.open) el.value.close();
});
</script>
<style scoped>
@import url(./dialog.css);
</style>
