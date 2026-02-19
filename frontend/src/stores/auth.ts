import { defineStore } from 'pinia'
import { api } from '../services/api'

type LoginPayload = {
  email: string
  password: string
}

type AuthTokenResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('accessToken') ?? '',
    refreshToken: localStorage.getItem('refreshToken') ?? '',
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken),
  },

  actions: {
    persistTokens(accessToken: string, refreshToken: string) {
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      localStorage.setItem('accessToken', this.accessToken)
      localStorage.setItem('refreshToken', this.refreshToken)
    },

    async login(payload: LoginPayload) {
      const response = await api.post<AuthTokenResponse>('/api/v1/auth/login', payload)
      this.persistTokens(response.data.accessToken, response.data.refreshToken)
    },

    async register(payload: LoginPayload) {
      const response = await api.post<AuthTokenResponse>('/api/v1/auth/register', payload)
      this.persistTokens(response.data.accessToken, response.data.refreshToken)
    },

    logout() {
      this.accessToken = ''
      this.refreshToken = ''
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
    },
  },
})
