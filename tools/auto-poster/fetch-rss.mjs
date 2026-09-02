#!/usr/bin/env node
/**
 * RSS/Atom -> file Markdown trong content/.
 *
 *  - Đọc danh sách feed từ feeds.txt (mỗi dòng 1 URL, '#' là chú thích)
 *    hoặc biến RSS_FEEDS (ngăn cách bằng dấu phẩy).
 *  - Mỗi entry MỚI -> ghi content/rss-YYYYMMDD-<slug>.md (frontmatter + tóm tắt).
 *  - .rss-seen.json nhớ entry đã lấy -> không tạo trùng.
 *  - Sau đó chạy post.mjs để đăng (mặc định các file này là BẢN NHÁP).
 *
 * Yêu cầu: Node >= 18. Không cần npm install.
 *
 * Env (đặt trong .env cạnh file này):
 *   RSS_FEEDS         danh sách URL, phẩy ngăn cách (nếu không dùng feeds.txt)
 *   RSS_MAX_PER_RUN   tối đa entry mới mỗi lần chạy (mặc định 5)
 *   RSS_VISIBILITY    public | friends | private   (mặc định public)
 *   RSS_PUBLISH       true -> xuất bản luôn; mặc định false (nháp)
 *   RSS_TAG           thẻ gắn cho mọi bài (mặc định "tin-tuc")
 *
 * CLI: --dry-run   --limit N   --feeds <path>
 */

import { readFile, readdir, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { createHash } from 'node:crypto';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));

/* ---------- .env ---------- */
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

/* ---------- CLI ---------- */
const args = process.argv.slice(2);
const DRY = args.includes('--dry-run');
const flag = (f, d) => { const i = args.indexOf(f); return i >= 0 ? args[i + 1] : d; };

/* ---------- helpers ---------- */
const sha1 = (s) => createHash('sha1').update(s).digest('hex');

function decodeEntities(s = '') {
    return s
        .replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, '$1')
        .replace(/&lt;/g, '<').replace(/&gt;/g, '>')
        .replace(/&quot;/g, '"').replace(/&#0?39;|&apos;/g, "'")
        .replace(/&nbsp;/g, ' ')
        .replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(+n))
        .replace(/&#x([0-9a-f]+);/gi, (_, n) => String.fromCodePoint(parseInt(n, 16)))
        .replace(/&amp;/g, '&');
}
const stripHtml = (s = '') => decodeEntities(s.replace(/<[^>]+>/g, ' ')).replace(/\s+/g, ' ').trim();

function slugify(s = '') {
    return s.normalize('NFD').replace(/[̀-ͯ]/g, '')   // bỏ dấu tiếng Việt
        .replace(/[đĐ]/g, (c) => (c === 'đ' ? 'd' : 'D'))
        .toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '')
        .slice(0, 60) || 'bai';
}
const tag = (xml, name) => {
    const m = xml.match(new RegExp(`<${name}(?:\\s[^>]*)?>([\\s\\S]*?)</${name}>`, 'i'));
    return m ? decodeEntities(m[1]).trim() : '';
};
const attr = (xml, name, a) => {
    const m = xml.match(new RegExp(`<${name}\\b[^>]*\\b${a}=["']([^"']+)["']`, 'i'));
    return m ? m[1] : '';
};

function parseFeed(xml) {
    const blocks = xml.match(/<item\b[\s\S]*?<\/item>/gi) || xml.match(/<entry\b[\s\S]*?<\/entry>/gi) || [];
    return blocks.map((b) => {
        const link =
            tag(b, 'link') ||
            attr(b, 'link', 'href') ||
            tag(b, 'guid');
        return {
            title: stripHtml(tag(b, 'title')) || '(không tiêu đề)',
            link: link.trim(),
            id: (tag(b, 'guid') || tag(b, 'id') || link || tag(b, 'title')).trim(),
            summary: stripHtml(tag(b, 'description') || tag(b, 'summary') || tag(b, 'content') || tag(b, 'content:encoded')),
            date: tag(b, 'pubDate') || tag(b, 'updated') || tag(b, 'published') || '',
        };
    });
}

