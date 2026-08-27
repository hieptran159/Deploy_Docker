import { authApi } from '@/storages/api';

// targetType: 'USER' | 'POST' | 'COMMENT'
export const sendReport = (targetType, targetId, reason = '') => {
    return authApi.post(`/report?targetType=${targetType}&targetId=${encodeURIComponent(targetId)}&reason=${encodeURIComponent(reason)}`);
}

export const getReports = (status = 'OPEN') => {
    return authApi.get(`/report/admin?status=${status}`);
}

export const handleReport = (reportId, status) => {
    return authApi.patch(`/report/admin/${reportId}?status=${status}`);
}

export const removeReportedTarget = (reportId) => {
    return authApi.post(`/report/admin/${reportId}/remove-target`);
}
