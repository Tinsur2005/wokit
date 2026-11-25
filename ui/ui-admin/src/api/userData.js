import request from '@/utils/request'

export default {
  exportData() {
    return request.get('/user-data/export', { responseType: 'blob' })
  },
  restoreData(file) {
    return request.post('/user-data/restore', file, {
      headers: { 'Content-Type': 'application/octet-stream' }
    })
  }
}
