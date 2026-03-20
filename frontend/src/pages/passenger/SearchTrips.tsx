import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { searchTrips, getOpenTrips } from '../../api/trips'
import { getFareEstimate } from '../../api/fare'

// ── Location history (localStorage) ─────────────────────────────────────────
const HISTORY_KEY = 'rideshare_location_history'
const MAX_HISTORY = 8

function getLocationHistory(): string[] {
  try { return JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]') } catch { return [] }
}
function addToHistory(location: string) {
  if (!location.trim() || location.length < 3) return
  const hist = getLocationHistory().filter(h => h !== location)
  hist.unshift(location)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(hist.slice(0, MAX_HISTORY)))
}

// ── Popular suggestions ──────────────────────────────────────────────────────
const POPULAR_ROUTES = [
  { from: 'Mumbai Central', to: 'Pune Station' },
  { from: 'Bangalore MG Road', to: 'Mysore Bus Stand' },
  { from: 'Delhi Connaught Place', to: 'Agra Taj Mahal' },
  { from: 'Chennai Central', to: 'Coimbatore' },
  { from: 'Hyderabad Hitech City', to: 'Vijayawada' },
]
const CITY_SUGGESTIONS = [
  'Mumbai Central', 'Pune Station', 'Bangalore MG Road', 'Mysore Bus Stand',
  'Delhi Connaught Place', 'Agra Taj Mahal', 'Chennai Central', 'Coimbatore',
  'Hyderabad Hitech City', 'Vijayawada', 'Ahmedabad', 'Surat', 'Jaipur',
  'Lucknow', 'Chandigarh', 'Kochi', 'Goa Panaji', 'Nagpur',
]
const PEAK_TIMES = ['6:00 AM', '8:00 AM', '12:00 PM', '5:00 PM', '7:00 PM', '9:00 PM']

