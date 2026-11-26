import { defineStore } from 'pinia'
import taskCategoryApi from '@/api/taskCategory'

let inflight = null

export const useTaskCategoryStore = defineStore('taskCategory', {
  state: () => ({
    categories: [],
    loaded: false
  }),
  getters: {
    categoryName: (state) => (id) => {
      if (id == null) return ''
      const category = state.categories.find((item) => Number(item.id) === Number(id))
      return category ? category.name : ''
    },
    categoryColor: (state) => (id) => {
      if (id == null) return '#98a2b3'
      const category = state.categories.find((item) => Number(item.id) === Number(id))
      return category?.color || '#98a2b3'
    }
  },
  actions: {
    async load(force = false) {
      if (this.loaded && !force) return
      if (inflight) return inflight
      inflight = taskCategoryApi.list()
        .then((result) => {
          if (result.code === 1) {
            this.categories = result.data || []
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
