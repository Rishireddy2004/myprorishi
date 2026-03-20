import api from './client'
import { setAuth, clearAuth, AuthUser } from '../store/auth'

export async function login(email: string, password: string) {
  const res = await api.post('/auth/login', { email, password })
  const { token, id, name, role } = res.data
  setAuth(token, { id, email, name, role } as AuthUser)
  return res.data
}

export async function register(data: {
  fullName: string
  email: string
  password: string
  role: string
  phone?: string
}) {
  const res = await api.post('/auth/register', data)
  return res.data
}

export async function getMyProfile() {
  const res = await api.get('/users/me')
  return res.data
}

export async function getPointsHistory() {
  const res = await api.get('/users/me/points-history')
  return res.data
}

export async function getTrustStats() {
  const res = await api.get('/users/me/trust-stats')
  return res.data
}

export async function updateMyProfile(data: { fullName?: string; phone?: string; photoUrl?: string }) {
  const res = await api.patch('/users/me', data)
  return res.data
}

export function logout() {
  clearAuth()
  window.location.href = '/login'
}
