import { defineStore } from 'pinia'
import projectApi from '@/api/project'

let inflight = null

export const useProjectStore = defineStore('project', {
  state: () => ({
    projects: [],
    categories: [],
    loaded: false
  }),
  getters: {
    projectName: (state) => (id) => {
      if (id === null || id === undefined) return ''
      const project = state.projects.find((item) => Number(item.id) === Number(id))
      return project ? project.name : ''
    },
    projectCategoryName: (state) => (id) => {
      if (id === null || id === undefined) return ''
      const project = state.projects.find((item) => Number(item.id) === Number(id))
      return project?.categoryName || ''
    }
  },
  actions: {
    async load(force = false) {
      if (this.loaded && !force) return
      if (inflight) return inflight
      inflight = Promise.all([projectApi.list(), projectApi.categories()])
        .then(([projectResult, categoryResult]) => {
          if (projectResult.code === 1) {
            this.projects = projectResult.data || []
          }
          if (categoryResult.code === 1) {
            this.categories = categoryResult.data || []
          }
          this.loaded = true
        })
        .finally(() => {
          inflight = null
        })
      return inflight
    },
    refresh() {
      return this.load(true)
    }
  }
})
