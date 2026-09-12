<template>
    <div class="page">
        <div class="card">
            <div class="flex items-center gap-2 mb-1">
                <button class="link text-sm" @click="router.back()">← Quay lại</button>
            </div>
            <div class="section-title">Thiết bị đang đăng nhập</div>
            <p class="muted text-sm mb-3 measure">
                Mỗi dòng là một lần đăng nhập còn hiệu lực. Thấy dòng nào lạ thì đăng xuất nó ngay,
                rồi đổi mật khẩu.
            </p>

            <div v-if="loading" class="state">Đang tải…</div>
            <div v-else-if="!sessions.length" class="state">Không có phiên nào</div>

            <div v-else class="flex flex-col">
                <div class="muted text-sm mb-2">{{ sessions.length }} thiết bị</div>
                <div
                    v-for="s in sessions"
                    :key="s.sessionId"
                    class="flex flex-wrap items-center gap-x-3 gap-y-1 py-3 border-t first:border-t-0"
                >
                    <span class="font-semibold">{{ s.device }}</span>
                    <span v-if="s.current" class="chip chip--turmeric">Thiết bị này</span>
                    <span v-if="s.remember" class="chip chip--quiet">Ghi nhớ</span>
                    <span class="muted text-sm">{{ s.ip || 'không rõ IP' }}</span>
                    <span class="muted text-sm">Đăng nhập {{ timeAgo(s.createdAt) }}</span>
                    <button
                        v-if="!s.current"
                        type="button"
                        class="sign-btn sign-btn--quiet sign-btn--quiet-danger ml-auto"
                        :disabled="busy"
                        @click="askRevoke(s)"
                    >Đăng xuất</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
/**
 * Danh sách thiết bị tách khỏi trang "Cài đặt tài khoản": tài khoản dùng lâu có
 * hàng chục phiên, nhét vào giữa trang hồ sơ thì đẩy mọi thẻ khác xuống quá xa.
 */
import { inject, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getMySessions, revokeSession } from '@/apis/user';
import { timeAgo } from '@/js/helper';

const router = useRouter();
const showDialog = inject('openDialogError', null);
const openConfirm = inject('openConfirm', null);
const toast = inject('toast', null);

const sessions = ref([]);
const loading = ref(true);
const busy = ref(false);

const load = async () => {
    loading.value = true;
    try {
        // ?.data?.data: axios response -> ResponseData -> mảng thật (quy ước chung của repo)
        sessions.value = (await getMySessions())?.data?.data || [];
    } catch (e) {
        sessions.value = [];
        showDialog?.('Thông báo', e?.description || 'Không tải được danh sách thiết bị');
    } finally {
        loading.value = false;
    }
};

const askRevoke = (s) => {
    openConfirm?.(
        'Đăng xuất thiết bị',
        `Đăng xuất "${s.device}"? Thiết bị đó sẽ phải đăng nhập lại.`,
        async () => {
            busy.value = true;
            try {
                await revokeSession(s.sessionId);
                toast?.('Đã đăng xuất thiết bị');
                await load();
            } catch (e) {
                showDialog?.('Thông báo', e?.description || 'Không đăng xuất được thiết bị');
            } finally {
                busy.value = false;
            }
        },
        { confirmText: 'Đăng xuất', danger: true },
    );
};

onMounted(load);
</script>
