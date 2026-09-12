<template>
	<div
		class="m-dialog-overlay"
		@keydown.prevent="handleKeyDown"
		tabindex="-1">
		<div class="m-dialog">
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
		</div>
	</div>
</template>

<script setup>
import AppIcon from '@/components/AppIcon.vue';
import { ref, onMounted } from 'vue';
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
const buttonRefs = ref();

/** type của nút (default/normal/danger) -> biến thể .sign-btn */
const btnClass = (t) =>
	t === 'danger' ? 'sign-btn--danger' : t === 'normal' ? 'sign-btn--outline' : '';
/**
 * Handle keydown
 * @author NTVu 3/11/2023
 * @param {KeyboardEvent} e
 */
const handleKeyDown = (e) => {
	if (e.key === 'Escape') {
		emit('close');
	}
};

/**
 * Focus vào button cuối của dialog
 * @author NTVu 3/11/2023
 */
const focusBtnEnd = () => {
	// <button> thuần -> ref chính là phần tử DOM (DxButton trước đây phải qua .instance)
	buttonRefs.value?.[buttonRefs.value.length - 1]?.focus();
};
onMounted(() => {
	focusBtnEnd();
});
</script>
<style scoped>
@import url(./dialog.css);
</style>