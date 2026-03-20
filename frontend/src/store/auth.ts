export type Role = 'PASSENGER' | 'DRIVER' | 'BOTH' | 'ADMIN'

export interface AuthUser {
  id: number
  email: string
  name: string
  role: Role
}

export function getToken(): string | null {
  return localStorage.getItem('token')
}

export function getUser(): AuthUser | null {
  const raw = localStorage.getItem('user')
  return raw ? JSON.parse(raw) : null
}

export function setAuth(token: string, user: AuthUser) {
  localStorage.setItem('token', token)
  localStorage.setItem('user', JSON.stringify(user))
}

export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

export function isLoggedIn(): boolean {
  return !!getToken()
}

export function canDrive(): boolean {
  const r = getUser()?.role
  return r === 'DRIVER' || r === 'BOTH'
}

export function canRide(): boolean {
  const r = getUser()?.role
  return r === 'PASSENGER' || r === 'BOTH'
}
