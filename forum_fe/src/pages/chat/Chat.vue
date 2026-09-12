<template>
    <div class="page page--wide">
        <div class="card card--flush flex" style="height: 74vh;">
            <!-- Sidebar -->
            <div class="w-[36%] min-w-[260px] border-r flex flex-col">
                <div class="p-3 border-b flex flex-col gap-3">
                    <!-- Tạo nhóm mới -->
                    <div>
                        <div class="flex items-center justify-between">
                            <span class="text-sm font-bold">Tạo nhóm mới</span>
                            <button class="icon-btn icon-btn--sm" :title="showCreate ? 'Thu gọn' : 'Tạo nhóm mới'" @click="toggleCreate">
                                <AppIcon :name="showCreate ? 'chevron-up' : 'plus'" :size="18" />
                            </button>
                        </div>
                        <div v-if="showCreate" class="mt-2 flex flex-col gap-2">
                            <input type="text" class="field" v-model="createName" placeholder="Tên nhóm" />
                            <input type="text" class="field" v-model="memberQuery" placeholder="Lọc bạn bè để thêm…" @input="searchMembers" @keyup.enter="searchMembers" />
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
                            <button type="button" class="sign-btn" @click="doCreateGroup">Tạo nhóm</button>
                        </div>
                    </div>

                    <!-- Tìm nhóm để tham gia -->
                    <div>
                        <span class="text-sm font-bold">Tìm nhóm để tham gia</span>
                        <div class="flex gap-2 mt-1">
                            <input type="text" class="field flex-1" v-model="searchName" placeholder="Tên nhóm…" @keyup.enter="handleSearch" />
                            <button class="sign-btn flex-none" title="Tìm nhóm" @click="handleSearch">
                                <AppIcon name="search" :size="16" />
                            </button>
                        </div>
                        <div v-if="searchResults.length" class="mt-1 border rounded-lg divide-y">
                            <div v-for="c in searchResults" :key="c.conversationId"
                                class="flex items-center justify-between px-2 py-1.5 text-sm">
                                <span class="truncate">{{ displayName(c) }}</span>
                                <button type="button" class="sign-btn sign-btn--quiet" @click="() => handleJoin(c)">Tham gia</button>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Cuộc trò chuyện -->
                <div class="flex-1 overflow-y-auto">
                    <div class="text-sm font-bold px-3 pt-3 pb-1">Cuộc trò chuyện</div>
                    <div v-if="conversations.length === 0" class="muted p-3 text-sm">Chưa có cuộc trò chuyện nào</div>
                    <div v-for="c in conversations" :key="c.conversationId"
                        class="px-3 py-2.5 cursor-pointer border-b transition hover:bg-gray-50"
                        :class="{ 'bg-[var(--brand-soft)]': active?.conversationId === c.conversationId }"
                        @click="() => openConversation(c)">
                        <div class="flex items-center gap-1.5">
                            <span class="relative flex-none">
                                <img v-if="convAvatar(c)" :src="convAvatar(c)" :key="convAvatar(c)"
                                    class="size-7 rounded-full object-cover bg-gray-100"
                                    @load="(e) => e.target.style.display = ''" @error="(e) => e.target.style.display = 'none'" />
                                <span v-else-if="isDm(c.conversationName)" class="avatar-fallback size-7 text-xs">{{ (displayName(c) || '?')[0] }}</span>
                                <span v-else class="conv-avatar size-7"><AppIcon name="users" :size="15" /></span>
                                <span v-if="dmPeerOnline(c)"
                                    class="absolute -bottom-0.5 -right-0.5 size-2.5 rounded-full bg-[var(--turmeric)] ring-2 ring-[var(--surface)]"></span>
                            </span>
                            <span class="font-semibold text-sm truncate flex-1"
                                :class="{ 'font-bold': unreadByConv[c.conversationId] }">{{ displayName(c) }}</span>
                            <span v-if="c.muted" class="flex-none muted" title="Đã tắt thông báo"><AppIcon name="bell-off" :size="13" /></span>
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
                        <span class="relative flex-none">
                            <img v-if="convAvatar(active)" :src="convAvatar(active)" :key="convAvatar(active)"
                                class="size-9 rounded-full object-cover bg-gray-100"
                                @load="(e) => e.target.style.display = ''" @error="(e) => e.target.style.display = 'none'" />
                            <span v-else-if="isDm(active.conversationName)" class="avatar-fallback size-9">{{ (displayName(active) || '?')[0] }}</span>
                            <span v-else class="conv-avatar size-9"><AppIcon name="users" :size="18" /></span>
                            <button v-if="!isDm(active.conversationName)"
                                class="conv-avatar__edit"
                                title="Đổi ảnh nhóm" @click="groupAvatarInput?.click()"><AppIcon name="camera" :size="11" /></button>
                            <input ref="groupAvatarInput" type="file" accept="image/*" class="hidden" @change="onGroupAvatar" />
                        </span>
                        <div class="min-w-0 flex-1">
                            <div v-if="renaming" class="flex items-center gap-1">
                                <input type="text" class="field flex-1" v-model="renameText" @keyup.enter="saveRename" />
                                <button class="icon-btn icon-btn--sm" title="Lưu tên" @click="saveRename"><AppIcon name="check" :size="17" /></button>
                                <button class="icon-btn icon-btn--sm" title="Huỷ" @click="renaming = false"><AppIcon name="x" :size="17" /></button>
                            </div>
                            <div v-else class="font-bold truncate flex items-center gap-1">
                                <span class="truncate">{{ displayName(active) }}</span>
                                <button v-if="!isDm(active.conversationName)" class="icon-btn icon-btn--sm"
                                    title="Đổi tên nhóm" @click="startRename"><AppIcon name="edit" :size="14" /></button>
                            </div>
                            <div class="text-xs" :class="statusClass">{{ statusText }}</div>
                        </div>
                        <button v-if="!isDm(active.conversationName)" class="icon-btn"
                            :title="showMembers ? 'Ẩn thành viên' : 'Xem thành viên'"
                            @click="toggleMembers">
                            <AppIcon :name="showMembers ? 'chevron-up' : 'users'" :size="18" />
                        </button>
                        <button v-if="!isDm(active.conversationName)" class="icon-btn"
                            title="Thêm thành viên" @click="openAddMember">
                            <AppIcon name="plus" :size="18" />
                        </button>
                        <button class="icon-btn"
                            :title="active.muted ? 'Bật lại thông báo' : 'Tắt thông báo'" @click="toggleMute">
                            <AppIcon :name="active.muted ? 'bell-off' : 'bell'" :size="18" />
                        </button>
                        <button type="button" class="sign-btn sign-btn--quiet sign-btn--quiet-danger" @click="handleLeave">Rời</button>
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
                                <input type="text" class="field flex-1" v-model="editText" @keyup.enter="saveEdit(m)" />
                                <button class="icon-btn icon-btn--sm" title="Lưu" @click="saveEdit(m)"><AppIcon name="check" :size="17" /></button>
                                <button class="icon-btn icon-btn--sm" title="Huỷ" @click="cancelEdit"><AppIcon name="x" :size="17" /></button>
                            </div>

                            <template v-else>
                                <div class="flex items-end gap-1 relative"
                                    :class="m.senderId === myId ? 'flex-row' : 'flex-row-reverse'">
                                    <!-- nút sửa / thu hồi / thả cảm xúc -->
                                    <div v-if="!m.recalled && !String(m.messageId || '').startsWith('local-')"
                                        class="flex-none flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition">
                                        <button class="text-sm px-1 hover:scale-125 transition" title="Thả cảm xúc"
                                            @click="reactPickFor = reactPickFor === m.messageId ? null : m.messageId">🙂</button>
                                        <template v-if="canModify(m)">
                                            <button class="text-[11px] muted hover:text-[var(--text)] px-1" @click="startEdit(m)">Sửa</button>
                                            <button class="text-[11px] muted hover:text-[var(--danger)] px-1" @click="confirmRecall(m)">Thu hồi</button>
                                        </template>
                                    </div>
                                    <div
                                        class="px-3 py-2 rounded-2xl text-sm"
                                        :class="[
                                            m.senderId === myId ? 'bg-[var(--ink)] text-white' : 'bg-[var(--surface)] text-[var(--text)] border-2 border-[var(--line-soft)]',
                                            m.recalled ? 'italic opacity-70' : '',
                                            (m.messageImg && !m.content && !m.recalled) ? 'msg-bubble--media' : ''
                                        ]">
                                        <span v-if="m.recalled">Tin nhắn đã được thu hồi</span>
                                        <template v-else>
                                            <span v-if="m.content"><template v-for="(p, pi) in msgParts(m.content)" :key="pi"><span
                                                v-if="p.mention" class="font-semibold underline cursor-pointer"
                                                @click="() => router.push('/user/' + p.id)">@{{ p.mention }}</span><template v-else>{{ p.t }}</template></template></span>
                                            <img v-if="m.messageImg && !String(m.messageImg).includes('null')"
                                                :src="IMAGE_BASE + m.messageImg" class="msg-img"
                                                :class="{ 'mt-1.5': !!m.content }"
                                                alt="Ảnh đã gửi"
                                                @click="openLightbox(IMAGE_BASE + m.messageImg)" />
                                        </template>
                                    </div>

                                    <!-- bảng chọn emoji -->
                                    <div v-if="reactPickFor === m.messageId"
                                        class="absolute -top-9 z-30 flex gap-0.5 bg-white border rounded-full shadow px-1.5 py-1"
                                        :class="m.senderId === myId ? 'right-0' : 'left-0'">
                                        <button v-for="e in REACT_EMOJIS" :key="e"
                                            class="text-lg leading-none hover:scale-125 transition"
                                            @click="toggleMsgReaction(m, e)">{{ e }}</button>
                                    </div>
                                </div>

                                <!-- chips cảm xúc -->
                                <div v-if="m.reactions && Object.keys(m.reactions).length"
                                    class="flex gap-1 mt-0.5 flex-wrap"
                                    :class="m.senderId === myId ? 'justify-end' : 'justify-start'">
                                    <button v-for="(cnt, e) in m.reactions" :key="e"
                                        class="text-[11px] px-1.5 py-0.5 rounded-full border transition"
                                        :class="m.myReaction === e ? 'bg-[var(--brand-soft)] border-[var(--brand)]' : 'bg-white border-[var(--border)]'"
                                        @click="toggleMsgReaction(m, e)">{{ e }} {{ cnt }}</button>
                                </div>

                                <span class="text-[11px] muted mt-0.5">
                                    <template v-if="idx === lastMineIndex && !m.recalled">{{ otherSeen ? 'Đã xem · ' : 'Đã gửi · ' }}</template>{{ formatTime(m.sentAt) }}
                                </span>
                            </template>
                        </div>

                        <div v-if="otherTyping" class="self-start flex items-center gap-1 px-3 py-2 bg-[var(--surface)] border-2 border-[var(--line-soft)] rounded-2xl">
                            <span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span>
                        </div>
                    </div>

                    <div class="p-3 border-t">
                      <!-- Ảnh đang chờ gửi: xem trước + tên tệp + nút bỏ chọn -->
                      <div v-if="pendingImg" class="pending-img">
                          <img v-if="pendingImgUrl" :src="pendingImgUrl" class="pending-img__thumb" alt="" />
                          <span class="pending-img__name">{{ pendingImg.name }}</span>
                          <span class="muted flex-none">{{ formatBytes(pendingImg.size) }}</span>
                          <button class="pending-img__x" title="Bỏ ảnh này" @click="clearPendingImg"><AppIcon name="x" :size="15" /></button>
                      </div>

                      <div class="flex items-center gap-2">
                        <button class="icon-btn flex-none" title="Đính kèm ảnh" @click="pickImg">
                            <AppIcon name="image" :size="19" />
                        </button>
                        <input ref="fileEl" type="file" accept="image/*" class="hidden" @change="onPickImg" />
                        <EmojiPicker direction="up" @pick="addDraftEmoji" />
                        <div class="flex-1 relative">
                            <input type="text" class="field w-full" v-model="draft" placeholder="Nhập tin nhắn… (@ để nhắc tên)" @keyup.enter="sendMessage" @input="onDraftInput" @focus="onInputFocus" @blur="onDraftBlur" />
                            <div v-if="mentionOpen && mentionResults.length"
                                class="absolute z-40 left-0 right-0 bottom-full mb-1 bg-[var(--surface)] border rounded-lg shadow-lg overflow-hidden">
                                <button v-for="u in mentionResults" :key="u.userId" type="button"
                                    class="w-full text-left px-3 py-2 text-sm hover:bg-[var(--brand-soft)] truncate flex items-center gap-2"
                                    @mousedown.prevent="pickChatMention(u)">
                                    <span class="avatar-fallback size-6 text-xs">{{ (u.fullName || '?')[0] }}</span>
                                    <span class="truncate">{{ u.fullName }}</span>
                                </button>
                            </div>
                        </div>
                        <button type="button" class="sign-btn" @click="sendMessage">Gửi</button>
                      </div>
                    </div>
                </template>
            </div>
        </div>

        <!-- Thêm thành viên vào nhóm đang mở (chỉ bạn bè) -->
        <AppModal title="Thêm bạn bè vào nhóm" v-model:open="showAddMember" :width="460">
            <div class="flex flex-col gap-2">
                <input type="text" class="field" v-model="addQuery" placeholder="Lọc bạn bè…" @input="searchAdd" @keyup.enter="searchAdd" />
                <div v-if="addResults.length" class="border rounded-lg divide-y max-h-72 overflow-y-auto">
                    <div v-for="u in addResults" :key="u.userId" class="flex items-center justify-between px-2 py-2 text-sm">
                        <span class="truncate">{{ u.fullName }}</span>
                        <button type="button" class="sign-btn sign-btn--quiet" @click="() => doAddMember(u)">Thêm</button>
                    </div>
                </div>
                <div v-else class="muted text-sm">
                    {{ friends.length ? 'Không có bạn bè phù hợp' : 'Bạn chưa có bạn bè nào để thêm' }}
                </div>
            </div>
        </AppModal>
    </div>
