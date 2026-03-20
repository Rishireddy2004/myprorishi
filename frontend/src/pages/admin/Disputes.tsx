import { useEffect, useState } from 'react'
import { getDisputes, resolveDispute } from '../../api/admin'

export default function Disputes() {
  const [disputes, setDisputes] = useState<any[]>([])
  const [resolving, setResolving] = useState<{ id: number; resolution: string } | null>(null)
  const [loading, setLoading] = useState(true)

  const load = () =>
    getDisputes()
      .then((d) => setDisputes(d.disputes || d || []))
      .finally(() => setLoading(false))

  useEffect(() => { load() }, [])

  const handleResolve = async () => {
    if (!resolving) return
    await resolveDispute(resolving.id, resolving.resolution)
    setResolving(null)
    load()
  }

  const open = disputes.filter(d => d.status === 'OPEN')
  const resolved = disputes.filter(d => d.status !== 'OPEN')

  return (
    <div className="max-w-3xl mx-auto p-4 sm:p-6">
      {/* Hero */}
      <div className="relative rounded-2xl overflow-hidden mb-6 h-36">
        <img
          src="https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=900&auto=format&fit=crop"
          alt="Dispute resolution"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-red-900/75 to-indigo-900/50 flex items-center px-8">
          <div className="text-white">
            <h1 className="text-2xl font-bold">Disputes</h1>
            <p className="text-red-200 text-sm mt-1">{open.length} open · {resolved.length} resolved</p>
          </div>
        </div>
      </div>

      {loading && <div className="space-y-3">{[1,2].map(i => <div key={i} className="h-24 rounded-2xl shimmer"/>)}</div>}

      {!loading && disputes.length === 0 && (
        <div className="text-center py-12 text-gray-400 fade-in">
          <div className="text-4xl mb-3">✅</div>
          <p className="font-semibold text-gray-600">No disputes</p>
          <p className="text-sm mt-1">All clear — no open disputes at the moment</p>
        </div>
      )}

      {!loading && disputes.length > 0 && (
        <div className="space-y-3">
          {disputes.map((d: any, idx: number) => (
            <div key={d.id} className="bg-white rounded-2xl border border-gray-100 p-5 card-glow slide-up"
              style={{ animationDelay: `${idx * 50}ms` }}>
              <div className="flex justify-between items-start gap-4">
                <div className="flex items-start gap-3 flex-1 min-w-0">
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-xl shrink-0 ${d.status === 'OPEN' ? 'bg-red-50' : 'bg-green-50'}`}>
                    {d.status === 'OPEN' ? '⚠️' : '✅'}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <p className="font-bold text-gray-800 text-sm">Dispute #{d.id}</p>
                      <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${d.status === 'OPEN' ? 'bg-amber-50 text-amber-700' : 'bg-green-50 text-green-700'}`}>
                        {d.status}
                      </span>
                    </div>
                    <p className="text-sm text-gray-500 leading-relaxed">{d.description}</p>
                    {d.resolution && (
                      <p className="text-xs text-green-600 mt-2 font-medium">Resolution: {d.resolution}</p>
                    )}
                  </div>
                </div>
                {d.status === 'OPEN' && (
                  <button onClick={() => setResolving({ id: d.id, resolution: '' })}
                    className="text-xs bg-indigo-100 text-indigo-700 px-4 py-2 rounded-xl hover:bg-indigo-200 font-semibold transition-colors shrink-0 btn-ripple">
                    Resolve
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {resolving && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-sm shadow-2xl space-y-4 slide-up">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-indigo-100 rounded-xl flex items-center justify-center text-xl">⚖️</div>
              <h2 className="font-bold text-lg text-gray-800">Resolve Dispute #{resolving.id}</h2>
            </div>
            <textarea placeholder="Enter resolution notes..." value={resolving.resolution}
              onChange={(e) => setResolving({ ...resolving, resolution: e.target.value })}
              className="w-full border-2 border-gray-200 rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:border-indigo-400 h-24 resize-none transition-colors"/>
            <div className="flex gap-3">
              <button onClick={handleResolve}
                className="flex-1 bg-indigo-600 text-white py-2.5 rounded-xl hover:bg-indigo-500 text-sm font-semibold transition-colors">
                Submit
              </button>
              <button onClick={() => setResolving(null)}
                className="flex-1 border-2 border-gray-200 py-2.5 rounded-xl text-sm hover:bg-gray-50 transition-colors">
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
