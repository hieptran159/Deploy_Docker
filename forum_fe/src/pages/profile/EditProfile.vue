<template>
    <div class="page">
        <!-- Xem trước hồ sơ công khai -->
        <div class="card card--flush overflow-hidden">
            <div class="h-28 sm:h-36 w-full bg-[var(--brand-soft)]">
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
            <div class="px-4 pb-4">
                <DxButton stylingMode="outlined" icon="user" text="Xem trang công khai của tôi" @click="route.push('/user/' + myId)" />
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
                    <DxTextBox v-model="profile.fullName" class="flex-1" placeholder="Tên của bạn" />
                    <span class="flex-none w-32 text-xs muted text-center">Luôn công khai</span>
                </div>
                <div v-for="f in fields" :key="f.key" class="flex flex-col sm:flex-row sm:items-center gap-2">
                    <label class="text-sm font-semibold w-32 flex-none">{{ f.label }}</label>
                    <DxTextArea v-if="f.area" v-model="profile[f.key]" class="flex-1" :height="60" />
                    <DxTextBox v-else v-model="profile[f.key]" class="flex-1" :placeholder="f.ph" />
                    <button
                        type="button"
                        class="flex-none px-3 py-1.5 rounded-full text-xs font-semibold border transition w-32 text-center"
                        :class="visible[f.key]
                            ? 'bg-green-50 border-green-300 text-green-700'
                            : 'bg-gray-100 border-gray-300 muted'"
                        @click="visible[f.key] = !visible[f.key]"
                    >
                        {{ visible[f.key] ? '🌐 Công khai' : '🔒 Riêng tư' }}
                    </button>
                </div>
                <div class="flex justify-end">
                    <DxButton type="default" text="Lưu thông tin" @click="saveProfile" />
                </div>
            </div>
        </div>

        <div class="card">
            <div class="section-title">Xác thực</div>
            <p class="muted text-sm mb-2">Nhập mật khẩu hiện tại — bắt buộc cho các thay đổi bên dưới.</p>
            <DxTextBox v-model="currentPassword" mode="password" placeholder="Mật khẩu hiện tại" />
        </div>

        <div class="card">
            <div class="section-title">Đổi email</div>
            <div class="flex flex-col sm:flex-row gap-2">
                <DxTextBox v-model="newEmail" class="flex-1" placeholder="Email mới" />
                <DxButton type="default" text="Cập nhật email" @click="updateEmail" />
            </div>
        </div>

        <div class="card">
            <div class="section-title">Đổi mật khẩu</div>
            <div class="flex flex-col sm:flex-row gap-2">
                <DxTextBox v-model="newPassword" mode="password" class="flex-1" placeholder="Mật khẩu mới (tối thiểu 5 ký tự)" />
                <DxButton type="default" text="Đổi mật khẩu" @click="updatePassword" />
            </div>
        </div>

        <div class="card">
            <div class="section-title">Xác thực 2 bước</div>
            <p class="muted text-sm mb-2">
                Khi bật, mỗi lần đăng nhập sẽ cần thêm mã 6 ký tự gửi tới email
                <b>{{ rawUser.email || '' }}</b>. Cần nhập mật khẩu hiện tại ở thẻ “Xác thực” phía trên.
            </p>
            <div class="flex items-center gap-3">
                <span class="text-sm font-semibold" :class="twoFAOn ? 'text-[var(--brand)]' : 'muted'">
                    {{ twoFAOn ? 'Đang bật' : 'Đang tắt' }}
                </span>
                <DxButton
                    :type="twoFAOn ? 'normal' : 'default'"
                    :stylingMode="twoFAOn ? 'outlined' : 'contained'"
                    :text="twoFAOn ? 'Tắt' : 'Bật'"
                    @click="toggleTwoFA"
                />
            </div>
        </div>

        <div class="card">
            <div class="section-title">Đổi ảnh đại diện</div>
            <div class="flex flex-col sm:flex-row sm:items-center gap-2">
                <input type="file" accept="image/*" class="flex-1" @change="handleFileChange" />
                <DxButton type="default" text="Cập nhật ảnh" @click="updateAvatar" />
            </div>
        </div>

        <div class="card">
            <div class="section-title">Đổi ảnh bìa</div>
            <div class="flex flex-col sm:flex-row sm:items-center gap-2">
                <input type="file" accept="image/*" class="flex-1" @change="handleCoverChange" />
                <DxButton type="default" text="Cập nhật ảnh bìa" @click="saveCover" />
            </div>
        </div>

        <div class="card border border-[var(--danger)]/40">
            <div class="section-title text-[var(--danger)]">Vùng nguy hiểm</div>
            <p class="muted text-sm mb-2">
                Nhập mật khẩu hiện tại ở thẻ “Xác thực” phía trên rồi chọn:
            </p>
            <div class="flex flex-col gap-2 items-start">
                <div>
                    <DxButton type="normal" stylingMode="outlined" text="Vô hiệu hoá tạm thời" @click="deactivateMe" />
                    <span class="muted text-xs ml-2">Ẩn tài khoản + nội dung khỏi người khác. Đăng nhập lại để kích hoạt.</span>
                </div>
                <div>
                    <DxButton type="danger" text="Xoá tài khoản của tôi" @click="deleteMe" />
                    <span class="muted text-xs ml-2">Xoá <b>vĩnh viễn</b> bài viết, bình luận, tin nhắn, bạn bè.</span>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { DxButton, DxTextBox, DxTextArea } from 'devextreme-vue';
