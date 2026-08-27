<template>
    <div class="reaction-bar" @mouseenter="openSoon" @mouseleave="closeSoon">
        <button
            type="button"
            class="reaction-trigger"
            :class="{ 'is-active': !!myReaction }"
            :style="myReaction ? { color: current.color } : {}"
            @click="onTriggerClick"
        >
            <span class="reaction-trigger__emoji">{{ current.emoji }}</span>
            <span>{{ current.label }}</span>
        </button>

        <span v-if="total > 0" class="reaction-summary" :title="summaryTitle">
            <span class="reaction-summary__emojis">{{ summaryEmojis }}</span>
            <b>{{ total }}</b>
        </span>

        <div v-if="open" class="reaction-pop" @mouseenter="cancelClose" @mouseleave="closeSoon">
            <button
                v-for="r in REACTIONS"
                :key="r.key"
                type="button"
                class="reaction-choice"
                :class="{ 'is-mine': r.key === myReaction }"
                :title="r.label"
                @click="pick(r.key)"
            >{{ r.emoji }}</button>
        </div>
    </div>
</template>

<script setup>
import { computed, ref, onBeforeUnmount } from 'vue';

const props = defineProps({
    myReaction: { type: String, default: null },
    counts: { type: Object, default: () => ({}) },
});
const emit = defineEmits(['react', 'unreact']);

const REACTIONS = [
    { key: 'LIKE', emoji: '👍', label: 'Thích', color: 'var(--brand)' },
    { key: 'LOVE', emoji: '❤️', label: 'Yêu thích', color: '#e0245e' },
    { key: 'HAHA', emoji: '😆', label: 'Haha', color: '#f7b125' },
    { key: 'WOW', emoji: '😮', label: 'Wow', color: '#f7b125' },
    { key: 'SAD', emoji: '😢', label: 'Buồn', color: '#f7b125' },
    { key: 'ANGRY', emoji: '😡', label: 'Phẫn nộ', color: '#e0245e' },
];
const META = Object.fromEntries(REACTIONS.map((r) => [r.key, r]));
const GREY = { emoji: '👍', label: 'Thích', color: '' };

const current = computed(() => (props.myReaction && META[props.myReaction]) || GREY);
const total = computed(() => Object.values(props.counts || {}).reduce((a, b) => a + (Number(b) || 0), 0));
const summaryEmojis = computed(() =>
    REACTIONS
        .filter((r) => (props.counts?.[r.key] || 0) > 0)
        .sort((a, b) => (props.counts[b.key] || 0) - (props.counts[a.key] || 0))
        .slice(0, 3)
        .map((r) => r.emoji)
        .join('')
);
const summaryTitle = computed(() =>
    REACTIONS
        .filter((r) => (props.counts?.[r.key] || 0) > 0)
        .map((r) => `${r.emoji} ${r.label}: ${props.counts[r.key]}`)
        .join('\n')
);

const open = ref(false);
let openT = null;
let closeT = null;
const openSoon = () => { clearTimeout(closeT); openT = setTimeout(() => { open.value = true; }, 220); };
const closeSoon = () => { clearTimeout(openT); closeT = setTimeout(() => { open.value = false; }, 220); };
const cancelClose = () => { clearTimeout(closeT); };

const onTriggerClick = () => {
    open.value = false;
    if (props.myReaction) emit('unreact');
    else emit('react', 'LIKE');
};
const pick = (key) => {
    open.value = false;
    if (key === props.myReaction) emit('unreact');
    else emit('react', key);
};

onBeforeUnmount(() => { clearTimeout(openT); clearTimeout(closeT); });
</script>

<style scoped>
.reaction-bar {
    position: relative;
    display: inline-flex;
    align-items: center;
    gap: 8px;
}
.reaction-trigger {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 13px;
    font-weight: 600;
    padding: 4px 10px;
    border-radius: 999px;
    border: 1px solid var(--border);
    background: var(--surface);
    color: var(--text-muted);
    transition: background .15s, border-color .15s;
}
.reaction-trigger:hover { background: var(--brand-soft); }
.reaction-trigger.is-active { border-color: currentColor; background: var(--brand-soft); }
.reaction-trigger__emoji { font-size: 15px; line-height: 1; }
.reaction-summary {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: var(--text-muted);
}
.reaction-summary__emojis { font-size: 13px; letter-spacing: -2px; }
.reaction-pop {
    position: absolute;
    bottom: calc(100% + 6px);
    left: 0;
    z-index: 40;
    display: flex;
    gap: 2px;
    padding: 4px 6px;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 999px;
    box-shadow: var(--shadow);
}
.reaction-choice {
    font-size: 22px;
    line-height: 1;
    padding: 3px;
    border-radius: 999px;
    transition: transform .12s;
}
.reaction-choice:hover { transform: scale(1.3) translateY(-2px); }
.reaction-choice.is-mine { background: var(--brand-soft); }
</style>
