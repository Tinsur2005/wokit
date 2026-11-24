import request from '@/utils/request'

const dashboardApi = {
  get() {
    return request.get('/dashboard')
  }
}

export default dashboardApi
