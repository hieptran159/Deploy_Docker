export const LOCALKEYS = {
    ACCESS_TOKEN: "Token",
    REFRESH_TOKEN: "RefreshToken",
    USER_ID: "UserId",
	USER_NAME: "UserName",
	LINK_AVT: "linkAvt",
	IS_ADMIN: "isAdmin"
}

// Cờ (luôn nằm ở localStorage) quyết định các khoá phiên lưu ở đâu:
//  - 'local'   -> localStorage : "ghi nhớ đăng nhập", sống qua các lần đóng/mở trình duyệt
//  - 'session' -> sessionStorage : phiên tạm, đóng tab/trình duyệt là mất -> phải đăng nhập lại
const PERSIST_FLAG = "__authPersist";
const AUTH_KEYS = new Set(Object.values(LOCALKEYS));

/**
 * @description Đặt chế độ lưu phiên. Gọi TRƯỚC khi setItemLocal các khoá token lúc đăng nhập.
 * @param {boolean} remember true = ghi nhớ (localStorage), false = phiên tạm (sessionStorage)
 */
export function setAuthPersistence(remember) {
	try { localStorage.setItem(PERSIST_FLAG, remember ? "local" : "session"); } catch (e) { /* ignore */ }
}

export function isRememberSession() {
	try { return localStorage.getItem(PERSIST_FLAG) !== "session"; } catch (e) { return true; }
}

function authStore() {
	try {
		return localStorage.getItem(PERSIST_FLAG) === "session" ? sessionStorage : localStorage;
	} catch (e) {
		return localStorage;
	}
}

// Khoá phiên -> store theo cờ; mọi khoá khác -> localStorage như cũ.
function storeFor(key) {
	return AUTH_KEYS.has(key) ? authStore() : localStorage;
}

/**
 * @description Lưu thông tin vào storage
 * @param {string} key
 * @param {any} value
 */
export function setItemLocal(key, value) {
	try { storeFor(key).setItem(key, JSON.stringify(value)); } catch (e) { /* ignore */ }
}

/**
 * @description Lấy thông tin từ storage
 * @param {string} key
 * @returns
 */
export function getItemLocal(key) {
	let value = null;
	try { value = storeFor(key).getItem(key); } catch (e) { /* ignore */ }
	if (value) {
		return JSON.parse(value);
	}
	return null;
}
/**
 * @description Xoá 1 khoá
 * @param {string} key
 * @returns
 */
export function delItemLocal(key) {
	try { storeFor(key).removeItem(key); } catch (e) { /* ignore */ }
}

/**
 * @description Xoá toàn bộ phiên ở CẢ hai store + cờ chế độ (đăng xuất / hết phiên)
 */
export function clearAuth() {
	try {
		Object.values(LOCALKEYS).forEach((k) => {
			localStorage.removeItem(k);
			sessionStorage.removeItem(k);
		});
		localStorage.removeItem(PERSIST_FLAG);
	} catch (e) { /* ignore */ }
}
