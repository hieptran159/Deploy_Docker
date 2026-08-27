// Cấu hình endpoint tập trung. Ghi đè bằng biến môi trường Vite (.env / .env.local):
//   VITE_API_URL, VITE_SOCKET_URL
export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081';
export const SOCKET_URL = import.meta.env.VITE_SOCKET_URL || 'http://localhost:8082';
export const IMAGE_BASE = API_URL + '/images/';
