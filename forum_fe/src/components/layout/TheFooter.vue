<template>
    <footer class="site-footer app-footer">
        <div class="site-footer__inner">
            <div class="site-footer__brand">
                <span class="site-footer__logo">HIPDN-EA</span>
                <span class="text-sm">Diễn đàn</span>
            </div>

            <nav class="site-footer__links">
                <RouterLink to="/">Trang chủ</RouterLink>
                <RouterLink to="/users">Tìm người dùng</RouterLink>
                <RouterLink to="/notifications">Thông báo</RouterLink>
                <RouterLink to="/saved">Đã lưu</RouterLink>
            </nav>

            <div class="site-footer__meta muted text-sm">
                © {{ year }} The Legacy of 'Di Đan'
            </div>
        </div>
    </footer>
</template>

<script setup>
import { RouterLink } from 'vue-router';
const year = new Date().getFullYear();
</script>

<style scoped>
.site-footer__inner {
    max-width: 1120px;
    margin: 0 auto;
    padding: 22px 16px;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px 28px;
}
.site-footer__brand {
    display: flex;
    align-items: baseline;
    gap: 9px;
    flex: 1 1 auto;
}
/* Cùng dấu hiệu nhận diện với header: khối vàng nghệ đóng lên nền mực */
.site-footer__logo {
    background: var(--turmeric);
    color: var(--ink);
    font-weight: 800;
    font-size: 14px;
    letter-spacing: .04em;
    padding: 2px 7px 3px;
}
.site-footer__links {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 20px;
    font-size: 14px;
}
.site-footer__links a:hover { text-decoration: underline; text-underline-offset: 3px; }
.site-footer__meta {
    flex: 1 1 100%;
    text-align: center;
    padding-top: 10px;
    border-top: 1px solid rgba(255, 255, 255, .18);
    opacity: .75;
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
