import { useEffect, useState } from 'react'
import { getAdminTrips } from '../../api/admin'

const statusConfig: Record<string, { color: string; bg: string }> = {
  SCHEDULED:   { color: 'text-blue-700',   bg: 'bg-blue-50' },
  IN_PROGRESS: { color: 'text-green-700',  bg: 'bg-green-50' },
  COMPLETED:   { color: 'text-indigo-700', bg: 'bg-indigo-50' },
  CANCELLED:   { color: 'text-red-600',    bg: 'bg-red-50' },
}

export default function AdminTrips() {
  const [trips, setTrips] = useState<any[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)

  const load = (q?: string) =>
    getAdminTrips(q ? { search: q } : undefined)
      .then((d) => setTrips(d.trips || d || []))
      .finally(() => setLoading(false))

  useEffect(() => { load() }, [])

  return (
    <div className="max-w-4xl mx-auto p-4 sm:p-6">
      {/* Hero */}
      <div className="relative rounded-2xl overflow-hidden mb-6 h-36">
        <img
          src="https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=900&auto=format&fit=crop"
          alt="All trips overview"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-indigo-900/65 flex items-center px-8">
          <div className="text-white">
            <h1 className="text-2xl font-bold">All Trips</h1>
            <p className="text-indigo-200 text-sm mt-1">Monitor and manage platform trips</p>
          </div>
        </div>
      </div>

      <form onSubmit={(e) => { e.preventDefault(); setLoading(true); load(search) }} className="flex gap-3 mb-5">
        <div className="relative flex-1">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
          </svg>
          <input placeholder="Search trips by route or driver" value={search} onChange={(e) => setSearch(e.target.value)}
            className="w-full border-2 border-gray-200 rounded-xl pl-9 pr-3 py-2.5 text-sm focus:outline-none focus:border-indigo-400 transition-colors"/>
        </div>
        <button type="submit" className="bg-indigo-600 text-white px-5 py-2.5 rounded-xl hover:bg-indigo-500 text-sm font-semibold transition-colors">
          Search
        </button>
      </form>

      {loading && <div className="space-y-3">{[1,2,3].map(i => <div key={i} className="h-20 rounded-2xl shimmer"/>)}</div>}

      {!loading && trips.length === 0 && (
        <div className="text-center py-12 text-gray-400 fade-in">
          <div className="text-4xl mb-3">🚗</div>
          <p className="font-semibold text-gray-600">No trips found</p>
        </div>
      )}

      {!loading && trips.length > 0 && (
        <div className="space-y-3">
          {trips.map((t: any, idx: number) => {
            const sc = statusConfig[t.status] || { color: 'text-gray-600', bg: 'bg-gray-50' }
            return (
              <div key={t.id} className="bg-white rounded-2xl border border-gray-100 p-4 flex items-center gap-4 card-glow slide-up"
                style={{ animationDelay: `${idx * 40}ms` }}>
                <div className="w-10 h-10 bg-indigo-50 rounded-xl flex items-center justify-center text-xl shrink-0">🚗</div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-1.5 mb-0.5">
                    <span className="w-2 h-2 rounded-full bg-green-500 shrink-0"/>
                    <p className="font-semibold text-gray-800 text-sm truncate">{t.originAddress}</p>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-red-500 shrink-0"/>
                    <p className="font-semibold text-gray-800 text-sm truncate">{t.destinationAddress}</p>
                  </div>
                  <p className="text-xs text-gray-400 mt-1">🕐 {t.departureTime} · Driver: {t.driverName}</p>
                </div>
                <span className={`text-xs font-bold px-3 py-1.5 rounded-xl shrink-0 ${sc.bg} ${sc.color}`}>
                  {t.status}
                </span>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
