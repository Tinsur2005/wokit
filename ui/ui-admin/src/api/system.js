import request from '@/utils/request'

export default {
  publicInfo() {
    return request.get('/system/public')
  },
  getConfig() {
    return request.get('/system/config')
  },
  updateConfig(data) {
    return request.put('/system/config', data)
  }
}