import { useRouter } from 'vue-router';
import { computed, inject, onMounted, ref } from 'vue';
import { editUser, updateProfile, getUserInfo, deleteAccount, deactivateAccount, updateCover } from '@/apis/user';
import { enableTwoFactor, disableTwoFactor } from '@/apis/auth';
import { LOCALKEYS, getItemLocal, setItemLocal, delItemLocal } from '@/storages/localStorage';
import { IMAGE_BASE } from '@/config';
import { applyAvatarUpdate } from '@/storages/appState';

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
const avatarFile = ref(null);

function handleFileChange(event) {
    avatarFile.value = event.target.files[0] || null;
}

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
    if (!newPassword.value || newPassword.value.length < 5) {
        showDialog("Thông báo", "Mật khẩu mới phải có tối thiểu 5 ký tự");
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

const updateAvatar = async () => {
    if (!requirePassword()) return;
    if (!avatarFile.value) { showDialog("Thông báo", "Chọn ảnh đại diện mới"); return; }
    try {
        await editUser({ password: currentPassword.value, avatar: avatarFile.value });
        await refreshLocalUser();
        await loadProfile();
        if (rawUser.value?.avtUrl) applyAvatarUpdate(getItemLocal(LOCALKEYS.USER_ID), rawUser.value.avtUrl);
        showDialog("Thông báo", "Cập nhật ảnh đại diện thành công");
        avatarFile.value = null;
    } catch (e) {
        showDialog("Thông báo", e?.description || "Cập nhật ảnh thất bại");
    }
}

const coverFile = ref(null);
const handleCoverChange = (e) => { coverFile.value = e.target.files[0] || null; };
const saveCover = async () => {
    if (!coverFile.value) { showDialog('Thông báo', 'Chọn ảnh bìa'); return; }
    try {
        await updateCover(coverFile.value);
        await loadProfile();
        toast?.('Đã cập nhật ảnh bìa');
        coverFile.value = null;
    } catch (e) {
        showDialog('Thông báo', e?.description || 'Cập nhật ảnh bìa thất bại');
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
                [LOCALKEYS.ACCESS_TOKEN, LOCALKEYS.USER_ID, LOCALKEYS.USER_NAME, LOCALKEYS.LINK_AVT, LOCALKEYS.IS_ADMIN]
                    .forEach(delItemLocal);
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
                [LOCALKEYS.ACCESS_TOKEN, LOCALKEYS.USER_ID, LOCALKEYS.USER_NAME, LOCALKEYS.LINK_AVT, LOCALKEYS.IS_ADMIN]
                    .forEach(delItemLocal);
                window.location.assign('/signup');
            } catch (e) {
                showDialog('Thông báo', e?.description || 'Xoá tài khoản thất bại');
            }
        },
        { danger: true, confirmText: 'Xoá vĩnh viễn' }
    );
}

onMounted(loadProfile);
</script>
