import api from './client'

export const getMetrics = () => api.get('/admin/metrics').then((r) => r.data)

export const getAdminUsers = (params?: object) =>
  api.get('/admin/users', { params }).then((r) => r.data)

export const suspendUser = (id: number) =>
  api.patch(`/admin/users/${id}/suspend`).then((r) => r.data)

export const unsuspendUser = (id: number) =>
  api.patch(`/admin/users/${id}/unsuspend`).then((r) => r.data)

export const getAdminTrips = (params?: object) =>
  api.get('/admin/trips', { params }).then((r) => r.data)

export const getDisputes = () => api.get('/admin/disputes').then((r) => r.data)

export const resolveDispute = (id: number, resolution: string) =>
  api.patch(`/admin/disputes/${id}/resolve`, { resolution }).then((r) => r.data)

export const getPlatformConfig = () => api.get('/admin/config').then((r) => r.data)

export const updatePlatformConfig = (data: object) =>
  api.put('/admin/config', data).then((r) => r.data)

export const processRefund = (bookingId: number, reason: string) =>
  api.post('/admin/refunds', { bookingId, reason }).then((r) => r.data)

export const adminResetPassword = (id: string, newPassword: string) =>
  api.post(`/admin/users/${id}/reset-password`, { newPassword }).then((r) => r.data)
