<template>
    <div class="page page--wide">
        <div class="card card--flush flex" style="height: 74vh;">
            <!-- Sidebar -->
            <div class="w-[36%] min-w-[260px] border-r flex flex-col">
                <div class="p-3 border-b flex flex-col gap-3">
                    <!-- Tạo nhóm mới -->
                    <div>
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-bold muted uppercase tracking-wide">Tạo nhóm mới</span>
                            <DxButton :icon="showCreate ? 'chevronup' : 'plus'" stylingMode="text" @click="toggleCreate" />
                        </div>
                        <div v-if="showCreate" class="mt-2 flex flex-col gap-2">
                            <DxTextBox v-model="createName" placeholder="Tên nhóm" />
                            <DxTextBox v-model="memberQuery" placeholder="Lọc bạn bè để thêm…"
                                @input="searchMembers" @enter-key="searchMembers" />
                            <div class="border rounded-lg divide-y max-h-40 overflow-y-auto">
                                <button v-for="u in memberResults" :key="u.userId"
                                    class="w-full text-left px-2 py-1.5 text-sm hover:bg-gray-50 flex justify-between"
                                    @click="pickMember(u)">
                                    <span class="truncate">{{ u.fullName }}</span>
                                    <span class="text-[var(--accent)]">+ thêm</span>
                                </button>
                                <div v-if="!memberResults.length" class="px-2 py-1.5 text-xs muted">
                                    {{ friends.length ? 'Không có bạn bè phù hợp' : 'Bạn chưa có bạn bè nào' }}
                                </div>
                            </div>
                            <div v-if="pickedMembers.length" class="flex flex-wrap gap-1">
                                <span v-for="u in pickedMembers" :key="u.userId"
                                    class="text-xs bg-[var(--brand-soft)] text-[var(--brand)] rounded-full px-2 py-0.5 flex items-center gap-1">
                                    {{ u.fullName }}
                                    <button @click="unpickMember(u.userId)">×</button>
                                </span>
                            </div>
                            <DxButton text="Tạo nhóm" type="default" @click="doCreateGroup" />
                        </div>
                    </div>

                    <!-- Tìm nhóm để tham gia -->
                    <div>
                        <span class="text-xs font-bold muted uppercase tracking-wide">Tìm nhóm để tham gia</span>
                        <div class="flex gap-2 mt-1">
                            <DxTextBox v-model="searchName" placeholder="Tên nhóm…" class="flex-1" @enter-key="handleSearch" />
                            <DxButton icon="search" @click="handleSearch" />
                        </div>
                        <div v-if="searchResults.length" class="mt-1 border rounded-lg divide-y">
                            <div v-for="c in searchResults" :key="c.conversationId"
                                class="flex items-center justify-between px-2 py-1.5 text-sm">
                                <span class="truncate">{{ displayName(c) }}</span>
                                <DxButton text="Tham gia" stylingMode="text" @click="() => handleJoin(c)" />
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Cuộc trò chuyện -->
                <div class="flex-1 overflow-y-auto">
                    <div class="text-xs font-bold muted uppercase tracking-wide px-3 pt-3 pb-1">Cuộc trò chuyện</div>
                    <div v-if="conversations.length === 0" class="muted p-3 text-sm">Chưa có cuộc trò chuyện nào</div>
                    <div v-for="c in conversations" :key="c.conversationId"
                        class="px-3 py-2.5 cursor-pointer border-b transition hover:bg-gray-50"
                        :class="{ 'bg-[var(--brand-soft)]': active?.conversationId === c.conversationId }"
                        @click="() => openConversation(c)">
                        <div class="flex items-center gap-1">
                            <span>{{ isDm(c.conversationName) ? '💬' : '👥' }}</span>
                            <span class="font-semibold text-sm truncate flex-1"
                                :class="{ 'font-bold': unreadByConv[c.conversationId] }">{{ displayName(c) }}</span>
                            <span v-if="unreadByConv[c.conversationId]"
                                class="flex-none text-[11px] font-bold text-white bg-[var(--danger)] rounded-full min-w-[18px] h-[18px] px-1 text-center leading-[18px]">
                                {{ unreadByConv[c.conversationId] > 9 ? '9+' : unreadByConv[c.conversationId] }}
                            </span>
                            <span class="flex-none text-[11px] muted">{{ timeAgo(c.lastMessageAt || c.createdAt) }}</span>
                        </div>
                        <div class="text-xs truncate mt-0.5"
                            :class="unreadByConv[c.conversationId] ? 'text-[var(--text)] font-semibold' : 'muted'">
                            {{ previewOf(c) }}
                        </div>
                    </div>
                </div>
            </div>

            <!-- Message pane -->
            <div class="flex-1 flex flex-col min-w-0">
                <div v-if="!active" class="flex-1 flex items-center justify-center muted">
                    Chọn một cuộc trò chuyện để bắt đầu
                </div>
                <template v-else>
                    <div class="p-3 border-b flex items-center gap-3">
                        <div class="min-w-0 flex-1">
                            <div v-if="renaming" class="flex items-center gap-1">
                                <DxTextBox v-model="renameText" class="flex-1" @enter-key="saveRename" />
                                <DxButton icon="check" type="success" stylingMode="text" @click="saveRename" />
                                <DxButton icon="close" stylingMode="text" @click="renaming = false" />
                            </div>
                            <div v-else class="font-bold truncate flex items-center gap-1">
                                <span class="truncate">{{ displayName(active) }}</span>
                                <button v-if="!isDm(active.conversationName)" class="text-xs muted hover:text-[var(--text)]"
                                    title="Đổi tên nhóm" @click="startRename">✎</button>
                            </div>
                            <div class="text-xs" :class="statusClass">{{ statusText }}</div>
                        </div>
                        <DxButton v-if="!isDm(active.conversationName)"
                            :icon="showMembers ? 'chevronup' : 'group'" stylingMode="text"
                            :hint="showMembers ? 'Ẩn thành viên' : 'Xem thành viên'"
                            @click="toggleMembers" />
                        <DxButton v-if="!isDm(active.conversationName)" icon="plus" stylingMode="text"
                            hint="Thêm thành viên" @click="openAddMember" />
                        <DxButton text="Rời" type="danger" stylingMode="text" @click="handleLeave" />
                    </div>

                    <div v-if="showMembers && !isDm(active.conversationName)" class="px-3 py-2 border-b bg-gray-50 flex flex-wrap gap-1.5">
                        <span class="text-xs muted mr-1 self-center">Thành viên ({{ members.length }}):</span>
                        <span v-for="mem in members" :key="mem.userId"
                            class="text-xs bg-white border rounded-full pl-2 pr-1 py-0.5 flex items-center gap-1">
                            <span class="cursor-pointer hover:underline" @click="() => router.push('/user/' + mem.userId)">
                                {{ mem.fullName }}{{ mem.userId === myId ? ' (bạn)' : '' }}
                            </span>
                            <button v-if="mem.userId !== myId" class="text-[var(--danger)] font-bold px-0.5"
                                title="Xoá khỏi nhóm" @click="() => confirmRemoveMember(mem)">×</button>
                        </span>
                    </div>

                    <div ref="listEl" class="flex-1 overflow-y-auto p-4 flex flex-col gap-1 bg-[var(--bg)]">
                        <div v-for="(m, idx) in messages" :key="m.messageId"
                            class="group flex flex-col max-w-[78%]"
                            :class="m.senderId === myId ? 'self-end items-end' : 'self-start items-start'">

                            <!-- tên người gửi (chỉ nhóm, tin của người khác, đầu mỗi cụm) -->
                            <span v-if="showSenderTag(m, idx)" class="text-[11px] font-semibold muted mb-0.5 px-1">
                                {{ senderName(m.senderId) }}
                            </span>

                            <!-- chế độ sửa -->
                            <div v-if="editingId === m.messageId" class="flex items-center gap-1 w-[280px]">
                                <DxTextBox v-model="editText" class="flex-1" @enter-key="saveEdit(m)" />
                                <DxButton icon="check" type="success" stylingMode="text" @click="saveEdit(m)" />
                                <DxButton icon="close" stylingMode="text" @click="cancelEdit" />
                            </div>

                            <template v-else>
                                <div class="flex items-end gap-1"
                                    :class="m.senderId === myId ? 'flex-row' : 'flex-row-reverse'">
                                    <!-- nút sửa / thu hồi cho tin của mình -->
                                    <div v-if="canModify(m)"
                                        class="flex-none flex gap-0.5 opacity-0 group-hover:opacity-100 transition">
                                        <button class="text-[11px] muted hover:text-[var(--text)] px-1" @click="startEdit(m)">Sửa</button>
                                        <button class="text-[11px] muted hover:text-[var(--danger)] px-1" @click="confirmRecall(m)">Thu hồi</button>
                                    </div>
                                    <div
                                        class="px-3 py-2 rounded-2xl text-sm shadow-sm"
                                        :class="[
                                            m.senderId === myId ? 'bg-[var(--accent)] text-white rounded-br-md' : 'bg-white text-[var(--text)] rounded-bl-md',
                                            m.recalled ? 'italic opacity-70' : ''
                                        ]">
                                        <span v-if="m.recalled">Tin nhắn đã được thu hồi</span>
                                        <template v-else>
                                            <span v-if="m.content">{{ m.content }}</span>
                                            <img v-if="m.messageImg && !String(m.messageImg).includes('null')"
                                                :src="IMAGE_BASE + m.messageImg" class="mt-1 max-w-[220px] rounded-lg cursor-zoom-in"
                                                @click="openLightbox(IMAGE_BASE + m.messageImg)" />
                                        </template>
                                    </div>
                                </div>
                                <span class="text-[11px] muted mt-0.5">
                                    <template v-if="idx === lastMineIndex && !m.recalled">{{ otherSeen ? 'Đã xem · ' : 'Đã gửi · ' }}</template>{{ formatTime(m.sentAt) }}
                                </span>
                            </template>
                        </div>

                        <div v-if="otherTyping" class="self-start flex items-center gap-1 px-3 py-2 bg-white rounded-2xl rounded-bl-md shadow-sm">
                            <span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span>
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
                        <EmojiPicker direction="up" @pick="addDraftEmoji" />
                        <DxTextBox v-model="draft" placeholder="Nhập tin nhắn…" class="flex-1"
                            @enter-key="sendMessage" @input="onTyping" @focus-in="onInputFocus" />
                        <DxButton text="Gửi" type="default" @click="sendMessage" />
                    </div>
                </template>
            </div>
        </div>

        <!-- Thêm thành viên vào nhóm đang mở (chỉ bạn bè) -->
        <DxPopup title="Thêm bạn bè vào nhóm" v-model:visible="showAddMember" :width="460" :height="440" :hide-on-outside-click="true">
            <div class="flex flex-col gap-2">
                <DxTextBox v-model="addQuery" placeholder="Lọc bạn bè…" @input="searchAdd" @enter-key="searchAdd" />
                <div v-if="addResults.length" class="border rounded-lg divide-y max-h-72 overflow-y-auto">
                    <div v-for="u in addResults" :key="u.userId" class="flex items-center justify-between px-2 py-2 text-sm">
                        <span class="truncate">{{ u.fullName }}</span>
                        <DxButton text="Thêm" stylingMode="text" @click="() => doAddMember(u)" />
                    </div>
                </div>
                <div v-else class="muted text-sm">
                    {{ friends.length ? 'Không có bạn bè phù hợp' : 'Bạn chưa có bạn bè nào để thêm' }}
                </div>
            </div>
        </DxPopup>
    </div>
