// Backend trả timestamp kiểu "2026-08-27 18:19:27.0" -> chuẩn hoá cho new Date()
function parseDate(input) {
    if (!input) return null;
    if (input instanceof Date) return input;
    // epoch millis (số hoặc chuỗi toàn số) - vd Jackson serialize java.util.Date
    if (typeof input === 'number' || /^\d{10,}$/.test(String(input))) {
        const d = new Date(Number(input));
        return Number.isNaN(d.getTime()) ? null : d;
    }
    const d = new Date(String(input).replace(' ', 'T'));
    return Number.isNaN(d.getTime()) ? null : d;
}

export function calculateTimeDifference(dateInput) {
    const date = parseDate(dateInput);
    if (!date) return '';
    const hoursDifference = (Date.now() - date.getTime()) / (1000 * 60 * 60);
    if (hoursDifference >= 24) {
        return `${Math.floor(hoursDifference / 24)} ngày`;
    }
    if (hoursDifference >= 1) {
        return `${Math.floor(hoursDifference)} giờ`;
    }
    const mins = Math.floor(hoursDifference * 60);
    return mins <= 0 ? 'vừa xong' : `${mins} phút`;
}

export function timeAgo(input) {
    const d = parseDate(input);
    if (!d) return '';
    const s = Math.floor((Date.now() - d.getTime()) / 1000);
    if (s < 60) return 'vừa xong';
    const m = Math.floor(s / 60);
    if (m < 60) return `${m} phút trước`;
    const h = Math.floor(m / 60);
    if (h < 24) return `${h} giờ trước`;
    const dd = Math.floor(h / 24);
    if (dd < 30) return `${dd} ngày trước`;
    return formatDateTime(input);
}

export function formatDateTime(input) {
    const d = parseDate(input);
    if (!d) return String(input ?? '');
    const p = (n) => String(n).padStart(2, '0');
    return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

export function formatTime(input) {
    const d = parseDate(input);
    if (!d) return '';
    const p = (n) => String(n).padStart(2, '0');
    return `${p(d.getHours())}:${p(d.getMinutes())}`;
}

export function convertName(name){
    const names = name.split(" ");
    return names[0][0] + names[names.length-1][0];
}
// Trích hashtag (#tag) từ văn bản — khớp luật backend HashtagUtils:
// chữ/số/gạch dưới (Unicode), 1..50 ký tự, viết thường, bỏ tag toàn số, tối đa 20, không lặp.
export function extractHashtags(...texts) {
    const re = /#([\p{L}\p{N}_]{1,50})/gu;
    const out = [];
    for (const text of texts) {
        if (!text) continue;
        for (const m of String(text).matchAll(re)) {
            const t = m[1].toLowerCase();
            if (/^\d+$/.test(t)) continue;
            if (!out.includes(t)) out.push(t);
            if (out.length >= 20) return out;
        }
    }
    return out;
}
