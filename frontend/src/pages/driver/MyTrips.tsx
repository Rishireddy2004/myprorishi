import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyTrips, cancelTrip, reopenTrip } from '../../api/trips'
import { getTrustStats } from '../../api/auth'
import { getErrorMessage } from '../../api/errorUtils'

const statusColor: Record<string, string> = {
  OPEN: 'bg-blue-100 text-blue-700',
  IN_PROGRESS: 'bg-green-100 text-green-700',
  COMPLETED: 'bg-indigo-100 text-indigo-700',
  CANCELLED: 'bg-red-100 text-red-600',
}

export default function MyTrips() {
  const [trips, setTrips] = useState<any[]>([])
  const [actionError, setActionError] = useState('')
  const [trustStats, setTrustStats] = useState<any>(null)
  const navigate = useNavigate()

  const load = () => getMyTrips().then((d) => setTrips(d.trips || d || []))
  useEffect(() => {
    load()
    getTrustStats().then(setTrustStats).catch(() => {})
  }, [])

  const handleCancel = async (id: string) => {
    if (!confirm('Cancel this trip?')) return
    setActionError('')
    try {
      await cancelTrip(id)
      load()
    } catch (err: any) {
      setActionError(getErrorMessage(err, 'Failed to cancel trip'))
    }
  }

  const handleReopen = async (id: string) => {
    if (!confirm('Reopen this cancelled trip?')) return
    setActionError('')
    try {
      await reopenTrip(id)
      load()
    } catch (err: any) {
      setActionError(getErrorMessage(err, 'Failed to reopen trip'))
    }
  }

  return (
    <div className="max-w-3xl mx-auto p-6">
      {/* Hero banner */}
      <div className="relative rounded-2xl overflow-hidden mb-6 h-40">
        <img
          src="https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=900&auto=format&fit=crop"
          alt="Driver on road"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-indigo-900/55 flex items-center justify-between px-8">
          <div className="text-white">
            <h1 className="text-2xl font-bold">My Trips</h1>
            <p className="text-indigo-200 text-sm mt-1">Manage your posted rides</p>
          </div>
          <button onClick={() => navigate('/post-trip')}
            className="bg-white text-indigo-700 px-5 py-2 rounded-xl font-semibold text-sm hover:bg-indigo-50 transition-colors shadow">
            + Post Trip
          </button>
        </div>
      </div>

      {actionError && (
        <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-xl px-4 py-3 mb-4 flex items-center gap-2">
          <span>⚠️</span>{actionError}
        </div>
      )}

      {/* Trust Score Banner */}
      {trustStats !== null && (
        <div className={`rounded-2xl p-4 mb-5 flex items-center gap-4 ${
          trustStats.trustScore >= 50 ? 'bg-gradient-to-r from-indigo-600 to-purple-600 text-white' :
          trustStats.trustScore >= 10 ? 'bg-gradient-to-r from-blue-500 to-indigo-500 text-white' :
          'bg-gray-50 border border-gray-200'
        }`}>
          <div className="text-3xl shrink-0">
            {trustStats.trustScore >= 50 ? '🏆' : trustStats.trustScore >= 10 ? '⭐' : '🚗'}
          </div>
          <div className="flex-1">
            <p className={`font-bold text-sm ${trustStats.trustScore >= 10 ? 'text-white' : 'text-gray-700'}`}>
              {trustStats.trustScore >= 50 ? 'Trusted Driver' : trustStats.trustScore >= 10 ? 'Rising Driver' : 'New Driver'}
            </p>
            <p className={`text-xs mt-0.5 ${trustStats.trustScore >= 10 ? 'text-white/80' : 'text-gray-400'}`}>
              {trustStats.trustScore >= 50
                ? 'Your trips appear first in passenger searches'
                : trustStats.trustScore >= 10
                ? 'Keep earning trust to rank higher in search'
                : 'Passengers redeeming points on your trips builds trust'}
            </p>
          </div>
          <div className="text-right shrink-0">
            <p className={`text-2xl font-black ${trustStats.trustScore >= 10 ? 'text-white' : 'text-indigo-600'}`}>
              {trustStats.trustScore}
            </p>
            <p className={`text-xs ${trustStats.trustScore >= 10 ? 'text-white/70' : 'text-gray-400'}`}>
              trust pts
            </p>
          </div>
        </div>
      )}

      {trips.length === 0 && (
        <div className="text-center py-16 text-gray-400">
          <img src="https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400&auto=format&fit=crop"
            alt="Empty road" className="w-48 h-32 object-cover rounded-xl mx-auto mb-4 opacity-50"/>
          <p className="font-medium">No trips posted yet</p>
          <p className="text-sm mt-1">Post your first trip to start earning</p>
        </div>
      )}

      <div className="space-y-4">
        {trips.map((t: any) => (
          <div key={t.id} className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md transition-shadow">
            <div className="flex">
              <div className="w-2 bg-indigo-500 shrink-0" />
              <div className="flex-1 p-5 flex justify-between items-start">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="w-2 h-2 rounded-full bg-green-500" />
                    <p className="font-semibold text-gray-800 text-sm">{t.originAddress}</p>
                  </div>
                  <div className="flex items-center gap-2 mb-3">
                    <span className="w-2 h-2 rounded-full bg-red-500" />
                    <p className="font-semibold text-gray-800 text-sm">{t.destinationAddress}</p>
                  </div>
                  <div className="flex flex-wrap gap-3 text-xs text-gray-500">
                    <span>🕐 {t.departureTime}</span>
                    <span>💺 {t.availableSeats} seats</span>
                    <span>₹{t.pricePerSeat}/seat</span>
                  </div>
                  <span className={`inline-block mt-2 text-xs font-medium px-2 py-0.5 rounded-full ${statusColor[t.status] || 'bg-gray-100 text-gray-500'}`}>
                    {t.status}
                  </span>
                </div>
                <div className="flex flex-col gap-2 ml-4">
                  <button onClick={() => navigate(`/manage-bookings/${t.id}`)}
                    className="text-xs bg-indigo-100 text-indigo-700 px-3 py-1.5 rounded-lg hover:bg-indigo-200 font-medium">
                    Bookings
                  </button>
                  {t.status === 'OPEN' && (
                    <button onClick={() => navigate(`/active-trip/${t.id}`)}
                      className="text-xs bg-green-100 text-green-700 px-3 py-1.5 rounded-lg hover:bg-green-200 font-medium">
                      Start
                    </button>
                  )}
                  {t.status === 'OPEN' && (
                    <button onClick={() => handleCancel(t.id)}
                      className="text-xs bg-red-100 text-red-600 px-3 py-1.5 rounded-lg hover:bg-red-200 font-medium">
                      Cancel
                    </button>
                  )}
                  {t.status === 'CANCELLED' && new Date(t.departureTime) > new Date() && (
                    <button onClick={() => handleReopen(t.id)}
                      className="text-xs bg-amber-100 text-amber-700 px-3 py-1.5 rounded-lg hover:bg-amber-200 font-medium">
                      ↩ Reopen
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
