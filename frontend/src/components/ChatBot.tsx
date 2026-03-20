import { useState, useRef, useEffect } from 'react'

// ── FAQ knowledge base ────────────────────────────────────────────────────────
const FAQ: { q: string[]; a: string }[] = [
  {
    q: ['how to book', 'book a ride', 'book trip', 'how do i book'],
    a: '🚗 To book a ride: Go to "Search Trips" → find a trip → click "Book →" → choose seats & payment → confirm. The driver will accept/reject your request.',
  },
  {
    q: ['cancel booking', 'how to cancel', 'cancel my ride'],
    a: '❌ To cancel: Go to "My Bookings" → find your booking → click "Cancel". Cancellations must be made 2+ hours before departure.',
  },
  {
    q: ['post trip', 'how to post', 'create trip', 'add trip'],
    a: '📋 To post a trip: Go to "Post Trip" → fill in vehicle details → enter pickup, drop-off, date/time, seats & fare → submit.',
  },
  {
    q: ['register', 'sign up', 'create account', 'new account'],
    a: '✍️ Click "Create one" on the login page. Fill in your name, email, password, phone and choose your role (Passenger / Driver / Both).',
  },
  {
    q: ['forgot password', 'reset password', 'cant login', "can't login"],
    a: '🔑 Click "Forgot password?" on the login page. Enter your email and you\'ll get reset instructions. Or use the direct reset option.',
  },
  {
    q: ['driver contact', 'contact driver', 'driver phone', 'call driver'],
    a: '📞 After booking, the driver\'s phone number appears on the booking confirmation screen and in "My Bookings". You can call or WhatsApp them directly.',
  },
  {
    q: ['co passenger', 'fellow passenger', 'who else', 'other passengers'],
    a: '👥 After booking, scroll down on the confirmation screen to see your co-passengers (names and masked phone numbers).',
  },
  {
    q: ['payment', 'how to pay', 'pay for ride', 'upi', 'cash'],
    a: '💳 Payment methods: Cash, UPI, or Card. Select your preferred method during booking. Payment is made directly to the driver.',
  },
  {
    q: ['track trip', 'track my ride', 'where is driver', 'live location'],
    a: '📍 Once your booking is CONFIRMED, go to "My Bookings" → click "📍 Track" to see the live trip location.',
  },
  {
    q: ['safety', 'is it safe', 'safety tips', 'safe ride'],
    a: '🛡️ Safety tips: Always verify the driver photo & number plate before boarding. Share your live location with a trusted contact. Emergency: call 112.',
  },
  {
    q: ['manage bookings', 'accept passenger', 'reject passenger', 'approve booking'],
    a: '✅ Drivers: Go to "My Trips" → click "Manage" on a trip → Accept or Reject passenger requests.',
  },
  {
    q: ['my trips', 'driver trips', 'posted trips'],
    a: '🗂️ Go to "My Trips" in the navbar to see all trips you\'ve posted as a driver.',
  },
  {
    q: ['vehicle', 'my vehicle', 'add vehicle', 'car details'],
    a: '🚙 Go to "My Vehicle" in the navbar to add or update your vehicle details (make, model, plate number, etc.).',
  },
  {
    q: ['helpline', 'support', 'help', 'contact support', 'phone number'],
    a: '📞 Helpline: 1800-RIDESHARE (toll-free) | Email: support@rideshare.app | Available 24/7.',
  },
  {
    q: ['hello', 'hi', 'hey', 'helo', 'hii'],
    a: '👋 Hi there! I\'m the RideShare assistant. Ask me anything about booking rides, posting trips, payments, safety, or anything else!',
  },
]

function getBotReply(input: string): string {
  const lower = input.toLowerCase()
  for (const entry of FAQ) {
    if (entry.q.some(kw => lower.includes(kw))) return entry.a
  }
  return "🤔 I'm not sure about that. Try asking about: booking a ride, cancelling, posting a trip, payments, safety, or contact the helpline at 1800-RIDESHARE."
}

