import { useUserInfoStore } from '@/store/userInfo'

/**
 * 按钮权限判断：与智慧养老系统一致，读取 userInfo 接口返回的 btnList
 */
export default function hasBtnPermission(value) {
  if (!value) return false
  return useUserInfoStore().hasBtnPermission(value)
}
