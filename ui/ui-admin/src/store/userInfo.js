import { defineStore } from 'pinia'

export const useUserInfoStore = defineStore('userInfo', {
  state: () => ({
    user: {}
  }),
  getters: {
    isAdmin: (state) => state.user && state.user.role === 1,
    displayName: (state) => state.user.realName || state.user.name || '未登录',
    roleName: (state) => (state.user && state.user.role === 1 ? '管理员' : '普通用户')
  },
  actions: {
    setUserInfo(user) {
      this.user = user || {}
    },
    removeUserInfo() {
      this.user = {}
    }
  },
  persist: true
})
