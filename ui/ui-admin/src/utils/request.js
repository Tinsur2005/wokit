import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useTokenStore } from '@/store/token'
import { useUserInfoStore } from '@/store/userInfo'

const baseURL = '/admin/api'
const request = axios.create({ baseURL })

// 标记是否正在跳转登录页，防止同一批 401 响应弹出多条提示、多次跳转
let isRedirectingToLogin = false

// 请求拦截器：附加 Token
request.interceptors.request.use(
  (config) => {
    const tokenStore = useTokenStore()
    if (tokenStore.token) {
      config.headers.Authorization = tokenStore.token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理 401 与网络异常，业务 code 交给页面处理
request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response && error.response.status === 401) {
      const tokenStore = useTokenStore()
      const userInfoStore = useUserInfoStore()
      tokenStore.removeToken()
      userInfoStore.removeUserInfo()
      if (!isRedirectingToLogin) {
        isRedirectingToLogin = true
        ElMessage.error('登录已失效，请重新登录')
        router.push('/login')
        setTimeout(() => {
          isRedirectingToLogin = false
        }, 2000)
      }
    } else {
      ElMessage.error('服务异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
