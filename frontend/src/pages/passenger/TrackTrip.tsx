import { useEffect, useRef, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getLocation } from '../../api/location'

interface DriverLocation {
  latitude: number
  longitude: number
  updatedAt?: number
}

export default function TrackTrip() {
  const { tripId } = useParams()
  const navigate = useNavigate()
  const [location, setLocation] = useState<DriverLocation | null>(null)
  const [connected, setConnected] = useState(false)
  const [tripStarted, setTripStarted] = useState(false)
  const clientRef = useRef<Client | null>(null)
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const applyLocation = useCallback((data: any) => {
    // Handle both { lat, lng } and { latitude, longitude } shapes
    const lat = data.latitude ?? data.lat
    const lng = data.longitude ?? data.lng
    if (lat != null && lng != null) {
      setLocation({ latitude: lat, longitude: lng, updatedAt: Date.now() })
      setTripStarted(true)
    }
  }, [])

  // REST poll for initial / fallback location
  const pollLocation = useCallback(() => {
    getLocation(tripId!).then(data => {
      if (data?.active) applyLocation(data)
    }).catch(() => {})
  }, [tripId, applyLocation])

  useEffect(() => {
    // Initial fetch
    pollLocation()

    // WebSocket — subscribe to correct topic
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true)
        client.subscribe(`/topic/trip/${tripId}/location`, (msg) => {
          try {
            const data = JSON.parse(msg.body)
            applyLocation(data)
          } catch {}
        })
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
    })
    client.activate()
    clientRef.current = client

    // Fallback polling every 5s in case WebSocket drops
    pollRef.current = setInterval(pollLocation, 5000)

    return () => {
      client.deactivate()
      if (pollRef.current) clearInterval(pollRef.current)
    }
  }, [tripId, applyLocation, pollLocation])

  const mapsUrl = location
    ? `https://maps.google.com/?q=${location.latitude},${location.longitude}`
    : null

  const embedUrl = location
    ? `https://maps.google.com/maps?q=${location.latitude},${location.longitude}&z=15&output=embed`
    : null

  const secondsAgo = location?.updatedAt
    ? Math.round((Date.now() - location.updatedAt) / 1000)
    : null

  return (
    <div className="max-w-lg mx-auto p-4 sm:p-6">
      {/* Header */}
      <div className="flex items-center gap-3 mb-5">
        <button onClick={() => navigate(-1)}
          className="w-9 h-9 rounded-xl bg-white border border-gray-200 flex items-center justify-center hover:bg-gray-50 transition-colors shadow-sm">
          <svg className="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7"/>
          </svg>
        </button>
        <div>
          <h1 className="text-xl font-bold text-gray-800">Live Driver Tracking</h1>
          <p className="text-xs text-gray-400">Trip #{tripId?.slice(0,8)}...</p>
        </div>
        <div className="ml-auto flex items-center gap-1.5">
          <span className={`w-2.5 h-2.5 rounded-full ${connected ? 'bg-green-500 animate-pulse' : 'bg-gray-300'}`}/>
          <span className={`text-xs font-semibold ${connected ? 'text-green-600' : 'text-gray-400'}`}>
            {connected ? 'Live' : 'Connecting...'}
          </span>
        </div>
      </div>

      {/* Map embed */}
      <div className="rounded-2xl overflow-hidden border border-gray-200 shadow-md mb-4 bg-gray-100" style={{height: 300}}>
        {embedUrl ? (
          <iframe
            title="Driver Location"
            src={embedUrl}
            width="100%" height="300"
            style={{border: 0}}
            allowFullScreen
            loading="lazy"
          />
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-gray-400 gap-3">
            <div className="w-14 h-14 rounded-full border-4 border-indigo-100 border-t-indigo-500 animate-spin"/>
            <p className="font-semibold text-gray-500 text-sm">Waiting for driver to start...</p>
            <p className="text-xs text-gray-400">Map will appear once the driver begins the trip</p>
          </div>
        )}
      </div>

      {/* Status card */}
      {location ? (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 space-y-4 slide-up">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-green-100 rounded-xl flex items-center justify-center">
                <span className="text-xl">🚗</span>
              </div>
              <div>
                <p className="font-bold text-gray-800 text-sm">Driver is on the way</p>
                <p className="text-xs text-gray-400">
                  {secondsAgo != null && secondsAgo < 60
                    ? `Updated ${secondsAgo}s ago`
                    : 'Location received'}
                </p>
              </div>
            </div>
            <span className="w-2.5 h-2.5 rounded-full bg-green-500 animate-pulse"/>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="bg-gray-50 rounded-xl p-3">
              <p className="text-xs text-gray-400 mb-1">Latitude</p>
              <p className="font-bold text-gray-800 text-sm">{location.latitude.toFixed(5)}</p>
            </div>
            <div className="bg-gray-50 rounded-xl p-3">
              <p className="text-xs text-gray-400 mb-1">Longitude</p>
              <p className="font-bold text-gray-800 text-sm">{location.longitude.toFixed(5)}</p>
            </div>
          </div>

          <a href={mapsUrl!} target="_blank" rel="noreferrer"
            className="flex items-center justify-center gap-2 w-full bg-gradient-to-r from-indigo-600 to-indigo-500 text-white py-3 rounded-xl font-semibold text-sm hover:from-indigo-700 hover:to-indigo-600 transition-all hover:shadow-lg hover:shadow-indigo-200">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/>
            </svg>
            Open in Google Maps
          </a>

          <div className="flex items-center gap-2 bg-amber-50 border border-amber-200 rounded-xl p-3">
            <span>🛡️</span>
            <p className="text-xs text-amber-700 font-medium">Share this page with a trusted contact for safety</p>
          </div>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5">
          <div className="flex items-center gap-3 text-gray-500">
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${connected ? 'bg-indigo-50' : 'bg-gray-50'}`}>
              <svg className={`w-5 h-5 ${connected ? 'text-indigo-500' : 'text-gray-400'}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.141 0M1.394 9.393c5.857-5.857 15.355-5.857 21.213 0"/>
              </svg>
            </div>
            <div>
              <p className="font-semibold text-sm text-gray-700">
                {connected ? 'Connected — waiting for driver' : 'Connecting to live tracking...'}
              </p>
              <p className="text-xs text-gray-400">Location will appear automatically when driver starts</p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
