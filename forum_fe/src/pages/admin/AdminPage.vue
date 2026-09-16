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
                    <div v-for="s in statCards" :key="s.key" class="border-2 border-[var(--ink)] p-3 bg-[var(--surface)]">
                        <div class="text-2xl font-extrabold tnum">{{ stats[s.key] ?? 0 }}</div>
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
                    <input type="text" class="field flex-1" v-model="grantId" placeholder="userId cần cấp quyền" />
                    <button type="button" class="sign-btn" @click="doGrant">Cấp quyền</button>
                </div>
            </div>

            <div class="card">
                <div class="section-title">Chặn người dùng</div>
                <div class="flex gap-2 flex-wrap">
                    <input type="text" class="field flex-1" v-model="banId" placeholder="userId cần chặn" />
                    <select v-model.number="banDays" class="field flex-none" style="width: auto">
                        <option v-for="d in BAN_DURATIONS" :key="d.days" :value="d.days">{{ d.label }}</option>
                    </select>
                    <button type="button" class="sign-btn sign-btn--danger" @click="() => doBan(banId, banDays)">Chặn</button>
                </div>
                <p class="muted text-xs mt-2">
                    Chặn có thời hạn tự hết khi tới ngày, không cần vào gỡ tay.
                </p>
            </div>

            <div class="card">
                <div class="section-title">Chuyên mục</div>
                <div class="flex gap-2 mb-3">
                    <input type="text" class="field flex-1" v-model="newCategoryName"
                        placeholder="Tên chuyên mục mới" @keyup.enter="doCreateCategory" />
                    <button type="button" class="sign-btn" :disabled="!newCategoryName.trim()" @click="doCreateCategory">Thêm</button>
                </div>
                <div v-if="!categories.length" class="state">Chưa có chuyên mục nào</div>
                <div v-for="c in categories" :key="c.categoryId" class="flex items-center gap-2 py-1.5 border-b last:border-b-0">
                    <input
                        v-if="editingCategoryId === c.categoryId"
                        type="text"
                        class="field flex-1"
                        v-model="editingCategoryName"
                        @keyup.enter="() => doRenameCategory(c)"
                    />
                    <span v-else class="flex-1">{{ c.name }}</span>
                    <template v-if="editingCategoryId === c.categoryId">
                        <button type="button" class="sign-btn sign-btn--quiet" @click="() => doRenameCategory(c)">Lưu</button>
                        <button type="button" class="sign-btn sign-btn--quiet" @click="editingCategoryId = null">Huỷ</button>
                    </template>
                    <template v-else>
                        <button type="button" class="sign-btn sign-btn--quiet" @click="() => { editingCategoryId = c.categoryId; editingCategoryName = c.name; }">Sửa</button>
                        <button type="button" class="sign-btn sign-btn--quiet sign-btn--quiet-danger" @click="() => doDeleteCategory(c)">Xoá</button>
                    </template>
                </div>
            </div>

            <div class="card">
                <div class="section-title">Bài đang chờ duyệt ({{ pendingTotal }})</div>
                <p class="muted text-sm mb-2">Chỉ áp dụng cho bài đăng đầu tiên của mỗi tài khoản.</p>
                <div v-if="!pendingPosts.length" class="state">Không có bài nào đang chờ</div>
                <div v-for="p in pendingPosts" :key="p.postId" class="border-b last:border-b-0 py-3">
                    <router-link :to="`/post/${p.postId}`" class="link font-semibold">{{ p.title || '(chưa có tiêu đề)' }}</router-link>
                    <div class="muted text-sm line-clamp-2 whitespace-pre-wrap mt-0.5">{{ p.body }}</div>
                    <div class="text-xs muted mt-1 flex items-center gap-3">
                        <span>{{ p.authorName }}</span>
                        <span>{{ timeAgo(p.postedAt) }}</span>
                    </div>
                    <div class="flex gap-2 mt-2">
                        <button type="button" class="sign-btn sign-btn--outline" @click="() => doApprovePending(p)">Duyệt</button>
                        <button type="button" class="sign-btn sign-btn--outline sign-btn--outline-danger" @click="() => doRejectPending(p)">Từ chối</button>
                    </div>
                </div>
                <div v-if="pendingPosts.length < pendingTotal" class="pt-2 text-center">
                    <button class="link text-sm" @click="() => loadPending(false)">Xem thêm</button>
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
                            <span class="chip mr-1"
                                :class="{
                                    'chip--ink': r.targetType === 'POST',
                                    'chip--turmeric': r.targetType === 'USER',
                                }">{{ r.targetType }}</span>
                            <span class="link" @click="() => openTarget(r)">{{ r.targetPreview || r.targetId }}</span>
                            <span v-if="r.sameTargetOpenCount > 1" class="text-xs text-[var(--danger)] font-semibold ml-1">
                                {{ r.sameTargetOpenCount }} báo cáo
                            </span>
                            <span v-if="r.targetStatus === 'hidden'" class="chip chip--cinnabar ml-1">đã tự ẩn</span>
                        </div>
                        <div class="text-xs muted mt-0.5 flex items-center gap-3 flex-wrap">
                            <span>{{ r.reporterName }}</span>
                            <span>{{ timeAgo(r.createdAt) }}</span>
                            <template v-if="r.reason"><span>“{{ r.reason }}”</span></template>
                            <span v-if="r.status !== 'OPEN'" class="font-semibold">{{ r.status }}</span>
                        </div>
                    </div>
                    <div v-if="r.status === 'OPEN'" class="flex gap-1 flex-none">
                        <button type="button" class="sign-btn sign-btn--outline" v-if="r.targetStatus === 'hidden'" @click="() => doRestore(r)">Khôi phục</button>
                        <button type="button" class="sign-btn sign-btn--outline sign-btn--outline-danger" v-if="r.targetType !== 'USER'" @click="() => doRemove(r)">Xoá nội dung</button>
                        <button type="button" class="sign-btn sign-btn--outline" @click="() => doHandle(r, 'RESOLVED')">Đã xử lý</button>
                        <button type="button" class="sign-btn sign-btn--quiet" @click="() => doHandle(r, 'DISMISSED')">Bỏ qua</button>
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
                                <th class="pr-3">Đến khi</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="u in blacklist" :key="u.userId" class="border-b">
                                <td class="py-2 pr-3 font-medium">{{ u.fullName }}</td>
                                <td class="pr-3">{{ u.email }}</td>
                                <td class="pr-3">
                                    <!-- activeBan do backend tính, FE không tự so ngày (lệch múi giờ) -->
                                    <span class="chip" :class="u.activeBan ? 'chip--cinnabar' : 'chip--quiet'">
                                        {{ u.activeBan ? 'đang chặn' : (u.reportStatus === 'blocked' ? 'hết hạn' : u.reportStatus) }}
                                    </span>
                                </td>
                                <td class="pr-3">{{ u.reportedQuantity }}</td>
                                <td class="pr-3">{{ u.blockedAt || '-' }}</td>
                                <td class="pr-3">{{ u.bannedUntil || (u.activeBan ? 'vĩnh viễn' : '-') }}</td>
                                <td>
                                    <button type="button" class="sign-btn sign-btn--outline" v-if="u.reportStatus === 'blocked'" @click="() => doUnban(u.userId)">Bỏ chặn</button>
                                    <button type="button" class="sign-btn sign-btn--outline sign-btn--outline-danger" v-else @click="() => doBan(u.userId, banDays)">Chặn</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="card">
                <div class="section-title">Nhật ký quản trị</div>
                <div v-if="!logs.length" class="state">Chưa có hoạt động nào</div>
                <div v-for="l in logs" :key="l.id" class="border-b last:border-b-0 py-2 text-sm flex flex-wrap gap-x-2 items-baseline">
                    <span class="font-semibold">{{ l.adminName }}</span>
                    <span class="px-1.5 py-0.5 rounded bg-[var(--brand-soft)] text-[var(--brand)] text-xs font-semibold">
                        {{ ACTION_LABEL[l.action] || l.action }}
                    </span>
                    <span v-if="l.detail" class="muted">{{ l.detail }}</span>
                    <span v-if="l.targetId" class="muted text-xs">({{ l.targetType }} {{ String(l.targetId).slice(0, 8) }})</span>
                    <span class="muted text-xs ml-auto">{{ timeAgo(l.createdAt) }}</span>
                </div>
                <div v-if="logs.length" class="pt-2 text-center">
                    <button v-if="logsHasMore" class="link text-sm" @click="loadMoreLogs">Xem thêm</button>
                    <span v-else class="muted text-xs">Đã hết</span>
                </div>
            </div>
        </template>
    </div>
