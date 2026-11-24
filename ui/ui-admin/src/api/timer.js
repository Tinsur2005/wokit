import request from '@/utils/request'

const timerApi = {
  list() {
    return request.get('/timers')
  },
  add(data) {
    return request.post('/timers', data)
  },
  update(id, data) {
    return request.put(`/timers/${id}`, data)
  },
  remove(id) {
    return request.delete(`/timers/${id}`)
  }
}

export default timerApi
