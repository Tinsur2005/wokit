<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import brandLockup from '@/assets/brand-lockup.png'
import userApi from '@/api/user'
import { useTokenStore } from '@/store/token'
import { useUserInfoStore } from '@/store/userInfo'
import { useSystemConfigStore } from '@/store/systemConfig'

const route = useRoute()
const router = useRouter()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()
const systemConfigStore = useSystemConfigStore()

const formRef = ref()
const loading = ref(false)
const form = ref({ name: '', password: '' })

const rules = {
  name: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const submit = async () => {
  if (loading.value || !formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    const result = await userApi.login(form.value)
    if (result.code !== 1) {
      ElMessage.error(result.msg)
      return
    }
    tokenStore.setToken(result.data)
    const info = await userApi.userInfo()
    if (info.code === 1) {
      userInfoStore.setUserInfo(info.data)
    }
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.push(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-shell">
      <aside class="login-aside">
        <div class="brand">
          <img class="brand-lockup" :src="brandLockup" alt="Wokit 工作助手" />
        </div>

        <div class="aside-copy">
          <h1>把每天的工作，<br />收进一个工作台</h1>
        </div>

        <ul class="feature-list">
          <li><span class="feature-dot"></span>快速查看每日任务，按优先级完成任务</li>
          <li><span class="feature-dot"></span>按任务快速添加周报，一键导出整周周报</li>
          <li><span class="feature-dot"></span>清晰的工时登记，快速提升工作效率</li>
        </ul>
      </aside>

      <main class="login-main">
        <div class="mobile-login-brand">
          <img :src="brandLockup" alt="Wokit 工作助手" />
        </div>
        <div class="form-head">
          <h2>欢迎回来</h2>
          <p class="form-sub">{{ systemConfigStore.loginHint }}</p>
        </div>

        <div class="form-wrap" @keyup.enter="submit">
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk>
            <el-form-item label="账号" prop="name">
              <el-input
                v-model="form.name"
                size="large"
                placeholder="请输入登录账号"
                :prefix-icon="User"
                autocomplete="username"
              />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                size="large"
                type="password"
                show-password
                placeholder="请输入密码"
                :prefix-icon="Lock"
                autocomplete="current-password"
              />
            </el-form-item>
            <el-button class="submit-button" type="primary" size="large" :loading="loading" @click="submit">
              进入工作台
            </el-button>
          </el-form>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background: #f3f5f7;
}

.login-shell {
  display: grid;
  grid-template-columns: 0.94fr 1.06fr;
  width: min(940px, 100%);
  min-height: 548px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e8ecf1;
  border-radius: 20px;
  box-shadow: 0 18px 46px rgba(16, 24, 40, 0.08);
}

.login-aside {
  display: flex;
  flex-direction: column;
  padding: 38px 40px 32px;
  background: #f2fbf6;
}

.brand {
  display: flex;
  align-items: center;
}

.brand-lockup {
  display: block;
  width: 236px;
  max-width: 100%;
  height: auto;
  margin-left: -5px;
}

.mobile-login-brand {
  display: none;
}

.aside-copy {
  margin-top: 54px;
}

.aside-copy h1 {
  margin-top: 0;
  font-size: 30px;
  font-weight: 600;
  line-height: 1.42;
  letter-spacing: -0.6px;
}

.feature-list {
  margin: 26px 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 11px;
}

.feature-list li {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  color: #475467;
  font-size: 12.5px;
  line-height: 1.6;
}

.feature-dot {
  width: 6px;
  height: 6px;
  margin-top: 7px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--wk-primary);
  opacity: 0.7;
}

.login-main {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 40px 52px;
  background: #fff;
}

.form-head h2 {
  margin-top: 0;
  font-size: 26px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.form-sub {
  margin-top: 5px;
  color: var(--wk-text-3);
  font-size: 12.5px;
}

.form-wrap {
  margin-top: 28px;
}

.submit-button {
  width: 100%;
  height: 42px;
  margin-top: 6px;
  font-size: 14px;
}

@media (max-width: 860px) {
  .login-page {
    padding: 20px 14px;
  }

  .login-shell {
    display: block;
    min-height: 0;
    border-radius: 16px;
  }

  .login-aside {
    display: none;
  }

  .login-main {
    padding: 30px 24px 26px;
  }

  .mobile-login-brand {
    display: block;
    width: 190px;
    margin-bottom: 24px;
  }

  .mobile-login-brand img {
    display: block;
    width: 100%;
    height: auto;
    margin-left: -4px;
  }

  .form-head h2 {
    font-size: 23px;
  }
}
</style>