</template>

<script setup>
import AppIcon from '@/components/AppIcon.vue';
import AppModal from '@/components/ui/AppModal.vue';
import { onMounted, onBeforeUnmount, nextTick, ref, computed, inject, watch } from 'vue';
import { useRouter } from 'vue-router';
import { io } from 'socket.io-client';
import {
    getMyConversations, searchConversations, getMessages,
    createConversation, joinConversation, leaveConversation, sendMessageRest, addMember, getMembers,
    editMessage, recallMessage, renameConversation, removeMember,
    reactMessage, unreactMessage, setConversationAvatar, muteConversation,
} from '@/apis/chat';
import { getUserInfo } from '@/apis/user';
import { getFriends } from '@/apis/friend';
import { markReadByTarget, getNotifications } from '@/apis/notification';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';
import { activeConversationId, bumpNotifRefresh, notifRefreshTick, onlinePeers, avatarUpdates } from '@/storages/appState';
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
const dmAvatars = ref({});   // conversationId -> avtUrl của người kia trong DM

// Ảnh đại diện hiển thị cho 1 hội thoại: DM -> avatar người kia; nhóm -> avatar nhóm
const convAvatar = (c) => {
    if (!c) return '';
    if (isDm(c.conversationName)) {
        const a = dmAvatars.value[c.conversationId];
        return a && !String(a).includes('null') ? IMAGE_BASE + a : '';
    }
    return c.avatarUrl ? IMAGE_BASE + c.avatarUrl : '';
};

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
const REACT_EMOJIS = ['👍', '❤️', '😆', '😮', '😢', '😡'];
const reactPickFor = ref(null);

