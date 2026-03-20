import api from './client'

export const getMyTransactions = () => api.get('/transactions/my').then((r) => r.data)
