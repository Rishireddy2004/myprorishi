import { useState } from 'react'
import { useNavigate, Link, useLocation } from 'react-router-dom'
import { login } from '../../api/auth'
import { getUser } from '../../store/auth'

const FEATURES = [
  { icon: '🛡️', title: 'Verified Drivers', desc: 'Every driver is background-checked' },
  { icon: '💎', title: 'Premium Comfort', desc: 'AC rides with top-rated drivers' },
  { icon: '⚡', title: 'Instant Booking', desc: 'Confirm your seat in seconds' },
  { icon: '🌍', title: '50+ Cities', desc: 'Nationwide coverage across India' },
]

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPass, setShowPass] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const successMsg = (location.state as any)?.message || ''

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(''); setLoading(true)
    try {
      await login(email, password)
      const user = getUser()
      if (user?.role === 'ADMIN') navigate('/admin')
      else if (user?.role === 'DRIVER') navigate('/my-trips')
      else navigate('/search')
    } catch {
      setError('Invalid email or password. Please try again.')
    } finally { setLoading(false) }
  }

  return (
    <div className="min-h-screen flex app-bg">
      {/* Blobs */}
      <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="blob blob-1"/><div className="blob blob-2"/><div className="blob blob-3"/>
      </div>

      {/* Left panel */}
      <div className="hidden lg:flex w-1/2 flex-col justify-between p-14 relative overflow-hidden">
        {/* Background image with overlay */}
        <div className="absolute inset-0">
          <img src="https://images.unsplash.com/photo-1494976388531-d1058494cdd8?w=1200&auto=format&fit=crop&q=80"
            alt="" className="w-full h-full object-cover opacity-30"/>
          <div className="absolute inset-0 bg-gradient-to-br from-indigo-950/90 via-purple-950/80 to-slate-950/95"/>
        </div>

        {/* Content */}
        <div className="relative z-10">
          <div className="flex items-center gap-3 mb-16">
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-xl shadow-indigo-500/40">
              <span className="text-xl">🚗</span>
            </div>
            <div>
              <p className="text-white font-bold text-xl" style={{fontFamily:'Playfair Display,serif'}}>RideShare</p>
              <p className="text-indigo-400 text-xs tracking-widest uppercase font-medium">Premium</p>
            </div>
          </div>

          <div className="mb-3">
            <span className="text-xs font-semibold tracking-widest uppercase text-indigo-400 border border-indigo-500/30 bg-indigo-500/10 px-3 py-1 rounded-full">
              India's #1 Ride Platform
            </span>
          </div>
          <h1 className="text-5xl font-bold text-white leading-tight mb-4" style={{fontFamily:'Playfair Display,serif'}}>
            Travel in<br/><span className="gold-shimmer">Luxury.</span><br/>Together.
          </h1>
          <p className="text-slate-300 text-base leading-relaxed max-w-sm mb-10">
            Share premium rides, split costs intelligently, and arrive in style — every single time.
          </p>

          <div className="grid grid-cols-2 gap-3">
            {FEATURES.map(f => (
              <div key={f.title} className="glass rounded-2xl p-4 group hover:bg-white/10 transition-all">
                <div className="text-2xl mb-2 float">{f.icon}</div>
                <p className="text-white font-semibold text-sm">{f.title}</p>
                <p className="text-slate-400 text-xs mt-0.5">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="relative z-10">
          <div className="divider-gold mb-6"/>
          <div className="flex gap-10">
            {[['10k+','Daily rides'],['50+','Cities'],['4.9★','Avg rating'],['₹0','Hidden fees']].map(([v,l]) => (
              <div key={l}>
                <p className="text-2xl font-bold gold-text">{v}</p>
                <p className="text-slate-400 text-xs mt-0.5">{l}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right panel — form */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-10">
        <div className="w-full max-w-md">
          {/* Mobile logo */}
          <div className="flex items-center gap-3 mb-8 lg:hidden">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center">
              <span className="text-lg">🚗</span>
            </div>
            <span className="text-white font-bold text-xl" style={{fontFamily:'Playfair Display,serif'}}>RideShare</span>
          </div>

          <div className="glass-card rounded-3xl p-8 sm:p-10">
            <div className="mb-8">
              <h2 className="text-3xl font-bold text-gray-900 mb-1" style={{fontFamily:'Playfair Display,serif'}}>Welcome back</h2>
              <p className="text-slate-500 text-sm">Sign in to your premium account</p>
            </div>

            {successMsg && (
              <div className="flex items-center gap-2 bg-emerald-50 border border-emerald-200 text-emerald-700 text-sm rounded-xl px-4 py-3 mb-5">
                <span>✓</span> {successMsg}
              </div>
            )}
            {error && (
              <div className="flex items-center gap-2 bg-red-50 border border-red-200 text-red-600 text-sm rounded-xl px-4 py-3 mb-5 slide-up">
                <svg className="w-4 h-4 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                </svg>
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label className="text-xs font-semibold text-slate-600 uppercase tracking-wide mb-2 block">Email Address</label>
                <div className="relative">
                  <svg className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
                  </svg>
                  <input type="email" placeholder="you@example.com" value={email}
                    onChange={e => setEmail(e.target.value)}
                    className="input-luxury" required/>
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs font-semibold text-slate-600 uppercase tracking-wide">Password</label>
                  <Link to="/forgot-password" className="text-xs text-indigo-600 hover:text-indigo-800 font-semibold transition-colors">Forgot password?</Link>
                </div>
                <div className="relative">
                  <svg className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                  </svg>
                  <input type={showPass ? 'text' : 'password'} placeholder="••••••••" value={password}
                    onChange={e => setPassword(e.target.value)}
                    className="input-luxury pr-10" required/>
                  <button type="button" onClick={() => setShowPass(s => !s)}
                    className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors text-xs">
                    {showPass ? '🙈' : '👁️'}
                  </button>
                </div>
              </div>

              <button type="submit" disabled={loading}
                className="w-full btn-luxury py-3.5 rounded-2xl text-sm btn-ripple disabled:opacity-60 flex items-center justify-center gap-2">
                {loading
                  ? <><svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/></svg>Signing in...</>
                  : <><span className="car-drive">🚗</span> Sign In to RideShare</>
                }
              </button>
            </form>

            <div className="relative my-6">
              <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-slate-200"/></div>
              <div className="relative flex justify-center"><span className="bg-white px-3 text-xs text-slate-400 font-medium">New to RideShare?</span></div>
            </div>

            <Link to="/register"
              className="w-full flex items-center justify-center gap-2 border-2 border-indigo-200 text-indigo-700 py-3 rounded-2xl text-sm font-semibold hover:bg-indigo-50 hover:border-indigo-400 transition-all">
              🚀 Create Free Account
            </Link>
          </div>

          <p className="text-center text-slate-400 text-xs mt-6">
            By signing in you agree to our <span className="text-indigo-400 cursor-pointer hover:underline">Terms</span> & <span className="text-indigo-400 cursor-pointer hover:underline">Privacy Policy</span>
          </p>
        </div>
      </div>
    </div>
  )
}
