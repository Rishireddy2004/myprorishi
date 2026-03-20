import api from './client'

export const getFareEstimate = (origin: string, destination: string) =>
  api.get('/fare/estimate', { params: { origin, destination } }).then((r) => r.data)
