import { defineStore } from 'pinia'
import systemApi from '@/api/system'

export const useSystemConfigStore = defineStore('systemConfig', {
  state: () => ({
    systemName: 'Wokit',
    systemSubName: '工作助手',
    browserTitle: 'Wokit 工作助手',
    loginHint: '使用平台账号进入工作台',
    taskDefaultStartTime: '09:00',
    taskDefaultEndTime: '18:30',
    loaded: false
  }),
  actions: {
    async load() {
      try {
        const result = await systemApi.publicInfo()
        if (result.code === 1 && result.data) {
          this.applyConfig(result.data)
          this.loaded = true
        }
      } catch (error) {
        // 读取失败时保留默认品牌，不阻断登录与工作台
      }
    },
    applyConfig(config) {
      this.systemName = config.systemName || 'Wokit'
      this.systemSubName = config.systemSubName || '工作助手'
      this.browserTitle = config.browserTitle || `${this.systemName} ${this.systemSubName}`.trim()
      this.loginHint = config.loginHint || '使用平台账号进入工作台'
      this.taskDefaultStartTime = /^([01]\d|2[0-3]):[0-5]\d$/.test(config.taskDefaultStartTime || '')
        ? config.taskDefaultStartTime
        : '09:00'
      this.taskDefaultEndTime = /^([01]\d|2[0-3]):[0-5]\d$/.test(config.taskDefaultEndTime || '')
        ? config.taskDefaultEndTime
        : '18:30'
    },
    applyDocumentTitle(pageTitle) {
      document.title = pageTitle ? `${pageTitle} · ${this.browserTitle}` : this.browserTitle
    }
  },
  persist: true
})
