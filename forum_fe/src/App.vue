<template>
    <TheHeader />
    <router-view></router-view>
    <TheFooter />

    <MDialog
        v-if="dlg.show"
        :title="dlg.title"
        :content="dlg.content"
        :buttons="dlg.buttons"
        @close="dismissDialog"
    />

    <div ref="toastWrap" class="toast-wrap" popover="manual">
        <div
            v-for="t in toasts"
            :key="t.id"
            class="toast"
            :class="[`toast--${t.type}`, { 'toast--clickable': !!t.onClick }]"
            @click="onToastClick(t)"
        >
            <img v-if="t.avatarUrl" :src="t.avatarUrl" class="toast__avatar toast__avatar--img"
                @error="(e) => { e.target.classList.add('hidden') }" />
            <div v-else-if="t.avatar" class="toast__avatar">{{ t.avatar }}</div>
            <div class="toast__body">
                <div class="toast__msg">{{ t.msg }}</div>
                <div v-if="t.sub" class="toast__sub">{{ t.sub }}</div>
            </div>
            <button class="toast__x" @click.stop="dismissToast(t.id)">×</button>
        </div>
    </div>

    <dialog v-if="lightbox" ref="lightboxEl" class="lightbox"
            @click="lightbox = ''" @close="lightbox = ''" @cancel.prevent="lightbox = ''">
        <img :src="lightbox" class="lightbox__img" @click.stop />
        <button class="lightbox__x" @click="lightbox = ''">×</button>
    </dialog>

    <ReportDialog
        v-if="report.show"
        :target-type="report.targetType"
        :target-id="report.targetId"
        :label="report.label"
        @close="report.show = false"
    />
</template>

<script setup>
import TheHeader from '@/components/layout/TheHeader.vue';
import TheFooter from '@/components/layout/TheFooter.vue';
import MDialog from './components/Dialog/MDialog.vue';
import ReportDialog from '@/components/ReportDialog.vue';
import { provide, ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';

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

const dismissToast = (id) => {
    toasts.value = toasts.value.filter((t) => t.id !== id);
};

const onToastClick = (t) => {
    const cb = t.onClick;
    dismissToast(t.id);
    if (cb) cb();
};

// toast(msg)  hoặc  toast(msg, 'error')  hoặc  toast(msg, { type, sub, avatar, onClick, duration })
const toast = (msg, opts = {}) => {
    if (typeof opts === 'string') opts = { type: opts };
    const id = ++toastId;
    toasts.value.push({
        id,
        msg,
        type: opts.type || 'success',
        sub: opts.sub || '',
        avatar: opts.avatar || '',
        avatarUrl: opts.avatarUrl || '',
        onClick: opts.onClick || null,
    });
    setTimeout(() => dismissToast(id), opts.duration || 3200);
};

/* ---------- báo cáo nội dung ---------- */
const report = ref({ show: false, targetType: '', targetId: '', label: '' });
const openReport = (targetType, targetId, label = '') => {
    report.value = { show: true, targetType, targetId, label };
};

/* ---------- lightbox xem ảnh phóng to ---------- */
const lightbox = ref('');
const lightboxEl = ref(null);
const openLightbox = (url) => { if (url) lightbox.value = url; };
// <dialog> chỉ vào top layer khi showModal(); v-if mới render nên phải đợi nextTick
watch(lightbox, async (v) => {
    if (!v) return;
    await nextTick();
    if (!lightboxEl.value?.open) lightboxEl.value?.showModal();
});

/* Toast phải nằm TRÊN hộp thoại đang mở. <dialog> modal ở top layer, cao hơn mọi
   z-index, nên popover là cách duy nhất đưa toast lên cùng tầng. Trình duyệt cũ
   không có popover thì giữ nguyên như trước — vẫn hiện, chỉ là bị hộp thoại che. */
const toastWrap = ref(null);
onMounted(() => {
    try { toastWrap.value?.showPopover?.(); } catch (e) { /* trình duyệt chưa hỗ trợ */ }
});
const onEsc = (e) => { if (e.key === 'Escape') lightbox.value = ''; };
onMounted(() => document.addEventListener('keydown', onEsc));
onBeforeUnmount(() => document.removeEventListener('keydown', onEsc));

provide('openDialogError', openDialogError);
provide('openConfirm', openConfirm);
provide('openReport', openReport);
provide('toast', toast);
provide('openLightbox', openLightbox);
</script>
