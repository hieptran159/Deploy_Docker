import axios from 'axios';

import { LOCALKEYS, getItemLocal } from './localStorage';
import { API_URL } from '../config';

const BASE_URL = API_URL + "/";

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
		(error) => {
			if (!silent) {
				console.log(error);
			}

			const status = error?.response?.status;
			if (status === 401 && typeof window !== 'undefined') {
				// Chỉ đá về /login khi ĐÃ có phiên (token hết hạn/không hợp lệ).
				// Khách chưa đăng nhập gọi endpoint công khai bị 401 thì bỏ qua.
				const hadToken = !!getItemLocal(LOCALKEYS.ACCESS_TOKEN);
				try {
					Object.values(LOCALKEYS).forEach((k) => localStorage.removeItem(k));
				} catch (e) {
					/* ignore */
				}
				if (hadToken && window.location.pathname !== '/login') {
					window.location.assign('/login');
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