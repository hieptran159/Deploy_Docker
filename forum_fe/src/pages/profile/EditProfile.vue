<template>
    <div class="page">
        <!-- Xem trước hồ sơ công khai -->
        <div class="card card--flush overflow-hidden">
            <div class="h-28 sm:h-36 w-full bg-[var(--wash)]">
                <img v-if="previewCover" :src="previewCover" class="w-full h-full object-cover" @error="prevCoverOk = false" />
            </div>
            <div class="p-4 flex items-center gap-4 -mt-10">
                <img
                    v-if="previewAvatar"
                    :src="previewAvatar"
                    class="size-20 rounded-full object-cover ring-4 ring-[var(--surface)] bg-gray-100 flex-none"
                    @error="prevAvatarOk = false"
                />
                <div v-else class="avatar-fallback size-20 text-2xl ring-4 ring-[var(--surface)] flex-none">
                    {{ (profile.fullName || '?')[0] }}
                </div>
                <div class="min-w-0 flex-1 pt-8">
                    <div class="font-bold text-lg truncate">
                        {{ profile.fullName || '—' }}
                        <span v-if="visible.nickname && profile.nickname" class="text-sm muted font-normal">({{ profile.nickname }})</span>
                    </div>
                    <div v-if="visible.slogan && profile.slogan" class="text-sm italic muted truncate">“{{ profile.slogan }}”</div>
                    <div class="text-xs muted mt-0.5">Đây là những gì người khác nhìn thấy.</div>
                </div>
            </div>
            <div class="px-4 pb-4 flex flex-wrap gap-x-5 gap-y-1 text-sm">
                <span v-if="visible.phone && profile.phone"><span class="muted">SĐT:</span> {{ profile.phone }}</span>
                <span v-if="visible.address && profile.address"><span class="muted">Địa chỉ:</span> {{ profile.address }}</span>
                <span v-if="visible.hobbies && profile.hobbies" class="whitespace-pre-wrap"><span class="muted">Sở thích:</span> {{ profile.hobbies }}</span>
            </div>
            <div class="px-4 pb-4 flex flex-wrap items-center gap-2">
                <!-- Nút nằm ngay cạnh ảnh chúng nó sửa, không phải ở tận cuối trang -->
                <button class="sign-btn sign-btn--outline" @click="avatarInput?.click()">
                    <AppIcon name="camera" :size="16" /> Đổi ảnh đại diện
                </button>
                <button class="sign-btn sign-btn--outline" @click="coverInput?.click()">
                    <AppIcon name="image" :size="16" /> Đổi ảnh bìa
                </button>
                <button class="sign-btn sign-btn--quiet" @click="route.push('/user/' + myId)">
                    <AppIcon name="user" :size="16" /> Xem trang công khai của tôi
                </button>
                <!-- input file ẩn: nút bấm xong là tải luôn, không cần bấm "Cập nhật" lần nữa -->
                <input ref="avatarInput" type="file" accept="image/*" class="hidden" @change="onAvatarPicked" />
                <input ref="coverInput" type="file" accept="image/*" class="hidden" @change="onCoverPicked" />
            </div>
        </div>

        <div class="section-title text-base mt-1">Cài đặt tài khoản</div>

        <div class="card">
            <div class="section-title">Thông tin cá nhân</div>
            <p class="muted text-sm mb-3">
                Bật công tắc để <b>công khai</b> trường đó với mọi người; tắt = chỉ mình bạn thấy.
            </p>

            <div class="flex flex-col gap-3">
                <div class="flex flex-col sm:flex-row sm:items-center gap-2">
                    <label class="text-sm font-semibold w-32 flex-none">Tên hiển thị</label>
                    <input type="text" class="field flex-1" v-model="profile.fullName" placeholder="Tên của bạn" />
                    <span class="flex-none w-32 text-xs muted text-center">Luôn công khai</span>
                </div>
                <div v-for="f in fields" :key="f.key" class="flex flex-col sm:flex-row sm:items-center gap-2">
                    <label class="text-sm font-semibold w-32 flex-none">{{ f.label }}</label>
                    <textarea style="height: 60px" class="field flex-1" v-if="f.area" v-model="profile[f.key]"></textarea>
                    <input type="text" class="field flex-1" v-else v-model="profile[f.key]" :placeholder="f.ph" />
                    <button
                        type="button"
                        class="act-pill flex-none w-32 justify-center"
                        :class="{ 'is-live': visible[f.key] }"
                        @click="visible[f.key] = !visible[f.key]"
                    >
                        <AppIcon :name="visible[f.key] ? 'globe' : 'lock'" :size="14" />
                        {{ visible[f.key] ? 'Công khai' : 'Riêng tư' }}
                    </button>
                </div>
                <div class="flex justify-end">
                    <button type="button" class="sign-btn" @click="saveProfile">Lưu thông tin</button>
                </div>
            </div>
        </div>

        <div class="card">
            <div class="section-title">Xác thực</div>
            <p class="muted text-sm mb-2">Nhập mật khẩu hiện tại — bắt buộc cho các thay đổi bên dưới.</p>
            <input type="password" class="field" v-model="currentPassword" placeholder="Mật khẩu hiện tại" />
        </div>

        <div class="card">
            <div class="section-title">Đổi email</div>
            <div class="flex flex-col sm:flex-row gap-2">
                <input type="text" class="field flex-1" v-model="newEmail" placeholder="Email mới" />
                <button type="button" class="sign-btn" @click="updateEmail">Cập nhật email</button>
            </div>
        </div>

        <div class="card">
            <div class="section-title">Đổi mật khẩu</div>
            <div class="flex flex-col sm:flex-row gap-2">
                <input type="password" class="field flex-1" v-model="newPassword" placeholder="Mật khẩu mới (tối thiểu 8 ký tự)" />
                <button type="button" class="sign-btn" @click="updatePassword">Đổi mật khẩu</button>
            </div>
        </div>

        <div class="card">
            <div class="section-title">Xác thực 2 bước</div>
            <p class="muted text-sm mb-2">
                Khi bật, mỗi lần đăng nhập sẽ cần thêm mã 6 ký tự gửi tới email
                <b>{{ rawUser.email || '' }}</b>. Cần nhập mật khẩu hiện tại ở thẻ “Xác thực” phía trên.
            </p>
            <div class="flex items-center gap-3">
                <span class="chip" :class="twoFAOn ? 'chip--turmeric' : 'chip--quiet'">
                    {{ twoFAOn ? 'Đang bật' : 'Đang tắt' }}
                </span>
                <button
                    type="button"
                    class="sign-btn"
                    :class="{ 'sign-btn--outline': twoFAOn }"
                    @click="toggleTwoFA">{{ twoFAOn ? 'Tắt' : 'Bật' }}</button>
            </div>
        </div>

        <div class="card">
            <div class="section-title">Thiết bị đang đăng nhập</div>
            <p class="muted text-sm mb-2">
                Mỗi dòng là một lần đăng nhập còn hiệu lực. Thấy dòng nào lạ thì đăng xuất nó ngay,
                rồi đổi mật khẩu.
            </p>
            <div v-if="sessionsLoading" class="muted text-sm">Đang tải…</div>
            <div v-else-if="!sessions.length" class="muted text-sm">Không có phiên nào</div>
            <div v-else class="flex flex-col">
                <div v-for="s in sessions" :key="s.sessionId"
                     class="flex flex-wrap items-center gap-x-3 gap-y-1 py-2 border-t first:border-t-0">
                    <span class="font-semibold">{{ s.device }}</span>
                    <span v-if="s.current" class="chip chip--turmeric">Thiết bị này</span>
                    <span v-if="s.remember" class="chip chip--quiet">Ghi nhớ</span>
                    <span class="muted text-sm">{{ s.ip || 'không rõ IP' }}</span>
                    <span class="muted text-sm">Đăng nhập {{ timeAgo(s.createdAt) }}</span>
                    <button v-if="!s.current" type="button" class="sign-btn sign-btn--quiet sign-btn--quiet-danger ml-auto"
                            @click="askRevoke(s)">Đăng xuất</button>
                </div>
            </div>
        </div>

        <div class="card card--danger">
            <div class="section-title" style="color: var(--cinnabar-ink)">Vùng nguy hiểm</div>
            <p class="muted text-sm mb-2">
                Nhập mật khẩu hiện tại ở thẻ “Xác thực” phía trên rồi chọn:
            </p>
            <div class="flex flex-col gap-2 items-start">
                <div>
                    <button type="button" class="sign-btn sign-btn--outline" @click="deactivateMe">Vô hiệu hoá tạm thời</button>
                    <span class="muted text-xs ml-2">Ẩn tài khoản + nội dung khỏi người khác. Đăng nhập lại để kích hoạt.</span>
                </div>
                <div>
                    <button type="button" class="sign-btn sign-btn--danger" @click="deleteMe">Xoá tài khoản của tôi</button>
                    <span class="muted text-xs ml-2">Xoá <b>vĩnh viễn</b> bài viết, bình luận, tin nhắn, bạn bè.</span>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import AppIcon from '@/components/AppIcon.vue';
