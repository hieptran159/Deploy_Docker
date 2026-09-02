import axios from 'axios';

import { LOCALKEYS, getItemLocal, setItemLocal, clearAuth } from './localStorage';
import { API_URL } from '../config';

const BASE_URL = API_URL + "/";

// --- Tự động làm mới access token khi hết hạn (một lần cho nhiều request 401 song song) ---
let refreshPromise = null;

const clearSessionAndRedirect = () => {
	clearAuth();
	if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
		window.location.assign('/login');
	}
};

// Gọi /auth/refresh bằng axios "trần" (không qua interceptor) -> tránh đệ quy.
const doRefresh = () => {
	if (refreshPromise) return refreshPromise;
	const rt = getItemLocal(LOCALKEYS.REFRESH_TOKEN);
	if (!rt) return Promise.reject(new Error('no-refresh-token'));
	refreshPromise = axios
		.post(`${BASE_URL}auth/refresh`, null, { params: { refreshToken: rt } })
		.then((res) => {
			const d = res?.data?.data || {};
			if (!d.accessToken) throw new Error('refresh-failed');
			setItemLocal(LOCALKEYS.ACCESS_TOKEN, d.accessToken);
			if (d.refreshToken) setItemLocal(LOCALKEYS.REFRESH_TOKEN, d.refreshToken);
			if (d.isAdmin !== undefined) setItemLocal(LOCALKEYS.IS_ADMIN, d.isAdmin === '1' || d.isAdmin === true);
			return d.accessToken;
		})
		.finally(() => { refreshPromise = null; });
	return refreshPromise;
};

/**
 * Khởi tạo cách truyền và xử lí Rest-API
 * @param {import('axios').CreateAxiosDefaults} config
 * @param {{auth: boolean, silent: boolean}} param2
 * @returns {import('axios').AxiosInstance}
 */
export const createApiInstance = (config, { auth = true, silent } = {}) => {
	const api = axios.create(config);

	api.interceptors.request.use(
		(config) => {
			if (auth && config?.headers) {
				const tk = getItemLocal(LOCALKEYS.ACCESS_TOKEN);
				// Khách (chưa đăng nhập) vẫn gọi được các endpoint công khai -> không gắn header rỗng
				if (tk) config.headers.Authorization = `Bearer ${tk}`;
			}
			return config;
		},
		(error) => {
			Promise.reject(error);
		},
	);
	api.interceptors.response.use(
		/**
		 * Nếu response nhận về là json về convert về dạng camelCase
		
		 * @param {import('axios').AxiosResponse} response
		 * @returns {import('axios').AxiosResponse}
		 */
		(response) => {
			return response;
		},
		/**
		 * Xử lí các trường hợp lỗi
		 
		 * @param {import('axios').AxiosError} error
		 * @returns {{message: string, data: object}} data
		 */
		async (error) => {
			if (!silent) {
				console.log(error);
			}

			const status = error?.response?.status;
			const original = error?.config || {};
			const url = original?.url || '';
			const isAuthCall = url.includes('/auth/');

			if (status === 401 && typeof window !== 'undefined') {
				const hadToken = !!getItemLocal(LOCALKEYS.ACCESS_TOKEN);

				// Khách chưa đăng nhập gọi endpoint công khai bị 401 -> bỏ qua, không đá về login.
				if (!hadToken) {
					return Promise.reject(error?.response?.data ?? error);
				}

				// Có phiên + chưa thử refresh + không phải chính lời gọi /auth/* -> thử làm mới token 1 lần.
				if (!original._retry && !isAuthCall && getItemLocal(LOCALKEYS.REFRESH_TOKEN)) {
					original._retry = true;
					try {
						const newToken = await doRefresh();
						original.headers = original.headers || {};
						original.headers.Authorization = `Bearer ${newToken}`;
						return api(original); // phát lại request gốc với token mới
					} catch (e) {
						clearSessionAndRedirect();
						return Promise.reject(error?.response?.data ?? error);
					}
				}

				// Không refresh được (hết refresh token / đã thử / lỗi login) -> kết thúc phiên.
				if (!isAuthCall) {
					clearSessionAndRedirect();
				}
			}

			return Promise.reject(error?.response?.data ?? error);
		},
	);

	return api;
};

/**
 * @description Tạo một apiInstance với content là json, không cần đăng nhập
 */
export const api = createApiInstance(
	{
		baseURL: BASE_URL,
		headers: {
			'Content-Type': 'application/json',
			Accept: '*/*',
		},
	},
	{ auth: false, silent: false },
);

/**
 * @description Tạo một apiInstance với content là form, không cần đăng nhập
 */
export const apiForm = createApiInstance(
	{
		baseURL: BASE_URL,
		headers: {
			'Content-Type': 'multipart/form-data',
			Accept: '*/*',
		},
	},
	{ auth: false, silent: false },
);

/**
 * @description Tạo một apiInstance với content là json, cần đăng nhập
 */
export const authApi = createApiInstance(
	{
		baseURL: BASE_URL,
		headers: {
			'Content-Type': 'application/json',
			Accept: '*/*',
		},
	},
	{ auth: true, silent: false },
);

/**
 * @description Tạo một apiInstance với content là form-data, cần đăng nhập
 */
export const authApiFormData = createApiInstance(
	{
		baseURL: BASE_URL,
		headers: {
			'Content-Type': 'multipart/form-data',
			Accept: '*/*',
		},
	},
	{ auth: true, silent: false },
);