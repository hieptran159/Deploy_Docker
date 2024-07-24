export const LOCALKEYS = {
    ACCESS_TOKEN: "Token",
    USER_ID: "UserId",
	USER_NAME: "UserName",
	LINK_AVT: "linkAvt"
}

/**
 * @description Lưu thông tin vào localStorage
 * @param {string} key
 * @param {any} value
 */
export function setItemLocal(key, value) {
	localStorage.setItem(key, JSON.stringify(value));
}

/**
 * @description Lấy thông tin từ localStorage
 * @param {string} key
 * @returns
 */
export function getItemLocal(key) {
	const value = localStorage.getItem(key);
	if (value) {
		return JSON.parse(value);
	}
	return null;
}
/**
 * @description Xoá thông tin
 * @param {string} key
 * @returns
 */
export function delItemLocal(key) {
	localStorage.removeItem(key);
}