<template>
    <div class="page page--wide">
        <div class="card card--flush flex" style="height: 72vh;">
            <!-- Sidebar -->
            <div class="w-[34%] min-w-[240px] border-r flex flex-col">
                <div class="p-3 border-b flex flex-col gap-2">
                    <div class="flex gap-2">
                        <DxTextBox v-model="newName" placeholder="Tên nhóm mới" class="flex-1" @enter-key="handleCreate"/>
                        <DxButton icon="plus" type="default" @click="handleCreate"/>
                    </div>
                    <div class="flex gap-2">
                        <DxTextBox v-model="searchName" placeholder="Tìm nhóm để tham gia" class="flex-1"
                            @enter-key="handleSearch"/>
                        <DxButton icon="search" @click="handleSearch"/>
                    </div>
                </div>

                <div class="flex-1 overflow-y-auto">
                    <div v-if="searchResults.length" class="p-2">
                        <div class="text-xs muted mb-1 px-1">Kết quả tìm kiếm</div>
                        <div v-for="c in searchResults" :key="c.conversationId"
                            class="flex items-center justify-between p-2 hover:bg-gray-50 rounded-lg">
                            <span class="truncate">{{ displayName(c) }}</span>
                            <DxButton text="Tham gia" stylingMode="text" @click="() => handleJoin(c)"/>
                        </div>
                        <hr class="my-2"/>
                    </div>

                    <div class="text-xs muted px-3 pt-2">Nhóm của bạn</div>
                    <div v-if="conversations.length === 0" class="muted p-3 text-sm">
                        Chưa tham gia nhóm nào
                    </div>
                    <div v-for="c in conversations" :key="c.conversationId"
                        class="px-3 py-2.5 cursor-pointer border-b transition hover:bg-gray-50"
                        :class="{ 'bg-[var(--brand-soft)]': active?.conversationId === c.conversationId }"
                        @click="() => openConversation(c)">
                        <div class="font-semibold text-sm truncate">{{ displayName(c) }}</div>
                        <div class="text-xs muted truncate">{{ formatDateTime(c.createdAt) }}</div>
                    </div>
                </div>
            </div>

            <!-- Message pane -->
            <div class="flex-1 flex flex-col min-w-0">
                <div v-if="!active" class="flex-1 flex items-center justify-center muted">
                    Chọn một nhóm để bắt đầu trò chuyện
                </div>
                <template v-else>
                    <div class="p-3 border-b flex items-center gap-3">
                        <div class="font-bold flex-1 truncate">{{ displayName(active) }}</div>
                        <span class="text-xs flex-none" :class="connected ? 'text-green-600' : 'muted'">
                            {{ connected ? '● trực tuyến' : '○ ngoại tuyến' }}
                        </span>
                        <DxButton text="Rời nhóm" type="danger" stylingMode="text" @click="handleLeave"/>
                    </div>

                    <div ref="listEl" class="flex-1 overflow-y-auto p-4 flex flex-col gap-1 bg-[var(--bg)]">
                        <div v-for="m in messages" :key="m.messageId"
                            class="flex flex-col max-w-[72%]"
                            :class="m.senderId === myId ? 'self-end items-end' : 'self-start items-start'">
                            <div
                                class="px-3 py-2 rounded-2xl text-sm shadow-sm"
                                :class="m.senderId === myId
                                    ? 'bg-[var(--accent)] text-white rounded-br-md'
                                    : 'bg-white text-[var(--text)] rounded-bl-md'">
                                <span v-if="m.content">{{ m.content }}</span>
                                <img v-if="m.messageImg && !String(m.messageImg).includes('null')"
                                    :src="IMAGE_BASE + m.messageImg" class="mt-1 max-w-[220px] rounded-lg"/>
                            </div>
                            <span class="text-[11px] muted mt-0.5">{{ formatTime(m.sentAt) }}</span>
                        </div>
                    </div>

                    <div class="p-3 border-t flex items-center gap-2">
                        <DxButton
                            :icon="pendingImg ? 'photo' : 'image'"
                            :type="pendingImg ? 'success' : 'normal'"
                            stylingMode="text"
                            hint="Đính kèm ảnh"
                            @click="pickImg"
                        />
                        <input ref="fileEl" type="file" accept="image/*" class="hidden" @change="onPickImg" />
                        <DxTextBox v-model="draft" placeholder="Nhập tin nhắn…" class="flex-1"
                            @enter-key="sendMessage"/>
                        <DxButton text="Gửi" type="default" @click="sendMessage"/>
                    </div>
                </template>
            </div>
        </div>
    </div>
