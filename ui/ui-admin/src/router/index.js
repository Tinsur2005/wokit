import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import Login from '@/views/Login.vue'
import Layout from '@/views/Layout.vue'
import Task from '@/views/Task.vue'
import Memo from '@/views/Memo.vue'
import WorkLog from '@/views/WorkLog.vue'
import Weekly from '@/views/Weekly.vue'
import Timer from '@/views/Timer.vue'
import { useTokenStore } from '@/store/token'
import { useSystemConfigStore } from '@/store/systemConfig'
import { useUserInfoStore } from '@/store/userInfo'

const Dashboard = () => import('@/views/Dashboard.vue')
const Project = () => import('@/views/Project.vue')
const SettingsLayout = () => import('@/views/settings/SettingsLayout.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', component: Login, meta: { title: '登录', public: true } },
    {
      path: '/',
      component: Layout,
      redirect: '/dashboard',
      children: [
        { path: '/dashboard', component: Dashboard, meta: { title: '首页' } },
        { path: '/tasks', component: Task, meta: { title: '每日任务' } },
        { path: '/weeklies', component: Weekly, meta: { title: '周报记录' } },
        { path: '/memos', component: Memo, meta: { title: '备忘录' } },
        { path: '/worklogs', component: WorkLog, meta: { title: '工时登记' } },
        { path: '/timers', component: Timer, meta: { title: '计时' } },
        { path: '/projects', component: Project, meta: { title: '项目管理' } }
      ]
    },
    {
      path: '/settings',
      component: SettingsLayout,
      redirect: '/settings/general',
      children: [
        { path: 'general', component: () => import('@/views/settings/General.vue'), meta: { title: '系统信息' } },
        { path: 'users', component: () => import('@/views/settings/User.vue'), meta: { title: '用户管理' } },
        { path: 'roles', component: () => import('@/views/settings/Role.vue'), meta: { title: '角色管理' } },
        { path: 'permissions', component: () => import('@/views/settings/Permission.vue'), meta: { title: '权限管理' } },
        { path: 'task-categories', component: () => import('@/views/settings/TaskCategories.vue'), meta: { title: '任务分类' } },
        { path: 'api-keys', component: () => import('@/views/settings/ApiKeys.vue'), meta: { title: 'API Key' } },
        { path: 'api-docs', component: () => import('@/views/settings/ApiDocs.vue'), meta: { title: '接口文档' } }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ]
})

// 路由守卫：未登录进登录页，已登录按角色权限校验可访问路由
router.beforeEach((to) => {
  const tokenStore = useTokenStore()
  const userInfoStore = useUserInfoStore()

  if (to.meta.public) {
    return tokenStore.token ? '/dashboard' : true
  }
  if (!tokenStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!userInfoStore.hasRoutePermission(to.path)) {
    const fallback = userInfoStore.routerPaths[0]
    ElMessage.warning('当前账号没有访问该页面的权限')
    if (fallback && fallback !== to.path) {
      return fallback
    }
    return '/login'
  }
  return true
})

router.afterEach((to) => {
  useSystemConfigStore().applyDocumentTitle(to.meta.title)
})

export default router