type Msg = { from: 'user' | 'bot'; text: string }

const QUICK_QUESTIONS = ['How to book?', 'Cancel booking', 'Post a trip', 'Contact driver', 'Safety tips']

export default function ChatBot() {
  const [open, setOpen] = useState(false)
  const [showHelp, setShowHelp] = useState(false)
  const [msgs, setMsgs] = useState<Msg[]>([
    { from: 'bot', text: '👋 Hi! I\'m your RideShare assistant. How can I help you today?' }
  ])
  const [input, setInput] = useState('')
  const [typing, setTyping] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [msgs, typing])

  const send = (text: string) => {
    if (!text.trim()) return
    setMsgs(m => [...m, { from: 'user', text }])
    setInput('')
    setTyping(true)
    setTimeout(() => {
      setTyping(false)
      setMsgs(m => [...m, { from: 'bot', text: getBotReply(text) }])
    }, 700)
  }

  return (
    <>
      {/* Helpline modal */}
      {showHelp && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={() => setShowHelp(false)}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-sm slide-up" onClick={e => e.stopPropagation()}>
            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 bg-red-100 rounded-2xl flex items-center justify-center text-2xl">🆘</div>
              <div>
                <h3 className="font-black text-gray-800 text-lg">Helpline</h3>
                <p className="text-xs text-gray-400">We're here 24/7</p>
              </div>
              <button onClick={() => setShowHelp(false)} className="ml-auto text-gray-400 hover:text-gray-600 text-xl">✕</button>
            </div>
            <div className="space-y-3">
              <a href="tel:18007433742" className="flex items-center gap-3 bg-green-50 border border-green-200 rounded-xl p-3 hover:bg-green-100 transition-colors">
                <span className="text-2xl">📞</span>
                <div>
                  <p className="font-bold text-green-800 text-sm">Toll-Free Helpline</p>
                  <p className="text-green-600 text-xs">1800-RIDESHARE · 24/7</p>
                </div>
              </a>
              <a href="mailto:support@rideshare.app" className="flex items-center gap-3 bg-blue-50 border border-blue-200 rounded-xl p-3 hover:bg-blue-100 transition-colors">
                <span className="text-2xl">📧</span>
                <div>
                  <p className="font-bold text-blue-800 text-sm">Email Support</p>
                  <p className="text-blue-600 text-xs">support@rideshare.app</p>
                </div>
              </a>
              <a href="https://wa.me/911800743374" target="_blank" rel="noreferrer" className="flex items-center gap-3 bg-emerald-50 border border-emerald-200 rounded-xl p-3 hover:bg-emerald-100 transition-colors">
                <span className="text-2xl">💬</span>
                <div>
                  <p className="font-bold text-emerald-800 text-sm">WhatsApp Support</p>
                  <p className="text-emerald-600 text-xs">Chat with us instantly</p>
                </div>
              </a>
              <div className="flex items-center gap-3 bg-red-50 border border-red-200 rounded-xl p-3">
                <span className="text-2xl">🚨</span>
                <div>
                  <p className="font-bold text-red-800 text-sm">Emergency</p>
                  <p className="text-red-600 text-xs">Call 112 for immediate help</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Chat window */}
      {open && (
        <div className="fixed bottom-24 right-4 sm:right-6 w-80 sm:w-96 bg-white rounded-2xl shadow-2xl border border-gray-100 z-40 flex flex-col overflow-hidden slide-up"
          style={{ maxHeight: '70vh' }}>
          {/* Header */}
          <div className="bg-gradient-to-r from-indigo-600 to-purple-600 px-4 py-3 flex items-center gap-3">
            <div className="w-9 h-9 bg-white/20 rounded-xl flex items-center justify-center text-xl">🤖</div>
            <div className="flex-1">
              <p className="text-white font-bold text-sm">RideShare Assistant</p>
              <div className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-green-400 bounce-dot"/>
                <p className="text-indigo-200 text-xs">Online · Instant replies</p>
              </div>
            </div>
            <button onClick={() => setShowHelp(true)} title="Helpline"
              className="text-white/70 hover:text-white text-lg transition-colors mr-1">📞</button>
            <button onClick={() => setOpen(false)} className="text-white/70 hover:text-white text-xl transition-colors">✕</button>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto p-3 space-y-2 bg-gray-50" style={{ minHeight: 200 }}>
            {msgs.map((m, i) => (
              <div key={i} className={`flex ${m.from === 'user' ? 'justify-end' : 'justify-start'}`}>
                {m.from === 'bot' && (
                  <div className="w-7 h-7 bg-indigo-100 rounded-full flex items-center justify-center text-sm mr-2 shrink-0 mt-0.5">🤖</div>
                )}
                <div className={`max-w-[75%] px-3 py-2 rounded-2xl text-sm leading-relaxed
                  ${m.from === 'user'
                    ? 'bg-indigo-600 text-white rounded-br-sm'
                    : 'bg-white text-gray-700 border border-gray-100 shadow-sm rounded-bl-sm'}`}>
                  {m.text}
                </div>
              </div>
            ))}
            {typing && (
              <div className="flex justify-start">
                <div className="w-7 h-7 bg-indigo-100 rounded-full flex items-center justify-center text-sm mr-2 shrink-0">🤖</div>
                <div className="bg-white border border-gray-100 shadow-sm px-4 py-3 rounded-2xl rounded-bl-sm flex gap-1 items-center">
                  <span className="w-2 h-2 bg-indigo-400 rounded-full bounce-dot" style={{ animationDelay: '0ms' }}/>
                  <span className="w-2 h-2 bg-indigo-400 rounded-full bounce-dot" style={{ animationDelay: '150ms' }}/>
                  <span className="w-2 h-2 bg-indigo-400 rounded-full bounce-dot" style={{ animationDelay: '300ms' }}/>
                </div>
              </div>
            )}
            <div ref={bottomRef}/>
          </div>

          {/* Quick questions */}
          <div className="px-3 py-2 bg-white border-t border-gray-100 flex gap-1.5 overflow-x-auto scrollbar-hide">
            {QUICK_QUESTIONS.map(q => (
              <button key={q} onClick={() => send(q)}
                className="shrink-0 text-xs bg-indigo-50 text-indigo-600 border border-indigo-200 px-2.5 py-1 rounded-lg hover:bg-indigo-100 font-medium transition-colors whitespace-nowrap">
                {q}
              </button>
            ))}
          </div>

          {/* Input */}
          <div className="px-3 py-2 bg-white border-t border-gray-100 flex gap-2">
            <input value={input} onChange={e => setInput(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && send(input)}
              placeholder="Ask anything..."
              className="flex-1 border border-gray-200 rounded-xl px-3 py-2 text-sm focus:outline-none focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100"/>
            <button onClick={() => send(input)}
              className="w-9 h-9 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl flex items-center justify-center transition-colors shrink-0 active:scale-90">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/>
              </svg>
            </button>
          </div>
        </div>
      )}

      {/* Floating buttons */}
      <div className="fixed bottom-4 right-4 sm:right-6 z-40 flex flex-col items-end gap-2">
        {/* Helpline button */}
        <button onClick={() => setShowHelp(true)}
          className="flex items-center gap-2 bg-red-500 hover:bg-red-600 text-white px-4 py-2.5 rounded-full shadow-lg font-semibold text-sm transition-all hover:shadow-red-200 hover:shadow-xl active:scale-95 btn-ripple">
          <span className="text-base">📞</span>
          <span className="hidden sm:inline">Helpline</span>
        </button>
        {/* Chat button */}
        <button onClick={() => setOpen(o => !o)}
          className={`w-14 h-14 rounded-full shadow-xl flex items-center justify-center text-2xl transition-all active:scale-90 btn-glow
            ${open ? 'bg-gray-700 hover:bg-gray-800' : 'bg-gradient-to-br from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700'}`}>
          {open ? '✕' : '💬'}
        </button>
      </div>
    </>
  )
}
