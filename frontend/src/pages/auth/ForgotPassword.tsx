import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../../api/client'
import { getErrorMessage } from '../../api/errorUtils'

type Tab = 'email' | 'direct'

export default function ForgotPassword() {
  const navigate = useNavigate()
  const [tab, setTab] = useState<Tab>('direct')

  // Email flow
  const [email, setEmail] = useState('')
  const [emailSent, setEmailSent] = useState(false)
  const [emailLoading, setEmailLoading] = useState(false)
  const [emailError, setEmailError] = useState('')

  // Direct reset flow
  const [dEmail, setDEmail] = useState('')
  const [dFullName, setDFullName] = useState('')
  const [dPassword, setDPassword] = useState('')
  const [dConfirm, setDConfirm] = useState('')
  const [dLoading, setDLoading] = useState(false)
  const [dError, setDError] = useState('')
  const [dDone, setDDone] = useState(false)

  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setEmailLoading(true); setEmailError('')
    try {
      await api.post('/auth/password-reset/request', { email })
      setEmailSent(true)
    } catch (err: any) {
      setEmailError(getErrorMessage(err, 'Failed to send reset email.'))
    } finally { setEmailLoading(false) }
  }

  const handleDirectReset = async (e: React.FormEvent) => {
    e.preventDefault()
    if (dPassword !== dConfirm) { setDError('Passwords do not match.'); return }
    if (dPassword.length < 8) { setDError('Password must be at least 8 characters.'); return }
    setDLoading(true); setDError('')
    try {
      await api.post('/auth/password-reset/direct', {
        email: dEmail,
        fullName: dFullName,
        newPassword: dPassword,
      })
      setDDone(true)
  
    } catch (err: any) {
      setDError(getErrorMessage(err, 'Could not reset password. Check your details and try again.'))
    } finally { setDLoading(false) }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-6">
      <div className="bg-white rounded-2xl shadow-lg p-8 w-full max-w-sm">

        <div className="flex items-center gap-2 mb-6">
          <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center">
            <span className="text-white text-sm">🚗</span>
          </div>
          <span className="font-bold text-indigo-700">RideShare</span>
        </div>

        {/* Tab switcher */}
        <div className="flex rounded-lg border border-gray-200 mb-6 overflow-hidden">
          <button onClick={() => setTab('direct')}
            className={`flex-1 py-2 text-sm font-medium transition-colors ${tab === 'direct' ? 'bg-indigo-600 text-white' : 'text-gray-500 hover:bg-gray-50'}`}>
            🔑 Direct Reset
          </button>
          <button onClick={() => setTab('email')}
            className={`flex-1 py-2 text-sm font-medium transition-colors ${tab === 'email' ? 'bg-indigo-600 text-white' : 'text-gray-500 hover:bg-gray-50'}`}>
            📧 By Email
          </button>
        </div>

        {/* ---- DIRECT RESET TAB ---- */}
        {tab === 'direct' && (
          dDone ? (
            <div className="text-center">
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4 text-3xl">✅</div>
              <h2 className="text-xl font-bold text-gray-800 mb-2">Password updated!</h2>
              <p className="text-sm text-gray-500 mb-6">You can now sign in with your new password.</p>
              <button onClick={() => navigate('/login')}
                className="block w-full bg-indigo-600 text-white py-2.5 rounded-lg text-sm font-medium text-center hover:bg-indigo-500 transition-colors">
                Sign In →
              </button>
            </div>
          ) : (
            <>
              <h1 className="text-2xl font-bold text-gray-800 mb-1">Reset Password</h1>
              <p className="text-sm text-gray-400 mb-5">
                Enter your email and the full name you registered with to verify your identity.
              </p>
              {dError && (
                <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-lg px-3 py-2 mb-4">⚠️ {dError}</div>
              )}
              <form onSubmit={handleDirectReset} className="space-y-3">
                <div>
                  <label className="text-xs font-medium text-gray-600 mb-1 block">Email address</label>
                  <input type="email" placeholder="you@example.com" value={dEmail}
                    onChange={e => setDEmail(e.target.value)} required
                    className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" />
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-600 mb-1 block">Full name (as registered)</label>
                  <input type="text" placeholder="Your full name" value={dFullName}
                    onChange={e => setDFullName(e.target.value)} required
                    className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" />
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-600 mb-1 block">New Password</label>
                  <input type="password" placeholder="Min. 8 characters" value={dPassword}
                    onChange={e => setDPassword(e.target.value)} required
                    className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" />
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-600 mb-1 block">Confirm Password</label>
                  <input type="password" placeholder="Repeat new password" value={dConfirm}
                    onChange={e => setDConfirm(e.target.value)} required
                    className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" />
                </div>
                <button type="submit" disabled={dLoading}
                  className="w-full bg-indigo-600 text-white py-2.5 rounded-lg hover:bg-indigo-500 font-medium text-sm transition-colors disabled:opacity-50">
                  {dLoading ? 'Resetting...' : 'Reset Password'}
                </button>
              </form>
            </>
          )
        )}

        {/* ---- EMAIL TAB ---- */}
        {tab === 'email' && (
          emailSent ? (
            <div className="text-center">
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4 text-3xl">📧</div>
              <h2 className="text-xl font-bold text-gray-800 mb-2">Check your inbox</h2>
              <p className="text-sm text-gray-500 mb-6">
                A reset token was sent to <span className="font-semibold text-gray-700">{email}</span>.
                Copy it and use it on the next page.
              </p>
              <Link to="/reset-password" state={{ email }}
                className="block w-full bg-indigo-600 text-white py-2.5 rounded-lg text-sm font-medium text-center hover:bg-indigo-500 transition-colors">
                Enter Reset Token →
              </Link>
              <button onClick={() => { setEmailSent(false); setEmailError('') }}
                className="block w-full text-sm text-center mt-3 text-gray-400 hover:text-gray-600">
                Didn't receive it? Try again
              </button>
            </div>
          ) : (
            <>
              <h1 className="text-2xl font-bold text-gray-800 mb-1">Reset via Email</h1>
              <p className="text-sm text-gray-400 mb-5">We'll send a reset token to your registered email.</p>
              {emailError && (
                <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-lg px-3 py-2 mb-4">⚠️ {emailError}</div>
              )}
              <form onSubmit={handleEmailSubmit} className="space-y-4">
                <div>
                  <label className="text-xs font-medium text-gray-600 mb-1 block">Email address</label>
                  <input type="email" placeholder="you@example.com" value={email}
                    onChange={e => setEmail(e.target.value)} required
                    className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" />
                </div>
                <button type="submit" disabled={emailLoading}
                  className="w-full bg-indigo-600 text-white py-2.5 rounded-lg hover:bg-indigo-500 font-medium text-sm transition-colors disabled:opacity-50">
                  {emailLoading ? 'Sending...' : 'Send Reset Token'}
                </button>
              </form>
            </>
          )
        )}

        <p className="text-sm text-center mt-5 text-gray-500">
          Remember it? <Link to="/login" className="text-indigo-600 hover:underline font-medium">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
