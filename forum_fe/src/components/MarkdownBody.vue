<template>
    <div class="markdown-body post-body" v-html="safeHtml"></div>
</template>

<script setup>
/**
 * Hiển thị thân bài dạng Markdown, đã lọc sạch trước khi vào v-html.
 *
 * ĐÂY LÀ v-html DUY NHẤT được phép trong repo, và khác hẳn lỗi XSS cũ (MDialog
 * từng chèn thẳng `content` — có thể là fullName người dùng — vào v-html mà
 * KHÔNG qua bước lọc nào). Ở đây luôn đi qua hai lớp trước khi tới v-html:
 *   1. markdown-it với `html: false` — cú pháp HTML thô gõ trong bài KHÔNG được
 *      dịch thành thẻ, chỉ hiện ra như chữ thường.
 *   2. DOMPurify.sanitize() — phòng trường hợp (1) có lỗi hoặc markdown-it tự
 *      sinh ra thứ gì đó không lường trước (ảnh lỗi thời, thuộc tính onerror...).
 * Bỏ một trong hai lớp này là mở lại đúng lỗ hổng đã ghi trong CLAUDE.md.
 */
import { computed } from 'vue';
import MarkdownIt from 'markdown-it';
import DOMPurify from 'dompurify';

const props = defineProps({
    text: { type: String, default: '' },
});

const md = new MarkdownIt({
    html: false,        // không cho HTML thô trong nguồn markdown
    linkify: true,       // tự nhận http://... thành link
    breaks: true,        // xuống dòng đơn (Enter) cũng ngắt dòng, khớp thói quen gõ chat
});

// Mọi link (kể cả tự sinh từ linkify) đều mở tab mới + không rò rỉ Referer/quyền window.opener
const defaultLinkOpen = md.renderer.rules.link_open ||
    ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options));
md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
    tokens[idx].attrSet('target', '_blank');
    tokens[idx].attrSet('rel', 'noopener noreferrer nofollow');
    return defaultLinkOpen(tokens, idx, options, env, self);
};

const safeHtml = computed(() => {
    const rendered = md.render(props.text || '');
    return DOMPurify.sanitize(rendered, {
        // Chỉ chữ, không script/style/form/iframe/svg... — thân bài không cần chúng.
        ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'del', 'code', 'pre',
            'blockquote', 'ul', 'ol', 'li', 'a', 'h1', 'h2', 'h3', 'img', 'hr'],
        ALLOWED_ATTR: ['href', 'target', 'rel', 'src', 'alt'],
    });
});
</script>

<style>
/* Không "scoped": nội dung được sinh bằng v-html nên style scoped (dựa vào
   thuộc tính data-v-*) không gắn được vào các thẻ bên trong. */
.markdown-body :is(h1, h2, h3) { font-family: var(--font-display); font-weight: 800; margin: 14px 0 6px; }
.markdown-body h1 { font-size: var(--fs-lg); }
.markdown-body h2 { font-size: var(--fs-ui); }
.markdown-body h3 { font-size: var(--fs-ui); }
.markdown-body p { margin: 8px 0; }
.markdown-body p:first-child { margin-top: 0; }
.markdown-body a { color: var(--cinnabar-ink); text-decoration: underline; text-underline-offset: 3px; }
.markdown-body code { background: var(--wash); padding: 1px 5px; border-radius: 3px; font-size: .92em; }
.markdown-body pre { background: var(--wash); padding: 10px 12px; overflow-x: auto; border-radius: var(--radius-control); }
.markdown-body pre code { background: none; padding: 0; }
/* Trích dẫn: vạch bên trái mang màu trung tính, KHÔNG dùng vàng nghệ (đó là
   tiếng nói của trang) hay đỏ son (hành động của bạn) — đây chỉ là cấu trúc. */
.markdown-body blockquote { border-left: 3px solid var(--stroke); padding-left: 12px; color: var(--text-muted); margin: 10px 0; }
.markdown-body ul, .markdown-body ol { padding-left: 1.4em; margin: 8px 0; }
.markdown-body img { border-radius: var(--radius-control); margin: 8px 0; }
.markdown-body hr { border: none; border-top: 1px solid var(--rule); margin: 14px 0; }
</style>
