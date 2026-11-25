<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock, Menu, MoreFilled, Setting, SwitchButton } from '@element-plus/icons-vue'
import userApi from '@/api/user'
import brandMark from '@/assets/brand-mark.png'
import { menuIconMap } from '@/utils/menuIcons'
import { useSystemConfigStore } from '@/store/systemConfig'
import { useTokenStore } from '@/store/token'
import { useUserInfoStore } from '@/store/userInfo'

const router = useRouter()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()
const systemConfigStore = useSystemConfigStore()

const drawerVisible = ref(false)
const passwordDialog = ref(false)
const passwordFormRef = ref()
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const savingPassword = ref(false)

const workbenchRoot = computed(() =>
  (userInfoStore.routerList || []).find((item) => item.type === 0 && item.path === '/')
)

const menus = computed(() => {
  const children = workbenchRoot.value?.children || []
  return children
    .filter((item) => item.type === 1 && item.status === 1 && item.path)
    .map((item) => ({
      path: item.path,
      name: item.name,
      icon: menuIconMap[item.icon] || Menu
    }))
})

const visibleMenus = computed(() =>
  menus.value.filter((item) => userInfoStore.hasRoutePermission(item.path))
)

const bottomMenus = computed(() =>
  visibleMenus.value.filter((item) => item.path !== '/projects')
)

const workbenchLabel = computed(() => workbenchRoot.value?.name || '工作台')

const canEnterSettings = computed(
  () => userInfoStore.isAdmin || userInfoStore.hasRoutePermission('/settings/general')
)


const avatarText = computed(() => {
  const name = userInfoStore.displayName
  return name ? name.slice(0, 1).toUpperCase() : 'W'
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const loadUserInfo = async () => {
  const result = await userApi.userInfo()
  if (result.code === 1) {
    userInfoStore.setUserInfo(result.data)
  }
}

const handleCommand = async (command) => {
  drawerVisible.value = false
  if (command === 'settings') {
    router.push('/settings/general')
    return
  }
  if (command === 'password') {
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    passwordDialog.value = true
    return
  }
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    tokenStore.removeToken()
    userInfoStore.removeUserInfo()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

const submitPassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate()
  savingPassword.value = true
  try {
    const result = await userApi.updatePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    if (result.code === 1) {
      ElMessage.success(result.msg)
      passwordDialog.value = false
      tokenStore.removeToken()
      userInfoStore.removeUserInfo()
      router.push('/login')
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    savingPassword.value = false
  }
}

onMounted(() => {
  if (tokenStore.token) {
    loadUserInfo()
  }
})
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <img class="brand-mark" :src="brandMark" alt="" />
        <div class="brand-text">
          <p class="brand-name">{{ systemConfigStore.systemName }}</p>
          <p class="brand-sub">{{ systemConfigStore.systemSubName }}</p>
        </div>
      </div>

      <nav class="nav">
        <p class="nav-label">{{ workbenchLabel }}</p>
        <router-link
          v-for="item in visibleMenus"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          active-class="is-active"
        >
          <el-icon :size="17"><component :is="item.icon" /></el-icon>
          <span>{{ item.name }}</span>
        </router-link>
      </nav>

      <div class="sidebar-foot">
        <div class="user-chip">
          <div class="user-avatar">{{ avatarText }}</div>
          <div class="user-meta">
            <p class="user-name">{{ userInfoStore.displayName }}</p>
            <p class="user-role">{{ userInfoStore.roleName }}</p>
          </div>
          <el-dropdown trigger="click" placement="top-end" @command="handleCommand">
            <button class="icon-button" type="button" aria-label="账号操作">
              <el-icon :size="16"><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="canEnterSettings" command="settings" :icon="Setting">系统设置</el-dropdown-item>
                <el-dropdown-item command="password" :icon="Lock">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </aside>

    <div class="main">
      <header class="mobile-bar">
        <button class="icon-button" type="button" aria-label="打开菜单" @click="drawerVisible = true">
          <el-icon :size="19"><Menu /></el-icon>
        </button>
        <div class="mobile-brand">
          <img class="brand-mark brand-mark--sm" :src="brandMark" alt="" />
          <span>{{ systemConfigStore.systemName }}</span>
        </div>
        <div class="user-avatar user-avatar--sm">{{ avatarText }}</div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>

    <nav class="mobile-nav">
      <router-link
        v-for="item in bottomMenus"
        :key="item.path"
        :to="item.path"
        class="mobile-nav-item"
        active-class="is-active"
      >
        <el-icon :size="20"><component :is="item.icon" /></el-icon>
        <span>{{ item.name }}</span>
      </router-link>
    </nav>

    <el-drawer v-model="drawerVisible" direction="ltr" size="252px" :with-header="false">
      <div class="drawer-body">
        <div class="brand brand--drawer">
          <img class="brand-mark" :src="brandMark" alt="" />
          <div class="brand-text">
            <p class="brand-name">{{ systemConfigStore.systemName }}</p>
            <p class="brand-sub">{{ systemConfigStore.systemSubName }}</p>
          </div>
        </div>
        <nav class="nav">
          <p class="nav-label">{{ workbenchLabel }}</p>
          <router-link
            v-for="item in visibleMenus"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            active-class="is-active"
            @click="drawerVisible = false"
          >
            <el-icon :size="17"><component :is="item.icon" /></el-icon>
            <span>{{ item.name }}</span>
          </router-link>
        </nav>
        <div class="drawer-foot">
          <div class="user-chip">
            <div class="user-avatar">{{ avatarText }}</div>
            <div class="user-meta">
              <p class="user-name">{{ userInfoStore.displayName }}</p>
              <p class="user-role">{{ userInfoStore.roleName }}</p>
            </div>
          </div>
          <div class="drawer-actions">
            <button v-if="canEnterSettings" type="button" class="drawer-action" @click="handleCommand('settings')">
              <el-icon :size="16"><Setting /></el-icon>
              <span>系统设置</span>
            </button>
            <button type="button" class="drawer-action" @click="handleCommand('password')">
              <el-icon :size="16"><Lock /></el-icon>
              <span>修改密码</span>
            </button>
            <button type="button" class="drawer-action drawer-action--danger" @click="handleCommand('logout')">
              <el-icon :size="16"><SwitchButton /></el-icon>
              <span>退出登录</span>
            </button>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="passwordDialog" title="修改密码" width="420px" :close-on-click-modal="false">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6-20 位字符" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingPassword" @click="submitPassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
  background: var(--wk-bg);
}

/* ---------- 侧边栏 ---------- */
.sidebar {
  width: 226px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 20px 14px 16px;
  background: #f7f8fa;
  border-right: 1px solid #eceff3;
  position: sticky;
  top: 0;
  height: 100vh;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 8px 20px;
}

.brand-mark {
  display: block;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  object-fit: contain;
}

.brand-mark--sm {
  width: 26px;
  height: 26px;
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: -0.2px;
}

.brand-sub {
  color: var(--wk-text-3);
  font-size: 11px;
  line-height: 1.4;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-label {
  padding: 12px 10px 6px;
  color: #98a2b3;
  font-size: 11px;
  letter-spacing: 0.6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 11px;
  border-radius: 9px;
  color: #475467;
  font-size: 13px;
  transition: background-color 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.7);
  color: var(--wk-text);
}

.nav-item.is-active {
  background: #fff;
  color: var(--wk-text);
  font-weight: 500;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.05);
}

.nav-item.is-active .el-icon {
  color: var(--wk-primary);
}

.sidebar-foot {
  margin-top: auto;
  padding-top: 14px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 9px;
  background: #fff;
  border: 1px solid #eceff3;
  border-radius: 10px;
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 31px;
  height: 31px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--wk-primary-soft);
  color: #0b8b4f;
  font-size: 13px;
  font-weight: 600;
}

