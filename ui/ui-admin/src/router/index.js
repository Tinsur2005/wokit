import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import Login from '@/views/Login.vue'
import Layout from '@/views/Layout.vue'
import Task from '@/views/Task.vue'
import User from '@/views/User.vue'
import Memo from '@/views/Memo.vue'
import WorkLog from '@/views/WorkLog.vue'
import Weekly from '@/views/Weekly.vue'
import Timer from '@/views/Timer.vue'
import { useTokenStore } from '@/store/token'
import { useUserInfoStore } from '@/store/userInfo'

const Dashboard = () => import('@/views/Dashboard.vue')
const Project = () => import('@/views/Project.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', component: Login },
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
        { path: '/projects', component: Project, meta: { title: '项目管理' } },
        { path: '/users', component: User, meta: { title: '用户管理' } }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ]
})

// 路由守卫：未登录强制进入登录页，普通用户不能进入用户管理
router.beforeEach((to) => {
  const tokenStore = useTokenStore()
  const userInfoStore = useUserInfoStore()

  if (to.path === '/login') {
    return tokenStore.token ? '/dashboard' : true
  }
  if (!tokenStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/users' && !userInfoStore.isAdmin) {
    ElMessage.warning('只有管理员可以进入用户管理')
    return '/dashboard'
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · Wokit` : 'Wokit 工作助手'
})

export default router
