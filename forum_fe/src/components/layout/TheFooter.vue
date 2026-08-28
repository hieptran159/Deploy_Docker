<template>
    <footer class="site-footer">
        <div class="site-footer__inner">
            <div class="site-footer__brand">
                <span class="site-footer__logo">HIPDN·EA</span>
                <span class="muted text-sm">Diễn đàn mạng xã hội</span>
            </div>

            <nav class="site-footer__links">
                <RouterLink to="/">Trang chủ</RouterLink>
                <RouterLink to="/users">Tìm người dùng</RouterLink>
                <RouterLink to="/notifications">Thông báo</RouterLink>
                <RouterLink to="/saved">Đã lưu</RouterLink>
            </nav>

            <div class="site-footer__meta muted text-sm">
                © {{ year }} HIPDN·EA Forum
            </div>
        </div>
    </footer>
</template>

<script setup>
import { RouterLink } from 'vue-router';
const year = new Date().getFullYear();
</script>

<style scoped>
.site-footer {
    border-top: 1px solid var(--border);
    background: var(--surface);
    margin-top: auto;
}
.site-footer__inner {
    max-width: 960px;
    margin: 0 auto;
    padding: 20px 16px;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px 24px;
}
.site-footer__brand {
    display: flex;
    align-items: baseline;
    gap: 8px;
    flex: 1 1 auto;
}
.site-footer__logo {
    font-weight: 800;
    letter-spacing: .5px;
    color: var(--brand);
}
.site-footer__links {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 18px;
    font-size: 14px;
}
.site-footer__links a {
    color: var(--text-muted);
}
.site-footer__links a:hover {
    color: var(--brand);
    text-decoration: underline;
}
.site-footer__meta {
    flex: 1 1 100%;
    text-align: center;
    padding-top: 8px;
    border-top: 1px dashed var(--border);
}
@media (min-width: 640px) {
    .site-footer__meta {
        flex: 0 0 auto;
        text-align: right;
        padding-top: 0;
        border-top: none;
    }
}
</style>