</template>

<script setup>
import { DxTextBox, DxButton } from 'devextreme-vue';
import { onMounted, onBeforeUnmount, nextTick, ref, inject, watch } from 'vue';
import { useRouter } from 'vue-router';
import { io } from 'socket.io-client';
import {
    getMyConversations, searchConversations, getMessages,
    createConversation, joinConversation, leaveConversation, sendMessageRest,
} from '@/apis/chat';
import { getUserInfo } from '@/apis/user';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { SOCKET_URL, IMAGE_BASE } from '@/config';
import { formatDateTime, formatTime } from '@/js/helper';

const showDialog = inject('openDialogError');
const openConfirm = inject('openConfirm');
const toast = inject('toast');
const router = useRouter();
const myId = getItemLocal(LOCALKEYS.USER_ID);

// Tên đối phương của các hội thoại 1-1, theo conversationId
const dmNames = ref({});

const isDm = (name) => typeof name === 'string' && (name.startsWith('dm:') || name.startsWith('dm_'));

const otherIdFromDm = (name) => {
    const raw = name.startsWith('dm:') ? name.slice(3) : name.slice(3);
    const parts = raw.split(name.startsWith('dm:') ? ':' : '_');
    return parts.find((p) => p && p !== myId) || null;
}

const resolveDmNames = async () => {
    for (const c of conversations.value) {
        if (!isDm(c.conversationName) || dmNames.value[c.conversationId]) continue;
        const otherId = otherIdFromDm(c.conversationName);
        if (!otherId) continue;
        try {
            const res = await getUserInfo(otherId);
            dmNames.value = {
                ...dmNames.value,
                [c.conversationId]: res?.data?.data?.fullName || 'Tin nhắn riêng',
            };
        } catch (e) {
            /* bỏ qua, giữ tên mặc định */
        }
    }
}

const displayName = (c) => {
    if (!c) return '';
    if (isDm(c.conversationName)) return dmNames.value[c.conversationId] || 'Tin nhắn riêng';
    return c.conversationName || '';
}

const conversations = ref([]);
const searchResults = ref([]);
const messages = ref([]);
const active = ref(null);
const connected = ref(false);

const newName = ref('');
const searchName = ref('');
const draft = ref('');
const listEl = ref(null);
const fileEl = ref(null);
const pendingImg = ref(null);

let socket = null;

const pickImg = () => fileEl.value?.click();
const onPickImg = (e) => { pendingImg.value = e.target.files[0] || null; };

const loadConversations = async () => {
    try {
        const res = await getMyConversations();
        conversations.value = res?.data?.data || [];
    } catch (e) {
        // backend ném lỗi khi chưa tham gia nhóm nào
        conversations.value = [];
    }
    resolveDmNames();
}

const handleCreate = async () => {
    if (!newName.value.trim()) return;
    try {
        await createConversation(newName.value.trim());
        newName.value = '';
        await loadConversations();
    } catch (e) {
        showDialog('Thông báo', e?.description || 'Tạo nhóm thất bại');
    }
}

const handleSearch = async () => {
    if (!searchName.value.trim()) { searchResults.value = []; return; }
    try {
        const res = await searchConversations(searchName.value.trim());
        searchResults.value = res?.data?.data || [];
    } catch (e) {
        searchResults.value = [];
    }
}