</template>

<script setup>
import { DxTextBox, DxButton, DxPopup } from 'devextreme-vue';
import { onMounted, onBeforeUnmount, nextTick, ref, computed, inject, watch } from 'vue';
import { useRouter } from 'vue-router';
import { io } from 'socket.io-client';
import {
    getMyConversations, searchConversations, getMessages,
    createConversation, joinConversation, leaveConversation, sendMessageRest, addMember, getMembers,
    editMessage, recallMessage, renameConversation, removeMember,
} from '@/apis/chat';
import { getUserInfo } from '@/apis/user';
import { getFriends } from '@/apis/friend';
import { markReadByTarget, getNotifications } from '@/apis/notification';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { activeConversationId, bumpNotifRefresh, notifRefreshTick } from '@/storages/appState';
import { SOCKET_URL, IMAGE_BASE } from '@/config';
import { formatTime, timeAgo } from '@/js/helper';
import EmojiPicker from '@/components/EmojiPicker.vue';

const showDialog = inject('openDialogError');
const openConfirm = inject('openConfirm');
const toast = inject('toast');
const openLightbox = inject('openLightbox', () => {});
const router = useRouter();
const myId = getItemLocal(LOCALKEYS.USER_ID);

/* ---------- state ---------- */
const conversations = ref([]);
const searchResults = ref([]);
const messages = ref([]);
const active = ref(null);
const connected = ref(false);
const dmNames = ref({});

