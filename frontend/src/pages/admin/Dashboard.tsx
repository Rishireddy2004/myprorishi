import { useEffect, useState } from 'react'
import { getMetrics } from '../../api/admin'

const metricConfig = [
  { key: 'totalUsers',      label: 'Total Users',      icon: '👥', color: 'from-blue-500 to-blue-600',    bg: 'bg-blue-50',   text: 'text-blue-700' },
  { key: 'totalTrips',      label: 'Total Trips',      icon: '🚗', color: 'from-indigo-500 to-indigo-600', bg: 'bg-indigo-50', text: 'text-indigo-700' },
  { key: 'totalBookings',   label: 'Total Bookings',   icon: '🎫', color: 'from-purple-500 to-purple-600', bg: 'bg-purple-50', text: 'text-purple-700' },
  { key: 'totalRevenue',    label: 'Revenue',          icon: '💰', color: 'from-green-500 to-green-600',   bg: 'bg-green-50',  text: 'text-green-700', prefix: '₹' },
  { key: 'activeTrips',     label: 'Active Trips',     icon: '📍', color: 'from-amber-500 to-amber-600',   bg: 'bg-amber-50',  text: 'text-amber-700' },
  { key: 'pendingDisputes', label: 'Pending Disputes', icon: '⚠️', color: 'from-red-500 to-red-600',      bg: 'bg-red-50',    text: 'text-red-700' },
]

export default function Dashboard() {
  const [metrics, setMetrics] = useState<any>(null)

  useEffect(() => { getMetrics().then(setMetrics).catch(() => {}) }, [])

  return (
    <div className="max-w-4xl mx-auto p-4 sm:p-6">
      {/* Hero */}
      <div className="relative rounded-2xl overflow-hidden mb-6 h-44">
        <img
          src="https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=1000&auto=format&fit=crop"
          alt="Admin dashboard analytics"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-indigo-900/85 to-indigo-700/50 flex items-center px-8">
          <div className="text-white">
            <p className="text-indigo-300 text-xs font-semibold uppercase tracking-widest mb-1">Admin Panel</p>
            <h1 className="text-3xl font-bold">Dashboard</h1>
            <p className="text-indigo-200 text-sm mt-1">Platform overview and key metrics</p>
          </div>
        </div>
      </div>

      {!metrics && (
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
          {[1,2,3,4,5,6].map(i => <div key={i} className="h-28 rounded-2xl shimmer"/>)}
        </div>
      )}

      {metrics && (
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
          {metricConfig.map((c, idx) => (
            <div key={c.key} className="bg-white rounded-2xl border border-gray-100 p-5 card-glow slide-up"
              style={{ animationDelay: `${idx * 60}ms` }}>
              <div className="flex items-center justify-between mb-3">
                <div className={`w-10 h-10 ${c.bg} rounded-xl flex items-center justify-center text-xl`}>
                  {c.icon}
                </div>
              </div>
              <p className="text-xs text-gray-400 uppercase tracking-wide font-semibold">{c.label}</p>
              <p className={`text-3xl font-black mt-1 ${c.text}`}>
                {c.prefix || ''}{metrics[c.key] ?? '—'}
              </p>
            </div>
          ))}
        </div>
      )}

      {/* Quick links */}
      {metrics && (
        <div className="mt-6 grid grid-cols-2 gap-3">
          <div className="bg-gradient-to-br from-indigo-50 to-purple-50 border border-indigo-100 rounded-2xl p-4 flex items-center gap-3">
            <span className="text-2xl">🚦</span>
            <div>
              <p className="font-bold text-indigo-800 text-sm">System Status</p>
              <p className="text-xs text-green-600 font-semibold">● All systems operational</p>
            </div>
          </div>
          <div className="bg-gradient-to-br from-green-50 to-emerald-50 border border-green-100 rounded-2xl p-4 flex items-center gap-3">
            <span className="text-2xl">📈</span>
            <div>
              <p className="font-bold text-green-800 text-sm">Growth</p>
              <p className="text-xs text-green-600 font-semibold">Platform is growing</p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
