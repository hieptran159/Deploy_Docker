<template>
    <div ref="rootEl" class="emoji-picker">
        <button
            type="button"
            class="emoji-trigger"
            :class="{ 'is-open': open }"
            title="Chèn biểu tượng cảm xúc"
            @click.stop="toggle"
        >
            😊
        </button>

        <div
            v-if="open"
            class="emoji-pop"
            :class="direction === 'down' ? 'emoji-pop--down' : 'emoji-pop--up'"
            @click.stop
        >
            <div class="emoji-tabs">
                <button
                    v-for="(g, key) in groups"
                    :key="key"
                    type="button"
                    class="emoji-tab"
                    :class="{ active: activeGroup === key }"
                    @click="activeGroup = key"
                >
                    {{ g.icon }}
                </button>
            </div>
            <div class="emoji-grid">
                <button
                    v-for="e in groups[activeGroup].list"
                    :key="e"
                    type="button"
                    class="emoji-cell"
                    @click="pick(e)"
                >
                    {{ e }}
                </button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';

defineProps({
    // 'up' = bảng mở lên trên (ô nhập ở dưới, như khung chat)
    // 'down' = bảng mở xuống dưới (ô nhập ở trên, như khung bình luận)
    direction: { type: String, default: 'up' },
});
const emit = defineEmits(['pick']);

const open = ref(false);
const rootEl = ref(null);
const activeGroup = ref('smileys');

const groups = {
    smileys: {
        icon: '😀',
        list: ['😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '🙂', '🙃', '😉', '😊', '😇', '🥰', '😍', '😘', '😗', '😚', '😙', '😋', '😛', '😜', '🤪', '😝', '🤗', '🤭', '🤫', '🤔', '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '😌', '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕', '🥴', '😵', '🤯', '🤠', '🥳', '😎', '🤓', '🧐', '😕', '🙁', '😮', '😯', '😲', '😳', '🥺', '😨', '😰', '😥', '😢', '😭', '😱', '😖', '😞', '😓', '😩', '😫', '🥱', '😤', '😡', '😠', '🤬'],
    },
    gestures: {
        icon: '👍',
        list: ['👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆', '👇', '☝️', '✋', '🤚', '🖐️', '🖖', '👋', '🤝', '🙏', '✍️', '💪', '👏', '🙌', '👐', '🤲', '🤦', '🤷', '💁', '🙆', '🙅', '🙋', '🤳', '👀'],
    },
    hearts: {
        icon: '❤️',
        list: ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '💌', '💋', '🌹', '🌸', '💐', '🔥'],
    },
    animals: {
        icon: '🐶',
        list: ['🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼', '🐨', '🐯', '🦁', '🐮', '🐷', '🐸', '🐵', '🐔', '🐧', '🐦', '🐤', '🦆', '🦉', '🦄', '🐝', '🦋', '🐢', '🐍', '🐙', '🦑', '🦀', '🐬', '🐳', '🐟', '🐊', '🐘', '🐎', '🐐', '🐑', '🐄', '🌵', '🌲', '🌴', '🌱', '🍀', '🍁', '🍂', '🍄'],
    },
    food: {
        icon: '🍔',
        list: ['🍎', '🍐', '🍊', '🍋', '🍌', '🍉', '🍇', '🍓', '🫐', '🍒', '🍑', '🥭', '🍍', '🥥', '🥝', '🍅', '🥑', '🍆', '🥕', '🌽', '🌶️', '🥒', '🥦', '🧄', '🧅', '🥜', '🍞', '🥐', '🥖', '🥨', '🥞', '🧇', '🧀', '🍖', '🍗', '🥩', '🥓', '🍔', '🍟', '🍕', '🌭', '🥪', '🌮', '🌯', '🥗', '🍿', '🍜', '🍲', '🍣', '🍱', '🍤', '🍚', '🍦', '🍰', '🎂', '🧁', '🍫', '🍬', '🍭', '🍯', '☕', '🍵', '🥤', '🍺', '🍻', '🥂', '🍷', '🥃'],
    },
    activities: {
        icon: '⚽',
        list: ['⚽', '🏀', '🏈', '⚾', '🎾', '🏐', '🏉', '🎱', '🏓', '🏸', '🥅', '🏒', '🏑', '🏏', '⛳', '🏹', '🎣', '🥊', '🥋', '⛸️', '🎿', '🏂', '🏋️', '🤸', '🤺', '🏌️', '🧘', '🏄', '🏊', '🚣', '🚴', '🚵', '🎯', '🎮', '🎲', '🎳', '🎸', '🎺', '🎻', '🥁', '🎤', '🎧', '🎬', '🏆', '🥇', '🥈', '🥉', '🏅'],
    },
    travel: {
        icon: '✈️',
        list: ['🚗', '🚕', '🚙', '🚌', '🏎️', '🚓', '🚑', '🚒', '🚚', '🚜', '🛵', '🏍️', '🚲', '✈️', '🚀', '🛸', '🚁', '⛵', '🚤', '🛳️', '⚓', '🚂', '🚆', '🚇', '🗺️', '🗿', '🗽', '🗼', '🏰', '🏯', '🎡', '🎢', '🎠', '🏖️', '🏝️', '⛰️', '🏔️', '🌋', '🏕️', '🌅', '🌄', '🌇', '🌃', '🌌', '🌉'],
    },
    symbols: {
        icon: '✨',
        list: ['✨', '⭐', '🌟', '💫', '⚡', '🔥', '💥', '🌈', '☀️', '⛅', '🌧️', '⛈️', '❄️', '☃️', '💧', '🌊', '✅', '❌', '❓', '❗', '💯', '🔔', '🎉', '🎊', '🎁', '🎈', '🚩', '💤', '💦', '💨', '💣', '💬', '💭', '♻️', '⚠️', '🆗', '🆒', '🆙', '🔴', '🟠', '🟡', '🟢', '🔵', '🟣', '⚫', '⚪'],
    },
};

