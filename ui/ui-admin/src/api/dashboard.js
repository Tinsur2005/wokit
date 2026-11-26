import request from '@/utils/request'

const dashboardApi = {
  get(params) {
    return request.get('/dashboard', { params })
  }
}

export default dashboardApi