import { useRouter } from 'vue-router';
import { computed, inject, onMounted, ref } from 'vue';
import { editUser, updateProfile, getUserInfo, deleteAccount, deactivateAccount, updateCover,
         updateAvatar, getMySessions, revokeSession } from '@/apis/user';
import { enableTwoFactor, disableTwoFactor } from '@/apis/auth';
import { LOCALKEYS, getItemLocal, setItemLocal, clearAuth } from '@/storages/localStorage';
import { IMAGE_BASE } from '@/config';
import { applyAvatarUpdate } from '@/storages/appState';
import { timeAgo } from '@/js/helper';

const route = useRouter();
const showDialog = inject("openDialogError");
const openConfirm = inject("openConfirm");
const toast = inject("toast");

const fields = [
    { key: 'nickname', label: 'Nickname', ph: 'Tên hiển thị khác' },
    { key: 'phone', label: 'Số điện thoại', ph: '' },
    { key: 'address', label: 'Địa chỉ', ph: '' },
    { key: 'hobbies', label: 'Sở thích', area: true },
    { key: 'slogan', label: 'Câu slogan', ph: '' },
];

const profile = ref({ fullName: '', nickname: '', phone: '', address: '', hobbies: '', slogan: '' });
const visible = ref({ nickname: true, phone: false, address: false, hobbies: true, slogan: true });