function toggle() {
    open.value = !open.value;
}
function pick(e) {
    emit('pick', e);
}
function onDocClick(ev) {
    if (open.value && rootEl.value && !rootEl.value.contains(ev.target)) open.value = false;
}
function onKey(ev) {
    if (ev.key === 'Escape') open.value = false;
}

onMounted(() => {
    document.addEventListener('click', onDocClick);
    document.addEventListener('keydown', onKey);
});
onBeforeUnmount(() => {
    document.removeEventListener('click', onDocClick);
    document.removeEventListener('keydown', onKey);
});
</script>

<style scoped>
.emoji-picker {
    position: relative;
    flex: none;
}
.emoji-trigger {
    width: 34px;
    height: 34px;
    border-radius: 999px;
    font-size: 18px;
    line-height: 1;
    border: var(--line-w) solid var(--ink);
    background: var(--surface);
    transition: background .15s, border-color .15s;
}
.emoji-trigger:hover,
.emoji-trigger.is-open {
    background: var(--brand-soft);
    border-color: var(--brand);
}
.emoji-pop {
    position: absolute;
    right: 0;
    z-index: 50;
    width: 268px;
    background: var(--surface);
    border: var(--line-w) solid var(--ink);
    border-radius: var(--radius);
    box-shadow: var(--lift);
    padding: 8px;
}
.emoji-pop--up {
    bottom: calc(100% + 8px);
}
.emoji-pop--down {
    top: calc(100% + 8px);
}
.emoji-tabs {
    display: flex;
    gap: 2px;
    border-bottom: 1px solid var(--border);
    padding-bottom: 6px;
    margin-bottom: 6px;
}
.emoji-tab {
    flex: 1;
    font-size: 15px;
    line-height: 1;
    padding: 4px 0;
    border-radius: 8px;
    opacity: .55;
    transition: opacity .15s, background .15s;
}
.emoji-tab:hover {
    opacity: 1;
}
.emoji-tab.active {
    opacity: 1;
    background: var(--brand-soft);
}
.emoji-grid {
    display: grid;
    grid-template-columns: repeat(8, 1fr);
    gap: 2px;
    max-height: 200px;
    overflow-y: auto;
}
.emoji-cell {
    font-size: 19px;
    line-height: 1;
    padding: 4px 0;
    border-radius: 8px;
    transition: background .12s;
}
.emoji-cell:hover {
    background: var(--brand-soft);
}
</style>
