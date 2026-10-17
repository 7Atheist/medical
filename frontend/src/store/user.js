/**
 * 用户状态：token 与会话画像（姓名/年龄/性别/称谓），持久化到 localStorage。
 */
import { defineStore } from 'pinia'
import { login as apiLogin } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    profile: JSON.parse(localStorage.getItem('profile') || 'null')
  }),
  getters: {
    isLogin: (s) => !!s.token
  },
  actions: {
    async login(name, idCard) {
      const data = await apiLogin(name, idCard)
      this.token = data.token
      this.profile = data.profile
      localStorage.setItem('token', data.token)
      localStorage.setItem('profile', JSON.stringify(data.profile))
      return data
    },
    logout() {
      this.token = ''
      this.profile = null
      localStorage.removeItem('token')
      localStorage.removeItem('profile')
    }
  }
})
