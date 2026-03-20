import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { updateLocation } from '../../api/location'
import { updateTripStatus } from '../../api/trips'

export default function ActiveTrip() {
  const { tripId } = useParams<{ tripId: string }>()
  const navigate = useNavigate()
  const [status, setStatus] = useState('OPEN')
  const [tracking, setTracking] = useState(false)
  const [error, setError] = useState('')
  const [currentPos, setCurrentPos] = useState<{ lat: number; lng: number } | null>(null)
  const [updateCount, setUpdateCount] = useState(0)
  const watchRef = useRef<number | null>(null)
  const lastPosRef = useRef<{ lat: number; lng: number } | null>(null)

  const startTracking = async () => {
    setError('')
    if (!navigator.geolocation) {
      setError('Geolocation is not supported by your browser.')
      return
    }
    try {
      await updateTripStatus(tripId!, 'IN_PROGRESS')
      setStatus('IN_PROGRESS')
      setTracking(true)
      watchRef.current = navigator.geolocation.watchPosition(
        (pos) => {
          const loc = { lat: pos.coords.latitude, lng: pos.coords.longitude }
          lastPosRef.current = loc
          setCurrentPos(loc)
          setUpdateCount(c => c + 1)
          updateLocation(tripId!, pos.coords.latitude, pos.coords.longitude).catch(() => {})
        },
        (err) => {
          if (err.code === err.PERMISSION_DENIED) {
            setError('Location access denied. Please enable location permissions in your browser.')
          } else {
            setError('Unable to get location. Make sure GPS is enabled.')
          }
        },
        { enableHighAccuracy: true, maximumAge: 3000, timeout: 10000 }
      )
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to start trip')
    }
  }

  const completeTrip = async () => {
    if (watchRef.current !== null) navigator.geolocation.clearWatch(watchRef.current)
    setTracking(false)
    setError('')
    try {
      const pos = lastPosRef.current
      await updateTripStatus(tripId!, 'COMPLETED', pos?.lat, pos?.lng)
      navigate('/my-trips')
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to complete trip.')
    }
  }

  useEffect(() => {
    return () => { if (watchRef.current !== null) navigator.geolocation.clearWatch(watchRef.current) }
  }, [])

  const embedUrl = currentPos
    ? `https://maps.google.com/maps?q=${currentPos.lat},${currentPos.lng}&z=15&output=embed`
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
          <h1 className="text-xl font-bold text-gray-800">Active Trip</h1>
          <p className="text-xs text-gray-400">Trip #{tripId?.slice(0,8)}...</p>
        </div>
        <div className="ml-auto">
          <span className={`inline-flex items-center gap-1.5 text-xs font-bold px-3 py-1.5 rounded-full ${
            tracking ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
          }`}>
            <span className={`w-2 h-2 rounded-full ${tracking ? 'bg-green-500 animate-pulse' : 'bg-gray-400'}`}/>
            {tracking ? 'LIVE' : status}
          </span>
        </div>
      </div>

      {/* Live map */}
      <div className="rounded-2xl overflow-hidden border border-gray-200 shadow-md mb-4 bg-gray-100" style={{height: 280}}>
        {embedUrl ? (
          <iframe
            key={Math.floor(updateCount / 3)} // refresh map every 3 updates
            title="Your Location"
            src={embedUrl}
            width="100%" height="280"
            style={{border: 0}}
            allowFullScreen
            loading="lazy"
          />
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-gray-400 gap-3">
            <div className="text-5xl">🗺️</div>
            <p className="text-sm font-medium text-gray-500">Map will show here once you start</p>
          </div>
        )}
      </div>

      {/* Live broadcast banner */}
      {tracking && (
        <div className="flex items-center gap-3 bg-green-50 border border-green-200 rounded-2xl p-4 mb-4 slide-up">
          <span className="w-3 h-3 rounded-full bg-green-500 animate-pulse shrink-0"/>
          <div className="flex-1">
            <p className="text-green-800 font-bold text-sm">Broadcasting live location</p>
            <p className="text-green-600 text-xs">All passengers can see your position · {updateCount} updates sent</p>
          </div>
          {currentPos && (
            <a href={`https://maps.google.com/?q=${currentPos.lat},${currentPos.lng}`}
              target="_blank" rel="noreferrer"
              className="text-xs bg-green-200 text-green-800 px-2.5 py-1.5 rounded-lg font-semibold hover:bg-green-300 transition-colors shrink-0">
              Maps ↗
            </a>
          )}
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-xl p-3 mb-4 flex items-center gap-2">
          <span>⚠️</span>{error}
        </div>
      )}

      {/* Action card */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 space-y-4">
        {/* Coords display */}
        {currentPos && (
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-gray-50 rounded-xl p-3">
              <p className="text-xs text-gray-400 mb-1">Latitude</p>
              <p className="font-bold text-gray-800 text-sm">{currentPos.lat.toFixed(5)}</p>
            </div>
            <div className="bg-gray-50 rounded-xl p-3">
              <p className="text-xs text-gray-400 mb-1">Longitude</p>
              <p className="font-bold text-gray-800 text-sm">{currentPos.lng.toFixed(5)}</p>
            </div>
          </div>
        )}

        {!tracking ? (
          <button onClick={startTracking}
            className="w-full bg-gradient-to-r from-green-600 to-green-500 text-white py-3.5 rounded-xl hover:from-green-700 hover:to-green-600 font-bold text-sm transition-all hover:shadow-lg hover:shadow-green-200 flex items-center justify-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/>
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            Start Trip & Share Live Location
          </button>
        ) : (
          <button onClick={completeTrip}
            className="w-full bg-gradient-to-r from-indigo-600 to-indigo-500 text-white py-3.5 rounded-xl hover:from-indigo-700 hover:to-indigo-600 font-bold text-sm transition-all hover:shadow-lg hover:shadow-indigo-200 flex items-center justify-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            Complete Trip
          </button>
        )}

        <p className="text-xs text-center text-gray-400">
          {tracking
            ? 'Your GPS location is being shared with all confirmed passengers in real-time'
            : 'Pressing Start will mark the trip as In Progress and begin sharing your location'}
        </p>
      </div>
    </div>
  )
}
