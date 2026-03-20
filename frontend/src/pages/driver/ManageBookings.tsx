import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getTripBookings, confirmBooking, rejectBooking } from '../../api/bookings'
import { getErrorMessage } from '../../api/errorUtils'

const statusStyle: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  CONFIRMED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-600',
  COMPLETED: 'bg-indigo-100 text-indigo-700',
  REJECTED: 'bg-gray-100 text-gray-500',
}

// Deterministic avatar color from name
const avatarColor = (name: string) => {
  const colors = ['bg-indigo-500', 'bg-purple-500', 'bg-pink-500', 'bg-blue-500', 'bg-teal-500', 'bg-orange-500']
  return colors[(name?.charCodeAt(0) || 0) % colors.length]
}

export default function ManageBookings() {
  const { tripId } = useParams()
  const [bookings, setBookings] = useState<any[]>([])
  const [actionError, setActionError] = useState('')

  const load = () => getTripBookings(tripId!).then((d) => setBookings(d.bookings || d || []))
  useEffect(() => { load() }, [tripId])

  const handleAction = async (fn: () => Promise<any>) => {
    setActionError('')
    try { await fn(); load() } catch (err: any) { setActionError(getErrorMessage(err, 'Action failed')) }
  }

  return (
    <div className="max-w-3xl mx-auto p-6">
      {/* Header with photo */}
      <div className="relative rounded-2xl overflow-hidden mb-6 h-36">
        <img
          src="https://images.unsplash.com/photo-1521791136064-7986c2920216?w=900&auto=format&fit=crop"
          alt="Passengers"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-indigo-900/60 flex items-center px-8">
          <div className="text-white">
            <div className="flex items-center gap-3">
              <span className="text-3xl car-drive">🚗</span>
              <h1 className="text-2xl font-bold">Manage Bookings</h1>
            </div>
            <p className="text-indigo-200 text-sm mt-1">Trip #{tripId} · {bookings.length} booking{bookings.length !== 1 ? 's' : ''}</p>
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
          <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-3">
            <svg className="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/>
            </svg>
          </div>
          <p className="font-medium">No bookings yet</p>
          <p className="text-sm mt-1">Passengers will appear here once they book</p>
        </div>
      )}

      <div className="space-y-3">
        {bookings.map((b: any) => (
          <div key={b.id} className={`bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex justify-between items-center transition-all booking-card-glow ${b.status === 'PENDING' ? 'pulse-ring' : ''}`}>
            <div className="flex items-center gap-4">
              <div className={`w-11 h-11 rounded-full ${avatarColor(b.passengerName)} flex items-center justify-center text-white font-bold text-lg shrink-0`}>
                {b.passengerName?.charAt(0)?.toUpperCase() || '?'}
              </div>
              <div>
                <p className="font-semibold text-gray-800">{b.passengerName}</p>
                <p className="text-sm text-gray-500">{b.seats} seat(s) · ₹{b.totalFare}</p>
                {b.passengerPhone && (
                  <div className="flex gap-2 mt-1.5 flex-wrap">
                    <a href={`tel:${b.passengerPhone}`}
                      className="inline-flex items-center gap-1 text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-lg hover:bg-blue-200 font-medium btn-ripple">
                      📞 {b.passengerPhone}
                    </a>
                    <a href={`https://wa.me/${b.passengerPhone.replace(/\D/g,'')}`} target="_blank" rel="noreferrer"
                      className="inline-flex items-center gap-1 text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-lg hover:bg-green-200 font-medium btn-ripple">
                      💬 WhatsApp
                    </a>
                  </div>
                )}
                <span className={`inline-block mt-1 text-xs font-medium px-2 py-0.5 rounded-full ${statusStyle[b.status] || 'bg-gray-100 text-gray-500'}`}>
                  {b.status}
                </span>
              </div>
            </div>
            {b.status === 'PENDING' && (
              <div className="flex gap-2">
                <button onClick={() => handleAction(() => confirmBooking(b.id))}
                  className="bg-green-600 text-white text-xs px-4 py-2 rounded-lg hover:bg-green-500 active:scale-95 font-medium transition-all btn-ripple btn-glow-green">
                  ✓ Accept
                </button>
                <button onClick={() => handleAction(() => rejectBooking(b.id))}
                  className="bg-red-100 text-red-600 text-xs px-4 py-2 rounded-lg hover:bg-red-200 active:scale-95 font-medium transition-all btn-ripple btn-glow-red">
                  ✕ Reject
                </button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
