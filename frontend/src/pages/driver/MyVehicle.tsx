import { useEffect, useState } from 'react'
import api from '../../api/client'
import { getErrorMessage } from '../../api/errorUtils'

export default function MyVehicle() {
  const [vehicle, setVehicle] = useState({
    make: '', model: '', year: new Date().getFullYear(),
    color: '', licensePlate: '', passengerCapacity: 4,
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    api.get('/users/me/vehicle').then(r => {
      if (r.data) setVehicle(r.data)
    }).catch(() => {}).finally(() => setLoading(false))
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true); setError(''); setSuccess('')
    try {
      await api.put('/users/me/vehicle', vehicle)
      setSuccess('Vehicle details updated successfully.')
    } catch (err: any) {
      setError(getErrorMessage(err, 'Failed to update vehicle.'))
    } finally { setSaving(false) }
  }

  const set = (k: string, v: any) => setVehicle(p => ({ ...p, [k]: v }))

  return (
    <div className="max-w-xl mx-auto p-4 sm:p-6">
      <div className="relative rounded-2xl overflow-hidden mb-6 h-36">
        <img src="https://images.unsplash.com/photo-1494976388531-d1058494cdd8?w=900&auto=format&fit=crop"
          alt="Vehicle" className="w-full h-full object-cover" />
        <div className="absolute inset-0 bg-indigo-900/65 flex items-center px-8">
          <div className="text-white">
            <h1 className="text-2xl font-bold">My Vehicle</h1>
            <p className="text-indigo-200 text-sm mt-1">Update your car details anytime</p>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="space-y-3">{[1,2,3].map(i => <div key={i} className="h-12 rounded-xl bg-gray-100 animate-pulse"/>)}</div>
      ) : (
        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 space-y-4">
          {success && <div className="bg-green-50 border border-green-200 text-green-700 text-sm rounded-lg px-3 py-2">✅ {success}</div>}
          {error && <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-lg px-3 py-2">⚠️ {error}</div>}

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">Make</label>
              <input placeholder="e.g. Toyota" value={vehicle.make} onChange={e => set('make', e.target.value)}
                className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">Model</label>
              <input placeholder="e.g. Innova" value={vehicle.model} onChange={e => set('model', e.target.value)}
                className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">Year</label>
              <input type="number" min={2000} max={2030} value={vehicle.year} onChange={e => set('year', Number(e.target.value))}
                className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">Color</label>
              <input placeholder="e.g. White" value={vehicle.color} onChange={e => set('color', e.target.value)}
                className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">License Plate</label>
              <input placeholder="e.g. TS09AB1234" value={vehicle.licensePlate} onChange={e => set('licensePlate', e.target.value)}
                className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-600 mb-1 block">Passenger Capacity</label>
              <input type="number" min={1} max={20} value={vehicle.passengerCapacity} onChange={e => set('passengerCapacity', Number(e.target.value))}
                className="w-full border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400" required />
            </div>
          </div>

          <button type="submit" disabled={saving}
            className="w-full bg-indigo-600 text-white py-3 rounded-xl hover:bg-indigo-500 font-semibold text-sm transition-all disabled:opacity-50">
            {saving ? 'Saving...' : '💾 Save Vehicle Details'}
          </button>
        </form>
      )}
    </div>
  )
}
