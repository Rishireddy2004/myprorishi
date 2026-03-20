import { useEffect, useState } from 'react'
import { getMyTransactions } from '../../api/transactions'

const typeConfig: Record<string, { color: string; bg: string; icon: string }> = {
  PAYMENT:      { color: 'text-red-600',    bg: 'bg-red-50',    icon: '💸' },
  REFUND:       { color: 'text-green-600',  bg: 'bg-green-50',  icon: '↩️' },
  PLATFORM_FEE: { color: 'text-amber-600',  bg: 'bg-amber-50',  icon: '🏷️' },
}

const statusColor: Record<string, string> = {
  SUCCESS: 'text-green-600 bg-green-50',
  PENDING: 'text-amber-600 bg-amber-50',
  FAILED:  'text-red-500 bg-red-50',
}

export default function Transactions() {
  const [txns, setTxns] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getMyTransactions()
      .then((d) => setTxns(d.transactions || d || []))
      .finally(() => setLoading(false))
  }, [])

  const totalSpent = txns.filter(t => t.type === 'PAYMENT').reduce((s, t) => s + (t.amount || 0), 0)
  const totalRefunded = txns.filter(t => t.type === 'REFUND').reduce((s, t) => s + (t.amount || 0), 0)

  return (
    <div className="max-w-3xl mx-auto p-4 sm:p-6">
      {/* Hero */}
      <div className="relative rounded-2xl overflow-hidden mb-6 h-40">
        <img
          src="https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=900&auto=format&fit=crop"
          alt="Payment transactions"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-indigo-900/80 to-indigo-600/40 flex items-center px-6">
          <div className="text-white">
            <h1 className="text-2xl font-bold">Transactions</h1>
            <p className="text-indigo-200 text-sm mt-1">Your payment history</p>
          </div>
        </div>
      </div>

      {/* Summary cards */}
      {txns.length > 0 && (
        <div className="grid grid-cols-3 gap-3 mb-6">
          <div className="bg-white rounded-2xl border border-gray-100 p-4 text-center card-glow">
            <p className="text-xs text-gray-400 mb-1">Total Spent</p>
            <p className="text-xl font-black text-red-500">₹{totalSpent.toFixed(0)}</p>
          </div>
          <div className="bg-white rounded-2xl border border-gray-100 p-4 text-center card-glow">
            <p className="text-xs text-gray-400 mb-1">Refunded</p>
            <p className="text-xl font-black text-green-600">₹{totalRefunded.toFixed(0)}</p>
          </div>
          <div className="bg-white rounded-2xl border border-gray-100 p-4 text-center card-glow">
            <p className="text-xs text-gray-400 mb-1">Transactions</p>
            <p className="text-xl font-black text-indigo-700">{txns.length}</p>
          </div>
        </div>
      )}

      {loading && (
        <div className="space-y-3">
          {[1,2,3].map(i => <div key={i} className="h-16 rounded-2xl shimmer"/>)}
        </div>
      )}

      {!loading && txns.length === 0 && (
        <div className="text-center py-16 text-gray-400 fade-in">
          <div className="text-5xl mb-4">💳</div>
          <p className="font-semibold text-gray-600">No transactions yet</p>
          <p className="text-sm mt-1">Your payment history will appear here</p>
        </div>
      )}

      {!loading && txns.length > 0 && (
        <div className="space-y-3">
          {txns.map((t: any, idx: number) => {
            const cfg = typeConfig[t.type] || { color: 'text-gray-700', bg: 'bg-gray-50', icon: '💰' }
            return (
              <div key={t.id} className="bg-white rounded-2xl border border-gray-100 p-4 flex items-center gap-4 card-glow slide-up"
                style={{ animationDelay: `${idx * 40}ms` }}>
                <div className={`w-11 h-11 rounded-xl ${cfg.bg} flex items-center justify-center text-xl shrink-0`}>
                  {cfg.icon}
                </div>
                <div className="flex-1 min-w-0">
                  <p className={`font-bold text-sm ${cfg.color}`}>{t.type}</p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    {t.createdAt ? new Date(t.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '—'}
                  </p>
                </div>
                <div className="text-right shrink-0">
                  <p className={`font-black text-base ${cfg.color}`}>
                    {t.type === 'REFUND' ? '+' : '−'}₹{t.amount}
                  </p>
                  {t.status && (
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${statusColor[t.status] || 'text-gray-500 bg-gray-50'}`}>
                      {t.status}
                    </span>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
