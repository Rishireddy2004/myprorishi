import { useState, useEffect } from 'react'
import { getMyProfile, updateMyProfile, getPointsHistory, getTrustStats } from '../../api/auth'
import { getUser } from '../../store/auth'

export default function Profile() {
  const user = getUser()
  const [profile, setProfile] = useState<any>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState('')
  const [pointsHistory, setPointsHistory] = useState<any[]>([])
  const [trustStats, setTrustStats] = useState<any>(null)

  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [photoUrl, setPhotoUrl] = useState('')

  const isDriver = user?.role === 'DRIVER' || user?.role === 'BOTH'

  useEffect(() => {
    getMyProfile()
      .then(data => {
        setProfile(data)
        setFullName(data.fullName || '')
        setPhone(data.phone || '')
        setPhotoUrl(data.photoUrl || '')
      })
      .catch(() => setError('Failed to load profile'))
      .finally(() => setLoading(false))
    getPointsHistory().then(setPointsHistory).catch(() => {})
    if (isDriver) getTrustStats().then(setTrustStats).catch(() => {})
  }, [])

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true); setError(''); setSuccess(false)
    try {
      const updated = await updateMyProfile({ fullName, phone, photoUrl })
      setProfile(updated)
      setSuccess(true)
      setTimeout(() => setSuccess(false), 3000)
    } catch {
      setError('Failed to update profile. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  const initials = fullName?.split(' ').map((n: string) => n[0]).join('').toUpperCase().slice(0, 2) || 'U'

  if (loading) return (
    <div className="flex items-center justify-center min-h-[60vh]">
      <div className="flex flex-col items-center gap-3">
        <svg className="animate-spin w-10 h-10 text-indigo-500" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
        </svg>
        <p className="text-gray-500 text-sm">Loading profile...</p>
      </div>
    </div>
  )

  return (
    <div className="max-w-2xl mx-auto p-4 sm:p-6">
      {/* Header card */}
      <div className="profile-hero rounded-3xl p-8 mb-6 text-center relative overflow-hidden">
        <div className="absolute inset-0 opacity-10"
          style={{backgroundImage:'radial-gradient(circle at 30% 50%, #6366f1 0%, transparent 60%), radial-gradient(circle at 70% 50%, #a78bfa 0%, transparent 60%)'}}/>
        <div className="relative">
          {photoUrl ? (
            <img src={photoUrl} alt={fullName}
              className="w-24 h-24 rounded-full object-cover mx-auto mb-4 ring-4 ring-white shadow-xl"/>
          ) : (
            <div className="w-24 h-24 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center mx-auto mb-4 ring-4 ring-white shadow-xl">
              <span className="text-white text-3xl font-bold">{initials}</span>
            </div>
          )}
          <h1 className="text-2xl font-bold text-gray-800">{profile?.fullName || 'Your Name'}</h1>
          <p className="text-gray-500 text-sm mt-1">{profile?.email}</p>
          <div className="flex items-center justify-center gap-3 mt-3">
            <span className="inline-flex items-center gap-1.5 bg-indigo-100 text-indigo-700 text-xs font-semibold px-3 py-1.5 rounded-full">
              {user?.role === 'BOTH' ? '🚗 Driver & Passenger' : user?.role === 'DRIVER' ? '🚗 Driver' : '🧳 Passenger'}
            </span>
            {profile?.isVerified && (
              <span className="inline-flex items-center gap-1 bg-green-100 text-green-700 text-xs font-semibold px-3 py-1.5 rounded-full">
                ✓ Verified
              </span>
            )}
          </div>
          {profile?.aggregateRating != null && (
            <div className="flex items-center justify-center gap-2 mt-3">
              <span className="text-yellow-500 text-lg">⭐</span>
              <span className="font-bold text-gray-800 text-lg">{profile.aggregateRating.toFixed(1)}</span>
              <span className="text-gray-400 text-sm">({profile.reviewCount} reviews)</span>
            </div>
          )}
        </div>
      </div>

      {/* Loyalty Points Card */}
      <div className={`rounded-3xl p-6 mb-6 ${(profile?.loyaltyPoints || 0) > 0 ? 'bg-gradient-to-r from-amber-50 to-yellow-50 border border-amber-200' : 'bg-gray-50 border border-gray-200'}`}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className={`w-12 h-12 rounded-2xl flex items-center justify-center text-2xl ${(profile?.loyaltyPoints || 0) > 0 ? 'bg-amber-100' : 'bg-gray-100'}`}>
              ⭐
            </div>
            <div>
              <p className="font-bold text-gray-800 text-base">Loyalty Points</p>
              <p className="text-xs text-gray-500 mt-0.5">1 point = ₹1 discount on next ride</p>
            </div>
          </div>
          <div className="text-right">
            <p className={`text-3xl font-black ${(profile?.loyaltyPoints || 0) > 0 ? 'text-amber-600' : 'text-gray-400'}`}>
              {profile?.loyaltyPoints || 0}
            </p>
            <p className="text-xs text-gray-500">pts · ₹{profile?.loyaltyPoints || 0} value</p>
          </div>
        </div>
        {(profile?.loyaltyPoints || 0) === 0 ? (
          <p className="text-xs text-gray-400 mt-3 text-center">Complete rides to earn 5–10 points per trip</p>
        ) : (
          <div className="mt-3 bg-white/70 rounded-xl px-4 py-2 text-xs text-amber-700 font-semibold text-center">
            🎉 Use your points when booking a ride for up to 50% off!
          </div>
        )}
      </div>

      {/* Trust Score Card — drivers only */}
      {isDriver && (
        <div className="rounded-3xl p-6 mb-6 bg-gradient-to-r from-indigo-50 to-blue-50 border border-indigo-200">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-indigo-100 flex items-center justify-center text-2xl">🏆</div>
              <div>
                <p className="font-bold text-gray-800 text-base">Driver Trust Score</p>
                <p className="text-xs text-gray-500 mt-0.5">Earned when passengers redeem points on your trips</p>
              </div>
            </div>
            <div className="text-right">
              <p className={`text-3xl font-black ${(trustStats?.trustScore || 0) > 0 ? 'text-indigo-600' : 'text-gray-400'}`}>
                {trustStats?.trustScore ?? profile?.trustScore ?? 0}
              </p>
              <p className="text-xs text-gray-500">trust pts</p>
            </div>
          </div>
          {/* Trust badge */}
          {(() => {
            const score = trustStats?.trustScore ?? profile?.trustScore ?? 0
            if (score >= 50) return (
              <div className="bg-indigo-600 text-white rounded-2xl px-4 py-2.5 flex items-center gap-2">
                <span className="text-xl">🏆</span>
                <div>
                  <p className="font-bold text-sm">Trusted Driver</p>
                  <p className="text-xs text-indigo-200">Your trips appear first in search results</p>
                </div>
              </div>
            )
            if (score >= 10) return (
              <div className="bg-blue-500 text-white rounded-2xl px-4 py-2.5 flex items-center gap-2">
                <span className="text-xl">⭐</span>
                <div>
                  <p className="font-bold text-sm">Rising Driver</p>
                  <p className="text-xs text-blue-100">Keep earning trust to rank higher in search</p>
                </div>
              </div>
            )
            return (
              <div className="bg-gray-100 text-gray-600 rounded-2xl px-4 py-2.5 flex items-center gap-2">
                <span className="text-xl">🚗</span>
                <div>
                  <p className="font-bold text-sm">New Driver</p>
                  <p className="text-xs text-gray-400">Passengers redeeming points on your trips builds trust</p>
                </div>
              </div>
            )
          })()}
          <div className="mt-3 grid grid-cols-2 gap-3">
            <div className="bg-white/70 rounded-xl px-3 py-2 text-center">
              <p className="text-lg font-black text-indigo-600">{trustStats?.totalPointsRedeemedOnMyTrips ?? 0}</p>
              <p className="text-xs text-gray-500">Points redeemed by passengers</p>
            </div>
            <div className="bg-white/70 rounded-xl px-3 py-2 text-center">
              <p className="text-lg font-black text-indigo-600">{trustStats?.completedTripsAsDriver ?? 0}</p>
              <p className="text-xs text-gray-500">Completed trips</p>
            </div>
          </div>
        </div>
      )}

      {/* Edit form */}
      <div className="profile-card rounded-3xl p-6">        <h2 className="text-lg font-bold text-gray-800 mb-5 flex items-center gap-2">
          <span className="w-8 h-8 bg-indigo-100 rounded-xl flex items-center justify-center text-indigo-600">✏️</span>
          Edit Profile
        </h2>

        {success && (
          <div className="mb-4 bg-green-50 border border-green-200 text-green-700 rounded-xl px-4 py-3 text-sm flex items-center gap-2">
            <span>✅</span> Profile updated successfully!
          </div>
        )}
        {error && (
          <div className="mb-4 bg-red-50 border border-red-200 text-red-700 rounded-xl px-4 py-3 text-sm flex items-center gap-2">
            <span>⚠️</span> {error}
          </div>
        )}

        <form onSubmit={handleSave} className="space-y-5">
          {/* Email (read-only) */}
          <div>
            <label className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1.5 block">Email Address</label>
            <div className="flex items-center gap-2 bg-gray-50 border border-gray-200 rounded-xl px-4 py-3">
              <span className="text-gray-400">📧</span>
              <span className="text-gray-600 text-sm">{profile?.email}</span>
              <span className="ml-auto text-xs text-gray-400 bg-gray-100 px-2 py-0.5 rounded-lg">Read-only</span>
            </div>
          </div>

          {/* Full Name */}
          <div>
            <label className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1.5 block">Full Name</label>
            <div className="flex items-center border-2 border-gray-200 rounded-xl focus-within:border-indigo-500 focus-within:ring-2 focus-within:ring-indigo-100 transition-all">
              <span className="pl-4 text-gray-400">👤</span>
              <input value={fullName} onChange={e => setFullName(e.target.value)}
                placeholder="Your full name"
                className="flex-1 px-3 py-3 text-sm bg-transparent focus:outline-none text-gray-800"/>
            </div>
          </div>

          {/* Phone */}
          <div>
            <label className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1.5 block">Phone Number</label>
            <div className="flex items-center border-2 border-gray-200 rounded-xl focus-within:border-indigo-500 focus-within:ring-2 focus-within:ring-indigo-100 transition-all">
              <span className="pl-4 text-gray-400">📱</span>
              <input value={phone} onChange={e => setPhone(e.target.value)}
                placeholder="+91 98765 43210" type="tel"
                className="flex-1 px-3 py-3 text-sm bg-transparent focus:outline-none text-gray-800"/>
            </div>
          </div>

          {/* Photo URL */}
          <div>
            <label className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1.5 block">Profile Photo URL</label>
            <div className="flex items-center border-2 border-gray-200 rounded-xl focus-within:border-indigo-500 focus-within:ring-2 focus-within:ring-indigo-100 transition-all">
              <span className="pl-4 text-gray-400">🖼️</span>
              <input value={photoUrl} onChange={e => setPhotoUrl(e.target.value)}
                placeholder="https://example.com/photo.jpg"
                className="flex-1 px-3 py-3 text-sm bg-transparent focus:outline-none text-gray-800"/>
            </div>
            {photoUrl && (
              <div className="mt-2 flex items-center gap-2">
                <img src={photoUrl} alt="Preview" className="w-10 h-10 rounded-full object-cover border-2 border-indigo-200"
                  onError={e => { (e.target as HTMLImageElement).style.display = 'none' }}/>
                <span className="text-xs text-gray-400">Preview</span>
              </div>
            )}
          </div>

          <button type="submit" disabled={saving}
            className="w-full bg-gradient-to-r from-indigo-600 to-purple-600 text-white py-3.5 rounded-xl font-bold text-sm flex items-center justify-center gap-2 hover:from-indigo-700 hover:to-purple-700 hover:shadow-lg hover:shadow-indigo-200 active:scale-[0.98] transition-all disabled:opacity-60">
            {saving ? (
              <><svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/></svg>Saving...</>
            ) : '💾 Save Changes'}
          </button>
        </form>
      </div>

      {/* Points History */}
      <div className="profile-card rounded-3xl p-6 mt-6">
        <h2 className="text-lg font-bold text-gray-800 mb-4 flex items-center gap-2">
          <span className="w-8 h-8 bg-amber-100 rounded-xl flex items-center justify-center">⭐</span>
          Points History
        </h2>
        {pointsHistory.length === 0 ? (
          <div className="text-center py-8">
            <p className="text-4xl mb-3">🎯</p>
            <p className="text-gray-500 text-sm font-medium">No points earned yet</p>
            <p className="text-gray-400 text-xs mt-1">Complete rides to start earning 5–10 pts per trip</p>
          </div>
        ) : (
          <div className="space-y-3">
            {pointsHistory.map((h: any, i: number) => (
              <div key={i} className="flex items-center gap-3 bg-amber-50 border border-amber-100 rounded-2xl px-4 py-3">
                <div className="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center text-lg shrink-0">⭐</div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-gray-800 truncate">
                    {h.origin} → {h.destination}
                  </p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    {h.departureTime ? new Date(h.departureTime).toLocaleDateString('en-IN', { day:'numeric', month:'short', year:'numeric' }) : ''}
                    {' · '}₹{(h.fare || 0).toFixed(0)} · {h.seats} seat{h.seats > 1 ? 's' : ''}
                  </p>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-base font-black text-amber-600">+{h.pointsEarned}</p>
                  <p className="text-xs text-gray-400">pts</p>
                </div>
              </div>
            ))}
            <div className="flex items-center justify-between bg-gradient-to-r from-amber-500 to-yellow-500 rounded-2xl px-4 py-3 mt-2">
              <span className="text-white font-bold text-sm">Total Earned</span>
              <span className="text-white font-black text-lg">
                {pointsHistory.reduce((s: number, h: any) => s + (h.pointsEarned || 0), 0)} pts
              </span>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
