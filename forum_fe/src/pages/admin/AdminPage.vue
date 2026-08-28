<template>
    <div class="page">
        <div v-if="!authorized" class="card state">
            <div class="section-title">Trang quản trị</div>
            <div>{{ deniedMsg }}</div>
        </div>

        <template v-else>
            <div class="card">
                <div class="section-title">Thống kê</div>
                <div v-if="stats" class="grid grid-cols-2 sm:grid-cols-4 gap-3">
                    <div v-for="s in statCards" :key="s.key" class="rounded-xl border p-3 bg-[var(--surface)]">
                        <div class="text-2xl font-bold">{{ stats[s.key] ?? 0 }}</div>
                        <div class="text-xs muted">{{ s.label }}</div>
                    </div>
                </div>
                <div v-else class="state text-sm">Đang tải…</div>

                <div v-if="stats?.postsPerDay?.length" class="mt-4">
                    <div class="text-xs muted mb-1">Bài đăng 14 ngày gần nhất</div>
                    <div class="flex items-end gap-1 h-24">
                        <div
                            v-for="p in stats.postsPerDay"
                            :key="p.date"
                            class="flex-1 bg-[var(--brand)] rounded-t min-h-[2px]"
                            :style="{ height: barH(p.count) }"
                            :title="`${p.date}: ${p.count} bài`"
                        ></div>
                    </div>
                    <div class="flex justify-between text-[10px] muted mt-1">
                        <span>{{ stats.postsPerDay[0].date.slice(5) }}</span>
                        <span>{{ stats.postsPerDay[stats.postsPerDay.length - 1].date.slice(5) }}</span>
                    </div>
                </div>
            </div>

            <div class="card">
                <div class="section-title">Cấp quyền admin</div>
                <div class="flex gap-2">
                    <DxTextBox v-model="grantId" placeholder="userId cần cấp quyền" class="flex-1"/>
                    <DxButton text="Cấp quyền" type="default" @click="doGrant"/>
                </div>
            </div>

            <div class="card">
                <div class="section-title">Chặn người dùng</div>
                <div class="flex gap-2">
                    <DxTextBox v-model="banId" placeholder="userId cần chặn" class="flex-1"/>
                    <DxButton text="Chặn" type="danger" @click="() => doBan(banId)"/>
                </div>
            </div>

            <div class="card">
                <div class="flex items-center gap-3 mb-2">
                    <span class="section-title mb-0 flex-1">Hàng đợi báo cáo</span>
                    <select v-model="reportFilter" @change="loadReports"
                        class="text-sm border rounded-lg px-2 py-1 bg-[var(--surface)]">
                        <option value="OPEN">Chưa xử lý</option>
                        <option value="RESOLVED">Đã xử lý</option>
                        <option value="DISMISSED">Đã bỏ qua</option>
                        <option value="ALL">Tất cả</option>
                    </select>
                </div>
                <div v-if="!reports.length" class="state">Không có báo cáo</div>
                <div v-for="r in reports" :key="r.reportId" class="border-b last:border-b-0 py-3 flex flex-wrap gap-x-4 gap-y-1 items-start">
                    <div class="min-w-0 flex-1">
                        <div class="text-sm">
                            <span class="px-2 py-0.5 rounded-full text-xs font-bold mr-1"
                                :class="{
                                    'bg-blue-50 text-blue-600': r.targetType === 'POST',
                                    'bg-purple-50 text-purple-600': r.targetType === 'COMMENT',
                                    'bg-amber-50 text-amber-600': r.targetType === 'USER',
                                }">{{ r.targetType }}</span>
                            <span class="link" @click="() => openTarget(r)">{{ r.targetPreview || r.targetId }}</span>
                            <span v-if="r.sameTargetOpenCount > 1" class="text-xs text-[var(--danger)] font-semibold">
                                · {{ r.sameTargetOpenCount }} báo cáo
                            </span>
                        </div>
                        <div class="text-xs muted mt-0.5">
                            {{ r.reporterName }} · {{ timeAgo(r.createdAt) }}
                            <template v-if="r.reason"> · “{{ r.reason }}”</template>
                            <span v-if="r.status !== 'OPEN'" class="font-semibold"> · {{ r.status }}</span>
                        </div>
                    </div>
                    <div v-if="r.status === 'OPEN'" class="flex gap-1 flex-none">
                        <DxButton v-if="r.targetType !== 'USER'" text="Xoá nội dung" type="danger" stylingMode="outlined"
                            @click="() => doRemove(r)" />
                        <DxButton text="Đã xử lý" stylingMode="outlined" @click="() => doHandle(r, 'RESOLVED')" />
                        <DxButton text="Bỏ qua" stylingMode="text" @click="() => doHandle(r, 'DISMISSED')" />
                    </div>
                </div>
            </div>

            <div class="card">
                <div class="section-title">Danh sách bị báo cáo / chặn</div>
                <div v-if="blacklist.length === 0" class="state">Không có ai trong danh sách</div>
                <div v-else class="overflow-x-auto">
                    <table class="w-full text-left text-sm">
                        <thead>
                            <tr class="border-b muted">
                                <th class="py-2 pr-3">Họ tên</th>
                                <th class="pr-3">Email</th>
                                <th class="pr-3">Trạng thái</th>
                                <th class="pr-3">Báo cáo</th>
                                <th class="pr-3">Bị chặn lúc</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="u in blacklist" :key="u.userId" class="border-b">
                                <td class="py-2 pr-3 font-medium">{{ u.fullName }}</td>
                                <td class="pr-3">{{ u.email }}</td>
                                <td class="pr-3">
                                    <span class="px-2 py-0.5 rounded-full text-xs font-semibold"
                                        :class="u.reportStatus === 'blocked'
                                            ? 'bg-rose-50 text-rose-600'
                                            : 'bg-amber-50 text-amber-600'">
                                        {{ u.reportStatus }}
                                    </span>
                                </td>
                                <td class="pr-3">{{ u.reportedQuantity }}</td>
                                <td class="pr-3">{{ u.blockedAt || '-' }}</td>
                                <td>
                                    <DxButton v-if="u.reportStatus === 'blocked'"
                                        text="Bỏ chặn" stylingMode="outlined" @click="() => doUnban(u.userId)"/>
                                    <DxButton v-else
                                        text="Chặn" type="danger" stylingMode="outlined" @click="() => doBan(u.userId)"/>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </template>
    </div>
