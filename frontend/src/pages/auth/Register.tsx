import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { register } from '../../api/auth'
import { getErrorMessage } from '../../api/errorUtils'

const PERKS = [
  { emoji: '🚗', text: 'Post trips & earn money' },
  { emoji: '🔍', text: 'Find affordable rides' },
  { emoji: '💳', text: 'Secure payments' },
  { emoji: '⭐', text: 'Verified community' },
  { emoji: '🛡️', text: 'Safety guaranteed' },
  { emoji: '📍', text: 'Live trip tracking' },
]

export default function Register() {
  const [form, setForm] = useState({ fullName: '', email: '', password: '', role: 'PASSENGER', phone: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPass, setShowPass] = useState(false)
  const navigate = useNavigate()
  const set = (k: string, v: string) => setForm(f => ({ ...f, [k]: v }))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(''); setLoading(true)
    try {
      await register(form)
      navigate('/login', { state: { message: 'Account created! Please sign in.' } })
    } catch (err: any) {
      setError(getErrorMessage(err, 'Registration failed'))
    } finally { setLoading(false) }
  }

  const roles = [
    { value: 'PASSENGER', emoji: '🧳', label: 'Passenger', desc: 'Find & book rides' },
    { value: 'DRIVER',    emoji: '🚗', label: 'Driver',    desc: 'Post trips & earn' },
    { value: 'BOTH',      emoji: '🔄', label: 'Both',      desc: 'Drive & ride' },
  ]

  return (
    <div className="min-h-screen flex app-bg">
      <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="blob blob-1"/><div className="blob blob-2"/><div className="blob blob-3"/>
      </div>

      {/* Left panel */}
      <div className="hidden lg:flex w-5/12 flex-col justify-between p-14 relative overflow-hidden">
        <div className="absolute inset-0">
          <img src="https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?w=1200&auto=format&fit=crop&q=80"
            alt="" className="w-full h-full object-cover opacity-25"/>
          <div className="absolute inset-0 bg-gradient-to-br from-slate-950/95 via-indigo-950/90 to-purple-950/95"/>
        </div>

        <div className="relative z-10">
          <div className="flex items-center gap-3 mb-14">
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-xl shadow-indigo-500/40">
              <span className="text-xl">🚗</span>
            </div>
            <div>
              <p className="text-white font-bold text-xl" style={{fontFamily:'Playfair Display,serif'}}>RideShare</p>
              <p className="text-indigo-400 text-xs tracking-widest uppercase">Premium</p>
            </div>
          </div>

          <h1 className="text-4xl font-bold text-white leading-tight mb-4" style={{fontFamily:'Playfair Display,serif'}}>
            Join thousands<br/>of <span className="gold-shimmer">smart</span><br/>commuters.
          </h1>
          <p className="text-slate-300 text-sm leading-relaxed mb-10 max-w-xs">
            One account. Unlimited rides. Whether you drive or ride — we've got you covered.
          </p>

          <div className="grid grid-cols-2 gap-2.5">
            {PERKS.map(p => (
              <div key={p.text} className="glass rounded-xl px-3 py-2.5 flex items-center gap-2.5 hover:bg-white/10 transition-all">
                <span className="text-lg">{p.emoji}</span>
                <span className="text-slate-200 text-xs font-medium">{p.text}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="relative z-10">
          <div className="divider-gold mb-5"/>
          <p className="text-slate-400 text-xs">Trusted by 10,000+ riders across India</p>
          <div className="flex items-center gap-1 mt-2">
            {[...Array(5)].map((_, i) => <span key={i} className="text-amber-400 text-sm">★</span>)}
            <span className="text-slate-300 text-xs ml-1">4.9 / 5 from 2,400+ reviews</span>
          </div>
        </div>
      </div>

      {/* Right panel */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-8 overflow-y-auto">
        <div className="w-full max-w-md py-4">
          <div className="flex items-center gap-3 mb-6 lg:hidden">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center">
              <span className="text-lg">🚗</span>
            </div>
            <span className="text-white font-bold text-xl" style={{fontFamily:'Playfair Display,serif'}}>RideShare</span>
          </div>

          <div className="glass-card rounded-3xl p-7 sm:p-9">
            <div className="mb-7">
              <h2 className="text-3xl font-bold text-gray-900 mb-1" style={{fontFamily:'Playfair Display,serif'}}>Create account</h2>
              <p className="text-slate-500 text-sm">Join the premium ride-sharing community</p>
            </div>

            {error && (
              <div className="flex items-start gap-2 bg-red-50 border border-red-200 text-red-600 text-sm rounded-xl px-4 py-3 mb-5 slide-up">
                <svg className="w-4 h-4 shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                </svg>
                <span>
                  {error}
                  {error.toLowerCase().includes('email') && error.toLowerCase().includes('already') && (
                    <> — <Link to="/login" className="underline font-semibold">Sign in instead →</Link></>
                  )}
                </span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 gap-4">
                <div>
                  <label className="text-xs font-semibold text-slate-600 uppercase tracking-wide mb-1.5 block">Full Name</label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">👤</span>
                    <input placeholder="John Doe" value={form.fullName} onChange={e => set('fullName', e.target.value)}
                      className="input-luxury" required/>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-600 uppercase tracking-wide mb-1.5 block">Email Address</label>
                  <div className="relative">
                    <svg className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
                    </svg>
                    <input type="email" placeholder="you@example.com" value={form.email} onChange={e => set('email', e.target.value)}
                      className="input-luxury" required/>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-600 uppercase tracking-wide mb-1.5 block">Password</label>
                  <div className="relative">
                    <svg className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                    </svg>
                    <input type={showPass ? 'text' : 'password'} placeholder="Min. 8 characters" value={form.password}
                      onChange={e => set('password', e.target.value)} className="input-luxury pr-10" required/>
                    <button type="button" onClick={() => setShowPass(s => !s)}
                      className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 text-xs">
                      {showPass ? '🙈' : '👁️'}
                    </button>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-600 uppercase tracking-wide mb-1.5 block">
                    Phone <span className="text-slate-400 font-normal normal-case">(optional)</span>
                  </label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">📱</span>
                    <input placeholder="+91 9876543210" value={form.phone} onChange={e => set('phone', e.target.value)}
                      className="input-luxury"/>
                  </div>
                </div>
              </div>

              {/* Role selector */}
              <div>
                <label className="text-xs font-semibold text-slate-600 uppercase tracking-wide mb-2.5 block">I want to</label>
                <div className="grid grid-cols-3 gap-2">
                  {roles.map(r => (
                    <button key={r.value} type="button" onClick={() => set('role', r.value)}
                      className={`flex flex-col items-center gap-1.5 py-3.5 px-2 rounded-2xl border-2 text-xs font-semibold transition-all btn-ripple
                        ${form.role === r.value
                          ? 'border-indigo-500 bg-indigo-50 text-indigo-700 shadow-lg shadow-indigo-100'
                          : 'border-slate-200 text-slate-600 hover:border-indigo-300 hover:bg-indigo-50/50'}`}>
                      <span className="text-2xl">{r.emoji}</span>
                      <span className="font-bold">{r.label}</span>
                      <span className={`text-[10px] font-normal ${form.role === r.value ? 'text-indigo-500' : 'text-slate-400'}`}>{r.desc}</span>
                    </button>
                  ))}
                </div>
              </div>

              <button type="submit" disabled={loading}
                className="w-full btn-luxury py-3.5 rounded-2xl text-sm btn-ripple disabled:opacity-60 flex items-center justify-center gap-2 mt-2">
                {loading
                  ? <><svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/></svg>Creating account...</>
                  : <>🚀 Create My Account</>
                }
              </button>
            </form>

            <div className="relative my-5">
              <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-slate-200"/></div>
              <div className="relative flex justify-center"><span className="bg-white px-3 text-xs text-slate-400">Already have an account?</span></div>
            </div>

            <Link to="/login"
              className="w-full flex items-center justify-center gap-2 border-2 border-indigo-200 text-indigo-700 py-3 rounded-2xl text-sm font-semibold hover:bg-indigo-50 hover:border-indigo-400 transition-all">
              Sign In Instead →
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
