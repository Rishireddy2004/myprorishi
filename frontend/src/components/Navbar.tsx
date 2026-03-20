import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { getUser, canDrive, canRide } from '../store/auth'
import { logout, getMyProfile } from '../api/auth'
import api from '../api/client'

export default function Navbar() {
  const user = getUser()
  const navigate = useNavigate()
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [loyaltyPoints, setLoyaltyPoints] = useState<number | null>(null)
  const [unreadCount, setUnreadCount] = useState(0)
  const [notifications, setNotifications] = useState<any[]>([])
  const [bellOpen, setBellOpen] = useState(false)
  const bellRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (user) {
      getMyProfile().then((p: any) => setLoyaltyPoints(p.loyaltyPoints || 0)).catch(() => {})
    }
  }, [user?.name])

  // Poll notifications every 15s
  useEffect(() => {
    if (!user) return
    const fetchNotifs = () => {
      api.get('/notifications').then((res: any) => {
        const list: any[] = res.data || []
        setNotifications(list.slice(0, 10))
        setUnreadCount(list.filter((n: any) => !n.read).length)
      }).catch(() => {})
    }
    fetchNotifs()
    const interval = setInterval(fetchNotifs, 15000)
    return () => clearInterval(interval)
  }, [user?.name])

  // Close bell dropdown on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (bellRef.current && !bellRef.current.contains(e.target as Node)) setBellOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const markAllRead = () => {
    notifications.filter(n => !n.read).forEach(n => {
      api.patch(`/notifications/${n.id}/read`).catch(() => {})
    })
    setNotifications(prev => prev.map(n => ({ ...n, read: true })))
    setUnreadCount(0)
  }

  const notifLabel = (n: any) => {
    try {
      const p = typeof n.payloadJson === 'string' ? JSON.parse(n.payloadJson) : n.payloadJson
      switch (p?.type) {
        case 'PASSENGER_BOOKED':
          return `🧳 ${p.passengerName} booked ${p.seats} seat(s)${p.tipAmount > 0 ? ` · ₹${p.tipAmount} tip 🎉` : ''}`
        case 'BOOKING_CONFIRMED': return '✅ Your booking was confirmed'
        case 'BOOKING_CANCELLED': return '❌ A booking was cancelled'
        case 'TRIP_CANCELLED': return '🚫 A trip you booked was cancelled'
        case 'RIDE_REMINDER': return '⏰ Your ride departs soon'
        case 'ACCOUNT_SUSPENDED': return '⛔ Your account was suspended'
        case 'ACCOUNT_UNSUSPENDED': return '✅ Your account was reinstated'
        default: return n.type || 'Notification'
      }
    } catch { return 'Notification' }
  }

  const handleLogout = () => { logout(); navigate('/login') }

  const isActive = (path: string) => location.pathname === path || location.pathname.startsWith(path + '/')

  const NavLink = ({ to, children }: { to: string; children: React.ReactNode }) => (
    <Link to={to}
      className={`nav-link ${isActive(to) ? 'text-white bg-white/10' : ''}`}
      onClick={() => setMobileOpen(false)}>
      {children}
    </Link>
  )

  return (
    <nav className="navbar-luxury sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6">
        <div className="flex items-center justify-between h-16">

          {/* Logo */}
          <Link to="/" className="flex items-center gap-2.5 group">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/30 group-hover:shadow-indigo-500/50 transition-shadow">
              <span className="text-lg">🚗</span>
            </div>
            <div className="flex flex-col leading-none">
              <span className="text-white font-bold text-base tracking-tight" style={{fontFamily:'Playfair Display, serif'}}>RideShare</span>
              <span className="text-indigo-400 text-[10px] font-medium tracking-widest uppercase">Premium</span>
            </div>
          </Link>

          {/* Desktop nav */}
          <div className="hidden md:flex items-center gap-1">
            {canRide() && (
              <>
                <NavLink to="/search">🔍 Search</NavLink>
                <NavLink to="/my-bookings">📋 Bookings</NavLink>
                <NavLink to="/transactions">💳 Payments</NavLink>
              </>
            )}
            {canDrive() && (
              <>
                <NavLink to="/post-trip">➕ Post Trip</NavLink>
                <NavLink to="/my-trips">🗂️ My Trips</NavLink>
                <NavLink to="/my-vehicle">🚙 Vehicle</NavLink>
              </>
            )}
            {user?.role === 'ADMIN' && (
              <>
                <NavLink to="/admin">📊 Dashboard</NavLink>
                <NavLink to="/admin/users">👥 Users</NavLink>
                <NavLink to="/admin/trips">🗺️ Trips</NavLink>
                <NavLink to="/admin/disputes">⚖️ Disputes</NavLink>
                <NavLink to="/admin/config">⚙️ Config</NavLink>
              </>
            )}
          </div>

          {/* Right side */}
          <div className="flex items-center gap-3">
            {user && (
              <div className="hidden sm:flex items-center gap-2 bg-white/5 border border-white/10 rounded-xl px-3 py-1.5">
                <Link to="/profile" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
                  <div className="w-6 h-6 rounded-full bg-gradient-to-br from-indigo-400 to-purple-500 flex items-center justify-center text-white text-xs font-bold">
                    {user.name?.charAt(0)?.toUpperCase() || 'U'}
                  </div>
                  <div className="flex flex-col leading-none">
                    <span className="text-white text-xs font-semibold">{user.name?.split(' ')[0]}</span>
                    <span className="text-indigo-400 text-[10px] uppercase tracking-wide">{user.role}</span>
                  </div>
                </Link>
                {loyaltyPoints != null && loyaltyPoints > 0 && (
                  <Link to="/profile"
                    className="flex items-center gap-1 bg-amber-400/20 border border-amber-400/30 text-amber-300 text-[10px] font-bold px-2 py-0.5 rounded-lg hover:bg-amber-400/30 transition-colors">
                    ⭐ {loyaltyPoints}
                  </Link>
                )}
              </div>
            )}

            {/* Notification bell */}
            {user && (
              <div className="relative" ref={bellRef}>
                <button onClick={() => { setBellOpen(o => !o); if (!bellOpen) markAllRead() }}
                  className="relative w-9 h-9 flex items-center justify-center rounded-xl bg-white/8 border border-white/10 hover:bg-white/15 transition-all">
                  <svg className="w-4 h-4 text-white/80" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
                  </svg>
                  {unreadCount > 0 && (
                    <span className="absolute -top-1 -right-1 w-4 h-4 bg-red-500 text-white text-[9px] font-black rounded-full flex items-center justify-center">
                      {unreadCount > 9 ? '9+' : unreadCount}
                    </span>
                  )}
                </button>

                {bellOpen && (
                  <div className="absolute right-0 mt-2 w-80 bg-white rounded-2xl shadow-2xl border border-gray-100 overflow-hidden z-50 slide-up">
                    <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
                      <p className="font-bold text-gray-800 text-sm">Notifications</p>
                      {notifications.length > 0 && (
                        <button onClick={markAllRead} className="text-xs text-indigo-600 hover:text-indigo-800 font-semibold">
                          Mark all read
                        </button>
                      )}
                    </div>
                    <div className="max-h-80 overflow-y-auto">
                      {notifications.length === 0 ? (
                        <div className="py-8 text-center">
                          <p className="text-2xl mb-2">🔔</p>
                          <p className="text-gray-400 text-sm">No notifications yet</p>
                        </div>
                      ) : (
                        notifications.map((n: any) => (
                          <div key={n.id}
                            className={`px-4 py-3 border-b border-gray-50 flex items-start gap-3 transition-colors ${!n.read ? 'bg-indigo-50' : 'hover:bg-gray-50'}`}>
                            <div className={`w-2 h-2 rounded-full mt-1.5 shrink-0 ${!n.read ? 'bg-indigo-500' : 'bg-gray-200'}`}/>
                            <div className="flex-1 min-w-0">
                              <p className="text-sm text-gray-800 font-medium leading-snug">{notifLabel(n)}</p>
                              <p className="text-xs text-gray-400 mt-0.5">
                                {n.createdAt ? new Date(n.createdAt).toLocaleString('en-IN', { dateStyle: 'short', timeStyle: 'short' }) : ''}
                              </p>
                            </div>
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}
            <button onClick={handleLogout}
              className="flex items-center gap-1.5 bg-white/8 hover:bg-red-500/20 border border-white/10 hover:border-red-400/40 text-white/70 hover:text-red-300 px-3 py-1.5 rounded-xl text-xs font-semibold transition-all">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
              </svg>
              <span className="hidden sm:inline">Sign Out</span>
            </button>

            {/* Mobile hamburger */}
            <button onClick={() => setMobileOpen(o => !o)}
              className="md:hidden w-9 h-9 flex flex-col items-center justify-center gap-1.5 rounded-xl bg-white/8 border border-white/10">
              <span className={`w-5 h-0.5 bg-white/80 rounded transition-all ${mobileOpen ? 'rotate-45 translate-y-2' : ''}`}/>
              <span className={`w-5 h-0.5 bg-white/80 rounded transition-all ${mobileOpen ? 'opacity-0' : ''}`}/>
              <span className={`w-5 h-0.5 bg-white/80 rounded transition-all ${mobileOpen ? '-rotate-45 -translate-y-2' : ''}`}/>
            </button>
          </div>
        </div>

        {/* Mobile menu */}
        {mobileOpen && (
          <div className="md:hidden pb-4 pt-2 border-t border-white/10 slide-up">
            <div className="flex flex-col gap-1">
              {canRide() && (
                <>
                  <NavLink to="/search">🔍 Search Trips</NavLink>
                  <NavLink to="/my-bookings">📋 My Bookings</NavLink>
                  <NavLink to="/transactions">💳 Transactions</NavLink>
                </>
              )}
              {canDrive() && (
                <>
                  <NavLink to="/post-trip">➕ Post Trip</NavLink>
                  <NavLink to="/my-trips">🗂️ My Trips</NavLink>
                  <NavLink to="/my-vehicle">🚙 My Vehicle</NavLink>
                </>
              )}
              {user?.role === 'ADMIN' && (
                <>
                  <NavLink to="/admin">📊 Dashboard</NavLink>
                  <NavLink to="/admin/users">👥 Users</NavLink>
                  <NavLink to="/admin/trips">🗺️ Trips</NavLink>
                  <NavLink to="/admin/disputes">⚖️ Disputes</NavLink>
                  <NavLink to="/admin/config">⚙️ Config</NavLink>
                </>
              )}
              <NavLink to="/profile">👤 My Profile</NavLink>
            </div>
          </div>
        )}
      </div>

      {/* Gold accent line */}
      <div className="divider-gold"/>
    </nav>
  )
}
