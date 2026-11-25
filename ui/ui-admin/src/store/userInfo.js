import { defineStore } from 'pinia'

const flattenRouter = (nodes, result = []) => {
  ;(nodes || []).forEach((node) => {
    if (node.type === 1 && node.path) {
      result.push(node.path)
    }
    if (node.children && node.children.length) {
      flattenRouter(node.children, result)
    }
  })
  return result
}

export const useUserInfoStore = defineStore('userInfo', {
  state: () => ({
    user: {},
    routerList: [],
    btnList: []
  }),
  getters: {
    isAdmin: (state) => state.user && state.user.role === 1,
    displayName: (state) => state.user.realName || state.user.name || '未登录',
    roleName: (state) => state.user.roleName || (state.user.role === 1 ? '管理员' : '普通用户'),
    routerPaths: (state) => flattenRouter(state.routerList)
  },
  actions: {
    setUserInfo(payload) {
      if (payload && payload.user) {
        this.user = payload.user || {}
        this.routerList = payload.routerList || []
        this.btnList = payload.btnList || []
      } else {
        this.user = payload || {}
        this.routerList = []
        this.btnList = []
      }
    },
    hasBtnPermission(value) {
      return this.btnList.includes(value)
    },
    hasRoutePermission(path) {
      return this.routerPaths.includes(path)
    },
    removeUserInfo() {
      this.user = {}
      this.routerList = []
      this.btnList = []
    }
  },
  persist: true
})