const searchName = ref('');
const draft = ref('');
const listEl = ref(null);
const fileEl = ref(null);
const pendingImg = ref(null);

// tạo nhóm
const showCreate = ref(false);
const createName = ref('');
const memberQuery = ref('');
const memberResults = ref([]);
const pickedMembers = ref([]);

// thêm thành viên vào nhóm đang mở
const showAddMember = ref(false);
const addQuery = ref('');
const addResults = ref([]);

// sửa / thu hồi tin nhắn
const editingId = ref(null);
const editText = ref('');

// đổi tên nhóm
const renaming = ref(false);
const renameText = ref('');

// typing / seen / presence
const otherTyping = ref(false);
const otherSeen = ref(false);
const otherOnline = ref(false);
let typingHideTimer = null;
let lastTypingEmit = 0;

// thành viên nhóm
const showMembers = ref(false);
const members = ref([]);
const senderMap = ref({}); // userId -> { fullName, avtUrl } để hiện tên người gửi trong nhóm

let socket = null;

const lastMineIndex = computed(() => {
    for (let i = messages.value.length - 1; i >= 0; i--) {
        if (messages.value[i].senderId === myId) return i;
    }
    return -1;
});

/* ---------- helpers tên hội thoại ---------- */
const isDm = (name) => typeof name === 'string' && (name.startsWith('dm:') || name.startsWith('dm_'));

