import 'devextreme/dist/css/dx.light.css';
import './main.css';
import { createApp } from 'vue';
import App from './App.vue';
import { locale, loadMessages } from 'devextreme/localization';
import viMessage from 'devextreme/localization/messages/vi.json';
import router from './router/index';

// khôi phục theme trước khi mount để tránh nhấp nháy
try {
    if (localStorage.getItem('theme') === 'dark') {
        document.documentElement.dataset.theme = 'dark';
    }
} catch (e) { /* ignore */ }

const app = createApp(App);

loadMessages(viMessage);
locale('vi-VN');

app.use(router);

app.mount("#app");
