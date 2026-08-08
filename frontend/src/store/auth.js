import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('wxzj_token') || '',
    user: JSON.parse(localStorage.getItem('wxzj_user') || 'null')
  }),
  getters: {
    role: (s) => s.user?.role || '',
    isAdmin: (s) => s.user?.role === 'ADMIN',
    isProperty: (s) => s.user?.role === 'PROPERTY',
    isOwner: (s) => s.user?.role === 'OWNER',
    isCommittee: (s) => s.user?.role === 'COMMITTEE'
  },
  actions: {
    setLogin(data) {
      this.token = data.token
      this.user = { id: data.id, username: data.username, realName: data.realName, role: data.role, phone: data.phone }
      localStorage.setItem('wxzj_token', data.token)
      localStorage.setItem('wxzj_user', JSON.stringify(this.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('wxzj_token')
      localStorage.removeItem('wxzj_user')
    }
  }
})
