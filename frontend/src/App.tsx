import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import ChatBot from './components/ChatBot'

import Login from './pages/auth/Login'
import Register from './pages/auth/Register'
import ForgotPassword from './pages/auth/ForgotPassword'
import ResetPassword from './pages/auth/ResetPassword'

import SearchTrips from './pages/passenger/SearchTrips'
import BookTrip from './pages/passenger/BookTrip'
import MyBookings from './pages/passenger/MyBookings'
import TrackTrip from './pages/passenger/TrackTrip'
import Transactions from './pages/passenger/Transactions'

import PostTrip from './pages/driver/PostTrip'
import MyTrips from './pages/driver/MyTrips'
import ManageBookings from './pages/driver/ManageBookings'
import ActiveTrip from './pages/driver/ActiveTrip'
import MyVehicle from './pages/driver/MyVehicle'

import Dashboard from './pages/admin/Dashboard'
import Users from './pages/admin/Users'
import AdminTrips from './pages/admin/Trips'
import Disputes from './pages/admin/Disputes'
import Config from './pages/admin/Config'
import Profile from './pages/profile/Profile'

import { isLoggedIn, getUser } from './store/auth'

function Home() {
  if (!isLoggedIn()) return <Navigate to="/login" replace />
  const user = getUser()
  if (user?.role === 'ADMIN') return <Navigate to="/admin" replace />
  if (user?.role === 'DRIVER') return <Navigate to="/my-trips" replace />
  // PASSENGER or BOTH → go to search
  return <Navigate to="/search" replace />
}

function Layout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <Navbar />
      {children}
    </>
  )
}

// BOTH role can access both driver and passenger routes
const DRIVER_ROLES = ['DRIVER', 'BOTH'] as const
const PASSENGER_ROLES = ['PASSENGER', 'BOTH'] as const

export default function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen app-bg">
        {/* Animated background blobs */}
        <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
          <div className="blob blob-1"/>
          <div className="blob blob-2"/>
          <div className="blob blob-3"/>
          <div className="blob blob-4"/>
        </div>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/" element={<Layout><Home /></Layout>} />

          {/* Passenger */}
          <Route path="/search" element={<Layout><ProtectedRoute roles={[...PASSENGER_ROLES]}><SearchTrips /></ProtectedRoute></Layout>} />
          <Route path="/book/:tripId" element={<Layout><ProtectedRoute roles={[...PASSENGER_ROLES]}><BookTrip /></ProtectedRoute></Layout>} />
          <Route path="/my-bookings" element={<Layout><ProtectedRoute roles={[...PASSENGER_ROLES]}><MyBookings /></ProtectedRoute></Layout>} />
          <Route path="/track/:tripId" element={<Layout><ProtectedRoute roles={[...PASSENGER_ROLES]}><TrackTrip /></ProtectedRoute></Layout>} />
          <Route path="/transactions" element={<Layout><ProtectedRoute roles={[...PASSENGER_ROLES]}><Transactions /></ProtectedRoute></Layout>} />

          {/* Driver */}
          <Route path="/post-trip" element={<Layout><ProtectedRoute roles={[...DRIVER_ROLES]}><PostTrip /></ProtectedRoute></Layout>} />
          <Route path="/my-trips" element={<Layout><ProtectedRoute roles={[...DRIVER_ROLES]}><MyTrips /></ProtectedRoute></Layout>} />
          <Route path="/my-vehicle" element={<Layout><ProtectedRoute roles={[...DRIVER_ROLES]}><MyVehicle /></ProtectedRoute></Layout>} />
          <Route path="/manage-bookings/:tripId" element={<Layout><ProtectedRoute roles={[...DRIVER_ROLES]}><ManageBookings /></ProtectedRoute></Layout>} />
          <Route path="/active-trip/:tripId" element={<Layout><ProtectedRoute roles={[...DRIVER_ROLES]}><ActiveTrip /></ProtectedRoute></Layout>} />

          {/* Admin */}
          <Route path="/admin" element={<Layout><ProtectedRoute roles={['ADMIN']}><Dashboard /></ProtectedRoute></Layout>} />
          <Route path="/admin/users" element={<Layout><ProtectedRoute roles={['ADMIN']}><Users /></ProtectedRoute></Layout>} />
          <Route path="/admin/trips" element={<Layout><ProtectedRoute roles={['ADMIN']}><AdminTrips /></ProtectedRoute></Layout>} />
          <Route path="/admin/disputes" element={<Layout><ProtectedRoute roles={['ADMIN']}><Disputes /></ProtectedRoute></Layout>} />
          <Route path="/admin/config" element={<Layout><ProtectedRoute roles={['ADMIN']}><Config /></ProtectedRoute></Layout>} />

          {/* Profile — all logged-in users */}
          <Route path="/profile" element={<Layout><ProtectedRoute roles={['PASSENGER','DRIVER','BOTH','ADMIN']}><Profile /></ProtectedRoute></Layout>} />
        </Routes>
        <ChatBot />
      </div>
    </BrowserRouter>
  )
}