const statusText = computed(() => {
    if (!active.value) return '';
    if (isDm(active.value.conversationName)) {
        return otherOnline.value ? '● Đang hoạt động' : '○ Không hoạt động';
    }
    return connected.value ? '● Đã kết nối' : '○ Mất kết nối';
});
const statusClass = computed(() => {
    const on = isDm(active.value?.conversationName) ? otherOnline.value : connected.value;
    return on ? 'text-green-600' : 'muted';
});

const otherIdFromDm = (name) => {
    const raw = name.slice(3);
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
            dmNames.value = { ...dmNames.value, [c.conversationId]: res?.data?.data?.fullName || 'Tin nhắn riêng' };
        } catch (e) { /* ignore */ }
    }
}

const displayName = (c) => {
    if (!c) return '';
    if (isDm(c.conversationName)) return dmNames.value[c.conversationId] || 'Tin nhắn riêng';
    return c.conversationName || '';
}

/* ---------- xem trước tin nhắn cuối + badge chưa đọc ---------- */
const unreadByConv = ref({});

const previewOf = (c) => {
    const hasImg = c.lastMessageImg && !String(c.lastMessageImg).includes('null');
    let body = c.lastMessage || '';
    if (!body && hasImg) body = '🖼 Hình ảnh';
    if (!body) return 'Chưa có tin nhắn';
    const mine = c.lastSenderId && c.lastSenderId === myId;
    return (mine ? 'Bạn: ' : '') + body;
}

const loadUnread = async () => {
    try {
        const list = (await getNotifications())?.data?.data || [];
        const m = {};
        for (const n of list) {
            if (n.type === 'MESSAGE' && !n.read && n.targetId) m[n.targetId] = (m[n.targetId] || 0) + 1;
        }
        // hội thoại đang mở thì luôn coi như đã đọc
        if (active.value) delete m[active.value.conversationId];
        unreadByConv.value = m;
    } catch (e) { /* ignore */ }
}

/* ---------- conversations ---------- */
const loadConversations = async () => {
    try {
        const res = await getMyConversations();
        conversations.value = res?.data?.data || [];
    } catch (e) {
        conversations.value = [];
    }
    resolveDmNames();
    loadUnread();
}

