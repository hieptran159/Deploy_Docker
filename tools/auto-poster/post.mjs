#!/usr/bin/env node
/**
 * Auto-poster MVP cho forum.
 *
 *  - Đọc các file content/<slug>.md (có frontmatter YAML tối giản)
 *  - Đăng nhập tài khoản bot qua /auth/signin
 *  - Đăng từng bài qua POST /post/new (multipart) — mặc định là BẢN NHÁP
 *  - Ghi .state.json để lần chạy sau KHÔNG đăng trùng
 *
 * Yêu cầu: Node >= 18 (dùng fetch/FormData/Blob có sẵn, không cần npm install).
 *
 * Cách dùng:
 *   cp .env.example .env      # điền BOT_EMAIL / BOT_PASSWORD / API_URL
 *   node post.mjs --dry-run   # xem sẽ đăng gì, không gọi API
 *   node post.mjs             # đăng (bản nháp)
 *   node post.mjs --publish   # đăng và xuất bản luôn
 *   node post.mjs --file content/bai-1.md
 *   node post.mjs --force     # bỏ qua .state.json, đăng lại tất cả
 */

import { readFile, readdir, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { createHash } from 'node:crypto';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));

/* ---------------- .env (parser tối giản, không ghi đè env sẵn có) ---------------- */
async function loadDotenv(p) {
    if (!existsSync(p)) return;
    for (const line of (await readFile(p, 'utf8')).split(/\r?\n/)) {
        const m = line.match(/^\s*([A-Za-z0-9_]+)\s*=\s*(.*?)\s*$/);
        if (!m || line.trimStart().startsWith('#')) continue;
        let v = m[2];
        if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) v = v.slice(1, -1);
        if (!(m[1] in process.env)) process.env[m[1]] = v;
    }
}

/* ---------------- CLI ---------------- */
const args = process.argv.slice(2);
const has = (f) => args.includes(f);
const val = (f, d) => {
    const a = args.find((x) => x.startsWith(f + '='));
    if (a) return a.slice(f.length + 1);
    const i = args.indexOf(f);
    return i >= 0 && args[i + 1] && !args[i + 1].startsWith('--') ? args[i + 1] : d;
};
const DRY = has('--dry-run');
const FORCE = has('--force');
const PUBLISH_ALL = has('--publish');

/* ---------------- frontmatter YAML tối giản ----------------
   Hỗ trợ:  key: value  |  key: "value"  |  key: [a, b, c]  |  true/false
   Body = phần sau khối --- ... ---                                            */
