<template>
    <svg
        class="app-icon"
        viewBox="0 0 24 24"
        :width="size"
        :height="size"
        fill="none"
        stroke="currentColor"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
        focusable="false"
    >
        <template v-for="(el, i) in shapes" :key="i">
            <path v-if="el.d" :d="el.d" />
            <circle v-else-if="el.r" :cx="el.cx" :cy="el.cy" :r="el.r" />
            <rect v-else :x="el.x" :y="el.y" :width="el.w" :height="el.h" :rx="el.rx" />
        </template>
    </svg>
</template>

<script setup>
/**
 * Bộ icon dùng chung, thay cho font icon của DevExtreme và emoji dùng làm icon.
 * Vì sao là SVG:
 *  - ăn theo currentColor nên tự khớp mọi trạng thái (đang chọn, hover, nền tối),
 *    trong khi font icon của DevExtreme cố định #333 và phải ghi đè !important;
 *  - emoji thì mỗi hệ điều hành vẽ một kiểu, không chỉnh được nét và màu.
 *
 * Hình vẽ theo bộ Feather (nét 2px, lưới 24, đầu nét bo tròn) cho đồng bộ với
 * các SVG viết tay sẵn có trong Post.vue / ReactionBar.vue.
 * Chỉ mô tả bằng dữ liệu rồi render qua v-for — KHÔNG dùng v-html.
 */
import { computed } from 'vue';

const props = defineProps({
    name: { type: String, required: true },
    size: { type: [Number, String], default: 18 },
    strokeWidth: { type: [Number, String], default: 2 },
});

const p = (d) => ({ d });
const c = (cx, cy, r) => ({ cx, cy, r });
const r = (x, y, w, h, rx) => ({ x, y, w, h, rx });

const ICONS = {
    /* điều hướng */
    home: [p('M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z'), p('M9 22V12h6v10')],
    'message-square': [p('M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z')],
    'message-circle': [p('M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8z')],
    users: [p('M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2'), c(9, 7, 4), p('M23 21v-2a4 4 0 0 0-3-3.87'), p('M16 3.13a4 4 0 0 1 0 7.75')],
    search: [c(11, 11, 8), p('M21 21l-4.35-4.35')],

    /* thông báo & giao diện */
    bell: [p('M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9'), p('M13.73 21a2 2 0 0 1-3.46 0')],
    'bell-off': [p('M13.73 21a2 2 0 0 1-3.46 0'), p('M18.63 13A17.89 17.89 0 0 1 18 8'), p('M6.26 6.26A5.86 5.86 0 0 0 6 8c0 7-3 9-3 9h14'), p('M18 8a6 6 0 0 0-9.33-5'), p('M1 1l22 22')],
    sun: [c(12, 12, 5), p('M12 1v2'), p('M12 21v2'), p('M4.22 4.22l1.42 1.42'), p('M18.36 18.36l1.42 1.42'), p('M1 12h2'), p('M21 12h2'), p('M4.22 19.78l1.42-1.42'), p('M18.36 5.64l1.42-1.42')],
    moon: [p('M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z')],
    settings: [c(12, 12, 3), p('M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z')],
    'log-out': [p('M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4'), p('M16 17l5-5-5-5'), p('M21 12H9')],
    shield: [p('M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z')],

    /* hành động */
    edit: [p('M12 20h9'), p('M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4z')],
    trash: [p('M3 6h18'), p('M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2'), p('M10 11v6'), p('M14 11v6')],
    plus: [p('M12 5v14'), p('M5 12h14')],
    check: [p('M20 6L9 17l-5-5')],
    x: [p('M18 6L6 18'), p('M6 6l12 12')],
    'more-vertical': [c(12, 12, 1), c(12, 5, 1), c(12, 19, 1)],
    warning: [p('M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z'), p('M12 9v4'), p('M12 17h.01')],
    bookmark: [p('M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z')],
    heart: [p('M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 1 0-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 0 0 0-7.78z')],
    repeat: [p('M17 1l4 4-4 4'), p('M3 11V9a4 4 0 0 1 4-4h14'), p('M7 23l-4-4 4-4'), p('M21 13v2a4 4 0 0 1-4 4H3')],

    /* nội dung */
    image: [r(3, 3, 18, 18, 2), c(8.5, 8.5, 1.5), p('M21 15l-5-5L5 21')],
    camera: [p('M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z'), c(12, 13, 4)],
    'file-text': [p('M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z'), p('M14 2v6h6'), p('M16 13H8'), p('M16 17H8'), p('M10 9H8')],

    /* phạm vi xem */
    globe: [c(12, 12, 10), p('M2 12h20'), p('M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z')],
    lock: [r(3, 11, 18, 11, 2), p('M7 11V7a5 5 0 0 1 10 0v4')],

    /* người dùng */
    user: [p('M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2'), c(12, 7, 4)],
    'user-plus': [p('M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2'), c(8.5, 7, 4), p('M20 8v6'), p('M23 11h-6')],
    'user-x': [p('M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2'), c(8.5, 7, 4), p('M18 8l5 5'), p('M23 8l-5 5')],

    /* mũi tên */
    'chevron-up': [p('M18 15l-6-6-6 6')],
    'chevron-left': [p('M15 18l-6-6 6-6')],
    'chevron-right': [p('M9 18l6-6-6-6')],
};

const shapes = computed(() => ICONS[props.name] || []);
</script>

<style scoped>
.app-icon {
    display: block;
    flex: none;
}
</style>
