import { useEffect, useState } from 'react'
import { getPlatformConfig, updatePlatformConfig } from '../../api/admin'

export default function Config() {
  const [config, setConfig] = useState<any>(null)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => { getPlatformConfig().then(setConfig).catch(() => {}) }, [])

  const set = (k: string, v: any) => setConfig((c: any) => ({ ...c, [k]: v }))

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault(); setError('')
    try {
      await updatePlatformConfig(config)
      setSaved(true); setTimeout(() => setSaved(false), 2500)
    } catch { setError('Failed to save config') }
  }

  return (
    <div className="max-w-lg mx-auto p-4 sm:p-6">
      {/* Hero */}
      <div className="relative rounded-2xl overflow-hidden mb-6 h-36">
        <img
          src="https://images.unsplash.com/photo-1518770660439-4636190af475?w=900&auto=format&fit=crop"
          alt="Platform configuration"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-gray-900/80 to-indigo-900/50 flex items-center px-8">
          <div className="text-white">
            <h1 className="text-2xl font-bold">Platform Config</h1>
            <p className="text-gray-300 text-sm mt-1">Manage system-wide settings</p>
          </div>
        </div>
      </div>

      {!config && (
        <div className="space-y-3">{[1,2,3,4].map(i => <div key={i} className="h-14 rounded-2xl shimmer"/>)}</div>
      )}

      {config && (
        <form onSubmit={handleSave} className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 space-y-4">
          {Object.entries(config).map(([key, val]) => (
            <div key={key}>
              <label className="text-xs font-semibold text-gray-500 block mb-1.5 capitalize">
                {key.replace(/([A-Z])/g, ' $1')}
              </label>
              <input value={String(val)} onChange={(e) => set(key, e.target.value)}
                className="w-full border-2 border-gray-200 rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:border-indigo-400 transition-colors"/>
            </div>
          ))}

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-xl p-3 flex items-center gap-2">
              <span>⚠️</span>{error}
            </div>
          )}
          {saved && (
            <div className="bg-green-50 border border-green-200 text-green-700 text-sm rounded-xl p-3 flex items-center gap-2 slide-up">
              <span>✅</span>Configuration saved successfully
            </div>
          )}

          <button type="submit"
            className="w-full bg-gradient-to-r from-indigo-600 to-indigo-500 text-white py-3 rounded-xl hover:from-indigo-700 hover:to-indigo-600 font-semibold text-sm transition-all hover:shadow-lg hover:shadow-indigo-200 btn-ripple">
            Save Configuration
          </button>
        </form>
      )}
    </div>
  )
}