const toggleMsgReaction = async (m, emoji) => {
    reactPickFor.value = null;
    if (!m?.messageId || String(m.messageId).startsWith('local-')) return;
    const removing = m.myReaction === emoji;
    try {
        const res = removing ? await unreactMessage(m.messageId) : await reactMessage(m.messageId, emoji);
        m.reactions = res?.data?.data || {};
        m.myReaction = removing ? null : emoji;
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Không thả được cảm xúc');
    }
};

// đổi tên nhóm
const renaming = ref(false);
const renameText = ref('');
const groupAvatarInput = ref(null);

const toggleMute = async () => {
    if (!active.value?.conversationId) return;
    const next = !active.value.muted;
    try {
        await muteConversation(active.value.conversationId, next);
        active.value = { ...active.value, muted: next };
        const i = conversations.value.findIndex((c) => c.conversationId === active.value.conversationId);
        if (i >= 0) conversations.value[i] = { ...conversations.value[i], muted: next };
        toast?.(next ? 'Đã tắt thông báo hội thoại' : 'Đã bật lại thông báo');
    } catch (err) {
        showDialog?.('Thông báo', err?.description || 'Không đổi được cài đặt thông báo');
    }
};

const onGroupAvatar = async (e) => {
    const f = e.target.files[0];
    e.target.value = '';
    if (!f || !active.value?.conversationId) return;
    try {
        const url = (await setConversationAvatar(active.value.conversationId, f))?.data?.data?.avatarUrl;
        if (url) {
            active.value = { ...active.value, avatarUrl: url };
            await loadConversations();
        }
        toast?.('Đã cập nhật ảnh nhóm');
    } catch (err) {
        showDialog?.('Thông báo', err?.description || 'Cập nhật ảnh nhóm thất bại');
    }
};

