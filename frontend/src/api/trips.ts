import api from './client'

export const searchTrips = (params: {
  origin: string
  destination: string
  date?: string
  seats?: number
  timeFrom?: string
  timeTo?: string
}) => api.get('/trips/search', { params }).then((r) => r.data)

export const getOpenTrips = () => api.get('/trips/open').then((r) => r.data)

export const createTrip = (data: object) => api.post('/trips', data).then((r) => r.data)

export const getTrip = (id: string) => api.get(`/trips/${id}`).then((r) => r.data)

export const getMyTrips = () => api.get('/trips/my').then((r) => r.data)

export const updateTrip = (id: string, data: object) =>
  api.patch(`/trips/${id}`, data).then((r) => r.data)

export const updateTripStatus = (id: string, status: string, lat?: number, lng?: number) =>
  api.patch(`/trips/${id}/status`, { status, currentLat: lat, currentLng: lng }).then((r) => r.data)

export const cancelTrip = (id: string) =>
  api.delete(`/trips/${id}`).then((r) => r.data)

export const reopenTrip = (id: string) =>
  api.post(`/trips/${id}/reopen`).then((r) => r.data)