</template>

<script setup>
import { onMounted, ref, inject } from 'vue';
import { useRouter } from 'vue-router';
import { getBlacklist, grantAdmin, banUser, unbanUser, getAdminStats, getAdminLogs,
    createCategory, updateCategory, deleteCategory } from '@/apis/admin';
import { getReports, handleReport, removeReportedTarget, restoreReportedTarget } from '@/apis/report';
import { getCategories, getPendingPosts, approvePendingPost, rejectPendingPost } from '@/apis/post';
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
const BAN_DURATIONS = [
    { days: 0, label: 'Vĩnh viễn' },
    { days: 1, label: '1 ngày' },
    { days: 7, label: '7 ngày' },
    { days: 30, label: '30 ngày' },
];
const banDays = ref(0);
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

const LOG_SIZE = 30;
const logs = ref([]);
const logPage = ref(0);
const logsHasMore = ref(true);
const ACTION_LABEL = {
    GRANT_ADMIN: 'Cấp quyền admin',
    BAN_USER: 'Chặn người dùng',
    UNBAN_USER: 'Bỏ chặn người dùng',
    HANDLE_REPORT: 'Xử lý báo cáo',
    REMOVE_TARGET: 'Xoá nội dung bị báo cáo',
    RESTORE_TARGET: 'Khôi phục bài viết',
};

