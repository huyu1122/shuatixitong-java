import api from './index'

/**
 * 后台管理接口（需 ADMIN 角色）
 */
export const adminApi = {
  dashboard: () => api.get('/admin/dashboard'),

  // 题目
  questions: (params) => api.get('/admin/questions', { params }),
  questionStats: () => api.get('/admin/questions/stats'),
  createQuestion: (data) => api.post('/admin/questions', data),
  updateQuestion: (id, data) => api.put(`/admin/questions/${id}`, data),
  deleteQuestion: (id) => api.delete(`/admin/questions/${id}`),
  batchDeleteQuestions: (ids) => api.post('/admin/questions/batch-delete', ids),

  // 用户
  users: (keyword) => api.get('/admin/users', { params: { keyword } }),
  setRole: (id, role) => api.put(`/admin/users/${id}/role`, { role }),
  unlock: (id) => api.post(`/admin/users/${id}/unlock`),
  resetPassword: (id, password) => api.post(`/admin/users/${id}/reset-password`, { password }),
  deleteUser: (id) => api.delete(`/admin/users/${id}`)
}

export default adminApi