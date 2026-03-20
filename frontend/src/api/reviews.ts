import api from './client'

// POST /bookings/{id}/review — passenger reviews driver from a booking
export const createReview = (data: { bookingId: string; rating: number; comment?: string }) =>
  api.post(`/bookings/${data.bookingId}/review`, { rating: data.rating, comment: data.comment })
    .then((r) => r.data)

export const getUserReviews = (userId: string) =>
  api.get(`/users/${userId}/reviews`).then((r) => r.data)
