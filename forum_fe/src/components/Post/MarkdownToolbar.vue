<template>
    <div class="md-toolbar">
        <button type="button" class="md-toolbar__btn" title="Đậm (Ctrl+B)" @click="wrap('**', '**')">
            <strong>B</strong>
        </button>
        <button type="button" class="md-toolbar__btn" title="Nghiêng (Ctrl+I)" @click="wrap('_', '_')">
            <em>I</em>
        </button>
        <button type="button" class="md-toolbar__btn" title="Trích dẫn" @click="linePrefix('> ')">
            <AppIcon name="message-circle" :size="15" />
        </button>
        <button type="button" class="md-toolbar__btn" title="Code" @click="wrap('`', '`')">&lt;/&gt;</button>
        <button type="button" class="md-toolbar__btn" title="Liên kết" @click="insertLink">
            <AppIcon name="link" :size="15" />
        </button>
        <button type="button" class="md-toolbar__btn" title="Danh sách" @click="linePrefix('- ')">
            <AppIcon name="list" :size="15" />
        </button>
    </div>
</template>

<script setup>
/**
 * Thao tác trực tiếp trên textarea qua selectionStart/selectionEnd — không cần
 * thư viện editor. Chèn CÚ PHÁP markdown, không xử lý ảnh thật: ảnh của bài vẫn
 * đi qua ô "Ảnh đính kèm" hiện có (hiển thị dạng dải ảnh riêng). Nút liên kết bọc
 * `[chữ](url)` để người dùng tự điền URL — không upload ảnh giữa dòng, tránh phải
 * thêm một API upload-rời và lo dọn ảnh mồ côi khi người dùng bỏ dở bài viết.
 */
import AppIcon from '@/components/AppIcon.vue';

const props = defineProps({
    // Ref tới phần tử <textarea> thật — component không tự giữ giá trị văn bản,
    // CHỈ thao tác DOM rồi bắn sự kiện input để v-model của cha tự cập nhật.
    textarea: { type: Object, default: null },
});

const fire = (el) => el.dispatchEvent(new Event('input', { bubbles: true }));

function wrap(before, after) {
    const el = props.textarea;
    if (!el) return;
    const { selectionStart: s, selectionEnd: e, value } = el;
    const selected = value.slice(s, e) || 'chữ';
    el.setRangeText(before + selected + after, s, e, 'select');
    // Chọn lại đúng phần chữ bên trong cặp ký hiệu, để gõ tiếp là thay được ngay
    el.setSelectionRange(s + before.length, s + before.length + selected.length);
    el.focus();
    fire(el);
}

function linePrefix(prefix) {
    const el = props.textarea;
    if (!el) return;
    const { selectionStart: s, value } = el;
    const lineStart = value.lastIndexOf('\n', s - 1) + 1;
    el.setRangeText(prefix, lineStart, lineStart, 'end');
    el.focus();
    fire(el);
}

function insertLink() {
    const el = props.textarea;
    if (!el) return;
    const { selectionStart: s, selectionEnd: e, value } = el;
    const label = value.slice(s, e) || 'chữ';
    const snippet = `[${label}](url)`;
    el.setRangeText(snippet, s, e, 'end');
    // Bôi đen "url" để người dùng gõ đè ngay, không phải tự tìm và xoá chữ mẫu
    const urlStart = s + snippet.indexOf('(url)') + 1;
    el.setSelectionRange(urlStart, urlStart + 3);
    el.focus();
    fire(el);
}
</script>

<style scoped>
.md-toolbar {
    display: flex;
    gap: 2px;
    border: var(--line-w) solid var(--stroke);
    border-bottom: 0;
    padding: 4px;
    background: var(--wash);
}
.md-toolbar__btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 28px;
    font-size: var(--fs-sm);
    font-family: inherit;
    border: 0;
    border-radius: var(--radius-control);
    background: transparent;
    color: var(--text);
    cursor: pointer;
}
.md-toolbar__btn:hover { background: var(--turmeric-wash); }
</style>
