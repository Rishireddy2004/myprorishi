import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyBookings, cancelBooking } from '../../api/bookings'
import { createReview } from '../../api/reviews'
import { getErrorMessage } from '../../api/errorUtils'

const statusStyle: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  CONFIRMED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-600',
  COMPLETED: 'bg-indigo-100 text-indigo-700',
}

export default function MyBookings() {
  const [bookings, setBookings] = useState<any[]>([])
  const [reviewForm, setReviewForm] = useState<{ bookingId: string; rating: number; comment: string } | null>(null)
  const [actionError, setActionError] = useState('')
  const navigate = useNavigate()

  const load = () => getMyBookings().then((d) => setBookings(d.bookings || d || []))
  useEffect(() => { load() }, [])

  const handleCancel = async (id: number) => {
    if (!confirm('Cancel this booking?')) return
    setActionError('')
    try {
      await cancelBooking(String(id))
      load()
    } catch (err: any) {
      setActionError(getErrorMessage(err, 'Failed to cancel booking'))
    }
  }

  const handleReview = async () => {
    if (!reviewForm) return
    try {
      await createReview(reviewForm)
      setReviewForm(null)
      load()
    } catch (err: any) {
      setActionError(getErrorMessage(err, 'Failed to submit review'))
    }
  }

  return (
    <div className="max-w-3xl mx-auto p-6">
      {/* Hero */}
      <div className="relative rounded-2xl overflow-hidden mb-6 h-36">
        <img
          src="https://images.unsplash.com/photo-1464219789935-c2d9d9aba644?w=900&auto=format&fit=crop"
          alt="Passengers in car"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-indigo-900/55 flex items-center px-8">
          <div className="text-white">
            <div className="flex items-center gap-3">
              <span className="text-3xl car-drive">🚗</span>
              <h1 className="text-2xl font-bold">My Bookings</h1>
            </div>
            <p className="text-indigo-200 text-sm mt-1">Track and manage your rides</p>
          </div>
        </div>
      </div>

      {actionError && (
        <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-xl px-4 py-3 mb-4 flex items-center gap-2">
          <span>⚠️</span>{actionError}
        </div>
      )}

      {bookings.length === 0 && (
        <div className="text-center py-12 text-gray-400">
          <img src="https://images.unsplash.com/photo-1506521781263-d8422e82f27a?w=400&auto=format&fit=crop"
            alt="Empty" className="w-48 h-28 object-cover rounded-xl mx-auto mb-4 opacity-40"/>
          <p className="font-medium">No bookings yet</p>
          <p className="text-sm mt-1">Search for a trip and book your first ride</p>
        </div>
      )}

      <div className="space-y-4">
        {bookings.map((b: any) => (
          <div key={b.id} className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden booking-card-glow transition-all">
            <div className="flex">
              <div className={`w-1.5 shrink-0 ${b.status === 'CONFIRMED' ? 'bg-green-500' : b.status === 'COMPLETED' ? 'bg-indigo-500' : b.status === 'CANCELLED' ? 'bg-red-400' : 'bg-yellow-400'}`} />
              <div className="flex-1 p-5">
                <div className="flex justify-between items-start">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="w-2 h-2 rounded-full bg-green-500 shrink-0" />
                      <p className="font-semibold text-gray-800 text-sm truncate">{b.tripOrigin}</p>
                    </div>
                    <div className="flex items-center gap-2 mb-3">
                      <span className="w-2 h-2 rounded-full bg-red-500 shrink-0" />
                      <p className="font-semibold text-gray-800 text-sm truncate">{b.tripDestination}</p>
                    </div>
                    <div className="flex flex-wrap gap-3 text-xs text-gray-500 mb-2">
                      <span>🕐 {b.departureTime}</span>
                      <span>💺 {b.seats} seat(s)</span>
                      <span>₹{b.totalFare}</span>
                    </div>
                    <span className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full ${statusStyle[b.status] || 'bg-gray-100 text-gray-500'}`}>
                      {b.status}
                    </span>
                    {/* Points earned on completion */}
                    {b.status === 'COMPLETED' && (
                      <span className="inline-flex items-center gap-1 ml-2 text-xs font-semibold bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full">
                        ⭐ {Math.min(10, Math.max(5, 5 + Math.floor((b.totalFare || 0) / 50)))} pts earned
                      </span>
                    )}
                    {/* Driver contact info — shown after booking */}
                    {(b.status === 'PENDING' || b.status === 'CONFIRMED') && b.driverName && (
                      <div className="mt-3 bg-indigo-50 border border-indigo-100 rounded-xl p-3 space-y-1.5">
                        <p className="text-xs font-bold text-indigo-700">🚗 Driver Contact</p>
                        <p className="text-xs text-gray-700 font-medium">{b.driverName}</p>
                        {b.driverPhone && (
                          <div className="flex gap-2 flex-wrap">
                            <a href={`tel:${b.driverPhone}`}
                              className="inline-flex items-center gap-1 text-xs bg-blue-100 text-blue-700 px-2.5 py-1 rounded-lg hover:bg-blue-200 font-medium btn-ripple">
                              📞 {b.driverPhone}
                            </a>
                            <a href={`https://wa.me/${b.driverPhone.replace(/\D/g,'')}`} target="_blank" rel="noreferrer"
                              className="inline-flex items-center gap-1 text-xs bg-green-100 text-green-700 px-2.5 py-1 rounded-lg hover:bg-green-200 font-medium btn-ripple">
                              💬 WhatsApp
                            </a>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                  <div className="flex flex-col gap-2 ml-4">
                    {b.status === 'CONFIRMED' && (
                      <button onClick={() => navigate(`/track/${b.tripId}`)}
                        className="text-xs bg-indigo-100 text-indigo-700 px-3 py-1.5 rounded-lg hover:bg-indigo-200 font-medium">
                        📍 Track
                      </button>
                    )}
                    {['PENDING', 'CONFIRMED'].includes(b.status) && b.tripOrigin && b.tripDestination && (
                      <a
                        href={`https://www.google.com/maps/dir/?api=1&origin=${encodeURIComponent(b.tripOrigin)}&destination=${encodeURIComponent(b.tripDestination)}&travelmode=driving`}
                        target="_blank" rel="noreferrer"
                        className="text-xs bg-blue-100 text-blue-700 px-3 py-1.5 rounded-lg hover:bg-blue-200 font-medium text-center">
                        🗺️ Directions
                      </a>
                    )}
                    {b.status === 'COMPLETED' && !b.reviewed && (
                      <button onClick={() => setReviewForm({ bookingId: b.id, rating: 5, comment: '' })}
                        className="text-xs bg-yellow-100 text-yellow-700 px-3 py-1.5 rounded-lg hover:bg-yellow-200 font-medium">
                        ⭐ Review
                      </button>
                    )}
                    {['PENDING', 'CONFIRMED'].includes(b.status) && (
                      <button onClick={() => handleCancel(b.id)}
                        className="text-xs bg-red-100 text-red-600 px-3 py-1.5 rounded-lg hover:bg-red-200 font-medium">
                        Cancel
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Review modal */}
      {reviewForm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-sm shadow-2xl space-y-4">
            <div className="flex items-center gap-3 mb-2">
              <div className="w-10 h-10 bg-yellow-100 rounded-xl flex items-center justify-center text-xl">⭐</div>
              <h2 className="font-bold text-lg text-gray-800">Leave a Review</h2>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-600 block mb-2">Rating</label>
              <div className="flex gap-2">
                {[1,2,3,4,5].map((n) => (
                  <button key={n} type="button" onClick={() => setReviewForm({ ...reviewForm, rating: n })}
                    className={`text-2xl transition-transform hover:scale-110 ${n <= reviewForm.rating ? 'opacity-100' : 'opacity-30'}`}>
                    ⭐
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-600 block mb-1">Comment (optional)</label>
              <textarea placeholder="How was your ride?" value={reviewForm.comment}
                onChange={(e) => setReviewForm({ ...reviewForm, comment: e.target.value })}
                className="w-full border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 h-24 resize-none"/>
            </div>
            <div className="flex gap-3">
              <button onClick={handleReview} className="flex-1 bg-indigo-600 text-white py-2.5 rounded-xl hover:bg-indigo-500 text-sm font-medium">Submit</button>
              <button onClick={() => setReviewForm(null)} className="flex-1 border py-2.5 rounded-xl text-sm hover:bg-gray-50">Cancel</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
