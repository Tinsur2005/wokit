import request from '@/utils/request'

export default {
  tree() {
    return request.get('/permissions/tree')
  },
  getById(id) {
    return request.get(`/permissions/${id}`)
  },
  add(data) {
    return request.post('/permissions', data)
  },
  update(id, data) {
    return request.put(`/permissions/${id}`, data)
  },
  remove(id) {
    return request.delete(`/permissions/${id}`)
  }
}
