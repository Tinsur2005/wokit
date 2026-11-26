import request from '@/utils/request'

export default {
  list(params) {
    return request.get('/api-keys', { params })
  },
  create(data) {
    return request.post('/api-keys', data)
  },
  changeStatus(id, status) {
    return request.put(`/api-keys/${id}/status`, null, { params: { status } })
  },
  reset(id) {
    return request.post(`/api-keys/${id}/reset`)
  },
  remove(id) {
    return request.delete(`/api-keys/${id}`)
  }
}
