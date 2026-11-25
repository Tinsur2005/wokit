<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft,
  Avatar,
  Key,
  Menu,
  Setting,
  User
} from '@element-plus/icons-vue'
import brandMark from '@/assets/brand-mark.png'
import { useSystemConfigStore } from '@/store/systemConfig'
import { useUserInfoStore } from '@/store/userInfo'

const router = useRouter()
const userInfoStore = useUserInfoStore()
const systemConfigStore = useSystemConfigStore()

const iconMap = {
  Avatar,
  Key,
  Setting,
  User
}

const settingsRoot = computed(() =>
  (userInfoStore.routerList || []).find((item) => item.type === 0 && item.path === '/settings')
)

const menus = computed(() => {
  const children = settingsRoot.value?.children || []
  return children
    .filter((item) => item.type === 1 && item.status === 1 && item.path)
    .map((item) => ({
      path: item.path,
      name: item.name,
      icon: iconMap[item.icon] || Menu
    }))
    .filter((item) => userInfoStore.hasRoutePermission(item.path))
})

const avatarText = computed(() => {
  const name = userInfoStore.displayName
  return name ? name.slice(0, 1).toUpperCase() : 'W'
})

const backToWorkbench = () => {
  router.push('/dashboard')
}
</script>

<template>
  <div class="settings-layout">
    <aside class="settings-sidebar">
      <div class="brand">
        <img class="brand-mark" :src="brandMark" alt="" />
        <div class="brand-text">
          <p class="brand-name">{{ systemConfigStore.systemName }}</p>
          <p class="brand-sub">{{ systemConfigStore.systemSubName }}</p>
        </div>
      </div>

      <div class="settings-title">
        <p class="settings-kicker">SYSTEM</p>
        <h1>系统设置</h1>
      </div>

      <nav class="settings-nav">
        <router-link
          v-for="item in menus"
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
        <button class="back-button" type="button" @click="backToWorkbench">
          <el-icon :size="16"><ArrowLeft /></el-icon>
          <span>返回工作台</span>
        </button>
        <div class="user-chip">
          <div class="user-avatar">{{ avatarText }}</div>
          <div class="user-meta">
            <p class="user-name">{{ userInfoStore.displayName }}</p>
            <p class="user-role">{{ userInfoStore.roleName }}</p>
          </div>
        </div>
      </div>
    </aside>

    <div class="settings-main">
      <header class="mobile-bar">
        <button class="icon-button" type="button" aria-label="返回工作台" @click="backToWorkbench">
          <el-icon :size="19"><ArrowLeft /></el-icon>
        </button>
        <div class="mobile-brand">
          <img class="brand-mark brand-mark--sm" :src="brandMark" alt="" />
          <span>系统设置</span>
        </div>
        <div class="user-avatar user-avatar--sm">{{ avatarText }}</div>
      </header>

      <nav class="mobile-settings-nav">
        <router-link
          v-for="item in menus"
          :key="item.path"
          :to="item.path"
          class="mobile-nav-item"
          active-class="is-active"
        >
          <el-icon :size="16"><component :is="item.icon" /></el-icon>
          <span>{{ item.name }}</span>
        </router-link>
      </nav>

      <main class="settings-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.settings-layout {
  display: flex;
  min-height: 100vh;
  background: var(--wk-bg);
}

/* ---------- 侧边栏 ---------- */
.settings-sidebar {
  width: 238px;
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
  padding: 2px 8px 18px;
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

.settings-title {
  padding: 2px 10px 14px;
  border-bottom: 1px solid #eceff3;
}

.settings-kicker {
  color: #98a2b3;
  font-size: 10px;
  letter-spacing: 1.2px;
}

.settings-title h1 {
  margin-top: 2px;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.2px;
}

.settings-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-top: 12px;
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
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.back-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  height: 36px;
  padding: 0 10px;
  border: 1px solid #e4e8ee;
  border-radius: 9px;
  background: #fff;
  color: #475467;
  font-size: 12.5px;
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease, background-color 0.15s ease;
}

.back-button:hover {
  border-color: var(--wk-primary-line);
  background: var(--wk-primary-soft);
  color: #0b8b4f;
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

/* ---------- 主区域 ---------- */
.settings-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.settings-content {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 22px 26px 30px;
}

.mobile-bar,
.mobile-settings-nav {
  display: none;
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
}

.icon-button:hover {
  background: #f2f4f7;
  color: var(--wk-text);
}

/* ---------- 响应式 ---------- */
@media (max-width: 900px) {
  .settings-sidebar {
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

  .mobile-settings-nav {
    display: flex;
    gap: 6px;
    padding: 10px 12px;
    overflow-x: auto;
    background: #fff;
    border-bottom: 1px solid var(--wk-line-soft);
  }

  .mobile-nav-item {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 6px 10px;
    border-radius: 8px;
    color: #667085;
    font-size: 12px;
    white-space: nowrap;
  }

  .mobile-nav-item.is-active {
    background: var(--wk-primary-soft);
    color: #0b8b4f;
    font-weight: 500;
  }

  .settings-content {
    padding: 14px 12px 30px;
  }
}

@media (max-width: 380px) {
  .settings-content {
    padding-left: 10px;
    padding-right: 10px;
  }
}
</style>
