<template>
    <dialog ref="el" class="rp-overlay" @click.self="close" @close="close" @cancel.prevent="close">
        <div class="rp-box">
            <div class="rp-title">Báo cáo {{ label || 'nội dung này' }}</div>
            <p class="muted text-sm">Chọn lý do để gửi tới quản trị viên.</p>

            <div class="flex flex-col gap-2 mt-1">
                <label v-for="r in REASONS" :key="r.key" class="flex items-center gap-2 text-sm cursor-pointer">
                    <input type="radio" :value="r.key" v-model="reason" />
                    <span>{{ r.label }}</span>
                </label>
            </div>

            <textarea
                v-model="detail"
                rows="2"
                :placeholder="reason === 'other' ? 'Mô tả cụ thể (bắt buộc)' : 'Mô tả thêm (tuỳ chọn)'"
                class="mt-2 w-full text-sm rounded-lg border px-2 py-1 bg-[var(--surface)]"
            ></textarea>

            <div class="flex justify-end gap-2 mt-3">
                <button class="rp-btn" :disabled="busy" @click="close">Huỷ</button>
                <button class="rp-btn rp-btn--danger" :disabled="busy || !canSubmit" @click="submit">
                    {{ busy ? 'Đang gửi…' : 'Gửi báo cáo' }}
                </button>
            </div>
        </div>
    </dialog>
</template>

<script setup>
import { ref, computed, inject, onBeforeUnmount, onMounted } from 'vue';
import { sendReport } from '@/apis/report';

const props = defineProps({
    targetType: { type: String, required: true }, // USER | POST | COMMENT
    targetId: { type: String, required: true },
    label: { type: String, default: '' },
});
const emit = defineEmits(['close']);

const toast = inject('toast', null);
const showDialog = inject('openDialogError', null);

const REASONS = [
    { key: 'spam', label: 'Spam / quảng cáo' },
    { key: 'harassment', label: 'Quấy rối / xúc phạm' },
    { key: 'hate', label: 'Ngôn từ thù ghét' },
    { key: 'nsfw', label: 'Nội dung phản cảm / 18+' },
    { key: 'misinfo', label: 'Thông tin sai lệch' },
    { key: 'other', label: 'Khác' },
];

const reason = ref('spam');
const detail = ref('');
const busy = ref(false);

const canSubmit = computed(() => reason.value !== 'other' || !!detail.value.trim());

const submit = async () => {
    if (!canSubmit.value || busy.value) return;
    busy.value = true;
    const label = REASONS.find((r) => r.key === reason.value)?.label || reason.value;
    const full = detail.value.trim() ? `${label}: ${detail.value.trim()}` : label;
    try {
        await sendReport(props.targetType, props.targetId, full);
        toast?.('Đã gửi báo cáo. Cảm ơn bạn!');
        emit('close');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Báo cáo thất bại');
    } finally {
        busy.value = false;
    }
};

const close = () => { if (!busy.value) emit('close'); };

// Phải là <dialog> modal chứ không phải div z-index: AppModal cũng là <dialog>,
// mà top layer nằm trên mọi z-index nên hộp báo cáo sẽ bị popup che mất.
const el = ref(null);
onMounted(() => el.value?.showModal());
onBeforeUnmount(() => { if (el.value?.open) el.value.close(); });
</script>

<style scoped>
.rp-overlay {
    position: fixed;
    inset: 0;
    width: 100%;
    max-width: 100%;
    height: 100%;
    max-height: 100%;
    margin: 0;
    border: 0;
    background: transparent;   /* nền mờ do ::backdrop lo, thân dialog phải trong suốt */
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px;
}
.rp-overlay::backdrop {
    background: rgba(16, 24, 40, .45);
}
.rp-box {
    background: var(--surface);
    border: var(--line-w) solid var(--ink);
    border-radius: var(--radius);
    box-shadow: var(--lift);
    padding: 20px;
    width: 100%;
    max-width: 420px;
}
.rp-title { font-weight: 700; font-size: 16px; margin-bottom: 4px; }
.rp-btn {
    padding: 6px 14px;
    border-radius: 8px;
    border: var(--line-w) solid var(--ink);
    font-size: 14px;
    font-weight: 600;
    background: var(--surface);
}
.rp-btn:disabled { opacity: .5; }
.rp-btn--danger { background: var(--danger); border-color: var(--danger); color: #fff; }
</style>
