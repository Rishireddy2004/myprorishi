import api from './client'

// Backend endpoint: POST /trips/{tripId}/bookings
export const createBooking = (tripId: string, seats: number, redeemPoints = 0, tipAmount = 0) =>
  api.post(`/trips/${tripId}/bookings`, { seats, redeemPoints, tipAmount }).then((r) => r.data)

export const getMyBookings = () => api.get('/bookings/my').then((r) => r.data)

export const getTripBookings = (tripId: string) =>
  api.get(`/bookings/trip/${tripId}`).then((r) => r.data)

export const cancelBooking = (id: string) =>
  api.delete(`/bookings/${id}`).then((r) => r.data)

export const confirmBooking = (id: string) =>
  api.patch(`/bookings/${id}/confirm`).then((r) => r.data)

export const rejectBooking = (id: string) =>
  api.patch(`/bookings/${id}/reject`).then((r) => r.data)

export const getCoPassengers = (tripId: string) =>
  api.get(`/trips/${tripId}/co-passengers`).then((r) => r.data)
