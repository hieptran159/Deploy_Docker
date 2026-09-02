<template>
    <div class="page">
        <div class="card card--flush overflow-hidden">
            <div class="h-40 sm:h-52 w-full bg-[var(--brand-soft)]">
                <img v-if="coverUrl" :src="coverUrl" class="w-full h-full object-cover"
                    @error="(e) => e.target.style.display = 'none'" />
            </div>
            <div class="p-5 flex items-center gap-5 -mt-12">
                <img v-if="avatarOk && avatarUrl" :key="avatarUrl" :src="avatarUrl" @error="avatarOk = false" @load="avatarOk = true"
                    class="size-24 rounded-full object-cover flex-none bg-white ring-4 ring-[var(--surface)]" />
                <div v-else class="avatar-fallback size-24 text-3xl ring-4 ring-[var(--surface)]">
                    {{ (user?.fullName || '?')[0] }}
                </div>

                <div class="min-w-0 flex-1 pt-10">
                <div class="text-2xl font-bold truncate">
                    {{ user?.fullName || '…' }}
                    <span v-if="user?.nickname" class="text-base muted font-normal">({{ user.nickname }})</span>
                </div>
                <div v-if="user?.slogan" class="text-sm italic muted">“{{ user.slogan }}”</div>
                <div class="flex flex-wrap gap-x-5 gap-y-1 mt-2 text-sm">
                    <span class="link" @click="goFriends"><b>{{ friendCount }}</b> bạn bè</span>
                    <span><b>{{ postsTotal || user?.posts || 0 }}</b> bài viết</span>
                </div>
            </div>

            <div v-if="isMe" class="flex flex-col gap-2 flex-none">
                <DxButton type="default" icon="edit" text="Chỉnh sửa hồ sơ" @click="route.push('/profile/edit')" />
            </div>
            <div v-else class="flex flex-col gap-2 flex-none">
                <template v-if="fStatus === 'blocked_out'">
                    <span class="text-xs muted">Bạn đã chặn người này</span>
                    <DxButton type="normal" stylingMode="outlined" text="Bỏ chặn" @click="doUnblock" />
                </template>
                <template v-else-if="fStatus === 'blocked_in'">
                    <span class="text-xs muted">Không khả dụng</span>
                </template>
                <template v-else>
                    <DxButton type="success" icon="message" text="Nhắn tin" @click="messageUser" />

                    <DxButton v-if="fStatus === 'none'" type="default" icon="user" text="Kết bạn" @click="doSend" />
                    <DxButton v-else-if="fStatus === 'pending_out'" type="normal" stylingMode="outlined" text="Huỷ lời mời" @click="doCancel" />
                    <template v-else-if="fStatus === 'pending_in'">
                        <DxButton type="default" text="Chấp nhận kết bạn" @click="doAccept" />
                        <DxButton type="normal" stylingMode="outlined" text="Từ chối" @click="doDecline" />
                    </template>
                    <DxButton v-else-if="fStatus === 'friends'" type="normal" stylingMode="outlined" icon="check" text="Bạn bè" @click="doUnfriend" />

                    <div class="flex gap-1">
                        <DxButton type="danger" stylingMode="text" icon="warning" text="Báo cáo" @click="report" />
                        <DxButton type="danger" stylingMode="text" icon="clearsquare" text="Chặn" @click="doBlock" />
                    </div>
                </template>
            </div>
            </div>
        </div>

        <div v-if="hasInfo" class="card">
            <div class="section-title">Thông tin</div>
            <div class="flex flex-col gap-1.5 text-sm">
                <div v-if="user?.phone" class="flex gap-2"><span class="w-28 flex-none muted">Số điện thoại</span><span>{{ user.phone }}</span></div>
                <div v-if="user?.address" class="flex gap-2"><span class="w-28 flex-none muted">Địa chỉ</span><span>{{ user.address }}</span></div>
                <div v-if="user?.hobbies" class="flex gap-2"><span class="w-28 flex-none muted">Sở thích</span><span class="whitespace-pre-wrap">{{ user.hobbies }}</span></div>
            </div>
        </div>

        <div class="card">
            <div class="section-title">Bài viết {{ postsTotal ? `(${postsTotal})` : '' }}</div>
            <div v-if="loading && !posts.length" class="state">Đang tải…</div>
            <div v-else-if="!posts.length" class="state">Chưa có bài viết</div>
            <div v-for="post in posts" :key="post.postId" class="border-b last:border-b-0">
                <Post :post="post" @refresh="() => loadPosts(true)" />
            </div>
            <div v-if="postsPage < postsTotalPages" class="pt-3 text-center">
                <button class="link text-sm" :disabled="loading" @click="loadPosts(false)">
                    {{ loading ? 'Đang tải…' : 'Xem thêm' }}
                </button>
            </div>
        </div>

        <div v-if="reposts.length" class="card">
            <div class="section-title">Đã chia sẻ {{ repostsTotal ? `(${repostsTotal})` : '' }}</div>
            <div v-for="post in reposts" :key="'rp-' + post.postId" class="border-b last:border-b-0">
                <Post :post="post" @refresh="() => loadReposts(true)" />
            </div>
            <div v-if="repostsPage < repostsTotalPages" class="pt-3 text-center">
                <button class="link text-sm" :disabled="repostsLoading" @click="loadReposts(false)">
                    {{ repostsLoading ? 'Đang tải…' : 'Xem thêm' }}
                </button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { DxButton } from 'devextreme-vue';
