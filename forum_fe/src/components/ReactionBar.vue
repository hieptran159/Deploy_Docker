<template>
    <div class="reaction-bar" @mouseenter="openSoon" @mouseleave="closeSoon">
        <button
            type="button"
            class="reaction-trigger"
            :class="{ 'is-active': !!myReaction }"
            :style="myReaction ? { color: current.color } : {}"
            @click="onTriggerClick"
        >
            <span class="rx-ico rx-ico--sm" v-html="current.svg"></span>
            <span>{{ current.label }}</span>
        </button>

        <span v-if="total > 0" class="reaction-summary" :title="summaryTitle">
            <span class="reaction-summary__icons">
                <span v-for="(svg, i) in summarySvgs" :key="i" class="rx-ico rx-ico--xs" v-html="svg"></span>
            </span>
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
            ><span class="rx-ico rx-ico--lg" v-html="r.svg"></span></button>
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

/* Icon cảm xúc kiểu Facebook — SVG tròn có màu, dựng sẵn (nội dung tĩnh, tin cậy). */
const SVG = {
    LIKE: '<svg viewBox="0 0 20 20"><circle cx="10" cy="10" r="10" fill="#1877f2"/><path fill="#fff" d="M5.4 8.7h1.9V15H5.4A.85.85 0 0 1 4.55 14.15V9.55A.85.85 0 0 1 5.4 8.7Zm2.95-.35 2.2-3.35c.32-.5 1-.63 1.5-.3.42.27.6.78.46 1.25l-.5 1.85h2.98c.7 0 1.2.66 1.03 1.34l-1.02 3.98c-.12.48-.56.82-1.06.82H8.85A.5.5 0 0 1 8.35 15V8.9c0-.2.03-.38.13-.55Z"/></svg>',
    LOVE: '<svg viewBox="0 0 20 20"><circle cx="10" cy="10" r="10" fill="#f3425f"/><path fill="#fff" d="M10 15.1a.7.7 0 0 1-.5-.2L6 11.45C4.35 9.85 4.4 7.75 5.8 6.7c1.2-.92 2.95-.68 3.9.42l.3.34.3-.34c.95-1.1 2.7-1.34 3.9-.42 1.4 1.05 1.45 3.15-.2 4.75l-3.5 3.45a.7.7 0 0 1-.5.2Z"/></svg>',
    HAHA: '<svg viewBox="0 0 20 20"><circle cx="10" cy="10" r="10" fill="#f7b125"/><path d="M5.7 7.6c.35-.55.9-.9 1.5-.9s1.15.35 1.5.9M11.3 7.6c.35-.55.9-.9 1.5-.9s1.15.35 1.5.9" stroke="#1a1a1a" stroke-width="1.15" stroke-linecap="round" fill="none"/><path fill="#1a1a1a" d="M5.5 10.4h9c0 2.5-2 4.5-4.5 4.5s-4.5-2-4.5-4.5Z"/><path fill="#fff" d="M7 13.9c.8.6 1.85 1 3 1s2.2-.4 3-1c-.65-.5-1.8-.8-3-.8s-2.35.3-3 .8Z"/></svg>',
    WOW: '<svg viewBox="0 0 20 20"><circle cx="10" cy="10" r="10" fill="#f7b125"/><path d="M5.2 6.4c.7-.6 1.7-.7 2.5-.3M14.8 6.4c-.7-.6-1.7-.7-2.5-.3" stroke="#1a1a1a" stroke-width="1.1" stroke-linecap="round" fill="none"/><ellipse cx="7" cy="8.5" rx="1.15" ry="1.5" fill="#1a1a1a"/><ellipse cx="13" cy="8.5" rx="1.15" ry="1.5" fill="#1a1a1a"/><ellipse cx="10" cy="13" rx="1.7" ry="2.2" fill="#1a1a1a"/></svg>',
    SAD: '<svg viewBox="0 0 20 20"><circle cx="10" cy="10" r="10" fill="#f7b125"/><path d="M5.7 7.9c.35-.5.9-.8 1.5-.8s1.15.3 1.5.8M11.3 7.9c.35-.5.9-.8 1.5-.8s1.15.3 1.5.8" stroke="#1a1a1a" stroke-width="1.1" stroke-linecap="round" fill="none"/><circle cx="7.1" cy="9.6" r="1.05" fill="#1a1a1a"/><circle cx="12.9" cy="9.6" r="1.05" fill="#1a1a1a"/><path d="M6.8 14c1-1.1 5.4-1.1 6.4 0" stroke="#1a1a1a" stroke-width="1.15" fill="none" stroke-linecap="round"/><path d="M6.7 10.4c0 1.3-1.5 2.9-1.5 2.9s-1.5-1.6-1.5-2.9a1.5 1.5 0 0 1 3 0Z" fill="#4aa3e0"/></svg>',
    ANGRY: '<svg viewBox="0 0 20 20"><circle cx="10" cy="10" r="10" fill="#e0562b"/><path d="M4.6 7.2 8.3 9M15.4 7.2 11.7 9" stroke="#1a1a1a" stroke-width="1.25" stroke-linecap="round"/><circle cx="7.2" cy="10" r="1.05" fill="#1a1a1a"/><circle cx="12.8" cy="10" r="1.05" fill="#1a1a1a"/><path d="M6.5 14.2c1.2-1.2 5.8-1.2 7 0" stroke="#1a1a1a" stroke-width="1.25" fill="none" stroke-linecap="round"/></svg>',
};
const GREY_SVG = '<svg viewBox="0 0 20 20"><path fill="none" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" d="M6 9.2 9 4.4c.9-.2 1.7.5 1.6 1.4l-.4 2.3h3.5c.9 0 1.5.8 1.3 1.6l-1 4c-.15.6-.7 1-1.3 1H6zM6 9.2H4.2v6.9H6z"/></svg>';