// cập nhật tin cuối + đẩy hội thoại lên đầu mà không cần gọi lại API
const bumpConversation = (convId, { content = '', messageImg = null, senderId = null } = {}) => {
    const i = conversations.value.findIndex((c) => c.conversationId === convId);
    if (i === -1) return;
    const c = { ...conversations.value[i], lastMessage: content, lastMessageImg: messageImg, lastMessageAt: new Date().toISOString(), lastSenderId: senderId };
    conversations.value.splice(i, 1);
    conversations.value.unshift(c);
}

/* ---------- bạn bè (chỉ thêm bạn bè vào nhóm) ---------- */
const friends = ref([]);
const loadFriends = async () => {
    try {
        const ids = (await getFriends(myId))?.data?.data?.userId || [];
        const out = [];
        for (const id of ids) {
            try {
                const u = await getUserInfo(id);
                out.push({ userId: id, fullName: u?.data?.data?.fullName || id });
            } catch (e) {
                out.push({ userId: id, fullName: id });
            }
        }
        friends.value = out;
    } catch (e) {
        friends.value = [];
    }
}
const filterFriends = (q, excludeIds) => {
    const s = (q || '').trim().toLowerCase();
    return friends.value.filter((f) =>
        !excludeIds.has(f.userId)
        && (s === '' || (f.fullName || '').toLowerCase().includes(s))
    );
}

/* ---------- tạo nhóm ---------- */
const toggleCreate = () => {
    showCreate.value = !showCreate.value;
    if (showCreate.value) searchMembers();
}
const resetCreate = () => {
    showCreate.value = false;
    createName.value = '';
    memberQuery.value = '';
    memberResults.value = [];
    pickedMembers.value = [];
}
const searchMembers = () => {
    const picked = new Set(pickedMembers.value.map((u) => u.userId));
    memberResults.value = filterFriends(memberQuery.value, picked);
}
const pickMember = (u) => {
    pickedMembers.value.push({ userId: u.userId, fullName: u.fullName });
    memberResults.value = memberResults.value.filter((x) => x.userId !== u.userId);
}
const unpickMember = (id) => {
    pickedMembers.value = pickedMembers.value.filter((u) => u.userId !== id);
}
const doCreateGroup = async () => {
    if (!createName.value.trim()) { showDialog?.('Thông báo', 'Nhập tên nhóm'); return; }
    try {
        const res = await createConversation(createName.value.trim());
        const d = res?.data?.data || {};
        const cid = d['conversationId: '] || d.conversationId || null;
        if (!cid) { showDialog?.('Thông báo', 'Tạo nhóm thất bại'); return; }
        await Promise.allSettled(pickedMembers.value.map((u) => addMember(cid, u.userId)));
        toast?.('Đã tạo nhóm');
        resetCreate();
        await loadConversations();
        const created = conversations.value.find((c) => c.conversationId === cid)
            || { conversationId: cid, conversationName: createName.value.trim() };
        openConversation(created);
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Tạo nhóm thất bại');
    }
}

/* ---------- thành viên nhóm ---------- */
const loadMembers = async () => {
    if (!active.value) return;
    try {
        const res = await getMembers(active.value.conversationId);
        members.value = res?.data?.data || [];
        const map = { ...senderMap.value };
        for (const m of members.value) map[m.userId] = { fullName: m.fullName, avtUrl: m.avtUrl };
        senderMap.value = map;
    } catch (e) {
        members.value = [];
    }
}
const toggleMembers = () => {
    showMembers.value = !showMembers.value;
    if (showMembers.value) loadMembers();
}

