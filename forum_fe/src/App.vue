<template>
  <TheHeader/>
  <router-view></router-view>
  <MDialog
		v-if="dialogErrorConfig.isShowError"
		:title="dialogErrorConfig.title"
		:content="dialogErrorConfig.content"
		@close="dialogErrorConfig.isShowError = false"
		:buttons="[
			{
				text: 'Đóng',
				type: 'default',
				onClick: () => {
					dialogErrorConfig.isShowError = false;
					dialogErrorConfig.onClick && dialogErrorConfig.onClick();
				},
			},
		]" />
</template>

<script setup>
import TheHeader from '@/components/layout/TheHeader.vue';
import MDialog from './components/Dialog/MDialog.vue';
import { provide, ref } from 'vue';

const dialogErrorConfig = ref({ isShowError: false });

 const openDialogError = (
	title = 'Thông báo',
	content = 'Đã có lỗi xảy ra.',
	onClick,
) => {
	dialogErrorConfig.value = {
		isShowError: true,
		title,
		content,
		onClick,
	};
};

provide('openDialogError', openDialogError);

</script>