</template>

<script setup>
import { DxTextBox, DxButton } from 'devextreme-vue';
import { onMounted, ref, inject } from 'vue';
import { useRouter } from 'vue-router';
import { getBlacklist, grantAdmin, banUser, unbanUser, getAdminStats } from '@/apis/admin';
import { getReports, handleReport, removeReportedTarget } from '@/apis/report';
import { timeAgo } from '@/js/helper';

const router = useRouter();
const showDialog = inject('openDialogError');
const openConfirm = inject('openConfirm');
const toast = inject('toast');

const authorized = ref(true);
const deniedMsg = ref('Đang kiểm tra quyền...');
const blacklist = ref([]);
const grantId = ref('');
const banId = ref('');
const reports = ref([]);
const reportFilter = ref('OPEN');

const stats = ref(null);
const statCards = [
    { key: 'users', label: 'Người dùng' },
    { key: 'admins', label: 'Admin' },
    { key: 'posts', label: 'Bài đã đăng' },
    { key: 'drafts', label: 'Bản nháp' },
    { key: 'comments', label: 'Bình luận' },
    { key: 'conversations', label: 'Cuộc trò chuyện' },
    { key: 'messages', label: 'Tin nhắn' },
    { key: 'bookmarks', label: 'Lượt lưu' },
    { key: 'blocks', label: 'Lượt chặn (user)' },
    { key: 'reportsOpen', label: 'Báo cáo chờ xử lý' },
    { key: 'reportsResolved', label: 'Báo cáo đã xử lý' },
    { key: 'bannedUsers', label: 'User bị khoá' },
];

const loadStats = async () => {
    try {
        stats.value = (await getAdminStats())?.data?.data || null;
    } catch (e) { stats.value = null; }
};

const barH = (c) => {
    const max = Math.max(1, ...(stats.value?.postsPerDay || []).map((p) => p.count));
    return Math.round((c / max) * 100) + '%';
};

const loadReports = async () => {
    try {
        reports.value = (await getReports(reportFilter.value))?.data?.data || [];
    } catch (e) { reports.value = []; }
}

const openTarget = (r) => {
    if (r.targetType === 'USER') router.push('/user/' + r.targetId);
    else if (r.targetType === 'POST') router.push('/post/' + r.targetId);
    else if (r.targetType === 'COMMENT') showDialog?.('Bình luận bị báo cáo', r.targetPreview || r.targetId);
}

const doHandle = async (r, status) => {
    try {
        await handleReport(r.reportId, status);
        toast?.(status === 'RESOLVED' ? 'Đã đánh dấu xử lý' : 'Đã bỏ qua');
        await loadReports();
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thao tác thất bại');
    }
}

const doRemove = (r) => {
    const what = r.targetType === 'POST' ? 'bài viết' : 'bình luận';
    openConfirm?.('Xoá nội dung', `Xoá ${what} bị báo cáo? Mọi báo cáo về nội dung này sẽ được đóng.`, async () => {
        try {
            await removeReportedTarget(r.reportId);
            toast?.('Đã xoá nội dung');
            await loadReports();
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Xoá thất bại');
        }
    }, { danger: true, confirmText: 'Xoá' });
}

const loadBlacklist = async () => {
    try {
        const res = await getBlacklist();
        blacklist.value = res?.data?.data || [];
        authorized.value = true;
    } catch (e) {
        authorized.value = false;
        deniedMsg.value = e?.description || 'Bạn không có quyền truy cập trang này';
    }
}

const doGrant = () => {
    const uid = grantId.value.trim();
    if (!uid) return;
    openConfirm?.('Cấp quyền admin', `Cấp quyền admin cho user ${uid}?`, async () => {
        try {
            await grantAdmin(uid);
            toast?.('Đã cấp quyền admin');
            grantId.value = '';
        } catch (e) {
            showDialog('Thông báo', e?.description || 'Cấp quyền thất bại');
        }
    });
}

const doBan = (userId) => {
    const uid = (userId || '').trim();
    if (!uid) return;
    openConfirm?.('Chặn người dùng', `Chặn user ${uid}?`, async () => {
        try {
            await banUser(uid);
            toast?.('Đã chặn người dùng');
            banId.value = '';
            await loadBlacklist();
        } catch (e) {
            showDialog('Thông báo', e?.description || 'Chặn thất bại');
        }
    }, { danger: true, confirmText: 'Chặn' });
}

const doUnban = (userId) => {
    openConfirm?.('Bỏ chặn', `Bỏ chặn user ${userId}?`, async () => {
        try {
            await unbanUser(userId);
            toast?.('Đã bỏ chặn');
            await loadBlacklist();
        } catch (e) {
            showDialog('Thông báo', e?.description || 'Bỏ chặn thất bại');
        }
    });
}

onMounted(() => { loadStats(); loadBlacklist(); loadReports(); });
</script>