const loadLogs = async (reset = false) => {
    if (reset) { logPage.value = 0; logsHasMore.value = true; }
    try {
        const batch = (await getAdminLogs(logPage.value, LOG_SIZE))?.data?.data || [];
        logs.value = reset ? batch : [...logs.value, ...batch];
        logsHasMore.value = batch.length === LOG_SIZE;
        logPage.value += 1;
    } catch (e) {
        if (reset) logs.value = [];
        logsHasMore.value = false;
    }
};
const loadMoreLogs = () => loadLogs(false);

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
        loadLogs(true);
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thao tác thất bại');
    }
}

const doRestore = async (r) => {
    try {
        await restoreReportedTarget(r.reportId);
        toast?.('Đã khôi phục bài viết');
        await loadReports();
        loadLogs(true);
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Khôi phục thất bại');
    }
}

const doRemove = (r) => {
    const what = r.targetType === 'POST' ? 'bài viết' : 'bình luận';
    openConfirm?.('Xoá nội dung', `Xoá ${what} bị báo cáo? Mọi báo cáo về nội dung này sẽ được đóng.`, async () => {
        try {
            await removeReportedTarget(r.reportId);
            toast?.('Đã xoá nội dung');
            await loadReports();
        loadLogs(true);
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
            loadLogs(true);
        } catch (e) {
            showDialog('Thông báo', e?.description || 'Cấp quyền thất bại');
        }
    });
}

const doBan = (userId, days = 0) => {
    const uid = (userId || '').trim();
    if (!uid) return;
    const han = days > 0 ? `${days} ngày` : 'vĩnh viễn';
    openConfirm?.('Chặn người dùng', `Chặn user ${uid} — ${han}?`, async () => {
        try {
            await banUser(uid, days);
            toast?.('Đã chặn người dùng');
            banId.value = '';
            await loadBlacklist();
            loadLogs(true);
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
            loadLogs(true);
        } catch (e) {
            showDialog('Thông báo', e?.description || 'Bỏ chặn thất bại');
        }
    });
}

/* ---------- chuyên mục ---------- */

const categories = ref([]);
const newCategoryName = ref('');
const editingCategoryId = ref(null);
const editingCategoryName = ref('');

const loadCategories = async () => {
    try { categories.value = (await getCategories())?.data?.data || []; }
    catch (e) { categories.value = []; }
}

const doCreateCategory = async () => {
    const name = newCategoryName.value.trim();
    if (!name) return;
    try {
        await createCategory(name, categories.value.length);
        newCategoryName.value = '';
        await loadCategories();
        toast?.('Đã thêm chuyên mục');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thêm chuyên mục thất bại');
    }
}

const doRenameCategory = async (c) => {
    const name = editingCategoryName.value.trim();
    if (!name) return;
    try {
        await updateCategory(c.categoryId, { name });
        editingCategoryId.value = null;
        await loadCategories();
        toast?.('Đã cập nhật chuyên mục');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Cập nhật thất bại');
    }
}

const doDeleteCategory = (c) => {
    openConfirm?.('Xoá chuyên mục', `Xoá chuyên mục "${c.name}"? Bài đã đăng trong đó chỉ mất nhãn, không bị xoá.`, async () => {
        try {
            await deleteCategory(c.categoryId);
            await loadCategories();
            toast?.('Đã xoá chuyên mục');
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Xoá thất bại');
        }
    }, { danger: true, confirmText: 'Xoá' });
}

/* ---------- bài chờ duyệt ---------- */

const PENDING_SIZE = 20;
const pendingPosts = ref([]);
const pendingTotal = ref(0);
const pendingPage = ref(0);

const loadPending = async (reset = true) => {
    const p = reset ? 0 : pendingPage.value + 1;
    try {
        const d = (await getPendingPosts(p, PENDING_SIZE))?.data?.data || {};
        const items = d.items || [];
        pendingPosts.value = reset ? items : [...pendingPosts.value, ...items];
        pendingTotal.value = d.total ?? pendingPosts.value.length;
        pendingPage.value = p;
    } catch (e) {
        if (reset) { pendingPosts.value = []; pendingTotal.value = 0; }
    }
}

const doApprovePending = async (p) => {
    try {
        await approvePendingPost(p.postId);
        pendingPosts.value = pendingPosts.value.filter((x) => x.postId !== p.postId);
        pendingTotal.value = Math.max(0, pendingTotal.value - 1);
        toast?.('Đã duyệt bài viết');
        loadLogs(true);
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Duyệt thất bại');
    }
}

const doRejectPending = (p) => {
    openConfirm?.('Từ chối bài viết', `Từ chối "${p.title}"? Bài sẽ bị xoá, tác giả nhận được thông báo.`, async () => {
        try {
            await rejectPendingPost(p.postId, 'Nội dung chưa phù hợp với nội quy diễn đàn');
            pendingPosts.value = pendingPosts.value.filter((x) => x.postId !== p.postId);
            pendingTotal.value = Math.max(0, pendingTotal.value - 1);
            toast?.('Đã từ chối bài viết');
            loadLogs(true);
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Từ chối thất bại');
        }
    }, { danger: true, confirmText: 'Từ chối' });
}

onMounted(() => { loadStats(); loadBlacklist(); loadReports(); loadLogs(true); loadCategories(); loadPending(); });
</script>
