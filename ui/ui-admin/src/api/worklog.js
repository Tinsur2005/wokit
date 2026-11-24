import request from '@/utils/request'

export default {
  list(params) {
    return request.get('/worklogs', { params })
  },
  stat(month) {
    return request.get('/worklogs/stat', { params: { month } })
  },
  add(data) {
    return request.post('/worklogs', data)
  },
  update(id, data) {
    return request.put(`/worklogs/${id}`, data)
  },
  remove(id) {
    return request.delete(`/worklogs/${id}`)
  }
}
