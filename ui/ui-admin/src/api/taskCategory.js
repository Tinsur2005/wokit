import request from '@/utils/request'

const taskCategoryApi = {
  list() {
    return request.get('/task-categories')
  },
  add(data) {
    return request.post('/task-categories', data)
  },
  update(id, data) {
    return request.put(`/task-categories/${id}`, data)
  },
  remove(id) {
    return request.delete(`/task-categories/${id}`)
  }
}

export default taskCategoryApi