const REACTIONS = [
    { key: 'LIKE', label: 'Thích', color: '#1877f2', svg: SVG.LIKE },
    { key: 'LOVE', label: 'Yêu thích', color: '#f3425f', svg: SVG.LOVE },
    { key: 'HAHA', label: 'Haha', color: '#f7b125', svg: SVG.HAHA },
    { key: 'WOW', label: 'Wow', color: '#f7b125', svg: SVG.WOW },
    { key: 'SAD', label: 'Buồn', color: '#f7b125', svg: SVG.SAD },
    { key: 'ANGRY', label: 'Phẫn nộ', color: '#e0562b', svg: SVG.ANGRY },
];
const META = Object.fromEntries(REACTIONS.map((r) => [r.key, r]));
const GREY = { label: 'Thích', color: '', svg: GREY_SVG };

const current = computed(() => (props.myReaction && META[props.myReaction]) || GREY);
const total = computed(() => Object.values(props.counts || {}).reduce((a, b) => a + (Number(b) || 0), 0));
const ranked = computed(() =>
    REACTIONS
        .filter((r) => (props.counts?.[r.key] || 0) > 0)
        .sort((a, b) => (props.counts[b.key] || 0) - (props.counts[a.key] || 0))
);
const summarySvgs = computed(() => ranked.value.slice(0, 3).map((r) => r.svg));
const summaryTitle = computed(() =>
    ranked.value.map((r) => `${r.label}: ${props.counts[r.key]}`).join('\n')
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
.rx-ico { display: inline-flex; line-height: 0; }
.rx-ico :deep(svg) { display: block; width: 100%; height: 100%; }
.rx-ico--sm { width: 16px; height: 16px; }
.rx-ico--xs { width: 15px; height: 15px; }
.rx-ico--lg { width: 26px; height: 26px; }
.reaction-summary {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: var(--text-muted);
}
.reaction-summary__icons { display: inline-flex; }
.reaction-summary__icons .rx-ico + .rx-ico { margin-left: -4px; }
.reaction-summary__icons .rx-ico :deep(svg) {
    outline: 1.5px solid var(--surface);
    border-radius: 999px;
}
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
    line-height: 0;
    padding: 3px;
    border-radius: 999px;
    transition: transform .12s;
}
.reaction-choice:hover { transform: scale(1.3) translateY(-2px); }
.reaction-choice.is-mine { background: var(--brand-soft); }
</style>
