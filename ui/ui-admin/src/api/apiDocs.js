import request from '@/utils/request'

export default {
  spec() {
    return request.get('/api-docs/spec')
  },
  export() {
    return request.get('/api-docs/export', { responseType: 'blob' })
  }
}