function parseDoc(input) {
    const raw = input.replace(/\r\n/g, '\n'); // chuẩn hoá xuống dòng
    const m = raw.match(/^﻿?---\n([\s\S]*?)\n---\n?([\s\S]*)$/);
    if (!m) return { fm: {}, body: raw.trim() };
    const fm = {};
    for (const line of m[1].split(/\r?\n/)) {
        if (!line.trim() || line.trimStart().startsWith('#')) continue;
        const kv = line.match(/^([A-Za-z0-9_]+)\s*:\s*(.*)$/);
        if (!kv) continue;
        let v = kv[2].trim();
        if (v === 'true' || v === 'false') fm[kv[1]] = v === 'true';
        else if (v.startsWith('[') && v.endsWith(']')) {
            fm[kv[1]] = v.slice(1, -1).split(',').map((s) => s.trim().replace(/^['"]|['"]$/g, '')).filter(Boolean);
        } else fm[kv[1]] = v.replace(/^['"]|['"]$/g, '');
    }
    return { fm, body: m[2].trim() };
}

const sha1 = (s) => createHash('sha1').update(s).digest('hex').slice(0, 12);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/* ---------------- API ---------------- */
const CFG = {
    apiUrl: (process.env.API_URL || 'http://localhost:8081').replace(/\/+$/, ''),
    email: process.env.BOT_EMAIL,
    password: process.env.BOT_PASSWORD,
    delayMs: Number(process.env.POST_DELAY_MS || 3000),
    contentDir: path.resolve(HERE, process.env.CONTENT_DIR || 'content'),
    stateFile: path.resolve(HERE, process.env.STATE_FILE || '.state.json'),
    bypassHeader: process.env.BYPASS_HEADER || 'X-Auto-Poster',
    bypassToken: process.env.BYPASS_TOKEN || '',
    draftLimit: Number(process.env.DRAFT_LIMIT || 0),  // >0: tạm dừng đăng khi số nháp đạt ngưỡng
};

async function jsonOf(res) {
    const t = await res.text();
    try { return JSON.parse(t); } catch { return { _raw: t }; }
}

// fetch có thông báo lỗi kết nối dễ hiểu (ECONNREFUSED / ENOTFOUND / timeout…)
async function hit(url, opts = {}) {
    // Header bí mật để WAF Cloudflare bỏ qua "managed challenge" (khi gọi qua tunnel từ CI)
    if (CFG.bypassToken) {
        opts.headers = { ...(opts.headers || {}), [CFG.bypassHeader]: CFG.bypassToken };
    }
    try {
        return await fetch(url, opts);
    } catch (e) {
        const why = e?.cause?.code || e?.cause?.message;
        // Chỉ bọc lỗi KẾT NỐI (undici luôn kèm e.cause hoặc message "fetch failed").
        // Lỗi khác (vd header chứa ký tự ngoài ASCII) -> ném nguyên văn cho dễ chẩn đoán.
        if (!why && !/^fetch failed$/i.test(e?.message || '')) throw e;
        throw new Error(`Không kết nối được ${new URL(url).origin} — ${why || e.message}.\n` +
            `  Kiểm tra API_URL trong tools/auto-poster/.env (đang là "${CFG.apiUrl}").`);
    }
}

async function login() {
    // Bỏ khoảng trắng / xuống dòng thừa (hay dính khi dán vào ô secret của GitHub)
    const email = (CFG.email || '').trim();
    const password = (CFG.password || '').replace(/[\r\n]+$/, '');
    const u = new URLSearchParams({ email, password, rememberMe: 'true' });

    for (let attempt = 1; ; attempt++) {
        const res = await hit(`${CFG.apiUrl}/auth/signin?${u}`, { method: 'POST' });
        const j = await jsonOf(res);
        if (res.ok && j?.data?.accessToken) {
            return { access: j.data.accessToken, refresh: j.data.refreshToken, userId: j.data.userId };
        }
        // 429: bị rate-limit (qua tunnel mọi client chung 1 bucket) -> chờ Retry-After rồi thử lại
        if (res.status === 429 && attempt <= 4) {
            const wait = Math.min(Number(res.headers.get('retry-after')) || 30, 120);
            console.log(`  rate-limit (429), chờ ${wait}s rồi thử lại (${attempt}/4)…`);
            await sleep(wait * 1000);
            continue;
        }
        const hint = res.status === 503 ? '  → kiểm tra FORUM_BOT_EMAIL / FORUM_BOT_PASSWORD (secret)' : '';
        throw new Error(`Đăng nhập thất bại (HTTP ${res.status}): ${j?.description || j?._raw || 'không rõ'}\n` +
            `  API_URL="${CFG.apiUrl}"  email="${email || '(trống)'}"  password=${password.length} ký tự${hint}`);
    }
}

// Số bản nháp hiện có của tài khoản bot (dùng cho ngưỡng DRAFT_LIMIT)
async function draftCount(tok) {
    try {
        const res = await hit(`${CFG.apiUrl}/post/drafts?page=0&size=1`, { headers: { Authorization: `Bearer ${tok.access}` } });
        const j = await jsonOf(res);
        return Number(j?.data?.total ?? 0);
    } catch (e) { return 0; }
}

async function refresh(tok) {
    const res = await hit(`${CFG.apiUrl}/auth/refresh?refreshToken=${encodeURIComponent(tok.refresh || '')}`, { method: 'POST' });
    const j = await jsonOf(res);
    if (j?.data?.accessToken) {
        tok.access = j.data.accessToken;
        if (j.data.refreshToken) tok.refresh = j.data.refreshToken;
        return true;
    }
    return false;
}

// Khôi phục phiên khi bị 401 giữa chừng: thử refresh trước, hỏng thì ĐĂNG NHẬP LẠI.
// (login của backend blacklist token cũ -> nếu có tiến trình khác login xen vào,
//  cả access lẫn refresh của ta đều chết, chỉ login lại mới cứu được.)
let reauths = 0;
async function reauth(tok) {
    if (tok.refresh && (await refresh(tok))) return true;
    if (reauths >= 3) return false;
    reauths++;
    try {
        const fresh = await login();
        tok.access = fresh.access; tok.refresh = fresh.refresh; tok.userId = fresh.userId;
        console.log(`  ↻ đăng nhập lại (lần ${reauths}) do 401 giữa chừng`);
        return true;
    } catch (e) {
        console.log(`  ✗ đăng nhập lại thất bại: ${e.message.split('\n')[0]}`);
        return false;
    }
}

// buildForm: hàm dựng lại FormData mỗi lần gọi (body multipart không tái sử dụng được sau retry)
async function createPost(tok, buildForm) {
    let res = await hit(`${CFG.apiUrl}/post/new`, {
        method: 'POST', headers: { Authorization: `Bearer ${tok.access}` }, body: await buildForm(),
    });
    if (res.status === 401 && (await reauth(tok))) {
        res = await hit(`${CFG.apiUrl}/post/new`, {
            method: 'POST', headers: { Authorization: `Bearer ${tok.access}` }, body: await buildForm(),
        });
    }
    const j = await jsonOf(res);
    if (!res.ok || j?.success === false) {
        throw new Error(`${res.status}: ${j?.description || j?._raw || 'không rõ'}`);
    }
    // data = { "postId: ": "<uuid>" }  -> lấy value đầu tiên
    const d = j?.data || {};
    return typeof d === 'string' ? d : Object.values(d)[0];
}

/* ---------------- nội dung ---------------- */
const IMG_OK = new Set(['.png', '.jpg', '.jpeg']);

function normVisibility(v) {
    v = String(v || '').toLowerCase();
    return v === 'friends' || v === 'private' ? v : 'public';
}

// gắn #tag vào cuối body nếu chưa có
function withTags(body, tags) {
    if (!Array.isArray(tags) || !tags.length) return body;
    const norm = tags.map((t) => String(t).replace(/^#/, '').trim()).filter(Boolean);
    const missing = norm.filter((t) => !new RegExp(`(^|\\s)#${t}(\\s|$)`, 'iu').test(body));
    return missing.length ? `${body}\n\n${missing.map((t) => '#' + t).join(' ')}` : body;
}

async function buildEntry(file) {
    const abs = path.resolve(CFG.contentDir, file);
    const raw = await readFile(abs, 'utf8');
    const { fm, body } = parseDoc(raw);
    const title =
        (fm.title && String(fm.title).trim()) ||
        (body.match(/^#\s+(.+)$/m)?.[1]?.trim()) ||
        path.basename(file, path.extname(file));
    if (!body) throw new Error('body rỗng');

    let img = null;
    if (fm.image) {
        const ip = path.resolve(path.dirname(abs), String(fm.image));
        if (!existsSync(ip)) throw new Error(`không thấy ảnh: ${fm.image}`);
        if (!IMG_OK.has(path.extname(ip).toLowerCase())) throw new Error('ảnh chỉ nhận .png/.jpg/.jpeg');
        img = { path: ip, name: path.basename(ip) };
    }

    const publish = PUBLISH_ALL || fm.publish === true;
    const finalBody = withTags(body, fm.tags);

    return {
        file, title, body: finalBody, visibility: normVisibility(fm.visibility), publish, img,
        hash: sha1(title + ' ' + finalBody + ' ' + (fm.image || '') + ' ' + (publish ? 'P' : 'D')),
        buildForm: async () => {
            const f = new FormData();
            f.append('title', title);
            f.append('body', finalBody);
            f.append('visibility', normVisibility(fm.visibility));
            if (!publish) f.append('draft', 'true');
            if (img) f.append('postImg', new Blob([await readFile(img.path)]), img.name);
            return f;
        },
    };
}

/* ---------------- main ---------------- */
async function main() {
    await loadDotenv(path.resolve(HERE, '.env'));
    // .env được nạp SAU khi CFG khởi tạo -> tính lại toàn bộ trường lấy từ env
    CFG.apiUrl = (process.env.API_URL || 'http://localhost:8081').replace(/\/+$/, '');
    CFG.email = process.env.BOT_EMAIL;
    CFG.password = process.env.BOT_PASSWORD;
    CFG.delayMs = Number(process.env.POST_DELAY_MS || 3000);
    CFG.contentDir = path.resolve(HERE, process.env.CONTENT_DIR || 'content');
    CFG.stateFile = path.resolve(HERE, process.env.STATE_FILE || '.state.json');
    CFG.bypassHeader = process.env.BYPASS_HEADER || 'X-Auto-Poster';
    CFG.bypassToken = process.env.BYPASS_TOKEN || '';
    // Header HTTP chỉ nhận ASCII. BYPASS_* dính ký tự lạ (vd dấu tiếng Việt) -> fetch ném lỗi khó hiểu.
    if (/[^\x20-\x7E]/.test(CFG.bypassToken) || /[^\x20-\x7E]/.test(CFG.bypassHeader)) {
        console.error('BYPASS_TOKEN / BYPASS_HEADER chứa ký tự ngoài ASCII. Chỉ dùng chuỗi hex ' +
            '(vd `openssl rand -hex 16`), hoặc BỎ TRỐNG nếu chạy trên máy Ubuntu/localhost.');
        process.exit(2);
    }
    if (!CFG.email || !CFG.password) {
        console.error('Thiếu BOT_EMAIL / BOT_PASSWORD (đặt trong tools/auto-poster/.env hoặc biến môi trường).');
        process.exit(2);
    }

    const single = val('--file');
    let files;
    if (single) {
        files = [path.relative(CFG.contentDir, path.resolve(single))];
    } else {
        if (!existsSync(CFG.contentDir)) { console.error('Không thấy thư mục:', CFG.contentDir); process.exit(2); }
        files = (await readdir(CFG.contentDir)).filter((f) => f.toLowerCase().endsWith('.md')).sort();
    }
    if (!files.length) { console.log('Không có file .md nào để đăng.'); return; }

    const state = existsSync(CFG.stateFile) ? JSON.parse(await readFile(CFG.stateFile, 'utf8')) : {};

    console.log(`API      : ${CFG.apiUrl}`);
    console.log(`Thư mục  : ${CFG.contentDir}`);
    console.log(`File .md  : ${files.length}   |   ${DRY ? 'DRY-RUN' : 'ĐĂNG THẬT'}${PUBLISH_ALL ? ' + PUBLISH' : ''}${FORCE ? ' + FORCE' : ''}\n`);

    let tok = null;
    const summary = { posted: 0, skipped: 0, failed: 0 };

    for (const file of files) {
        let e;
        try { e = await buildEntry(file); }
        catch (err) { console.log(`✗ ${file}  — lỗi đọc: ${err.message}`); summary.failed++; continue; }

        const prev = state[file];
        if (prev && !FORCE) {
            if (prev.hash === e.hash) { console.log(`· ${file}  — đã đăng (${prev.postId}), bỏ qua`); summary.skipped++; continue; }
            console.log(`! ${file}  — đã đăng trước đó nhưng nội dung ĐỔI; vẫn bỏ qua (dùng --force để đăng lại thành bài mới)`);
            summary.skipped++; continue;
        }

        if (DRY) {
            console.log(`○ ${file}  → "${e.title}"  [${e.visibility}${e.publish ? '' : ', nháp'}${e.img ? ', +ảnh' : ''}]  ${e.body.length} ký tự`);
            summary.skipped++; continue;
        }

        if (!tok) {
            try { tok = await login(); console.log(`✓ đăng nhập: ${tok.userId}`); } catch (err) { console.error(err.message); process.exit(1); }
            if (CFG.draftLimit > 0) {
                const n = await draftCount(tok);
                if (n >= CFG.draftLimit) {
                    console.log(`\n⏸  Đang có ${n} bản nháp (≥ giới hạn ${CFG.draftLimit}) — tạm dừng đăng.\n   Vào /drafts duyệt bớt (hoặc "Đăng tất cả"/"Xoá tất cả") rồi chạy lại.`);
                    process.exit(0);
                }
                console.log(`  (bản nháp hiện có: ${n}/${CFG.draftLimit})`);
            }
            console.log('');
        }

        try {
            const postId = await createPost(tok, e.buildForm);
            state[file] = { hash: e.hash, postId, publish: e.publish, at: new Date().toISOString() };
            await writeFile(CFG.stateFile, JSON.stringify(state, null, 2)); // lưu ngay -> an toàn khi crash
            console.log(`✓ ${file}  → ${postId}  ${e.publish ? '(đã xuất bản)' : '(bản nháp)'}`);
            summary.posted++;
        } catch (err) {
            console.log(`✗ ${file}  — đăng lỗi: ${err.message}`);
            summary.failed++;
        }
        await sleep(CFG.delayMs);
    }

    console.log(`\nXong. Đăng ${summary.posted}, bỏ qua ${summary.skipped}, lỗi ${summary.failed}.`);
    process.exit(summary.failed ? 1 : 0);
}

main().catch((e) => { console.error(e); process.exit(1); });
