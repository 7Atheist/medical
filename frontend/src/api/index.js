/**
 * 后端 API 封装。
 */
import http from './http'
import axios from 'axios'

// AI 服务独立实例（FastAPI 8000，经 Vite 代理 /ai-api 转发）
// 超时设 5 秒：AI 包装是增强体验，超时就降级用原题，不能让老人干等
const aiHttp = axios.create({ baseURL: '/ai-api', timeout: 5000 })
aiHttp.interceptors.response.use(r => r.data?.data ?? r.data, e => Promise.reject(e))

// ---------------- AI：题目生活化包装（把医学题干改成聊天开场白） ----------------
export const wrapQuestion = (questionText, options, profile) =>
  aiHttp.post('/api/ai/wrap', { question_text: questionText, options, profile })

// ---------------- 认证 ----------------
export const login = (name, idCard) => http.post('/auth/login', { name, idCard })

// ---------------- 评估 ----------------
export const createTask = (inputMode) => http.post('/assess/create', { inputMode })
export const listDrafts = () => http.get('/assess/drafts')
export const listHistory = () => http.get('/assess/history')
export const getQuestion = (taskId) => http.get(`/assess/${taskId}/question`)
export const submitAnswer = (taskId, payload) => http.post(`/assess/${taskId}/answer`, payload)
export const deleteTask = (taskId) => http.delete(`/assess/${taskId}`)
export const submitAudio = (taskId, questionId, file) => {
  const fd = new FormData()
  fd.append('questionId', questionId)
  fd.append('file', file)
  return http.post(`/assess/${taskId}/audio`, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}

// ---------------- 报告 ----------------
export const getReportByTask = (taskId) => http.get(`/report/task/${taskId}`)
export const listReports = () => http.get('/report/list')
export const reportPdfUrl = (reportId) => `/api/report/${reportId}/pdf?token=${localStorage.getItem('token') || ''}`

// ---------------- 后台管理 ----------------
export const adminLogin = (adminToken) => localStorage.setItem('adminToken', adminToken)
export const adminStats = () => http.get('/admin/stats')
export const adminUsers = (name) => http.get('/admin/users', { params: { name } })
export const adminScales = () => http.get('/admin/scales')
export const adminSaveScale = (scale) => http.post('/admin/scales', scale)
export const adminQuestions = (scaleId) => http.get(`/admin/scales/${scaleId}/questions`)
export const adminSaveQuestion = (q) => http.post('/admin/questions', q)
export const adminDictionary = () => http.get('/admin/dictionary')
export const adminSaveDictionary = (d) => http.post('/admin/dictionary', d)
export const adminDialogs = () => http.get('/admin/dialog')
export const adminSaveDialog = (d) => http.post('/admin/dialog', d)
export const adminParams = () => http.get('/admin/params')
export const adminUpdateParam = (key, value) => http.put(`/admin/params/${key}`, { value })
export const adminLogs = () => http.get('/admin/logs')
