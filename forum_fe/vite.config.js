import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  // Chạy FE local nhưng dùng backend đã deploy: đặt DEV_API_TARGET trong .env.local,
  // rồi để VITE_API_URL=/api-proxy. Request đi same-origin qua dev server nên không
  // vướng CORS. Chỉ có tác dụng với `npm run dev`, bản build không dùng tới.
  const proxyTarget = env.DEV_API_TARGET

  return {
    plugins: [
      vue(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: proxyTarget ? {
      proxy: {
        '/api-proxy': {
          target: proxyTarget,
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/api-proxy/, ''),
          // Backend chỉ cho phép origin của FE đã deploy. Bỏ Origin/Referer để
          // request đi qua như same-origin, không bị CORS chặn.
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq) => {
              proxyReq.removeHeader('origin');
              proxyReq.removeHeader('referer');
            });
          },
        },
      },
    } : undefined,
  }
})