// tên người gửi (dùng cho nhóm); tự nạp nếu người đó không còn trong danh sách thành viên
const ensureSender = async (uid) => {
    if (!uid || uid === myId || senderMap.value[uid]) return;
    senderMap.value = { ...senderMap.value, [uid]: { fullName: 'Thành viên', avtUrl: '' } };
    try {
        const u = await getUserInfo(uid);
        senderMap.value = { ...senderMap.value, [uid]: { fullName: u?.data?.data?.fullName || 'Thành viên', avtUrl: u?.data?.data?.avtUrl || '' } };
    } catch (e) { /* giữ nhãn mặc định */ }
}
const senderName = (uid) => senderMap.value[uid]?.fullName || 'Thành viên';
const showSenderTag = (m, idx) => {
    if (!active.value || isDm(active.value.conversationName)) return false;
    if (m.senderId === myId || !m.senderId) return false;
    return idx === 0 || messages.value[idx - 1]?.senderId !== m.senderId;
}
const hydrateSenders = () => {
    const seen = new Set(messages.value.map((m) => m.senderId));
    for (const uid of seen) ensureSender(uid);
}

/* ---------- đổi tên nhóm / xoá thành viên ---------- */
const setConvName = (convId, name) => {
    if (active.value?.conversationId === convId) active.value = { ...active.value, conversationName: name };
    const i = conversations.value.findIndex((c) => c.conversationId === convId);
    if (i !== -1) conversations.value[i] = { ...conversations.value[i], conversationName: name };
}

const startRename = () => {
    renameText.value = active.value?.conversationName || '';
    renaming.value = true;
}
const saveRename = async () => {
    const name = (renameText.value || '').trim();
    if (!name) { showDialog?.('Thông báo', 'Tên nhóm không được để trống'); return; }
    if (name === active.value?.conversationName) { renaming.value = false; return; }
    try {
        await renameConversation(active.value.conversationId, name);
        setConvName(active.value.conversationId, name);
        renaming.value = false;
        toast?.('Đã đổi tên nhóm');
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Đổi tên thất bại');
    }
}

const confirmRemoveMember = (mem) => {
    openConfirm?.('Xoá thành viên', `Xoá ${mem.fullName} khỏi nhóm?`, async () => {
        try {
            await removeMember(active.value.conversationId, mem.userId);
            members.value = members.value.filter((m) => m.userId !== mem.userId);
            toast?.(`Đã xoá ${mem.fullName}`);
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Xoá thành viên thất bại');
        }
    }, { danger: true, confirmText: 'Xoá' });
}

const openAddMember = async () => {
    showAddMember.value = true;
    addQuery.value = '';
    await loadMembers();
    searchAdd();
}
const searchAdd = () => {
    const inGroup = new Set(members.value.map((m) => m.userId));
    addResults.value = filterFriends(addQuery.value, inGroup);
}
const doAddMember = async (u) => {
    try {
        await addMember(active.value.conversationId, u.userId);
        toast?.(`Đã thêm ${u.fullName}`);
        addResults.value = addResults.value.filter((x) => x.userId !== u.userId);
        if (showMembers.value) loadMembers();
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Thêm thành viên thất bại');
    }
}

/* ---------- tìm & tham gia nhóm ---------- */
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
    openConfirm?.('Rời cuộc trò chuyện', `Rời "${displayName(conv)}"?`, async () => {
        try {
            await leaveConversation(conv.conversationId);
            teardownSocket();
            active.value = null;
            activeConversationId.value = null;
            messages.value = [];
            toast?.('Đã rời');
            await loadConversations();
        } catch (e) {
            showDialog('Thông báo', e?.description || 'Rời thất bại');
        }
    }, { danger: true, confirmText: 'Rời' });
}

/* ---------- messages / socket ---------- */
const scrollToBottom = () => {
    nextTick(() => { if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight; });
}

const teardownSocket = () => {
    if (socket) {
        socket.removeAllListeners();
        socket.disconnect();
        socket = null;
    }
    connected.value = false;
    clearTimeout(typingHideTimer);
    otherTyping.value = false;
}

const markConvRead = async () => {
    if (!active.value) return;
    try {
        await markReadByTarget(active.value.conversationId);
        bumpNotifRefresh();
    } catch (e) { /* ignore */ }
}

