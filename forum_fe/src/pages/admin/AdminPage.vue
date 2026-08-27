<template>
    <div class="content flex justify-center flex-wrap">
        <div class="w-[80%]">
            <div v-if="!authorized" class="border rounded p-8 text-center text-gray-500">
                <div class="text-2xl mb-2">Trang quản trị</div>
                <div>{{ deniedMsg }}</div>
            </div>

            <template v-else>
                <div class="border rounded p-5 mb-5">
                    <div class="text-[22px] text-orange-400 font-bold mb-3">Cấp quyền admin</div>
                    <div class="flex">
                        <DxTextBox v-model="grantId" placeholder="userId cần cấp quyền" class="flex-1"/>
                        <DxButton class="ml-2" text="Cấp quyền" type="default" @click="doGrant"/>
                    </div>
                </div>

                <div class="border rounded p-5 mb-5">
                    <div class="text-[22px] text-orange-400 font-bold mb-3">Chặn người dùng</div>
                    <div class="flex">
                        <DxTextBox v-model="banId" placeholder="userId cần chặn" class="flex-1"/>
                        <DxButton class="ml-2" text="Chặn" type="danger" @click="() => doBan(banId)"/>
                    </div>
                </div>

                <div class="border rounded p-5">
                    <div class="text-[22px] text-orange-400 font-bold mb-3">Danh sách bị báo cáo / chặn</div>
                    <div v-if="blacklist.length === 0" class="text-gray-500">Không có ai trong danh sách</div>
                    <table v-else class="w-full text-left">
                        <thead>
                            <tr class="border-b text-gray-600">
                                <th class="py-2">Họ tên</th>
                                <th>Email</th>
                                <th>Trạng thái</th>
                                <th>Số báo cáo</th>
                                <th>Bị chặn lúc</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="u in blacklist" :key="u.userId" class="border-b">
                                <td class="py-2">{{ u.fullName }}</td>
                                <td>{{ u.email }}</td>
                                <td>{{ u.reportStatus }}</td>
                                <td>{{ u.reportedQuantity }}</td>
                                <td>{{ u.blockedAt || '-' }}</td>
                                <td>
                                    <DxButton v-if="u.reportStatus === 'blocked'"
                                        text="Bỏ chặn" @click="() => doUnban(u.userId)"/>
                                    <DxButton v-else
                                        text="Chặn" type="danger" @click="() => doBan(u.userId)"/>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </template>
        </div>
    </div>
</template>

<script setup>
import { DxTextBox, DxButton } from 'devextreme-vue';
import { onMounted, ref, inject } from 'vue';
import { getBlacklist, grantAdmin, banUser, unbanUser } from '@/apis/admin';

const showDialog = inject('openDialogError');

const authorized = ref(true);
const deniedMsg = ref('Đang kiểm tra quyền...');
const blacklist = ref([]);
const grantId = ref('');
const banId = ref('');

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

const doGrant = async () => {
    if (!grantId.value.trim()) return;
    try {
        await grantAdmin(grantId.value.trim());
        showDialog('Thông báo', 'Đã cấp quyền admin');
        grantId.value = '';
    } catch (e) {
        showDialog('Thông báo', e?.description || 'Cấp quyền thất bại');
    }
}

const doBan = async (userId) => {
    if (!userId || !userId.trim()) return;
    try {
        await banUser(userId.trim());
        showDialog('Thông báo', 'Đã chặn người dùng');
        banId.value = '';
        await loadBlacklist();
    } catch (e) {
        showDialog('Thông báo', e?.description || 'Chặn thất bại');
    }
}

const doUnban = async (userId) => {
    try {
        await unbanUser(userId);
        showDialog('Thông báo', 'Đã bỏ chặn');
        await loadBlacklist();
    } catch (e) {
        showDialog('Thông báo', e?.description || 'Bỏ chặn thất bại');
    }
}

onMounted(loadBlacklist);
</script>
