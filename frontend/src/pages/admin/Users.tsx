import { useEffect, useState } from 'react'
import { getAdminUsers, suspendUser, unsuspendUser } from '../../api/admin'

export default function Users() {
  const [users, setUsers] = useState<any[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)

  const load = (q?: string) =>
    getAdminUsers(q ? { search: q } : undefined)
      .then((d: any) => setUsers(d.users || d || []))
      .finally(() => setLoading(false))

  useEffect(() => { load() }, [])

  const toggleSuspend = async (user: any) => {
    if (user.suspended) await unsuspendUser(user.id)
    else await suspendUser(user.id)
    load(search)
  }

  const roleColor: Record<string, string> = {
    ADMIN: 'bg-purple-50 text-purple-700',
    DRIVER: 'bg-blue-50 text-blue-700',
    PASSENGER: 'bg-green-50 text-green-700',
  }

  return (
    <div className="max-w-4xl mx-auto p-4 sm:p-6">
      <div className="relative rounded-2xl overflow-hidden mb-6 h-36">
        <img src="https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=900&auto=format&fit=crop" alt="Users" className="w-full h-full object-cover" />
        <div className="absolute inset-0 bg-indigo-900/65 flex items-center px-8">
          <div className="text-white">
            <h1 className="text-2xl font-bold">Users</h1>
            <p className="text-indigo-200 text-sm mt-1">Manage platform users</p>
          </div>
        </div>
      </div>
      <form onSubmit={(e) => { e.preventDefault(); setLoading(true); load(search) }} className="flex gap-3 mb-5">
        <input placeholder="Search by name or email" value={search} onChange={(e) => setSearch(e.target.value)}
          className="flex-1 border-2 border-gray-200 rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:border-indigo-400" />
        <button type="submit" className="bg-indigo-600 text-white px-5 py-2.5 rounded-xl hover:bg-indigo-500 text-sm font-semibold">Search</button>
      </form>
      {loading && <div className="space-y-3">{[1,2,3].map(i => <div key={i} className="h-16 rounded-2xl shimmer" />)}</div>}
      {!loading && users.length === 0 && (
        <div className="text-center py-12 text-gray-400"><div className="text-4xl mb-3"></div><p className="font-semibold text-gray-600">No users found</p></div>
      )}
      {!loading && users.map((u: any, idx: number) => (
        <div key={u.id} className="bg-white rounded-2xl border border-gray-100 p-4 flex items-center gap-4 mb-3 card-glow slide-up" style={{ animationDelay: `${idx * 40}ms` }}>
          <div className="w-10 h-10 rounded-full bg-indigo-500 flex items-center justify-center text-white font-bold text-sm shrink-0">
            {u.fullName?.charAt(0)?.toUpperCase() || '?'}
          </div>
          <div className="flex-1 min-w-0">
            <p className="font-semibold text-gray-800 text-sm truncate">{u.fullName}</p>
            <p className="text-xs text-gray-400 truncate">{u.email}</p>
          </div>
          <span className={`text-xs font-bold px-2.5 py-1 rounded-xl shrink-0 ${roleColor[u.role] || 'bg-gray-50 text-gray-600'}`}>{u.role}</span>
          <button onClick={() => toggleSuspend(u)}
            className={`text-xs font-semibold px-4 py-2 rounded-xl transition-colors shrink-0 ${u.suspended ? 'bg-green-100 text-green-700 hover:bg-green-200' : 'bg-red-100 text-red-600 hover:bg-red-200'}`}>
            {u.suspended ? 'Unsuspend' : 'Suspend'}
          </button>
        </div>
      ))}
    </div>
  )
}