import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/'

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

let refreshPromise: Promise<string> | null = null

const clearAuthAndRedirect = () => {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

const refreshAccessToken = async () => {
  if (refreshPromise) return refreshPromise

  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) {
    clearAuthAndRedirect()
    throw new Error('Refresh token ausente')
  }

  const refreshUrl =
    API_BASE_URL.endsWith('/') ? `${API_BASE_URL}api/v1/auth/refresh` : `${API_BASE_URL}/api/v1/auth/refresh`

  refreshPromise = axios
    .post(refreshUrl, { refreshToken })
    .then((response) => {
      const newAccessToken = response.data.accessToken as string
      const newRefreshToken = response.data.refreshToken as string

      if (!newAccessToken || newAccessToken === 'undefined' || newAccessToken === 'null') {
        throw new Error('Access token inválido no refresh')
      }
      if (!newRefreshToken || newRefreshToken === 'undefined' || newRefreshToken === 'null') {
        throw new Error('Refresh token inválido no refresh')
      }

      localStorage.setItem('accessToken', newAccessToken)
      localStorage.setItem('refreshToken', newRefreshToken)

      return newAccessToken
    })
    .finally(() => {
      refreshPromise = null
    })

  return refreshPromise
}

api.interceptors.request.use((config) => {
  if (config.data instanceof FormData && config.headers) {
    delete config.headers['Content-Type']
  }

  const token = localStorage.getItem('accessToken')
  if (token && token !== 'undefined' && token !== 'null') {
    config.headers.Authorization = `Bearer ${token}`
  } else if (config.headers?.Authorization) {
    delete config.headers.Authorization
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as (typeof error.config & { _retry?: boolean })
    const status = error.response?.status
    const requestUrl: string = originalRequest?.url ?? ''

    const isAuthEndpoint =
      requestUrl.includes('/api/v1/auth/login') ||
      requestUrl.includes('/api/v1/auth/register') ||
      requestUrl.includes('/api/v1/auth/refresh')

    if (status === 401 && originalRequest && !originalRequest._retry && !isAuthEndpoint) {
      originalRequest._retry = true

      try {
        const newAccessToken = await refreshAccessToken()

        originalRequest.headers = originalRequest.headers ?? {}
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`

        return api(originalRequest)
      } catch (refreshError: unknown) {
        clearAuthAndRedirect()
        return Promise.reject(refreshError)
      }
    }

    return Promise.reject(error)
  },
)