import { onMounted, ref, computed, watch, inject } from 'vue';
import { useRouter } from 'vue-router';
import { getUserInfo } from '@/apis/user';
import { getPostsByUser, getRepostsOf } from '@/apis/post';
import Post from '@/components/Post/Post.vue';
import {
    friendStatus, getFriends, sendFriendRequest, cancelFriendRequest,
    acceptFriendRequest, declineFriendRequest, unfriend,
    blockUser, unblockUser,
} from '@/apis/friend';
import { openDirectConversation } from '@/apis/chat';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { bumpNotifRefresh, avatarUpdates } from '@/storages/appState';
import { IMAGE_BASE } from '@/config';

const showDialog = inject('openDialogError');
const openConfirm = inject('openConfirm');
const openReport = inject('openReport');
const toast = inject('toast');

const route = useRouter();
const myId = getItemLocal(LOCALKEYS.USER_ID);
const userId = computed(() => route.currentRoute.value.params.id);

const POSTS_SIZE = 10;
const user = ref(null);
const posts = ref([]);
const reposts = ref([]);
const repostsPage = ref(0);
const repostsTotal = ref(0);
const repostsTotalPages = ref(1);
const repostsLoading = ref(false);
const postsPage = ref(0);
const postsTotal = ref(0);
const postsTotalPages = ref(1);
const avatarOk = ref(true);
const loading = ref(false);
const fStatus = ref('none');       // none|pending_out|pending_in|friends|self
const friendCount = ref(0);

const isMe = computed(() => userId.value === myId);
const avatarUrl = computed(() => {
    const upd = avatarUpdates.value?.[userId.value];
    if (upd !== undefined && upd !== null && upd !== '') return IMAGE_BASE + upd;
    return user.value?.avtUrl ? IMAGE_BASE + user.value.avtUrl : "";
});
const coverUrl = computed(() => (user.value?.coverUrl ? IMAGE_BASE + user.value.coverUrl : ""));
const hasInfo = computed(() => !!(user.value?.phone || user.value?.address || user.value?.hobbies));

const refreshStatus = async () => {
    // 2 call độc lập -> chạy song song
    const tasks = [
        getFriends(userId.value)
            .then((r) => { friendCount.value = r?.data?.data?.quantity || 0; })
            .catch(() => { friendCount.value = 0; }),
    ];
    if (isMe.value) {
        fStatus.value = 'self';
    } else {
        tasks.push(
            friendStatus(userId.value)
                .then((r) => { fStatus.value = r?.data?.data?.status || 'none'; })
                .catch(() => { fStatus.value = 'none'; })
        );
    }
    await Promise.all(tasks);
}

