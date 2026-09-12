<template>
    <div class="poll" v-if="poll">
        <button
            v-for="o in poll.options"
            :key="o.optionId"
            type="button"
            class="poll-opt"
            :class="{ 'is-mine': o.optionId === poll.myOptionId }"
            :disabled="busy || !isLogin"
            :aria-pressed="o.optionId === poll.myOptionId"
            @click.stop="pick(o)"
        >
            <span class="poll-opt__bar" :style="{ width: pct(o) + '%' }" aria-hidden="true"></span>
            <span class="poll-opt__text">{{ o.text }}</span>
            <span class="poll-opt__num tnum">{{ pct(o) }}%</span>
        </button>

        <div class="poll__foot">
            <span>{{ poll.totalVotes.toLocaleString('vi-VN') }} phiếu</span>
            <button v-if="poll.myOptionId" type="button" class="link" :disabled="busy" @click.stop="withdraw">
                Rút phiếu
            </button>
            <span v-else-if="!isLogin" class="muted">Đăng nhập để bình chọn</span>
        </div>
    </div>
</template>

<script setup>
/**
 * Kết quả bình chọn luôn hiển thị, kể cả khi chưa bỏ phiếu — một trạng thái ít hơn
 * so với kiểu "giấu tới khi bỏ phiếu", và bình chọn ở diễn đàn là để biết chứ không
 * phải để thi.
 */
import { inject, ref } from 'vue';
import { votePoll, unvotePoll } from '@/apis/post';
import { LOCALKEYS, getItemLocal } from '@/storages/localStorage';

const props = defineProps({
    poll: { type: Object, default: null },
    postId: { type: String, required: true },
});
const emit = defineEmits(['update:poll']);

const showDialog = inject('openDialogError', null);
const busy = ref(false);
const isLogin = !!getItemLocal(LOCALKEYS.ACCESS_TOKEN);

// Chưa ai bỏ phiếu thì mọi phương án đều 0% (không chia cho 0)
const pct = (o) => (props.poll.totalVotes ? Math.round((o.votes * 100) / props.poll.totalVotes) : 0);

const send = async (fn) => {
    if (busy.value) return;
    busy.value = true;
    try {
        // Backend trả về kết quả mới nhất -> dùng thẳng, khỏi tải lại cả bài
        emit('update:poll', (await fn())?.data?.data);
    } catch (e) {
        showDialog?.('Thông báo', e?.description || 'Không gửi được phiếu');
    } finally {
        busy.value = false;
    }
};

const pick = (o) => {
    if (!isLogin) return;
    // Bấm lại đúng phương án đang chọn = rút phiếu, đỡ phải đi tìm nút khác
    if (o.optionId === props.poll.myOptionId) return withdraw();
    send(() => votePoll(props.postId, o.optionId));
};

const withdraw = () => send(() => unvotePoll(props.postId));
</script>
