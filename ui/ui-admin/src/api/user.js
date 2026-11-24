import request from '@/utils/request'

export default {
  login(data) {
    return request.post('/users/login', data)
  },
  userInfo() {
    return request.get('/users/userInfo')
  },
  list(params) {
    return request.get('/users', { params })
  },
  getById(id) {
    return request.get(`/users/${id}`)
  },
  add(data) {
    return request.post('/users', data)
  },
  update(id, data) {
    return request.put(`/users/${id}`, data)
  },
  changeStatus(id, status) {
    return request.put(`/users/status/${id}`, null, { params: { status } })
  },
  remove(id) {
    return request.delete(`/users/${id}`)
  },
  updatePassword(data) {
    return request.put('/users/password', data)
  }
}