const myId = getItemLocal(LOCALKEYS.USER_ID);
const rawUser = ref({});
const prevAvatarOk = ref(true);
const prevCoverOk = ref(true);
const previewAvatar = computed(() => (prevAvatarOk.value && rawUser.value.avtUrl ? IMAGE_BASE + rawUser.value.avtUrl : ''));
const previewCover = computed(() => (prevCoverOk.value && rawUser.value.coverUrl ? IMAGE_BASE + rawUser.value.coverUrl : ''));

const currentPassword = ref("");
const newEmail = ref("");
const newPassword = ref("");
const avatarInput = ref(null);
const coverInput = ref(null);

const requirePassword = () => {
    if (!currentPassword.value) {
        showDialog("Thông báo", "Vui lòng nhập mật khẩu hiện tại");
        return false;
    }
    return true;
}

const loadProfile = async () => {
    try {
        const d = (await getUserInfo(getItemLocal(LOCALKEYS.USER_ID)))?.data?.data || {};
        rawUser.value = d;
        prevAvatarOk.value = true;
        prevCoverOk.value = true;
        profile.value.fullName = d.fullName || '';
        for (const f of fields) profile.value[f.key] = d[f.key] || '';
        visible.value = {
            nickname: d.nicknamePublic !== false,
            phone: d.phonePublic !== false,
            address: d.addressPublic !== false,
            hobbies: d.hobbiesPublic !== false,
            slogan: d.sloganPublic !== false,
        };
    } catch (e) { /* giữ mặc định */ }
}

const saveProfile = async () => {
    if (!profile.value.fullName || !profile.value.fullName.trim()) {
        showDialog("Thông báo", "Tên hiển thị không được để trống");
        return;
    }
    try {
        await updateProfile({
            fullName: profile.value.fullName,
            nickname: profile.value.nickname,
            phone: profile.value.phone,
            address: profile.value.address,
            hobbies: profile.value.hobbies,
            slogan: profile.value.slogan,
            nicknamePublic: visible.value.nickname ? 1 : 0,
            phonePublic: visible.value.phone ? 1 : 0,
            addressPublic: visible.value.address ? 1 : 0,
            hobbiesPublic: visible.value.hobbies ? 1 : 0,
            sloganPublic: visible.value.slogan ? 1 : 0,
        });
        await loadProfile();   // nạp lại từ server để chắc chắn đã lưu
        setItemLocal(LOCALKEYS.USER_NAME, profile.value.fullName);  // cập nhật tên hiển thị ở header
        toast?.('Đã lưu thông tin cá nhân');
    } catch (e) {
        showDialog("Thông báo", e?.description || "Lưu thông tin thất bại");
    }
}

const refreshLocalUser = async () => {
    try {
        const data = await getUserInfo(getItemLocal(LOCALKEYS.USER_ID));
        setItemLocal(LOCALKEYS.USER_NAME, data?.data?.data?.fullName);
        setItemLocal(LOCALKEYS.LINK_AVT, IMAGE_BASE + data?.data?.data?.avtUrl);
    } catch (error) {
        console.log(error);
    }
}

const updateEmail = async () => {
    if (!requirePassword()) return;
    if (!newEmail.value) { showDialog("Thông báo", "Nhập email mới"); return; }
    try {
        await editUser({ password: currentPassword.value, email: newEmail.value });
        await refreshLocalUser();
        showDialog("Thông báo", "Cập nhật email thành công");
        newEmail.value = "";
    } catch (e) {
        showDialog("Thông báo", e?.description || "Cập nhật email thất bại");
    }
}

