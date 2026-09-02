<template>
    <span class="whitespace-pre-wrap break-words"><template
        v-for="(seg, i) in segments"
        :key="i"
    ><a
            v-if="seg.url"
            :href="seg.url"
            target="_blank"
            rel="noopener noreferrer nofollow"
            class="text-[var(--brand)] underline break-all"
            @click.stop
        >{{ seg.text }}</a><template v-else>{{ seg.text }}</template></template></span>
</template>

<script setup>
/**
 * Hiển thị text thuần nhưng tự biến URL (http/https) thành link mở tab mới.
 * KHÔNG dùng v-html — text được render qua {{ }} nên an toàn XSS.
 */
import { computed } from 'vue';

const props = defineProps({
    text: { type: String, default: '' },
});

const URL_RE = /(https?:\/\/[^\s<]+)/g;
const TRAIL_RE = /[.,;:!?)\]}'"»…]+$/;

const segments = computed(() => {
    const src = props.text || '';
    const out = [];
    let last = 0;
    let m;
    URL_RE.lastIndex = 0;
    while ((m = URL_RE.exec(src)) !== null) {
        let url = m[0];
        let tail = '';
        const t = url.match(TRAIL_RE);
        if (t) { tail = t[0]; url = url.slice(0, -tail.length); }
        if (m.index > last) out.push({ text: src.slice(last, m.index) });
        out.push({ url, text: url });
        if (tail) out.push({ text: tail });
        last = m.index + m[0].length;
    }
    if (last < src.length) out.push({ text: src.slice(last) });
    if (!out.length) out.push({ text: src });
    return out;
});
</script>
