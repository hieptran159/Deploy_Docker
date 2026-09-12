import './main.css';
import { createApp } from 'vue';
import App from './App.vue';
import router from './router/index';

// khôi phục theme trước khi mount để tránh nhấp nháy
try {
    if (localStorage.getItem('theme') === 'dark') {
        document.documentElement.dataset.theme = 'dark';
    }
} catch (e) { /* ignore */ }

const app = createApp(App);

app.use(router);

app.mount("#app");
