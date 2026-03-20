import api from './client'

export const updateLocation = (tripId: string, lat: number, lng: number) =>
  api.post(`/trips/${tripId}/location`, { lat, lng }).then((r) => r.data)

export const getLocation = (tripId: string) =>
  api.get(`/trips/${tripId}/location`).then((r) => r.data)
