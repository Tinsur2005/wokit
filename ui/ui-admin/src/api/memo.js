import request from '@/utils/request'

export default {
  list(params) {
    return request.get('/memos', { params })
  },
  add(data) {
    return request.post('/memos', data)
  },
  update(id, data) {
    return request.put(`/memos/${id}`, data)
  },
  togglePin(id) {
    return request.put(`/memos/pin/${id}`)
  },
  remove(id) {
    return request.delete(`/memos/${id}`)
  }
}
