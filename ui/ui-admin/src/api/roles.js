import request from '@/utils/request'

export default {
  listAll() {
    return request.get('/roles/list')
  },
  page(params) {
    return request.get('/roles', { params })
  },
  getById(id) {
    return request.get(`/roles/${id}`)
  },
  add(data) {
    return request.post('/roles', data)
  },
  update(id, data) {
    return request.put(`/roles/${id}`, data)
  },
  remove(id) {
    return request.delete(`/roles/${id}`)
  },
  selectPermissionById(id) {
    return request.get(`/roles/selectPermissionById/${id}`)
  },
  updatePermission(id, permissionIds) {
    return request.put(`/roles/updatePermission/${id}`, permissionIds)
  }
}
