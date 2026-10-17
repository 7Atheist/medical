/**
 * Axios 实例：自动附加登录 token，统一处理未授权跳转。
 */
import axios from 'axios'
import router from '@/router'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截：附加 Authorization（后台接口用管理令牌，其余用登录令牌）
http.interceptors.request.use((config) => {
  const isAdmin = String(config.url || '').startsWith('/admin')
  const token = isAdmin
    ? (localStorage.getItem('adminToken') || '')
    : (localStorage.getItem('token') || '')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

// 响应拦截：统一解包 code/message
http.interceptors.response.use(
  (resp) => {
    const res = resp.data
    // 后端统一返回 {code, message, data}
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 0) {
        return res.data
      }
      // 未授权
      if (res.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (err) => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    return Promise.reject(err)
  }
)

export default http