const updatePassword = async () => {
    if (!requirePassword()) return;
    if (!newPassword.value || newPassword.value.length < 8) {
        showDialog("Thông báo", "Mật khẩu mới phải có tối thiểu 8 ký tự");
        return;
    }
    try {
        await editUser({ password: currentPassword.value, newPassword: newPassword.value });
        showDialog("Thông báo", "Đổi mật khẩu thành công, vui lòng đăng nhập lại", () => route.push('/login'));
        newPassword.value = "";
        currentPassword.value = "";
    } catch (e) {
        showDialog("Thông báo", e?.description || "Đổi mật khẩu thất bại");
    }
}

/* Chọn ảnh xong là tải luôn — không bắt bấm thêm "Cập nhật", và KHÔNG hỏi mật
   khẩu: đổi ảnh không phải thao tác nhạy cảm như đổi email hay mật khẩu.
   /user/avatar và /user/cover đều là endpoint riêng, không đi qua /user/edit. */
const onAvatarPicked = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';           // chọn lại đúng file đó vẫn phải kích hoạt change
    if (!file) return;
    try {
        await updateAvatar(file);
        await refreshLocalUser();
        await loadProfile();
        if (rawUser.value?.avtUrl) applyAvatarUpdate(getItemLocal(LOCALKEYS.USER_ID), rawUser.value.avtUrl);
        toast?.('Đã cập nhật ảnh đại diện');
    } catch (err) {
        showDialog('Thông báo', err?.description || 'Cập nhật ảnh thất bại');
    }
};

const onCoverPicked = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    try {
        await updateCover(file);
        await loadProfile();
        toast?.('Đã cập nhật ảnh bìa');
    } catch (err) {
        showDialog('Thông báo', err?.description || 'Cập nhật ảnh bìa thất bại');
    }
};

const twoFAOn = computed(() => rawUser.value?.twoFactorEnabled === true);
const toggleTwoFA = async () => {
    if (!requirePassword()) return;
    const turningOn = !twoFAOn.value;
    try {
        if (turningOn) await enableTwoFactor(currentPassword.value);
        else await disableTwoFactor(currentPassword.value);
        await loadProfile();
        toast?.(turningOn ? 'Đã bật xác thực 2 bước' : 'Đã tắt xác thực 2 bước');
    } catch (e) {
        showDialog('Thông báo', e?.description || 'Không đổi được cài đặt 2FA');
    }
};

const deactivateMe = () => {
    if (!requirePassword()) return;
    openConfirm?.(
        'Vô hiệu hoá tài khoản',
        'Tài khoản và nội dung của bạn sẽ bị ẩn khỏi người khác. Bạn sẽ bị đăng xuất; đăng nhập lại bất cứ lúc nào để kích hoạt lại. Tiếp tục?',
        async () => {
            try {
                await deactivateAccount(currentPassword.value);
                clearAuth();
                window.location.assign('/login');
            } catch (e) {
                showDialog('Thông báo', e?.description || 'Vô hiệu hoá thất bại');
            }
        },
        { confirmText: 'Vô hiệu hoá' }
    );
}

const deleteMe = () => {
    if (!requirePassword()) return;
    openConfirm?.(
        'Xoá tài khoản',
        'Hành động này KHÔNG THỂ hoàn tác. Toàn bộ bài viết, bình luận, tin nhắn của bạn sẽ bị xoá. Tiếp tục?',
        async () => {
            try {
                await deleteAccount(currentPassword.value);
                clearAuth();
                window.location.assign('/signup');
            } catch (e) {
                showDialog('Thông báo', e?.description || 'Xoá tài khoản thất bại');
            }
        },
        { danger: true, confirmText: 'Xoá vĩnh viễn' }
    );
}

/* ---------- Thiết bị đang đăng nhập ---------- */
const sessions = ref([]);
const sessionsLoading = ref(true);

const loadSessions = async () => {
    sessionsLoading.value = true;
    try {
        // ?.data?.data: axios response -> ResponseData -> mảng thật (quy ước chung của repo)
        sessions.value = (await getMySessions())?.data?.data || [];
    } catch (e) {
        // Không chặn cả trang hồ sơ chỉ vì một card phụ hỏng
        sessions.value = [];
    } finally {
        sessionsLoading.value = false;
    }
};

const askRevoke = (s) => {
    openConfirm(
        'Đăng xuất thiết bị',
        `Đăng xuất "${s.device}"? Thiết bị đó sẽ phải đăng nhập lại.`,
        async () => {
            try {
                await revokeSession(s.sessionId);
                toast?.('Đã đăng xuất thiết bị');
                loadSessions();
            } catch (e) {
                showDialog('Thông báo', e?.description || 'Không đăng xuất được thiết bị');
            }
        },
        { confirmText: 'Đăng xuất', danger: true },
    );
};

onMounted(() => {
    loadProfile();
    loadSessions();
});
</script>