// ── Calendar ─────────────────────────────────────────────────────────────────
function CalendarPicker({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  const today = new Date(); today.setHours(0,0,0,0)
  const [viewYear, setViewYear] = useState(today.getFullYear())
  const [viewMonth, setViewMonth] = useState(today.getMonth())
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const selected = value ? new Date(value + 'T00:00:00') : null
  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']
  const fullMonths = ['January','February','March','April','May','June','July','August','September','October','November','December']
  const firstDay = new Date(viewYear, viewMonth, 1).getDay()
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate()

  useEffect(() => {
    const handler = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false) }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const prev = () => viewMonth === 0 ? (setViewMonth(11), setViewYear(y=>y-1)) : setViewMonth(m=>m-1)
  const next = () => viewMonth === 11 ? (setViewMonth(0), setViewYear(y=>y+1)) : setViewMonth(m=>m+1)

  const pick = (day: number) => {
    const d = new Date(viewYear, viewMonth, day)
    if (d < today) return
    onChange(`${viewYear}-${String(viewMonth+1).padStart(2,'0')}-${String(day).padStart(2,'0')}`)
    setOpen(false)
  }

  const display = selected ? selected.toLocaleDateString('en-IN',{day:'numeric',month:'short',year:'numeric'}) : 'Pick a date'

  return (
    <div className="relative" ref={ref}>
      <button type="button" onClick={() => setOpen(o=>!o)}
        className={`w-full flex items-center gap-2 border-2 rounded-xl px-3 py-2.5 text-sm text-left transition-all
          ${open ? 'border-indigo-500 ring-2 ring-indigo-100' : 'border-gray-200 hover:border-indigo-300'}
          ${value ? 'text-gray-800 font-medium' : 'text-gray-400'}`}>
        <svg className="w-4 h-4 text-indigo-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
        </svg>
        {display}
        {value && <button type="button" onClick={(e)=>{e.stopPropagation();onChange('')}} className="ml-auto text-gray-300 hover:text-red-400 text-xs">✕</button>}
      </button>
      {open && (
        <div className="absolute z-50 mt-2 bg-white border border-gray-100 rounded-2xl shadow-2xl p-4 w-72 slide-up">
          <div className="flex items-center justify-between mb-3">
            <button type="button" onClick={prev} className="w-7 h-7 rounded-lg hover:bg-indigo-50 flex items-center justify-center text-gray-500">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7"/></svg>
            </button>
            <span className="font-bold text-sm text-gray-700">{fullMonths[viewMonth]} {viewYear}</span>
            <button type="button" onClick={next} className="w-7 h-7 rounded-lg hover:bg-indigo-50 flex items-center justify-center text-gray-500">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7"/></svg>
            </button>
          </div>
          <div className="grid grid-cols-7 mb-1">
            {['S','M','T','W','T','F','S'].map((d,i) => <div key={i} className="text-center text-xs text-gray-400 font-semibold py-1">{d}</div>)}
          </div>
          <div className="grid grid-cols-7 gap-y-1">
            {Array.from({length:firstDay}).map((_,i)=><div key={`e${i}`}/>)}
            {Array.from({length:daysInMonth}).map((_,i)=>{
              const day=i+1, d=new Date(viewYear,viewMonth,day)
              const past=d<today
              const sel=selected&&selected.getFullYear()===viewYear&&selected.getMonth()===viewMonth&&selected.getDate()===day
              const tod=d.getTime()===today.getTime()
              return (
                <button key={day} type="button" onClick={()=>pick(day)} disabled={past}
                  className={`text-center text-sm py-1.5 rounded-lg transition-all font-medium
                    ${past?'text-gray-200 cursor-not-allowed':'cursor-pointer'}
                    ${sel?'bg-indigo-600 text-white shadow-md shadow-indigo-200':''}
                    ${tod&&!sel?'ring-2 ring-indigo-400 text-indigo-600':''}
                    ${!past&&!sel?'hover:bg-indigo-50 text-gray-700':''}`}>
                  {day}
                </button>
              )
            })}
          </div>
          <div className="mt-3 pt-3 border-t border-gray-100">
            <p className="text-xs text-gray-400 mb-2 font-medium">Popular travel days</p>
            <div className="flex gap-1 flex-wrap">
              {['Today','Tomorrow','This Weekend'].map(label=>{
                const d=new Date(); if(label==='Tomorrow') d.setDate(d.getDate()+1); if(label==='This Weekend'){const day=d.getDay();d.setDate(d.getDate()+(6-day))}
                const iso=`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
                return <button key={label} type="button" onClick={()=>{onChange(iso);setOpen(false)}}
                  className="text-xs bg-indigo-50 text-indigo-600 px-2 py-1 rounded-lg hover:bg-indigo-100 font-medium tag-pop">{label}</button>
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ── Location input with suggestions ──────────────────────────────────────────
function LocationInput({ label, value, onChange, placeholder, icon, onUseLocation }:
  { label: string; value: string; onChange: (v:string)=>void; placeholder: string; icon: React.ReactNode; onUseLocation?: ()=>void }) {
  const [open, setOpen] = useState(false)
  const [locating, setLocating] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const history = getLocationHistory()

  // Combine: history matches first, then city suggestions
  const filtered = value.length > 0
    ? [
        ...history.filter(h => h.toLowerCase().includes(value.toLowerCase()) && h !== value),
        ...CITY_SUGGESTIONS.filter(c => c.toLowerCase().includes(value.toLowerCase()) && c !== value && !history.includes(c))
      ].slice(0, 7)
    : history.slice(0, 5)  // show recent history when input is empty/focused

  useEffect(() => {
    const h = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false) }
    document.addEventListener('mousedown', h); return () => document.removeEventListener('mousedown', h)
  }, [])

  const handleGPS = () => {
    if (!onUseLocation) return
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        onChange(`${pos.coords.latitude.toFixed(4)}, ${pos.coords.longitude.toFixed(4)} (Current)`)
        setLocating(false); setOpen(false)
        onUseLocation()
      },
      () => { onChange('Location unavailable'); setLocating(false) },
      { timeout: 8000 }
    )
  }

  const handleSelect = (s: string) => { onChange(s); setOpen(false) }

  return (
    <div ref={ref} className="relative">
      <label className="text-xs font-semibold text-gray-500 mb-1.5 block uppercase tracking-wide">{label}</label>
      <div className={`flex items-center border-2 rounded-xl transition-all ${open ? 'border-indigo-500 ring-2 ring-indigo-100' : 'border-gray-200 hover:border-indigo-300'}`}>
        <span className="pl-3 shrink-0">{icon}</span>
        <input value={value} onChange={e=>{onChange(e.target.value);setOpen(true)}} onFocus={()=>setOpen(true)}
          placeholder={placeholder}
          className="flex-1 px-2 py-2.5 text-sm bg-transparent focus:outline-none text-gray-800 placeholder-gray-400"/>
        {onUseLocation && (
          <button type="button" onClick={handleGPS} title="Use current location"
            className="pr-3 text-indigo-400 hover:text-indigo-600 transition-colors shrink-0">
            {locating
              ? <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/></svg>
              : <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0zM15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
            }
          </button>
        )}
      </div>
      {open && filtered.length > 0 && (
        <div className="absolute z-40 mt-1 w-full bg-white border border-gray-100 rounded-xl shadow-xl overflow-hidden slide-up">
          {/* History section */}
          {value.length === 0 && history.length > 0 && (
            <div className="px-3 pt-2 pb-1">
              <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest flex items-center gap-1">
                🕐 Recent Searches
              </p>
            </div>
          )}
          {filtered.map((s, i) => {
            const isHistory = history.includes(s)
            return (
              <button key={s} type="button" onClick={() => handleSelect(s)}
                className="w-full text-left px-4 py-2.5 text-sm hover:bg-indigo-50 text-gray-700 flex items-center gap-2 transition-colors">
                {isHistory && value.length === 0
                  ? <span className="text-amber-400 shrink-0">🕐</span>
                  : <svg className="w-3.5 h-3.5 text-gray-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0zM15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
                    </svg>
                }
                <span className="flex-1">{s}</span>
                {isHistory && <span className="text-[10px] text-gray-300 shrink-0">recent</span>}
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}

// ── Preference toggle chip ────────────────────────────────────────────────────
function Chip({ label, active, onClick, color='indigo' }: { label:string; active:boolean; onClick:()=>void; color?:string }) {
  const colors: Record<string,string> = {
    indigo: active ? 'bg-indigo-600 text-white border-indigo-600 shadow-md shadow-indigo-200' : 'bg-white text-gray-600 border-gray-200 hover:border-indigo-300 hover:text-indigo-600',
    green:  active ? 'bg-green-600 text-white border-green-600 shadow-md shadow-green-200'   : 'bg-white text-gray-600 border-gray-200 hover:border-green-300 hover:text-green-600',
    purple: active ? 'bg-purple-600 text-white border-purple-600 shadow-md shadow-purple-200': 'bg-white text-gray-600 border-gray-200 hover:border-purple-300 hover:text-purple-600',
    amber:  active ? 'bg-amber-500 text-white border-amber-500 shadow-md shadow-amber-200'   : 'bg-white text-gray-600 border-gray-200 hover:border-amber-300 hover:text-amber-600',
  }
  return (
    <button type="button" onClick={onClick}
      className={`px-3 py-1.5 rounded-xl border text-xs font-semibold transition-all tag-pop ${colors[color]} ${active?'active':''}`}>
      {label}
    </button>
  )
}

// ── Main component ────────────────────────────────────────────────────────────
export default function SearchTrips() {
  const [origin, setOrigin] = useState('')
  const [destination, setDestination] = useState('')
  const [date, setDate] = useState('')
  const [timeFrom, setTimeFrom] = useState('')
  const [timeTo, setTimeTo] = useState('')
  const [seats, setSeats] = useState(1)
  const [results, setResults] = useState<any[]>([])
  const [fare, setFare] = useState<any>(null)
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const [showPrefs, setShowPrefs] = useState(false)
  const [timeFallback, setTimeFallback] = useState(false)

  // Preferences
  const [prefs, setPrefs] = useState({
    ac: false, nonAc: false,
    budget: false, comfort: false, luxury: false,
    morning: false, afternoon: false, evening: false, night: false,
    ladiesOnly: false, petFriendly: false, musicOk: false, smokingNo: true,
  })
  const togglePref = (k: keyof typeof prefs) => setPrefs(p => ({ ...p, [k]: !p[k] }))

  const navigate = useNavigate()

  // Parse departure time from array [y,m,d,h,min] or ISO string
  const parseTime = (t: any): Date => {
    if (Array.isArray(t)) return new Date(t[0], t[1]-1, t[2], t[3]||0, t[4]||0)
    return new Date(t)
  }

  // Smart sort: today's trips first (by departure time asc), then future trips
  const sortTrips = (list: any[]) => {
    const now = new Date()
    const todayStart = new Date(now); todayStart.setHours(0,0,0,0)
    const todayEnd = new Date(now); todayEnd.setHours(23,59,59,999)
    const todays = list.filter(t => { const d=parseTime(t.departureTime); return d>=todayStart&&d<=todayEnd })
      .sort((a,b) => parseTime(a.departureTime).getTime()-parseTime(b.departureTime).getTime())
    const future = list.filter(t => parseTime(t.departureTime)>todayEnd)
      .sort((a,b) => parseTime(a.departureTime).getTime()-parseTime(b.departureTime).getTime())
    return [...todays, ...future]
  }

  // Auto-load all open trips on mount
  useEffect(() => {
    setLoading(true)
    getOpenTrips()
      .then((data) => {
        const list: any[] = data.trips || (Array.isArray(data) ? data : [])
        setResults(sortTrips(list))
        setSearched(true)
      })
      .catch(() => setResults([]))
      .finally(() => setLoading(false))
  }, [])

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true); setSearched(true)
    // Save to history
    if (origin.trim()) addToHistory(origin.trim())
    if (destination.trim()) addToHistory(destination.trim())
    try {
      const params: any = { origin, destination, seats }
      if (date) params.date = date
      if (timeFrom) params.timeFrom = timeFrom
      if (timeTo) params.timeTo = timeTo
      const data = await searchTrips(params)
      // Backend returns { results: [...], message?: string }
      let list: any[] = data.results || data.trips || (Array.isArray(data) ? data : [])
      const wasTimeFallback = list.length > 0 && (timeFrom || timeTo) &&
        list.some((t: any) => {
          const h = parseTime(t.departureTime).getHours()
          const m = parseTime(t.departureTime).getMinutes()
          const tripMins = h * 60 + m
          const fromMins = timeFrom ? parseInt(timeFrom.split(':')[0]) * 60 + parseInt(timeFrom.split(':')[1]) : 0
          const toMins = timeTo ? parseInt(timeTo.split(':')[0]) * 60 + parseInt(timeTo.split(':')[1]) : 1440
          return tripMins < fromMins || tripMins > toMins
        })
      if (wasTimeFallback) setTimeFallback(true)
      else setTimeFallback(false)
      // Client-side preference filtering / sorting
      if (prefs.ac) list = list.filter((t:any) => t.hasAC !== false)
      if (prefs.budget) list = [...list].sort((a,b) => a.baseFarePerKm - b.baseFarePerKm)
      if (prefs.luxury) list = [...list].sort((a,b) => b.baseFarePerKm - a.baseFarePerKm)
      if (prefs.morning) list = list.filter((t:any) => { const h=parseTime(t.departureTime).getHours(); return h>=5&&h<12 })
      if (prefs.evening) list = list.filter((t:any) => { const h=parseTime(t.departureTime).getHours(); return h>=17&&h<21 })
      setResults(sortTrips(list))
      if (origin && destination) { try { const f = await getFareEstimate(origin, destination); setFare(f) } catch {} }
    } catch { setResults([]) }
    finally { setLoading(false) }
  }

  const usePopularRoute = (r: typeof POPULAR_ROUTES[0]) => { setOrigin(r.from); setDestination(r.to) }

  const safetyScore = (trip: any) => {
    let score = 80
    if (trip.driverRating >= 4.5) score += 10
    if (trip.totalTrips > 20) score += 5
    if (trip.verifiedDriver) score += 5
    return Math.min(score, 100)
  }

  const activePrefsCount = Object.values(prefs).filter(Boolean).length

  return (
    <div className="max-w-3xl mx-auto p-4 sm:p-6">
      {/* Hero */}
      <div className="relative rounded-2xl overflow-hidden mb-6 h-36">
        <img src="https://images.unsplash.com/photo-1502877338535-766e1452684a?w=900&auto=format&fit=crop"
          alt="Find a ride" className="w-full h-full object-cover"/>
        <div className="absolute inset-0 bg-gradient-to-r from-indigo-900/80 to-indigo-600/40 flex items-center px-6">
          <div className="text-white">
            <div className="flex items-center gap-3">
              <span className="text-3xl car-drive">🚗</span>
              <h1 className="text-2xl font-bold text-glow-indigo">Find Your Ride</h1>
            </div>
            <p className="text-indigo-200 text-sm mt-1">Smart suggestions · Safety scores · Your preferences</p>
          </div>
        </div>
      </div>

      {/* Popular routes */}
      <div className="mb-4">
        <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-2">Popular Routes</p>
        <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
          {POPULAR_ROUTES.map(r => (
            <button key={r.from} type="button" onClick={() => usePopularRoute(r)}
              className="shrink-0 flex items-center gap-1.5 bg-white border border-gray-200 rounded-xl px-3 py-2 text-xs font-medium text-gray-700 hover:border-indigo-400 hover:text-indigo-700 hover:shadow-md transition-all tag-pop whitespace-nowrap">
              <span className="text-green-500">●</span>{r.from.split(' ')[0]}
              <svg className="w-3 h-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7"/></svg>
              <span className="text-red-500">●</span>{r.to.split(' ')[0]}
            </button>
          ))}
        </div>
      </div>

      {/* Search form */}
      <form onSubmit={handleSearch} className="bg-white rounded-2xl shadow-lg border border-gray-100 p-5 mb-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
          <LocationInput label="From" value={origin} onChange={setOrigin} placeholder="Origin city or address"
            icon={<span className="w-3 h-3 rounded-full bg-green-500 inline-block"/>}
            onUseLocation={() => {}}/>
          <LocationInput label="To" value={destination} onChange={setDestination} placeholder="Destination city"
            icon={<svg className="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0zM15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>}/>
          <div>
            <label className="text-xs font-semibold text-gray-500 mb-1.5 block uppercase tracking-wide">Date</label>
            <CalendarPicker value={date} onChange={setDate}/>
          </div>
          <div>
            <label className="text-xs font-semibold text-gray-500 mb-1.5 block uppercase tracking-wide">Seats Needed</label>
            <div className="flex items-center border-2 border-gray-200 rounded-xl overflow-hidden hover:border-indigo-300 transition-colors">
              <button type="button" onClick={() => setSeats(s=>Math.max(1,s-1))}
                className="w-11 h-11 flex items-center justify-center text-gray-500 hover:bg-indigo-50 text-xl font-bold transition-colors btn-ripple">−</button>
              <span className="flex-1 text-center font-bold text-gray-800 text-lg">{seats}</span>
              <button type="button" onClick={() => setSeats(s=>s+1)}
                className="w-11 h-11 flex items-center justify-center text-gray-500 hover:bg-indigo-50 text-xl font-bold transition-colors btn-ripple">+</button>
            </div>
          </div>
        </div>

        {/* Time range filter */}
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div>
            <label className="text-xs font-semibold text-gray-500 mb-1.5 block uppercase tracking-wide">Departure From</label>
            <input type="time" value={timeFrom} onChange={e => setTimeFrom(e.target.value)}
              className="w-full border-2 border-gray-200 rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 hover:border-indigo-300 transition-all"/>
          </div>
          <div>
            <label className="text-xs font-semibold text-gray-500 mb-1.5 block uppercase tracking-wide">Departure Until</label>
            <input type="time" value={timeTo} onChange={e => setTimeTo(e.target.value)}
              className="w-full border-2 border-gray-200 rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 hover:border-indigo-300 transition-all"/>
          </div>
        </div>

        {/* Peak time suggestions */}
        <div className="mb-4">
          <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-2">Most Booked Times</p>
          <div className="flex gap-2 flex-wrap">
            {PEAK_TIMES.map(t => (
              <button key={t} type="button"
                className="text-xs bg-amber-50 text-amber-700 border border-amber-200 px-2.5 py-1 rounded-lg hover:bg-amber-100 font-medium tag-pop transition-all">
                🔥 {t}
              </button>
            ))}
          </div>
        </div>

        {/* Preferences toggle */}
        <div className="mb-4">
          <button type="button" onClick={() => setShowPrefs(p=>!p)}
            className="flex items-center gap-2 text-sm font-semibold text-indigo-600 hover:text-indigo-800 transition-colors">
            <svg className={`w-4 h-4 transition-transform ${showPrefs?'rotate-180':''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7"/>
            </svg>
            My Preferences
            {activePrefsCount > 0 && <span className="bg-indigo-600 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">{activePrefsCount}</span>}
          </button>

          {showPrefs && (
            <div className="mt-3 space-y-3 slide-up">
              <div>
                <p className="text-xs text-gray-400 font-semibold mb-2">🌡️ AC Preference</p>
                <div className="flex gap-2 flex-wrap">
                  <Chip label="❄️ AC" active={prefs.ac} onClick={()=>togglePref('ac')} color="indigo"/>
                  <Chip label="🌬️ Non-AC" active={prefs.nonAc} onClick={()=>togglePref('nonAc')} color="indigo"/>
                </div>
              </div>
              <div>
                <p className="text-xs text-gray-400 font-semibold mb-2">💰 Budget</p>
                <div className="flex gap-2 flex-wrap">
                  <Chip label="💸 Budget" active={prefs.budget} onClick={()=>togglePref('budget')} color="green"/>
                  <Chip label="🛋️ Comfort" active={prefs.comfort} onClick={()=>togglePref('comfort')} color="green"/>
                  <Chip label="👑 Luxury" active={prefs.luxury} onClick={()=>togglePref('luxury')} color="purple"/>
                </div>
              </div>
              <div>
                <p className="text-xs text-gray-400 font-semibold mb-2">🕐 Preferred Time</p>
                <div className="flex gap-2 flex-wrap">
                  <Chip label="🌅 Morning" active={prefs.morning} onClick={()=>togglePref('morning')} color="amber"/>
                  <Chip label="☀️ Afternoon" active={prefs.afternoon} onClick={()=>togglePref('afternoon')} color="amber"/>
                  <Chip label="🌆 Evening" active={prefs.evening} onClick={()=>togglePref('evening')} color="amber"/>
                  <Chip label="🌙 Night" active={prefs.night} onClick={()=>togglePref('night')} color="indigo"/>
                </div>
              </div>
              <div>
                <p className="text-xs text-gray-400 font-semibold mb-2">🛡️ Safety & Comfort</p>
                <div className="flex gap-2 flex-wrap">
                  <Chip label="👩 Ladies Only" active={prefs.ladiesOnly} onClick={()=>togglePref('ladiesOnly')} color="purple"/>
                  <Chip label="🐾 Pet Friendly" active={prefs.petFriendly} onClick={()=>togglePref('petFriendly')} color="green"/>
                  <Chip label="🎵 Music OK" active={prefs.musicOk} onClick={()=>togglePref('musicOk')} color="indigo"/>
                  <Chip label="🚭 No Smoking" active={prefs.smokingNo} onClick={()=>togglePref('smokingNo')} color="green"/>
                </div>
              </div>
            </div>
          )}
        </div>

        <button type="submit"
          className="w-full bg-gradient-to-r from-indigo-600 to-indigo-500 text-white py-3 rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-all hover:from-indigo-700 hover:to-indigo-600 hover:shadow-lg hover:shadow-indigo-200 active:scale-[0.98] btn-ripple btn-glow">
          {loading ? (
            <><svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/></svg>Searching...</>
          ) : (
            <><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>Search Rides</>
          )}
        </button>
      </form>

      {/* Fare estimate */}
      {fare && (
        <div className="bg-gradient-to-r from-indigo-50 to-purple-50 border border-indigo-200 rounded-2xl p-4 mb-5 flex items-center gap-4 slide-up">
          <div className="w-12 h-12 bg-indigo-100 rounded-xl flex items-center justify-center shrink-0 text-2xl">💰</div>
          <div>
            <p className="text-xs text-indigo-500 font-semibold uppercase tracking-wide">Estimated Fare</p>
            <p className="text-indigo-800 font-bold text-2xl">₹{fare.estimatedFare ?? fare.fare ?? '—'}
              {fare.distanceKm && <span className="text-sm font-normal text-indigo-400 ml-2">· {fare.distanceKm} km</span>}
            </p>
          </div>
          <div className="ml-auto text-right">
            <p className="text-xs text-gray-400">Safety window</p>
            <p className="text-sm font-bold text-green-600">+15 min buffer</p>
          </div>
        </div>
      )}

      {/* Time fallback notice */}
      {searched && !loading && timeFallback && results.length > 0 && (
        <div className="mb-3 bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 flex items-start gap-2 slide-up">
          <span className="text-amber-500 shrink-0 mt-0.5">⏰</span>
          <p className="text-xs text-amber-700 font-medium">
            No rides found at your requested time — showing all available rides to this destination instead
          </p>
        </div>
      )}

      {/* Results header */}
      {searched && !loading && results.length > 0 && (() => {
        const now = new Date()
        const todayEnd = new Date(now); todayEnd.setHours(23,59,59,999)
        const parseTime = (t: any): Date => Array.isArray(t)
          ? new Date(t[0], t[1]-1, t[2], t[3]||0, t[4]||0) : new Date(t)
        const todayCount = results.filter(t => parseTime(t.departureTime) <= todayEnd).length
        return (
          <div className="flex items-center justify-between mb-3 px-1">
            <p className="text-sm font-semibold text-gray-700">
              {todayCount > 0
                ? <><span className="text-indigo-600">{todayCount} ride{todayCount>1?'s':''} today</span>{results.length > todayCount ? ` · ${results.length - todayCount} upcoming` : ''}</>
                : <span>{results.length} ride{results.length>1?'s':''} available</span>
              }
            </p>
            <span className="text-xs text-gray-400">🏆 Trusted drivers first</span>
          </div>
        )
      })()}

      {/* Results */}
      <div className="space-y-3">
        {searched && !loading && results.length === 0 && (
          <div className="text-center py-16 text-gray-400 fade-in">
            <div className="text-5xl mb-4">🔍</div>
            <p className="font-semibold text-gray-600">No trips found</p>
            <p className="text-sm mt-1">Try different dates, locations, or relax your preferences</p>
          </div>
        )}

        {results.map((trip: any, idx: number) => {
          const safety = safetyScore(trip)
          const safetyColor = safety >= 90 ? 'text-green-600 bg-green-50' : safety >= 75 ? 'text-amber-600 bg-amber-50' : 'text-red-500 bg-red-50'
          return (
            <div key={trip.id} className="bg-white rounded-2xl border border-gray-100 p-5 card-glow slide-up"
              style={{ animationDelay: `${idx * 60}ms` }}>
              <div className="flex justify-between items-start gap-4">
                <div className="flex-1 min-w-0">
                  {/* Route */}
                  <div className="flex items-center gap-2 mb-1">
                    <span className="w-2.5 h-2.5 rounded-full bg-green-500 shrink-0"/>
                    <p className="text-sm font-bold text-gray-800 truncate">{trip.originAddress}</p>
                  </div>
                  <div className="ml-1 w-0.5 h-3 bg-gray-200 ml-1 mb-1"/>
                  <div className="flex items-center gap-2 mb-3">
                    <span className="w-2.5 h-2.5 rounded-full bg-red-500 shrink-0"/>
                    <p className="text-sm font-bold text-gray-800 truncate">{trip.destinationAddress}</p>
                  </div>

                  {/* Meta chips */}
                  <div className="flex flex-wrap gap-2 text-xs">
                    <span className="flex items-center gap-1 bg-gray-50 px-2 py-1 rounded-lg text-gray-600">
                      🕐 {Array.isArray(trip.departureTime)
                        ? new Date(trip.departureTime[0], trip.departureTime[1]-1, trip.departureTime[2], trip.departureTime[3]||0, trip.departureTime[4]||0).toLocaleString('en-IN', {dateStyle:'medium', timeStyle:'short'})
                        : new Date(trip.departureTime).toLocaleString('en-IN', {dateStyle:'medium', timeStyle:'short'})}
                    </span>
                    <span className="flex items-center gap-1 bg-gray-50 px-2 py-1 rounded-lg text-gray-600">
                      💺 {trip.availableSeats} seats
                    </span>
                    {trip.hasAC !== false && (
                      <span className="flex items-center gap-1 bg-blue-50 px-2 py-1 rounded-lg text-blue-600">❄️ AC</span>
                    )}
                    <span className={`flex items-center gap-1 px-2 py-1 rounded-lg font-semibold ${safetyColor}`}>
                      🛡️ {safety}% safe
                    </span>
                  </div>

                  {/* Driver */}
                  <div className="flex items-center gap-2 mt-3">
                    <div className="w-7 h-7 rounded-full bg-indigo-500 flex items-center justify-center text-white text-xs font-bold shrink-0">
                      {trip.driverName?.charAt(0)?.toUpperCase() || 'D'}
                    </div>
                    <div className="flex-1 min-w-0">
                      <span className="text-xs text-gray-600 font-semibold">{trip.driverName}</span>
                      <div className="flex items-center gap-2 mt-0.5">
                        <span className="text-[10px] text-yellow-600 font-semibold">
                          ⭐ {trip.driverRating != null ? Number(trip.driverRating).toFixed(1) : 'New'}
                        </span>
                        <span className="text-[10px] text-gray-400">·</span>
                        <span className="text-[10px] text-gray-500">{trip.driverCompletedTrips ?? 0} trips done</span>
                        {trip.driverTrustScore >= 50 && (
                          <span className="inline-flex items-center gap-0.5 bg-indigo-100 text-indigo-700 text-[10px] font-bold px-1.5 py-0.5 rounded-full">
                            🏆 Trusted
                          </span>
                        )}
                        {trip.driverTrustScore >= 10 && trip.driverTrustScore < 50 && (
                          <span className="inline-flex items-center gap-0.5 bg-blue-100 text-blue-700 text-[10px] font-bold px-1.5 py-0.5 rounded-full">
                            ⭐ Rising
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                </div>

                {/* Price + Book */}
                <div className="text-right shrink-0 flex flex-col items-end gap-2">
                  <div>
                    <p className="text-2xl font-black text-indigo-700">₹{trip.baseFarePerKm || trip.pricePerSeat}</p>
                    <p className="text-xs text-gray-400">per km</p>
                  </div>
                  <button onClick={() => navigate(`/book/${trip.tripId || trip.id}`)}
                    className="bg-gradient-to-r from-indigo-600 to-indigo-500 text-white px-5 py-2 rounded-xl font-bold text-sm hover:from-indigo-700 hover:to-indigo-600 hover:shadow-lg hover:shadow-indigo-200 active:scale-95 transition-all btn-ripple btn-glow">
                    🚗 Book →
                  </button>
                </div>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
