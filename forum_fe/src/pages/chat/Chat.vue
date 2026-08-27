<template>
    <div class="content flex justify-center">
        <div class="w-[90%] border rounded flex" style="height: 70vh;">
            <!-- Sidebar -->
            <div class="w-[32%] border-r flex flex-col">
                <div class="p-3 border-b">
                    <div class="flex">
                        <DxTextBox v-model="newName" placeholder="Tên nhóm mới" class="flex-1"/>
                        <DxButton class="ml-2" icon="plus" type="default" @click="handleCreate"/>
                    </div>
                    <div class="flex mt-2">
                        <DxTextBox v-model="searchName" placeholder="Tìm nhóm để tham gia" class="flex-1"
                            @enter-key="handleSearch"/>
                        <DxButton class="ml-2" icon="search" @click="handleSearch"/>
                    </div>
                </div>

                <div class="flex-1 overflow-y-auto">
                    <div v-if="searchResults.length" class="p-2">
                        <div class="text-xs text-gray-500 mb-1">Kết quả tìm kiếm</div>
                        <div v-for="c in searchResults" :key="c.conversationId"
                            class="flex items-center justify-between p-2 hover:bg-orange-50 rounded">
                            <span>{{ c.conversationName }}</span>
                            <DxButton text="Tham gia" @click="() => handleJoin(c)"/>
                        </div>
                        <hr class="my-2"/>
                    </div>

                    <div class="text-xs text-gray-500 px-2 pt-2">Nhóm của bạn</div>
                    <div v-if="conversations.length === 0" class="text-gray-400 p-3 text-sm">
                        Chưa tham gia nhóm nào
                    </div>
                    <div v-for="c in conversations" :key="c.conversationId"
                        class="p-3 cursor-pointer border-b hover:bg-orange-50"
                        :class="{ 'bg-orange-100': active?.conversationId === c.conversationId }"
                        @click="() => openConversation(c)">
                        <div class="font-bold">{{ displayName(c) }}</div>
                        <div class="text-xs text-gray-500">{{ c.createdAt }}</div>
                    </div>
                </div>
            </div>

            <!-- Message pane -->
            <div class="flex-1 flex flex-col">
                <div v-if="!active" class="flex-1 flex items-center justify-center text-gray-400">
                    Chọn một nhóm để bắt đầu trò chuyện
                </div>
                <template v-else>
                    <div class="p-3 border-b flex items-center">
                        <div class="font-bold flex-1">{{ displayName(active) }}</div>
                        <span class="text-xs mr-3" :class="connected ? 'text-green-600' : 'text-gray-400'">
                            {{ connected ? '● trực tuyến' : '○ ngoại tuyến' }}
                        </span>
                        <DxButton text="Rời nhóm" type="danger" @click="handleLeave"/>
                    </div>

                    <div ref="listEl" class="flex-1 overflow-y-auto p-3 flex flex-col gap-2">
                        <div v-for="m in messages" :key="m.messageId"
                            class="max-w-[70%] px-3 py-2 rounded"
                            :class="m.senderId === myId
                                ? 'self-end bg-blue-500 text-white'
                                : 'self-start bg-gray-200 text-black'">
                            {{ m.content }}
                            <img v-if="m.messageImg && !m.messageImg.includes('null')"
                                :src="IMAGE_BASE + m.messageImg" class="mt-1 max-w-[200px]"/>
                        </div>
                    </div>

                    <div class="p-3 border-t flex">
                        <DxTextBox v-model="draft" placeholder="Nhập tin nhắn..." class="flex-1"
                            @enter-key="sendMessage"/>
                        <DxButton class="ml-2" text="Gửi" type="default" @click="sendMessage"/>
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
    createConversation, joinConversation, leaveConversation,
} from '@/apis/chat';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { SOCKET_URL, IMAGE_BASE } from '@/config';

const showDialog = inject('openDialogError');
const router = useRouter();
const myId = getItemLocal(LOCALKEYS.USER_ID);

const displayName = (c) => {
    const n = c?.conversationName || '';
    if (n.startsWith('dm:') || n.startsWith('dm_')) return '💬 Tin nhắn riêng';
    return n;
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

let socket = null;

const loadConversations = async () => {
    try {
        const res = await getMyConversations();
        conversations.value = res?.data?.data || [];
    } catch (e) {
        // backend ném lỗi khi chưa tham gia nhóm nào
        conversations.value = [];
    }
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

const handleLeave = async () => {
    if (!active.value) return;
    try {
        await leaveConversation(active.value.conversationId);
        teardownSocket();
        active.value = null;
        messages.value = [];
        await loadConversations();
    } catch (e) {
        showDialog('Thông báo', e?.description || 'Rời nhóm thất bại');
    }
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
        if (msg && msg.content) {
            messages.value.push(msg);
            scrollToBottom();
        }
    });
}

const sendMessage = () => {
    const text = draft.value.trim();
    if (!text || !active.value) return;
    if (socket && connected.value) {
        socket.emit('send_message', { content: text });
    }
    // server không gửi lại tin cho chính người gửi -> tự thêm vào danh sách
    messages.value.push({
        messageId: 'local-' + Date.now(),
        content: text,
        senderId: myId,
        sentAt: new Date().toISOString(),
        conversationId: active.value.conversationId,
    });
    draft.value = '';
    scrollToBottom();
}

const openFromQuery = () => {
    const q = router.currentRoute.value.query;
    if (q.c) {
        openConversation({
            conversationId: String(q.c),
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