// Ai đó đổi avatar (realtime) -> cập nhật avatar hiển thị trong DM tương ứng
watch(avatarUpdates, (m) => {
    for (const c of conversations.value) {
        if (!isDm(c.conversationName)) continue;
        const other = otherIdFromDm(c.conversationName);
        if (other && m[other] !== undefined) {
            dmAvatars.value = { ...dmAvatars.value, [c.conversationId]: m[other] };
        }
    }
}, { deep: true });

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

const activeDmPeerOnline = computed(() => {
    if (!active.value || !isDm(active.value.conversationName)) return false;
    if (otherOnline.value) return true;
    const other = otherIdFromDm(active.value.conversationName);
    return !!other && onlinePeers.value.has(other);
});
const statusText = computed(() => {
    if (!active.value) return '';
    if (isDm(active.value.conversationName)) {
        return activeDmPeerOnline.value ? '● Đang hoạt động' : '○ Không hoạt động';
    }
    return connected.value ? '● Đã kết nối' : '○ Mất kết nối';
});
const statusClass = computed(() => {
    const on = isDm(active.value?.conversationName) ? activeDmPeerOnline.value : connected.value;
    return on ? 'status-on' : 'muted';
});

const otherIdFromDm = (name) => {
    const raw = name.slice(3);
    const parts = raw.split(name.startsWith('dm:') ? ':' : '_');
    return parts.find((p) => p && p !== myId) || null;
}

