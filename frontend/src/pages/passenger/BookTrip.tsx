import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getTrip } from '../../api/trips'
import { createBooking, getCoPassengers } from '../../api/bookings'
import { getMyProfile } from '../../api/auth'
import { getErrorMessage } from '../../api/errorUtils'

// Safety tips shown during booking
const SAFETY_TIPS = [
  { icon: '📸', text: 'Check driver photo matches before boarding' },
  { icon: '🔢', text: 'Verify vehicle number plate' },
  { icon: '📍', text: 'Share your live location with a trusted contact' },
  { icon: '🆘', text: 'Save emergency contact: 112' },
]

// Animated confirm button states
type BtnState = 'idle' | 'loading' | 'success'

export default function BookTrip() {
  const { tripId } = useParams()
  const navigate = useNavigate()
  const [trip, setTrip] = useState<any>(null)
  const [seats, setSeats] = useState(1)
  const [error, setError] = useState('')
  const [btnState, setBtnState] = useState<BtnState>('idle')
  const [agreedSafety, setAgreedSafety] = useState(false)
  const [specialNote, setSpecialNote] = useState('')
  const [payMethod, setPayMethod] = useState<'cash'|'upi'|'card'>('cash')
  const [bookedInfo, setBookedInfo] = useState<any>(null)
  const [coPassengers, setCoPassengers] = useState<any[]>([])
  const [myPoints, setMyPoints] = useState(0)
  const [redeemPoints, setRedeemPoints] = useState(0)
  const [tipAmount, setTipAmount] = useState(0)

  useEffect(() => {
    getTrip(tripId!).then(setTrip).catch(() => setError('Trip not found'))
    getMyProfile().then((p: any) => setMyPoints(p.loyaltyPoints || 0)).catch(() => {})
  }, [tripId])

  const handleBook = async () => {
    if (!agreedSafety) { return }
    setBtnState('loading')
    setError('')
    try {
      const booking = await createBooking(tripId!, seats, redeemPoints, tipAmount)
      setBtnState('success')
      setBookedInfo(booking)
      // Load co-passengers after booking
      try {
        const cp = await getCoPassengers(tripId!)
        setCoPassengers(cp.passengers || [])
      } catch { /* no co-passengers yet is fine */ }
    } catch (err: any) {
      setError(getErrorMessage(err, 'Booking failed'))
      setBtnState('idle')
    }
  }

  if (!trip) return (
    <div className="max-w-lg mx-auto p-6 flex flex-col items-center justify-center min-h-[60vh]">
      <div className="w-16 h-16 rounded-full border-4 border-indigo-100 border-t-indigo-600 animate-spin mb-4"/>
      <p className="text-gray-400 text-sm">Loading trip details...</p>
    </div>
  )

  // Show contact card after successful booking
  if (bookedInfo) {
    const phone = bookedInfo.driverPhone
    return (
      <div className="max-w-lg mx-auto p-6 flex flex-col items-center justify-center min-h-[60vh] slide-up">
        <div className="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center text-4xl mb-4 confetti-pop success-pulse">✅</div>
        <div className="text-5xl mb-2 car-drive">🚗</div>
        <h2 className="text-2xl font-black text-gray-800 mb-1 text-glow-green">Booking Requested!</h2>
        <p className="text-gray-500 text-sm mb-2 text-center">Your request is pending driver approval. Contact the driver below.</p>
        {bookedInfo?.remainingPoints != null && (
          <div className="flex items-center gap-2 bg-amber-50 border border-amber-200 rounded-xl px-4 py-2 mb-4 text-sm">
            <span>⭐</span>
            <span className="text-amber-700 font-semibold">
              You'll earn <strong>{Math.min(10, Math.max(5, 5 + Math.floor((bookedInfo.fareLocked || 0) / 50)))}</strong> pts when this ride completes · Balance: {bookedInfo.remainingPoints} pts
            </span>
          </div>
        )}
        <div className="w-full bg-white rounded-2xl border border-gray-100 shadow-lg p-5 mb-4 glow-border">
          <p className="text-xs font-bold text-indigo-600 uppercase tracking-wide mb-3">🚗 Driver Contact</p>
          <div className="flex items-center gap-3 mb-4">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white text-xl font-black">
              {bookedInfo.driverName?.charAt(0)?.toUpperCase() || 'D'}
            </div>
            <div>
              <p className="font-bold text-gray-800">{bookedInfo.driverName || 'Driver'}</p>
              <p className="text-xs text-gray-400">{bookedInfo.driverEmail}</p>
            </div>
          </div>
          {phone ? (
            <div className="flex gap-3">
              <a href={`tel:${phone}`}
                className="flex-1 flex items-center justify-center gap-2 bg-blue-600 text-white py-3 rounded-xl font-bold text-sm hover:bg-blue-700 active:scale-95 transition-all btn-ripple">
                📞 Call Driver
              </a>
              <a href={`https://wa.me/${phone.replace(/\D/g,'')}`} target="_blank" rel="noreferrer"
                className="flex-1 flex items-center justify-center gap-2 bg-green-500 text-white py-3 rounded-xl font-bold text-sm hover:bg-green-600 active:scale-95 transition-all btn-ripple">
                💬 WhatsApp
              </a>
            </div>
          ) : (
            <p className="text-xs text-gray-400 text-center">Driver phone not available yet</p>
          )}
        </div>
        <button onClick={() => navigate('/my-bookings')}
          className="w-full bg-indigo-600 text-white py-3 rounded-xl font-bold hover:bg-indigo-700 active:scale-95 transition-all btn-ripple">
          View My Bookings →
        </button>

        {/* Co-passengers */}
        {coPassengers.length > 0 && (
          <div className="w-full bg-white rounded-2xl border border-indigo-100 shadow p-5 mt-2 slide-up">
            <p className="text-xs font-bold text-indigo-600 uppercase tracking-wide mb-3">👥 Your Co-Passengers ({coPassengers.length})</p>
            <div className="space-y-2">
              {coPassengers.map((cp: any, i: number) => (
                <div key={i} className="flex items-center gap-3 bg-indigo-50 rounded-xl px-3 py-2">
                  <div className="w-9 h-9 rounded-full bg-gradient-to-br from-purple-500 to-indigo-600 flex items-center justify-center text-white font-bold text-sm shrink-0">
                    {cp.name?.charAt(0)?.toUpperCase() || '?'}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-gray-800 truncate">{cp.name}</p>
                    <p className="text-xs text-gray-400">{cp.seats} seat(s) · 📞 {cp.phone}</p>
                  </div>
                  <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full font-medium">Fellow rider</span>
                </div>
              ))}
            </div>
            <p className="text-xs text-gray-400 mt-3 text-center">Phone numbers are partially hidden for privacy</p>
          </div>
        )}
        {coPassengers.length === 0 && (
          <div className="w-full bg-gray-50 rounded-2xl border border-gray-100 p-4 mt-2 text-center">
            <p className="text-xs text-gray-400">🧍 You're the first passenger on this trip!</p>
          </div>
        )}
      </div>
    )
  }

  const total = trip.pricePerSeat * seats
  const discount = Math.min(redeemPoints, total * 0.5)
  const finalTotal = Math.max(0, total - discount) + tipAmount
  // Points earned preview: 5 base + 1 per ₹50, capped at 10
  const pointsPreview = Math.min(10, Math.max(5, 5 + Math.floor(finalTotal / 50)))
  const safetyScore = Math.min(100, 80 + (trip.driverRating >= 4.5 ? 10 : 0) + (trip.totalTrips > 20 ? 5 : 0) + (trip.verifiedDriver ? 5 : 0))
  const safetyColor = safetyScore >= 90 ? 'text-green-600' : safetyScore >= 75 ? 'text-amber-600' : 'text-red-500'

  return (
    <div className="max-w-lg mx-auto p-4 sm:p-6">
      {/* Hero */}
      <div className="relative rounded-2xl overflow-hidden mb-5 h-52">
        <img src="https://images.unsplash.com/photo-1471444928139-48c5bf5173f8?w=800&auto=format&fit=crop"
          alt="Road trip" className="w-full h-full object-cover"/>
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent flex items-end p-5">
          <div className="text-white w-full">
            <div className="flex items-center gap-2 mb-1">
              <span className="w-2 h-2 rounded-full bg-green-400"/>
              <p className="text-sm font-semibold">{trip.originAddress}</p>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-red-400"/>
              <p className="text-sm font-semibold">{trip.destinationAddress}</p>
            </div>
            <p className="text-gray-300 text-xs mt-2">🕐 {trip.departureTime}</p>
          </div>
          <span className="absolute top-4 right-4 text-3xl car-drive">🚗</span>
        </div>
      </div>

      <div className="space-y-4">
        {/* Trip stats */}
        <div className="grid grid-cols-3 gap-3">
          {[
            { label: 'Available', value: `${trip.availableSeats} seats`, bg: 'bg-gray-50', text: 'text-gray-800' },
            { label: 'Per Seat', value: `₹${trip.pricePerSeat}`, bg: 'bg-indigo-50', text: 'text-indigo-700' },
            { label: 'Safety', value: `${safetyScore}%`, bg: 'bg-green-50', text: safetyColor },
          ].map(s => (
            <div key={s.label} className={`${s.bg} rounded-2xl p-3 text-center card-glow`}>
              <p className="text-xs text-gray-400 mb-1">{s.label}</p>
              <p className={`font-black text-base ${s.text}`}>{s.value}</p>
            </div>
          ))}
        </div>

        {/* Driver card */}
        <div className="bg-white rounded-2xl border border-gray-100 p-4 flex items-center gap-4 card-glow">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white text-2xl font-black shrink-0">
            {trip.driverName?.charAt(0)?.toUpperCase() || 'D'}
          </div>
          <div className="flex-1">
            <p className="font-bold text-gray-800">{trip.driverName || 'Driver'}</p>
            <div className="flex items-center gap-2 mt-1 flex-wrap">
              <span className="text-xs text-yellow-500 font-semibold">
                ⭐ {trip.driverRating != null ? Number(trip.driverRating).toFixed(1) : 'New'}
              </span>
              <span className="text-xs text-gray-400">·</span>
              <span className="text-xs text-gray-500">{trip.driverCompletedTrips ?? 0} trips completed</span>
              {trip.verifiedDriver && <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full font-semibold">✓ Verified</span>}
            </div>
            {/* Trust badge */}
            <div className="mt-1.5">
              {trip.driverTrustScore >= 50 ? (
                <span className="inline-flex items-center gap-1 bg-indigo-100 text-indigo-700 text-xs font-bold px-2 py-0.5 rounded-full">
                  🏆 Trusted Driver · {trip.driverTrustScore} pts
                </span>
              ) : trip.driverTrustScore >= 10 ? (
                <span className="inline-flex items-center gap-1 bg-blue-100 text-blue-700 text-xs font-bold px-2 py-0.5 rounded-full">
                  ⭐ Rising Driver · {trip.driverTrustScore} pts
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 bg-gray-100 text-gray-500 text-xs px-2 py-0.5 rounded-full">
                  🚗 New Driver
                </span>
              )}
            </div>
          </div>
          <div className="text-right shrink-0">
            {trip.hasAC !== false && <span className="text-xs bg-blue-100 text-blue-600 px-2 py-1 rounded-lg font-semibold block mb-1">❄️ AC</span>}
            <span className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded-lg font-semibold">{trip.vehicleType || 'Sedan'}</span>
          </div>
        </div>

        {/* Directions button */}
        {trip.originAddress && trip.destinationAddress && (
          <a
            href={`https://www.google.com/maps/dir/?api=1&origin=${encodeURIComponent(trip.originAddress)}&destination=${encodeURIComponent(trip.destinationAddress)}&travelmode=driving`}
            target="_blank" rel="noreferrer"
            className="flex items-center justify-center gap-2 w-full bg-white border-2 border-indigo-200 text-indigo-700 py-3 rounded-2xl font-semibold text-sm hover:bg-indigo-50 transition-colors">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/>
            </svg>
            View Route on Google Maps
          </a>
        )}

        {/* Safety info */}
        <div className="bg-gradient-to-r from-green-50 to-emerald-50 border border-green-200 rounded-2xl p-4">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <span className="text-lg">🛡️</span>
              <p className="font-bold text-green-800 text-sm">Safety Score: {safetyScore}/100</p>
            </div>
            <div className="w-24 h-2 bg-green-200 rounded-full overflow-hidden">
              <div className="h-full bg-green-500 rounded-full transition-all" style={{ width: `${safetyScore}%` }}/>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2">
            {SAFETY_TIPS.map(t => (
              <div key={t.text} className="flex items-start gap-1.5 text-xs text-green-700">
                <span className="shrink-0">{t.icon}</span>
                <span>{t.text}</span>
              </div>
            ))}
          </div>
          <p className="text-xs text-green-600 mt-3 font-semibold">
            ⏱️ Recommended arrival: {trip.departureTime ? (() => {
              try { const d = new Date(trip.departureTime); d.setMinutes(d.getMinutes() - 10); return d.toLocaleTimeString('en-IN',{hour:'2-digit',minute:'2-digit'}) } catch { return '10 min early' }
            })() : '10 min early'} (10 min buffer)
          </p>
        </div>

        {/* Seat selector */}
        <div className="bg-white rounded-2xl border border-gray-100 p-4 card-glow">
          <label className="text-xs font-bold text-gray-500 uppercase tracking-wide block mb-3">Seats to Book</label>
          <div className="flex items-center gap-4">
            <button type="button" onClick={() => setSeats(s=>Math.max(1,s-1))}
              className="w-12 h-12 rounded-2xl bg-gray-100 hover:bg-indigo-100 hover:text-indigo-700 flex items-center justify-center text-gray-600 font-black text-2xl transition-all active:scale-90 btn-ripple">−</button>
            <div className="flex-1 text-center">
              <span className="text-4xl font-black text-indigo-700">{seats}</span>
              <p className="text-xs text-gray-400 mt-1">seat{seats > 1 ? 's' : ''}</p>
            </div>
            <button type="button" onClick={() => setSeats(s=>Math.min(trip.availableSeats,s+1))}
              className="w-12 h-12 rounded-2xl bg-gray-100 hover:bg-indigo-100 hover:text-indigo-700 flex items-center justify-center text-gray-600 font-black text-2xl transition-all active:scale-90 btn-ripple">+</button>
          </div>
        </div>

        {/* Loyalty points */}
        {myPoints > 0 && (
          <div className="bg-gradient-to-r from-amber-50 to-yellow-50 border border-amber-200 rounded-2xl p-4 card-glow">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <span className="text-xl">⭐</span>
                <div>
                  <p className="font-bold text-amber-800 text-sm">Loyalty Points</p>
                  <p className="text-xs text-amber-600">You have {myPoints} pts · ₹{myPoints} value</p>
                </div>
              </div>
              <span className="text-xs bg-amber-100 text-amber-700 px-2 py-1 rounded-lg font-bold">{myPoints} pts</span>
            </div>
            <label className="text-xs font-semibold text-amber-700 block mb-2">Redeem points (max 50% of fare)</label>
            <div className="flex items-center gap-3">
              <input type="range" min={0} max={myPoints} value={redeemPoints}
                onChange={e => setRedeemPoints(Number(e.target.value))}
                className="flex-1 accent-amber-500"/>
              <span className="text-sm font-black text-amber-700 w-16 text-right">{redeemPoints} pts</span>
            </div>
            {redeemPoints > 0 && (
              <p className="text-xs text-green-700 font-semibold mt-2">
                💰 Discount: ₹{Math.min(redeemPoints, total * 0.5).toFixed(0)} off
              </p>
            )}
          </div>
        )}

        {/* Tip for driver */}
        <div className="bg-white rounded-2xl border border-gray-100 p-4 card-glow">
          <label className="text-xs font-bold text-gray-500 uppercase tracking-wide block mb-3">
            💝 Tip for Driver <span className="font-normal text-gray-400">(optional)</span>
          </label>
          <div className="flex gap-2 flex-wrap">
            {[0, 10, 20, 50, 100].map(amt => (
              <button key={amt} type="button"
                onClick={() => setTipAmount(amt)}
                className={`px-4 py-2 rounded-xl border-2 text-sm font-bold transition-all btn-ripple
                  ${tipAmount === amt
                    ? 'border-pink-500 bg-pink-50 text-pink-700 shadow-md shadow-pink-100'
                    : 'border-gray-200 text-gray-600 hover:border-pink-300 hover:text-pink-600'}`}>
                {amt === 0 ? 'No tip' : `₹${amt}`}
              </button>
            ))}
          </div>
          {tipAmount > 0 && (
            <p className="text-xs text-pink-600 font-semibold mt-2">
              🎉 ₹{tipAmount} tip will be added — drivers love this!
            </p>
          )}
        </div>

        {/* Payment method */}
        <div className="bg-white rounded-2xl border border-gray-100 p-4 card-glow">
          <label className="text-xs font-bold text-gray-500 uppercase tracking-wide block mb-3">Payment Method</label>
          <div className="grid grid-cols-3 gap-2">
            {([['cash','💵','Cash'],['upi','📱','UPI'],['card','💳','Card']] as const).map(([m,icon,label])=>(
              <button key={m} type="button" onClick={()=>setPayMethod(m)}
                className={`flex flex-col items-center gap-1 py-3 rounded-xl border-2 text-sm font-semibold transition-all btn-ripple
                  ${payMethod===m ? 'border-indigo-500 bg-indigo-50 text-indigo-700 shadow-md shadow-indigo-100' : 'border-gray-200 text-gray-600 hover:border-indigo-300'}`}>
                <span className="text-xl">{icon}</span>{label}
              </button>
            ))}
          </div>
        </div>

        {/* Special note */}
        <div className="bg-white rounded-2xl border border-gray-100 p-4 card-glow">
          <label className="text-xs font-bold text-gray-500 uppercase tracking-wide block mb-2">Special Note to Driver <span className="font-normal text-gray-400">(optional)</span></label>
          <textarea value={specialNote} onChange={e=>setSpecialNote(e.target.value)}
            placeholder="e.g. I have luggage, please wait 2 min at pickup..."
            className="w-full border-2 border-gray-200 rounded-xl px-3 py-2 text-sm focus:outline-none focus:border-indigo-400 resize-none h-20 text-gray-700 placeholder-gray-400 transition-colors"/>
        </div>

        {/* Total */}
        <div className="bg-gradient-to-r from-indigo-600 to-purple-600 rounded-2xl p-4 text-white">
          <div className="flex justify-between items-center">
            <div>
              <p className="text-indigo-200 text-xs font-semibold uppercase tracking-wide">Total Amount</p>
              <p className="text-3xl font-black mt-1">₹{finalTotal.toFixed(0)}</p>
              <p className="text-indigo-200 text-xs mt-1">
                {seats} seat{seats>1?'s':''} × ₹{trip.pricePerSeat}
                {discount > 0 && <span className="text-green-300"> − ₹{discount.toFixed(0)} pts</span>}
                {tipAmount > 0 && <span className="text-pink-300"> + ₹{tipAmount} tip</span>}
                {' '}via {payMethod.toUpperCase()}
              </p>
              <p className="text-amber-300 text-xs mt-1 font-semibold">⭐ Earn {pointsPreview} loyalty pts on completion</p>
            </div>
            <div className="text-right">
              <p className="text-indigo-200 text-xs">Est. duration</p>
              <p className="text-lg font-bold">{trip.estimatedDuration || '~2h 30m'}</p>
            </div>
          </div>
        </div>

        {/* Safety agreement */}
        <label className="flex items-start gap-3 cursor-pointer">
          <div className={`w-5 h-5 rounded-md border-2 flex items-center justify-center shrink-0 mt-0.5 transition-all
            ${agreedSafety ? 'bg-indigo-600 border-indigo-600' : 'border-gray-300 hover:border-indigo-400'}`}
            onClick={() => setAgreedSafety(a=>!a)}>
            {agreedSafety && <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7"/></svg>}
          </div>
          <span className="text-xs text-gray-500 leading-relaxed">
            I have read the safety guidelines and agree to the <span className="text-indigo-600 font-semibold">terms of service</span>. I will verify the driver and vehicle before boarding.
          </span>
        </label>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-xl p-3 flex items-center gap-2 slide-up">
            <span>⚠️</span>{error}
          </div>
        )}

        {/* Confirm button */}
        <button onClick={handleBook} disabled={btnState !== 'idle'}
          className={`w-full py-4 rounded-2xl font-black text-base flex items-center justify-center gap-3 transition-all btn-ripple btn-glow
            ${btnState === 'success'
              ? 'bg-green-500 text-white shadow-lg shadow-green-200 success-pulse'
              : !agreedSafety
              ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-white hover:from-amber-600 hover:to-orange-600 hover:shadow-lg hover:shadow-amber-200 active:scale-[0.98]'
              : 'bg-gradient-to-r from-indigo-600 to-purple-600 text-white hover:from-indigo-700 hover:to-purple-700 hover:shadow-xl hover:shadow-indigo-200 active:scale-[0.98]'
            }`}>
          {btnState === 'loading' && <svg className="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/></svg>}
          {btnState === 'success' && <span className="text-xl confetti-pop">✓</span>}
          {btnState === 'idle' && !agreedSafety && <span>🛡️</span>}
          {btnState === 'idle' && agreedSafety && <span className="car-drive">🚗</span>}
          {btnState === 'loading' && <span className="car-zoom">🚗</span>}
          <span>
            {btnState === 'loading' ? 'Confirming...'
              : btnState === 'success' ? 'Booked! Redirecting...'
              : !agreedSafety ? 'Agree to Safety Guidelines First'
              : `Confirm Booking · ₹${finalTotal.toFixed(0)}`}
          </span>
        </button>
      </div>
    </div>
  )
}
