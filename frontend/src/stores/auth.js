import { defineStore } from 'pinia'
import { authApi } from '../api'

const TOKEN_KEY = 'quiz_token'
const USER_KEY = 'quiz_user'

/**
 * 认证状态：token 与用户信息持久化到 localStorage
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    nickname: (state) => state.user?.nickname || state.user?.username || '同学'
  },

  actions: {
    setSession({ token, user }) {
      this.token = token
      this.user = user
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    },

    clear() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },

    async login(username, password) {
      const data = await authApi.login({ username, password })
      this.setSession(data)
      return data
    },

    async register(username, password, nickname) {
      const data = await authApi.register({ username, password, nickname })
      this.setSession(data)
      return data
    },

    async logout() {
      try {
        await authApi.logout()
      } catch {
        // 忽略登出失败，本地状态照常清理
      }
      this.clear()
    },

    /** 刷新页面时用 token 换回最新用户信息 */
    async refreshMe() {
      if (!this.token) return
      try {
        const user = await authApi.me()
        this.user = user
        localStorage.setItem(USER_KEY, JSON.stringify(user))
      } catch {
        this.clear()
      }
    }
  }
})