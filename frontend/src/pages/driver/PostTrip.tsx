import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createTrip } from '../../api/trips'
import api from '../../api/client'
import { getErrorMessage } from '../../api/errorUtils'

const CAR_IMG = 'https://images.unsplash.com/photo-1494976388531-d1058494cdd8?w=900&auto=format&fit=crop'

export default function PostTrip() {
  const navigate = useNavigate()
  const [step, setStep] = useState<'vehicle' | 'trip'>('vehicle')

  const [vehicle, setVehicle] = useState({
    make: '', model: '', year: new Date().getFullYear(), color: '', licensePlate: '', passengerCapacity: 4,
  })
  const [form, setForm] = useState({
    originAddress: '', destinationAddress: '',
    departureTime: '', totalSeats: 1, baseFarePerKm: 5,
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const setV = (k: string, v: any) => setVehicle(p => ({ ...p, [k]: v }))
  const setF = (k: string, v: any) => setForm(p => ({ ...p, [k]: v }))

  const handleVehicleNext = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true); setError('')
    try {
      await api.put('/users/me/vehicle', vehicle)
      setStep('trip')
    } catch (err: any) {
      setError(getErrorMessage(err, 'Failed to save vehicle'))
    } finally { setLoading(false) }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true); setError('')
    try {
      // Backend expects LocalDateTime format (no timezone suffix)
      const dt = form.departureTime // datetime-local gives "yyyy-MM-ddTHH:mm" — backend accepts this directly
      await createTrip({ ...form, departureTime: dt })
      navigate('/my-trips')
    } catch (err: any) {
      setError(getErrorMessage(err, 'Failed to post trip'))
    } finally { setLoading(false) }
  }

  return (
    <div className="max-w-2xl mx-auto p-6">
      <div className="relative rounded-2xl overflow-hidden mb-6 h-44">
        <img src={CAR_IMG} alt="Post a trip" className="w-full h-full object-cover" />
        <div className="absolute inset-0 bg-gradient-to-r from-indigo-900/75 to-transparent flex items-center px-8">
          <div className="text-white">
            <div className="flex items-center gap-3">
              <span className="text-3xl car-drive">🚗</span>
              <h1 className="text-2xl font-bold">Post a Trip</h1>
            </div>
            <p className="text-indigo-200 text-sm mt-1">Share your journey and earn money</p>
          </div>
        </div>
      </div>

      {/* Step indicator */}
      <div className="flex items-center gap-3 mb-6">
        {['Vehicle Details', 'Trip Details'].map((label, i) => {
          const active = (i === 0 && step === 'vehicle') || (i === 1 && step === 'trip')
          const done = i === 0 && step === 'trip'
          return (
            <div key={label} className="flex items-center gap-2">
              <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold ${done ? 'bg-green-500 text-white' : active ? 'bg-indigo-600 text-white' : 'bg-gray-200 text-gray-500'}`}>
                {done ? '✓' : i + 1}
              </div>
              <span className={`text-sm font-medium ${active ? 'text-indigo-700' : 'text-gray-400'}`}>{label}</span>
              {i === 0 && <span className="text-gray-300 mx-1">→</span>}
            </div>
          )
        })}
      </div>

      {step === 'vehicle' && (
        <form onSubmit={handleVehicleNext} className="bg-white rounded-2xl shadow-md p-6 space-y-4">
          <p className="text-sm text-gray-500 mb-2">Enter your vehicle details before posting a trip.</p>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">Make</label>
              <input placeholder="e.g. Toyota" value={vehicle.make} onChange={e => setV('make', e.target.value)}
                className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">Model</label>
              <input placeholder="e.g. Innova" value={vehicle.model} onChange={e => setV('model', e.target.value)}
                className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">Year</label>
              <input type="number" min={2000} max={2030} value={vehicle.year} onChange={e => setV('year', Number(e.target.value))}
                className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">Color</label>
              <input placeholder="e.g. White" value={vehicle.color} onChange={e => setV('color', e.target.value)}
                className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">License Plate</label>
              <input placeholder="e.g. TS09AB1234" value={vehicle.licensePlate} onChange={e => setV('licensePlate', e.target.value)}
                className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">Passenger Capacity</label>
              <input type="number" min={1} max={20} value={vehicle.passengerCapacity} onChange={e => setV('passengerCapacity', Number(e.target.value))}
                className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
          </div>
          {error && <p className="text-red-500 text-sm">{error}</p>}
          <button type="submit" disabled={loading}
            className="w-full bg-indigo-600 text-white py-3 rounded-xl hover:bg-indigo-500 font-bold text-sm transition-all disabled:opacity-50 btn-glow btn-ripple active:scale-95">
            {loading ? '⏳ Saving...' : 'Next: Trip Details →'}
          </button>
        </form>
      )}

      {step === 'trip' && (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-md p-6 space-y-5">
          <div>
            <label className="text-xs font-medium text-gray-600 mb-1 block">Pickup Location</label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 w-3 h-3 rounded-full bg-green-500" />
              <input placeholder="e.g. Jagityal Bus Stand" value={form.originAddress}
                onChange={e => setF('originAddress', e.target.value)}
                className="w-full border rounded-lg pl-8 pr-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
          </div>
          <div>
            <label className="text-xs font-medium text-gray-600 mb-1 block">Drop-off Location</label>
            <div className="relative">
              <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0zM15 11a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <input placeholder="e.g. Hyderabad MGBS" value={form.destinationAddress}
                onChange={e => setF('destinationAddress', e.target.value)}
                className="w-full border rounded-lg pl-9 pr-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
          </div>
          <div>
            <label className="text-xs font-medium text-gray-600 mb-1 block">Departure Date & Time</label>
            <input type="datetime-local" value={form.departureTime}
              onChange={e => setF('departureTime', e.target.value)}
              className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">Available Seats</label>
              <div className="flex items-center gap-2">
                <button type="button" onClick={() => setF('totalSeats', Math.max(1, form.totalSeats - 1))}
                  className="w-9 h-9 rounded-lg border flex items-center justify-center text-gray-600 hover:bg-gray-50 font-bold text-lg">−</button>
                <span className="flex-1 text-center font-semibold text-gray-800">{form.totalSeats}</span>
                <button type="button" onClick={() => setF('totalSeats', form.totalSeats + 1)}
                  className="w-9 h-9 rounded-lg border flex items-center justify-center text-gray-600 hover:bg-gray-50 font-bold text-lg">+</button>
              </div>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">Fare per km (₹)</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm font-medium">₹</span>
                <input type="number" min={0} step={0.5} value={form.baseFarePerKm}
                  onChange={e => setF('baseFarePerKm', Number(e.target.value))}
                  className="w-full border rounded-lg pl-7 pr-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
              </div>
            </div>
          </div>

          {form.originAddress && form.destinationAddress && (
            <div className="bg-indigo-50 rounded-xl p-4 flex items-center gap-3 border border-indigo-100">
              <div className="w-10 h-10 bg-indigo-100 rounded-lg flex items-center justify-center shrink-0">🚗</div>
              <div className="text-sm">
                <p className="font-semibold text-indigo-800">{form.originAddress} → {form.destinationAddress}</p>
                <p className="text-indigo-500">{form.totalSeats} seats · ₹{form.baseFarePerKm}/km</p>
              </div>
            </div>
          )}

          {error && <p className="text-red-500 text-sm">{error}</p>}
          <div className="flex gap-3">
            <button type="button" onClick={() => setStep('vehicle')}
              className="flex-1 border-2 border-gray-200 py-3 rounded-xl text-sm font-semibold hover:bg-gray-50 transition-all">
              ← Back
            </button>
            <button type="submit" disabled={loading}
              className="flex-1 bg-gradient-to-r from-indigo-600 to-indigo-500 text-white py-3 rounded-xl hover:from-indigo-700 hover:to-indigo-600 font-bold text-sm transition-all disabled:opacity-50 btn-glow btn-ripple active:scale-95">
              {loading ? '⏳ Posting...' : '🚗 Post Trip'}
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
