import { api, apiForm } from '../storages/api.js';

export const login = (param) => {
    return api.post(`/auth/signin?email=${param.email}&password=${param.password}`)
}

export const signup = (data) => {
    return apiForm.post("/auth/signup", data);
}