const openConversation = async (c) => {
    active.value = c;
    activeConversationId.value = c.conversationId;
    messages.value = [];
    otherTyping.value = false;
    otherSeen.value = false;
    otherOnline.value = false;
    showMembers.value = false;
    members.value = [];
    senderMap.value = {};
    renaming.value = false;
    editingId.value = null;
    try {
        const res = await getMessages(c.conversationId);
        messages.value = res?.data?.data || [];
    } catch (e) {
        messages.value = [];
    }
    if (!isDm(c.conversationName)) {
        await loadMembers();
        hydrateSenders();
    }
    scrollToBottom();
    connectSocket(c.conversationId);
    markConvRead();
    const m = { ...unreadByConv.value };
    delete m[c.conversationId];
    unreadByConv.value = m;
}

const connectSocket = (conversationId) => {
    teardownSocket();
    socket = io(SOCKET_URL, {
        transports: ['websocket'],
        query: { conversationID: conversationId, token: getItemLocal(LOCALKEYS.ACCESS_TOKEN) },
    });
    socket.on('connect', () => {
        connected.value = true;
        socket.emit('seen', {});
    });
    socket.on('disconnect', () => { connected.value = false; });
    socket.on('connect_error', () => { connected.value = false; });
    socket.on('get_message', (msg) => {
        if (msg && (msg.content || msg.messageImg)) {
            otherTyping.value = false;
            if (msg.senderId) ensureSender(msg.senderId);
            messages.value.push(msg);
            scrollToBottom();
            bumpConversation(conversationId, { content: msg.content || '', messageImg: msg.messageImg || null, senderId: msg.senderId || null });
            if (document.visibilityState === 'visible' && socket) socket.emit('seen', {});
        }
    });
    socket.on('typing', () => {
        otherTyping.value = true;
        otherOnline.value = true;
        scrollToBottom();
        clearTimeout(typingHideTimer);
        typingHideTimer = setTimeout(() => { otherTyping.value = false; }, 3000);
    });
    socket.on('seen', () => { otherSeen.value = true; otherOnline.value = true; });
    socket.on('message_updated', (dto) => { applyUpdated(dto); });
    socket.on('conversation_renamed', (p) => {
        if (p && p.conversationId) setConvName(p.conversationId, p.conversationName);
    });
    socket.on('member_removed', (p) => {
        if (!p || p.conversationId !== active.value?.conversationId) return;
        if (p.userId === myId) {
            toast?.('Bạn đã bị xoá khỏi nhóm này');
            teardownSocket();
            active.value = null;
            activeConversationId.value = null;
            messages.value = [];
            loadConversations();
        } else {
            members.value = members.value.filter((m) => m.userId !== p.userId);
        }
    });
    socket.on('presence', (p) => {
        otherOnline.value = !!(p && p.online);
        if (p && !p.online) otherSeen.value = false;
    });
}

const onTyping = () => {
    if (!socket || !connected.value) return;
    const now = Date.now();
    if (now - lastTypingEmit > 1800) {
        socket.emit('typing', {});
        lastTypingEmit = now;
    }
}

const onInputFocus = () => { markConvRead(); }

const appendLocal = (extra) => {
    const localId = 'local-' + Date.now();
    messages.value.push({
        messageId: localId,
        senderId: myId,
        sentAt: new Date().toISOString(),
        conversationId: active.value.conversationId,
        content: '',
        recalled: false,
        ...extra,
    });
    otherSeen.value = false;
    scrollToBottom();
    bumpConversation(active.value.conversationId, { content: extra.content || '', messageImg: extra.messageImg || null, senderId: myId });
    return localId;
}

// thay tin tạm bằng tin server đã lưu (có id thật) -> hiện được nút Sửa/Thu hồi ngay
const replaceLocal = (localId, saved) => {
    if (!saved || !saved.messageId) return;
    const i = messages.value.findIndex((x) => x.messageId === localId);
    if (i === -1) return;
    messages.value[i] = {
        ...messages.value[i],
        messageId: saved.messageId,
        content: saved.content != null ? saved.content : messages.value[i].content,
        messageImg: saved.messageImg != null ? saved.messageImg : messages.value[i].messageImg,
        sentAt: saved.sentAt || messages.value[i].sentAt,
        recalled: !!saved.recalled,
    };
}