const loadPosts = async (reset = false) => {
    if (reset) { postsPage.value = 0; posts.value = []; }
    loading.value = true;
    try {
        const d = (await getPostsByUser(userId.value, postsPage.value, POSTS_SIZE))?.data?.data || {};
        const batch = d.items || [];
        posts.value = reset ? batch : [...posts.value, ...batch];
        postsTotal.value = d.total ?? posts.value.length;
        postsTotalPages.value = d.totalPages ?? 1;
        postsPage.value += 1;
    } catch (e) {
        if (reset) posts.value = [];
    } finally {
        loading.value = false;
    }
}

const loadReposts = async (reset = false) => {
    if (reset) { repostsPage.value = 0; reposts.value = []; }
    repostsLoading.value = true;
    try {
        const d = (await getRepostsOf(userId.value, repostsPage.value, 10))?.data?.data;
        const data = Array.isArray(d) ? { items: d } : (d || {});
        const batch = data.items || [];
        reposts.value = reset ? batch : [...reposts.value, ...batch];
        repostsTotal.value = data.total ?? reposts.value.length;
        repostsTotalPages.value = data.totalPages ?? 1;
        if (batch.length) repostsPage.value += 1; else repostsTotalPages.value = repostsPage.value;
    } catch (e) {
        if (reset) reposts.value = [];
    } finally {
        repostsLoading.value = false;
    }
}

const load = async () => {
    user.value = null;
    reposts.value = [];
    avatarOk.value = true;
    // 4 nhóm dữ liệu độc lập -> phát cùng lúc thay vì nối đuôi
    loadReposts(true);
    loadPosts(true);
    refreshStatus();
    try {
        const res = await getUserInfo(userId.value);
        user.value = res?.data?.data || null;
    } catch (error) {
        console.log(error);
    }
}

const wrap = (fn, okMsg) => async () => {
    try {
        await fn(userId.value);
        if (okMsg) toast?.(okMsg);
        bumpNotifRefresh();
        await refreshStatus();
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thao tác thất bại');
    }
};
const doSend = wrap(sendFriendRequest, 'Đã gửi lời mời kết bạn');
const doCancel = wrap(cancelFriendRequest, 'Đã huỷ lời mời');
const doAccept = wrap(acceptFriendRequest, 'Đã kết bạn');
const doDecline = wrap(declineFriendRequest, 'Đã từ chối');
const doUnfriend = () => {
    openConfirm?.('Huỷ kết bạn', `Huỷ kết bạn với ${user.value?.fullName || 'người này'}?`, async () => {
        try {
            await unfriend(userId.value);
            toast?.('Đã huỷ kết bạn');
            await refreshStatus();
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Huỷ kết bạn thất bại');
        }
    }, { danger: true, confirmText: 'Huỷ kết bạn' });
};

const doBlock = () => {
    openConfirm?.('Chặn người dùng', `Chặn ${user.value?.fullName || 'người này'}? Hai người sẽ không thấy bài của nhau, không nhắn tin hay kết bạn được. Quan hệ bạn bè hiện tại (nếu có) sẽ bị huỷ.`, async () => {
        try {
            await blockUser(userId.value);
            toast?.('Đã chặn');
            await refreshStatus();
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Chặn thất bại');
        }
    }, { danger: true, confirmText: 'Chặn' });
};
const doUnblock = async () => {
    try {
        await unblockUser(userId.value);
        toast?.('Đã bỏ chặn');
        await refreshStatus();
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Bỏ chặn thất bại');
    }
};

const report = () => {
    openReport?.('USER', userId.value, user.value?.fullName || 'người dùng này');
};

const goFriends = () => {
    route.push({ path: '/follow', query: { user: userId.value, tab: 'friends' } });
};

const messageUser = async () => {
    try {
        const res = await openDirectConversation(userId.value);
        const conv = res?.data?.data;
        if (conv?.conversationId) {
            route.push({
                path: '/chat',
                query: { c: conv.conversationId, name: user.value?.fullName || 'Tin nhắn riêng' },
            });
        } else {
            showDialog?.('Thông báo', 'Không mở được cuộc trò chuyện');
        }
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Không mở được cuộc trò chuyện');
    }
};

watch(userId, load);
watch(avatarUrl, () => { avatarOk.value = true; });
onMounted(load);
</script>