const handleJoin = async (c) => {
    try {
        await joinConversation(c.conversationId);
        searchResults.value = [];
        searchName.value = '';
        await loadConversations();
        openConversation(c);
    } catch (e) {
        showDialog('Thông báo', e?.description || 'Không tham gia được nhóm này');
    }
}

const handleLeave = () => {
    if (!active.value) return;
    const conv = active.value;
    openConfirm?.('Rời nhóm', `Rời "${displayName(conv)}"? Bạn sẽ không nhắn được trong nhóm này nữa.`, async () => {
        try {
            await leaveConversation(conv.conversationId);
            teardownSocket();
            active.value = null;
            messages.value = [];
            toast?.('Đã rời nhóm');
            await loadConversations();
        } catch (e) {
            showDialog('Thông báo', e?.description || 'Rời nhóm thất bại');
        }
    }, { danger: true, confirmText: 'Rời nhóm' });
}

const scrollToBottom = () => {
    nextTick(() => {
        if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight;
    });
}

const teardownSocket = () => {
    if (socket) {
        socket.removeAllListeners();
        socket.disconnect();
        socket = null;
    }
    connected.value = false;
}

const openConversation = async (c) => {
    active.value = c;
    messages.value = [];
    try {
        const res = await getMessages(c.conversationId);
        messages.value = res?.data?.data || [];
    } catch (e) {
        messages.value = [];
    }
    scrollToBottom();
    connectSocket(c.conversationId);
}

const connectSocket = (conversationId) => {
    teardownSocket();
    socket = io(SOCKET_URL, {
        transports: ['websocket'],
        query: {
            conversationID: conversationId,
            token: getItemLocal(LOCALKEYS.ACCESS_TOKEN),
        },
    });
    socket.on('connect', () => { connected.value = true; });
    socket.on('disconnect', () => { connected.value = false; });
    socket.on('connect_error', () => { connected.value = false; });
    socket.on('get_message', (msg) => {
        if (msg && (msg.content || msg.messageImg)) {
            messages.value.push(msg);
            scrollToBottom();
        }
    });
}

const appendLocal = (extra) => {
    messages.value.push({
        messageId: 'local-' + Date.now(),
        senderId: myId,
        sentAt: new Date().toISOString(),
        conversationId: active.value.conversationId,
        content: '',
        ...extra,
    });
    scrollToBottom();
}

const sendMessage = async () => {
    if (!active.value) return;
    const text = draft.value.trim();
    const img = pendingImg.value;
    if (!text && !img) return;

    if (img) {
        // ảnh phải đi qua REST (socket không nhận file); phía kia thấy khi tải lại
        try {
            const form = { content: text || '' };
            form.messageImg = img;
            const res = await sendMessageRest(active.value.conversationId, form);
            appendLocal({ content: text || '', messageImg: res?.data?.data?.messageImg || null });
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Gửi ảnh thất bại');
        }
        pendingImg.value = null;
        if (fileEl.value) fileEl.value.value = '';
        draft.value = '';
        return;
    }

    if (socket && connected.value) {
        socket.emit('send_message', { content: text });
    }
    appendLocal({ content: text });
    draft.value = '';
}

const openFromQuery = () => {
    const q = router.currentRoute.value.query;
    if (q.c) {
        const id = String(q.c);
        if (q.name) {
            // hiển thị ngay tên đối phương truyền từ trang hồ sơ
            dmNames.value = { ...dmNames.value, [id]: String(q.name) };
        }
        const inList = conversations.value.find((c) => c.conversationId === id);
        openConversation(inList || {
            conversationId: id,
            conversationName: q.name ? String(q.name) : 'Tin nhắn riêng',
        });
    }
}

onMounted(async () => {
    await loadConversations();
    openFromQuery();
});

// mở hội thoại khi điều hướng /chat?c=... từ trang hồ sơ trong lúc đang ở trang chat
watch(() => router.currentRoute.value.query.c, (c) => {
    if (c && c !== active.value?.conversationId) openFromQuery();
});

onBeforeUnmount(teardownSocket);
</script>
