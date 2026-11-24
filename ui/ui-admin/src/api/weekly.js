import request from '@/utils/request'

export default {
  list(weekStart) {
    return request.get('/weeklies', { params: { weekStart } })
  },
  add(data) {
    return request.post('/weeklies', data)
  },
  update(id, data) {
    return request.put(`/weeklies/${id}`, data)
  },
  remove(id) {
    return request.delete(`/weeklies/${id}`)
  }
}
