import { api, apiForm, authApi } from '../storages/api.js';

export const login = (param) => {
    return api.post(`/auth/signin?email=${param.email}&password=${param.password}`)
}

export const signup = (data) => {
    return apiForm.post("/auth/signup", data);
}

export const logout = () => {
    return authApi.post("/auth/logout");
}

export const requestResetToken = (email) => {
    return api.post(`/auth/token-reset?email=${encodeURIComponent(email)}`);
}

export const resetPassword = (token, newPassword) => {
    return api.patch(`/auth/reset?token=${encodeURIComponent(token)}&newPassword=${encodeURIComponent(newPassword)}`);
}

export const verifyEmail = (email, code) => {
    return api.post(`/auth/verify?email=${encodeURIComponent(email)}&code=${encodeURIComponent(code)}`);
}

export const resendVerify = (email) => {
    return api.post(`/auth/resend-verify?email=${encodeURIComponent(email)}`);
}