/* ---------- main ---------- */
async function main() {
    await loadDotenv(path.resolve(HERE, '.env'));

    const CFG = {
        contentDir: path.resolve(HERE, 'content'),
        seenFile: path.resolve(HERE, '.rss-seen.json'),
        max: Number(flag('--limit', process.env.RSS_MAX_PER_RUN || 5)),
        visibility: (process.env.RSS_VISIBILITY || 'public').toLowerCase(),
        publish: /^(1|true|yes)$/i.test(process.env.RSS_PUBLISH || ''),
        tag: (process.env.RSS_TAG || 'tin-tuc').replace(/^#/, ''),
    };

    // nguồn feed
    let feeds = [];
    const feedsPath = path.resolve(HERE, flag('--feeds', 'feeds.txt'));
    if (existsSync(feedsPath)) {
        feeds = (await readFile(feedsPath, 'utf8')).split(/\r?\n/)
            .map((l) => l.trim()).filter((l) => l && !l.startsWith('#'));
    }
    if (process.env.RSS_FEEDS) feeds.push(...process.env.RSS_FEEDS.split(',').map((s) => s.trim()).filter(Boolean));
    feeds = [...new Set(feeds)];
    if (!feeds.length) {
        console.error(`Chưa có feed. Tạo ${feedsPath} (mỗi dòng 1 URL) hoặc đặt RSS_FEEDS trong .env.`);
        process.exit(2);
    }

    const seen = existsSync(CFG.seenFile) ? JSON.parse(await readFile(CFG.seenFile, 'utf8')) : {};
    const existing = existsSync(CFG.contentDir)
        ? new Set((await readdir(CFG.contentDir)).filter((f) => f.endsWith('.md')))
        : new Set();

    console.log(`Feed: ${feeds.length}  |  tối đa ${CFG.max} bài/lần  |  ${DRY ? 'DRY-RUN' : 'GHI FILE'}  |  ${CFG.publish ? 'xuất bản' : 'nháp'}\n`);

    let made = 0;
    for (const url of feeds) {
        if (made >= CFG.max) break;
        let xml;
        try {
            const res = await fetch(url, { headers: { 'user-agent': 'auto-poster/1.0 (+rss)' }, redirect: 'follow' });
            if (!res.ok) { console.log(`✗ ${url} — HTTP ${res.status}`); continue; }
            xml = await res.text();
        } catch (e) { console.log(`✗ ${url} — ${e.cause?.code || e.message}`); continue; }

        const items = parseFeed(xml);
        if (!items.length) { console.log(`· ${url} — không đọc được entry nào`); continue; }
        console.log(`  ${url} → ${items.length} entry`);

        for (const it of items) {
            if (made >= CFG.max) break;
            const key = sha1(it.id || it.link || it.title).slice(0, 16);
            if (seen[key]) continue;

            const d = it.date ? new Date(it.date) : new Date();
            const stamp = (isNaN(d) ? new Date() : d).toISOString().slice(0, 10).replace(/-/g, '');
            const fname = `rss-${stamp}-${slugify(it.title)}.md`;
            if (existing.has(fname)) { seen[key] = { at: new Date().toISOString(), file: fname }; continue; }

            const title = it.title.replace(/["\r\n]+/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 200);
            const body =
                (it.summary ? it.summary.slice(0, 900).trim() : title) +
                (it.link ? `\n\nNguồn: ${it.link}` : '');
            const md =
                `---\n` +
                `title: "${title}"\n` +
                `visibility: ${['friends', 'private'].includes(CFG.visibility) ? CFG.visibility : 'public'}\n` +
                `tags: [${CFG.tag}]\n` +
                `publish: ${CFG.publish}\n` +
                (it.link ? `source: ${it.link}\n` : '') +
                `---\n${body}\n`;

            console.log(`  ${DRY ? '○' : '✓'} ${fname}  — "${title.slice(0, 60)}"`);
            if (!DRY) {
                await writeFile(path.join(CFG.contentDir, fname), md, 'utf8');
                seen[key] = { at: new Date().toISOString(), file: fname, link: it.link };
                existing.add(fname);
            }
            made++;
        }
    }

    if (!DRY) await writeFile(CFG.seenFile, JSON.stringify(seen, null, 2));
    console.log(`\nXong. ${DRY ? 'Sẽ tạo' : 'Đã tạo'} ${made} file .md.` + (made && !DRY ? ' Chạy `node post.mjs` để đăng.' : ''));
}

main().catch((e) => { console.error(e); process.exit(1); });
