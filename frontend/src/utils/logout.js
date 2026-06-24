import { logout } from '@/api/auth/auth.js'
import router from '@/router'

export async function handleLogout(redirectPath = '/login') {
  try {
    const refreshToken = localStorage.getItem('refresh_token')
    if (refreshToken) {
      await logout(refreshToken)
    }
  } catch {
    // ignore
  }
  
  localStorage.removeItem('access_token')
  localStorage.removeItem('refresh_token')
  localStorage.removeItem('rememberedUsername')
  localStorage.removeItem('username')
  localStorage.removeItem('userInfo')
  
  router.push(redirectPath)
}
