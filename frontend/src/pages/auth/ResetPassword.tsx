import { useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import api from '../../api/client'

export default function ResetPassword() {
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState((location.state as any)?.email || '')
  const [token, setToken] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (password !== confirm) { setError('Passwords do not match.'); return }
    if (password.length < 8) { setError('Password must be at least 8 characters.'); return }
    setLoading(true); setError('')
    try {
      await api.post('/auth/password-reset/confirm', { email, token, newPassword: password })
      navigate('/login', { state: { message: 'Password reset successful. Please sign in.' } })
    } catch (err: any) {
      const msg = err?.response?.data?.error?.message || err?.response?.data?.message || 'Invalid or expired token.'
      setError(msg)
    } finally { setLoading(false) }
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

        <h1 className="text-2xl font-bold text-gray-800 mb-1">Reset password</h1>
        <p className="text-sm text-gray-400 mb-6">Enter the token from your email and choose a new password.</p>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-lg px-3 py-2 mb-4">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-xs font-medium text-gray-600 mb-1 block">Email</label>
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-600 mb-1 block">Reset Token</label>
            <input placeholder="Paste token from email" value={token} onChange={e => setToken(e.target.value)} required
              className="w-full border rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-indigo-400" />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-600 mb-1 block">New Password</label>
            <input type="password" placeholder="Min. 8 characters" value={password}
              onChange={e => setPassword(e.target.value)} required
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" />
          </div>
          <div>
            <label className="text-xs font-medium text-gray-600 mb-1 block">Confirm Password</label>
            <input type="password" placeholder="Repeat new password" value={confirm}
              onChange={e => setConfirm(e.target.value)} required
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" />
          </div>
          <button type="submit" disabled={loading}
            className="w-full bg-indigo-600 text-white py-2.5 rounded-lg hover:bg-indigo-500 font-medium text-sm transition-colors disabled:opacity-50">
            {loading ? 'Resetting...' : 'Reset Password'}
          </button>
        </form>
        <p className="text-sm text-center mt-5 text-gray-500">
          <Link to="/login" className="text-indigo-600 hover:underline font-medium">Back to Sign in</Link>
        </p>
      </div>
    </div>
  )
}
