<template>
    <div class="flex flex-col gap-3 py-3">
        <div>
            <label class="text-sm muted">Tiêu đề</label>
            <input type="text" class="field" v-model="data.title" />
        </div>
        <div>
            <label class="text-sm muted">Nội dung</label>
            <textarea style="height: 120px" class="field" v-model="data.body"></textarea>
        </div>
        <div>
            <label class="text-sm muted">Ảnh đính kèm (tuỳ chọn)</label>
            <input type="file" accept="image/*" @change="onFile" />
        </div>
        <div class="flex items-center gap-2">
            <label class="text-sm muted">Ai xem được</label>
            <select v-model="data.visibility" class="text-sm border rounded-lg px-2 py-1 bg-[var(--surface)]">
                <option value="public">🌐 Mọi người</option>
                <option value="friends">👥 Chỉ bạn bè</option>
                <option value="private">🔒 Chỉ mình tôi</option>
            </select>
        </div>
        <div v-if="tags.length" class="flex flex-wrap items-center gap-1.5 text-xs">
            <span class="muted">Hashtag:</span>
            <span v-for="t in tags" :key="t" class="tag-chip tag-chip--sm">#{{ t }}</span>
        </div>
        <div class="flex justify-end gap-2">
            <button type="button" class="sign-btn sign-btn--outline" :disabled="busy" @click="() => submit(true)">Lưu nháp</button>
            <button type="button" class="sign-btn" :disabled="busy" @click="() => submit(false)">Đăng bài</button>
        </div>
    </div>
</template>

<script setup>
import { createdPost } from '@/apis/post';
import { ref, computed, inject } from 'vue';
import { extractHashtags } from '@/js/helper';

const emits = defineEmits(['close', 'post-fail']);
const showDialog = inject('openDialogError', null);
const toast = inject('toast', null);

const busy = ref(false);
const data = ref({
    title: "",
    body: "",
    postImg: null,
    visibility: "public",
});

const tags = computed(() => extractHashtags(data.value.title, data.value.body));

const onFile = (e) => {
    data.value.postImg = e.target.files[0] || null;
}

const submit = async (draft) => {
    if (draft && !data.value.title?.trim() && !data.value.body?.trim()) {
        showDialog?.('Thông báo', 'Bản nháp cần ít nhất tiêu đề hoặc nội dung');
        return;
    }
    busy.value = true;
    try {
        const payload = { ...data.value };
        if (draft) payload.draft = 'true';
        await createdPost(payload);
        if (draft) toast?.('Đã lưu bản nháp');
        emits("close");
    } catch (e) {
        if (draft) showDialog?.('Thông báo', e?.description || 'Lưu nháp thất bại');
        else emits("post-fail");
    } finally {
        busy.value = false;
    }
}
</script>
