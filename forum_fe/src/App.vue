<template>
    <TheHeader />
    <router-view></router-view>

    <MDialog
        v-if="dlg.show"
        :title="dlg.title"
        :content="dlg.content"
        :buttons="dlg.buttons"
        @close="dismissDialog"
    />

    <div class="toast-wrap">
        <div v-for="t in toasts" :key="t.id" class="toast" :class="`toast--${t.type}`">
            {{ t.msg }}
        </div>
    </div>
</template>

<script setup>
import TheHeader from '@/components/layout/TheHeader.vue';
import MDialog from './components/Dialog/MDialog.vue';
import { provide, ref } from 'vue';

/* ---------- modal (lỗi + xác nhận) ---------- */
const dlg = ref({ show: false, title: '', content: '', buttons: [], onDismiss: null });

const dismissDialog = () => {
    const cb = dlg.value.onDismiss;
    dlg.value.show = false;
    if (cb) cb();
};

const openDialogError = (title = 'Thông báo', content = 'Đã có lỗi xảy ra.', onClick) => {
    dlg.value = {
        show: true,
        title,
        content,
        onDismiss: onClick,
        buttons: [
            { text: 'Đóng', type: 'default', onClick: () => { dlg.value.show = false; if (onClick) onClick(); } },
        ],
    };
};

const openConfirm = (title, content, onConfirm, opts = {}) => {
    dlg.value = {
        show: true,
        title: title || 'Xác nhận',
        content: content || 'Bạn có chắc chắn?',
        onDismiss: null,
        buttons: [
            { text: opts.cancelText || 'Huỷ', type: 'normal', onClick: () => { dlg.value.show = false; } },
            {
                text: opts.confirmText || 'Đồng ý',
                type: opts.danger ? 'danger' : 'default',
                onClick: () => { dlg.value.show = false; if (onConfirm) onConfirm(); },
            },
        ],
    };
};

/* ---------- toast ---------- */
const toasts = ref([]);
let toastId = 0;
const toast = (msg, type = 'success') => {
    const id = ++toastId;
    toasts.value.push({ id, msg, type });
    setTimeout(() => {
        toasts.value = toasts.value.filter((t) => t.id !== id);
    }, 3000);
};

provide('openDialogError', openDialogError);
provide('openConfirm', openConfirm);
provide('toast', toast);
</script>