.user-avatar--sm {
  width: 28px;
  height: 28px;
  font-size: 12px;
}

.user-meta {
  flex: 1;
  min-width: 0;
}

.user-name {
  overflow: hidden;
  color: var(--wk-text);
  font-size: 12.5px;
  font-weight: 500;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role {
  color: var(--wk-text-3);
  font-size: 10.5px;
  line-height: 1.4;
}

.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--wk-text-3);
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.icon-button:hover {
  background: #f2f4f7;
  color: var(--wk-text);
}

/* ---------- 主区域 ---------- */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.content {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 22px 26px 30px;
}

.mobile-bar,
.mobile-nav {
  display: none;
}

/* ---------- 抽屉 ---------- */
.drawer-body {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.brand--drawer {
  padding-left: 4px;
}

.drawer-foot {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.drawer-actions {
  display: flex;
  gap: 8px;
}

.drawer-action {
  display: flex;
  flex: 1 1 0;
  min-width: 0;
  height: 52px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 0 4px;
  border: 1px solid #eceff3;
  border-radius: 10px;
  background: #fff;
  color: #667085;
  font-size: 11px;
  line-height: 1.2;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}

.drawer-action:hover,
.drawer-action:active {
  border-color: var(--wk-primary-line);
  background: var(--wk-primary-soft);
  color: #0b8b4f;
}

.drawer-action--danger {
  color: #b42318;
}

.drawer-action--danger:hover,
.drawer-action--danger:active {
  border-color: #fecdca;
  background: #fff6f5;
  color: #b42318;
}

/* ---------- 响应式 ---------- */
@media (max-width: 900px) {
  .sidebar {
    display: none;
  }

  .mobile-bar {
    position: sticky;
    top: 0;
    z-index: 20;
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 56px;
    padding: 0 14px;
    background: #fff;
    border-bottom: 1px solid var(--wk-line);
  }

  .mobile-brand {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
    letter-spacing: -0.2px;
  }

  .content {
    padding: 14px 12px calc(76px + env(safe-area-inset-bottom));
  }

  .mobile-nav {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 30;
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    padding: 6px 6px calc(4px + env(safe-area-inset-bottom));
    background: #fff;
    border-top: 1px solid var(--wk-line);
  }

  .mobile-nav-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    padding: 5px 0;
    border-radius: 8px;
    color: #667085;
    font-size: 11px;
  }

  .mobile-nav-item.is-active {
    color: var(--wk-primary);
    font-weight: 500;
  }
}

@media (max-width: 380px) {
  .content {
    padding-left: 10px;
    padding-right: 10px;
  }
}
</style>