// chấm xanh cho DM khi người kia đang online (từ friend_presence toàn cục)
const dmPeerOnline = (c) => {
    if (!c || !isDm(c.conversationName)) return false;
    const other = otherIdFromDm(c.conversationName);
    return !!other && onlinePeers.value.has(other);
}

const resolveDmNames = async () => {
    const targets = conversations.value.filter(
        (c) => isDm(c.conversationName) && !dmNames.value[c.conversationId] && otherIdFromDm(c.conversationName)
    );
    // chạy song song thay vì tuần tự từng DM
    await Promise.all(targets.map(async (c) => {
        const otherId = otherIdFromDm(c.conversationName);
        try {
            const u = (await getUserInfo(otherId))?.data?.data || {};
            dmNames.value = { ...dmNames.value, [c.conversationId]: u.fullName || 'Tin nhắn riêng' };
            dmAvatars.value = { ...dmAvatars.value, [c.conversationId]: u.avtUrl || '' };
        } catch (e) { /* ignore */ }
    }));
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
    let body = (c.lastMessage || '').replace(/@\[([^\]]+)\]\([0-9a-fA-F-]{8,}\)/g, '@$1');
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
        const data = (await getFriends(myId))?.data?.data || {};
        // backend đã trả kèm tên -> khỏi gọi /user/{id} cho từng người
        if (Array.isArray(data.users) && data.users.length) {
            friends.value = data.users.map((u) => ({ userId: u.userId, fullName: u.fullName || u.userId }));
            return;
        }
        const ids = data.userId || [];
        friends.value = await Promise.all(ids.map(async (id) => {
            try {
                const u = await getUserInfo(id);
                return { userId: id, fullName: u?.data?.data?.fullName || id };
            } catch (e) {
                return { userId: id, fullName: id };
            }
        }));
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
    nextTick(() => {
        const el = listEl.value;
        if (!el) return;
        el.scrollTop = el.scrollHeight;
        // nextTick chỉ đảm bảo DOM đã cập nhật, KHÔNG đảm bảo ảnh đã tải. Ảnh
        // load xong sau đó làm khung cao thêm -> view kẹt lại phía trên tin nhắn
        // vừa gửi. Cuộn lại một lần nữa khi từng ảnh chưa tải xong hoàn tất.
        el.querySelectorAll('img').forEach((img) => {
            if (img.complete) return;
            const again = () => { if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight; };
            img.addEventListener('load', again, { once: true });
            img.addEventListener('error', again, { once: true });
        });
    });
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
    socket.on('message_reaction', (p) => {
        if (!p?.messageId) return;
        const msg = messages.value.find((x) => x.messageId === p.messageId);
        if (msg) msg.reactions = p.reactions || {};
    });
    socket.on('conversation_avatar', (p) => {
        if (!p?.conversationId) return;
        if (active.value?.conversationId === p.conversationId) active.value.avatarUrl = p.avatarUrl;
        const c = conversations.value.find((x) => x.conversationId === p.conversationId);
        if (c) c.avatarUrl = p.avatarUrl;
    });
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

/* ---------- @nhắc tên trong tin nhắn nhóm ---------- */
const mentionOpen = ref(false);
const mentionResults = ref([]);
const chatPicked = ref([]);            // [{ name, id }]
const MENTION_TAIL = /@([^\s@[\]]{0,30})$/;