const sendMessage = async () => {
    if (!active.value) return;
    const text = draft.value.trim();
    const img = pendingImg.value;
    if (!text && !img) return;

    if (img) {
        try {
            const res = await sendMessageRest(active.value.conversationId, { content: text || '', messageImg: img });
            const saved = res?.data?.data;
            const localId = appendLocal({ content: text || '', messageImg: saved?.messageImg || null });
            replaceLocal(localId, saved);
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Gửi ảnh thất bại');
        }
        pendingImg.value = null;
        if (fileEl.value) fileEl.value.value = '';
        draft.value = '';
        return;
    }

    const localId = appendLocal({ content: text });
    draft.value = '';
    if (socket && connected.value) {
        socket.emit('send_message', { content: text }, (saved) => { replaceLocal(localId, saved); });
    }
}

const addDraftEmoji = (e) => { draft.value = (draft.value || '') + e; };

/* ---------- sửa / thu hồi tin nhắn ---------- */
const canModify = (m) => m.senderId === myId && !m.recalled && !String(m.messageId || '').startsWith('local-');

const startEdit = (m) => { editingId.value = m.messageId; editText.value = m.content || ''; };
const cancelEdit = () => { editingId.value = null; editText.value = ''; };

const applyUpdated = (dto) => {
    if (!dto || !dto.messageId) return;
    const i = messages.value.findIndex((x) => x.messageId === dto.messageId);
    if (i === -1) return;
    const next = { ...messages.value[i] };
    if ('recalled' in dto) next.recalled = !!dto.recalled;
    if (next.recalled) {
        next.content = '';
        next.messageImg = null;
    } else {
        if ('content' in dto) next.content = dto.content || '';
        if ('messageImg' in dto) next.messageImg = dto.messageImg;
    }
    messages.value[i] = next;
};

const saveEdit = async (m) => {
    const text = (editText.value || '').trim();
    if (!text) { showDialog?.('Thông báo', 'Nội dung không được để trống'); return; }
    if (text === (m.content || '')) { cancelEdit(); return; }
    try {
        const res = await editMessage(m.messageId, text);
        applyUpdated(res?.data?.data || { messageId: m.messageId, content: text });
        cancelEdit();
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Sửa tin nhắn thất bại');
    }
};

const confirmRecall = (m) => {
    openConfirm?.('Thu hồi tin nhắn', 'Thu hồi tin nhắn này với mọi người?', async () => {
        try {
            const res = await recallMessage(m.messageId);
            applyUpdated(res?.data?.data || { messageId: m.messageId, recalled: true, content: '' });
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Thu hồi thất bại');
        }
    }, { danger: true, confirmText: 'Thu hồi' });
};

const pickImg = () => fileEl.value?.click();
const onPickImg = (e) => { pendingImg.value = e.target.files[0] || null; };

/* ---------- mở từ query ?c= ---------- */
const openFromQuery = () => {
    const q = router.currentRoute.value.query;
    if (q.c) {
        const id = String(q.c);
        if (q.name) dmNames.value = { ...dmNames.value, [id]: String(q.name) };
        const inList = conversations.value.find((c) => c.conversationId === id);
        openConversation(inList || {
            conversationId: id,
            conversationName: q.name ? String(q.name) : 'Tin nhắn riêng',
        });
    }
}

let sidebarTimer = null;

onMounted(async () => {
    loadFriends();
    await loadConversations();
    openFromQuery();
    sidebarTimer = setInterval(() => { loadConversations(); }, 20000);
});

watch(() => router.currentRoute.value.query.c, (c) => {
    if (c && c !== active.value?.conversationId) openFromQuery();
});

watch(notifRefreshTick, () => { loadUnread(); });

onBeforeUnmount(() => {
    teardownSocket();
    clearInterval(sidebarTimer);
    activeConversationId.value = null;
});
</script>

<style scoped>
.typing-dot {
    width: 6px;
    height: 6px;
    border-radius: 999px;
    background: var(--text-muted);
    display: inline-block;
    animation: typing-bounce 1.2s infinite ease-in-out;
}
.typing-dot:nth-child(2) { animation-delay: .15s; }
.typing-dot:nth-child(3) { animation-delay: .3s; }
@keyframes typing-bounce {
    0%, 60%, 100% { transform: translateY(0); opacity: .5; }
    30% { transform: translateY(-4px); opacity: 1; }
}
</style>
