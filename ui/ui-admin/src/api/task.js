import request from '@/utils/request'

export default {
  list(params) {
    return request.get('/tasks', { params })
  },
  listByDate(date) {
    return request.get('/tasks/date', { params: { date } })
  },
  calendar(month) {
    return request.get('/tasks/calendar', { params: { month } })
  },
  stat() {
    return request.get('/tasks/stat')
  },
  getById(id) {
    return request.get(`/tasks/${id}`)
  },
  add(data) {
    return request.post('/tasks', data)
  },
  update(id, data) {
    return request.put(`/tasks/${id}`, data)
  },
  changeStatus(id, status) {
    return request.put(`/tasks/status/${id}`, null, { params: { status } })
  },
  remove(id) {
    return request.delete(`/tasks/${id}`)
  }
}