const mentionCandidates = () => {
    // chỉ nhắc trong nhóm; lấy từ danh sách thành viên (senderMap) + members
    if (!active.value || isDm(active.value.conversationName)) return [];
    const seen = new Set();
    const out = [];
    for (const m of members.value) {
        if (m.userId === myId || seen.has(m.userId)) continue;
        seen.add(m.userId); out.push({ userId: m.userId, fullName: m.fullName });
    }
    for (const [uid, v] of Object.entries(senderMap.value)) {
        if (uid === myId || seen.has(uid)) continue;
        seen.add(uid); out.push({ userId: uid, fullName: v.fullName });
    }
    return out;
};

const onDraftInput = () => {
    onTyping();
    const v = draft.value || '';
    const mm = v.match(MENTION_TAIL);
    if (!mm || (active.value && isDm(active.value.conversationName))) { mentionOpen.value = false; return; }
    const q = mm[1].toLowerCase();
    mentionResults.value = mentionCandidates()
        .filter((u) => (u.fullName || '').toLowerCase().includes(q))
        .slice(0, 6);
    mentionOpen.value = mentionResults.value.length > 0;
};
const onDraftBlur = () => { setTimeout(() => { mentionOpen.value = false; }, 120); };

const pickChatMention = (u) => {
    draft.value = (draft.value || '').replace(MENTION_TAIL, `@${u.fullName} `);
    if (!chatPicked.value.some((p) => p.id === u.userId)) chatPicked.value.push({ name: u.fullName, id: u.userId });
    mentionOpen.value = false;
};
const resolveChatMentions = (text) => {
    let out = text || '';
    for (const { name, id } of chatPicked.value) {
        const tag = '@' + name;
        if (out.includes(tag)) out = out.split(tag).join(`@[${name}](${id})`);
    }
    return out;
};

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
    const raw = draft.value.trim();
    const text = resolveChatMentions(raw);
    const img = pendingImg.value;
    if (!raw && !img) return;

    if (img) {
        try {
            const res = await sendMessageRest(active.value.conversationId, { content: text || '', messageImg: img });
            const saved = res?.data?.data;
            const localId = appendLocal({ content: text || '', messageImg: saved?.messageImg || null });
            replaceLocal(localId, saved);
        } catch (e) {
            showDialog?.('Thông báo', e?.description || 'Gửi ảnh thất bại');
        }
        clearPendingImg();
        draft.value = '';
        chatPicked.value = [];
        mentionOpen.value = false;
        return;
    }

    const localId = appendLocal({ content: text });
    draft.value = '';
    chatPicked.value = [];
    mentionOpen.value = false;
    if (socket && connected.value) {
        socket.emit('send_message', { content: text }, (saved) => { replaceLocal(localId, saved); });
    }
}

const addDraftEmoji = (e) => { draft.value = (draft.value || '') + e; };

// tách @[Tên](id) trong tin nhắn để hiển thị @Tên
const msgParts = (text) => {
    const re = /@\[([^\]]+)\]\(([0-9a-fA-F-]{8,})\)/g;
    const out = [];
    let last = 0, m;
    while ((m = re.exec(text || '')) !== null) {
        if (m.index > last) out.push({ t: text.slice(last, m.index) });
        out.push({ mention: m[1], id: m[2] });
        last = m.index + m[0].length;
    }
    if (last < (text || '').length) out.push({ t: text.slice(last) });
    return out;
};

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

/* Xem trước ảnh đang chờ gửi. objectURL phải revoke tay, nếu không mỗi lần chọn
   ảnh lại giữ nguyên blob trong bộ nhớ cho tới khi rời trang. */
const pendingImgUrl = ref('');
const setPendingImg = (file) => {
    if (pendingImgUrl.value) URL.revokeObjectURL(pendingImgUrl.value);
    pendingImgUrl.value = file ? URL.createObjectURL(file) : '';
    pendingImg.value = file;
};
const onPickImg = (e) => setPendingImg(e.target.files[0] || null);
const clearPendingImg = () => {
    setPendingImg(null);
    if (fileEl.value) fileEl.value.value = '';
};

const formatBytes = (n) => {
    if (!n && n !== 0) return '';
    return n < 1024 * 1024 ? Math.round(n / 1024) + ' KB' : (n / 1024 / 1024).toFixed(1) + ' MB';
};

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
    if (pendingImgUrl.value) URL.revokeObjectURL(pendingImgUrl.value);
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
