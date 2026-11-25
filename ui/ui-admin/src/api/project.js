import request from '@/utils/request'

const projectApi = {
  list(params) {
    return request.get('/projects', { params })
  },
  add(data) {
    return request.post('/projects', data)
  },
  update(id, data) {
    return request.put(`/projects/${id}`, data)
  },
  remove(id) {
    return request.delete(`/projects/${id}`)
  },
  categories() {
    return request.get('/project-categories')
  },
  addCategory(data) {
    return request.post('/project-categories', data)
  },
  updateCategory(id, data) {
    return request.put(`/project-categories/${id}`, data)
  },
  removeCategory(id) {
    return request.delete(`/project-categories/${id}`)
  }
}

export default projectApi
