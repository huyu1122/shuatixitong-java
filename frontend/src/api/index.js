import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'

/**
 * axios 实例：统一处理 token、错误提示与 401 跳转
 */
const api = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：自动带上 token
api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

// 响应拦截：剥离统一响应外壳，处理业务错误
api.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body?.data !== undefined ? body.data : body
  },
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.message || err.message || '网络错误'
    if (status === 401) {
      const auth = useAuthStore()
      auth.clear()
      if (router.currentRoute.value.name !== 'login') {
        router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
      }
    }
    return Promise.reject(new Error(msg))
  }
)

/* ---------- 认证 ---------- */
export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  logout: () => api.post('/auth/logout'),
  me: () => api.get('/auth/me')
}

/* ---------- 题库 ---------- */
export const metaApi = {
  subjects: () => api.get('/meta/subjects'),
  chapters: (subject) => api.get('/meta/chapters', { params: { subject } }),
  questions: (params) => api.get('/meta/questions', { params }),
  bySubject: (subject) => api.get('/meta/questions/by-subject', { params: { subject } }),
  random: (limit, subject) => api.get('/meta/questions/random', { params: { limit, subject } }),
  batch: (ids) => api.post('/meta/questions/batch', ids)
}

/* ---------- 练习 ---------- */
export const practiceApi = {
  submit: (questionId, answer) => api.post('/practice/submit', { questionId, answer }),
  toggleFavorite: (questionId) => api.post(`/practice/favorite/${questionId}`),
  wrong: () => api.get('/practice/wrong'),
  favorites: () => api.get('/practice/favorites'),
  removeMark: (questionId, type) => api.delete(`/practice/mark/${questionId}`, { params: { type } }),
  statistics: () => api.get('/practice/statistics'),
  saveExam: (data) => api.post('/practice/exam', data),
  examHistory: () => api.get('/practice/exam/history')
}

export default api