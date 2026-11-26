<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Monitor, Timer } from '@element-plus/icons-vue'
import systemApi from '@/api/system'
import brandMark from '@/assets/brand-mark.png'
import hasBtnPermission from '@/utils/btnPermission'
import { useSystemConfigStore } from '@/store/systemConfig'

const route = useRoute()
const systemConfigStore = useSystemConfigStore()

const loading = ref(false)
const saving = ref(false)
const form = ref({
  systemName: '',
  systemSubName: '',
  browserTitle: '',
  loginHint: '',
  taskDefaultStartTime: '09:00',
  taskDefaultEndTime: '18:30'
})

const canSave = computed(() => hasBtnPermission('config:save'))

const loadConfig = async () => {
  loading.value = true
  try {
    const result = await systemApi.getConfig()
    if (result.code === 1 && result.data) {
      form.value = { ...result.data }
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    loading.value = false
  }
}

const saveConfig = async () => {
  if (saving.value) return
  if (!form.value.systemName.trim()) {
    ElMessage.error('系统名称不能为空')
    return
  }
  if (!form.value.browserTitle.trim()) {
    ElMessage.error('浏览器标题后缀不能为空')
    return
  }
  const timePattern = /^([01]\d|2[0-3]):[0-5]\d$/
  if (!timePattern.test(form.value.taskDefaultStartTime || '') ||
      !timePattern.test(form.value.taskDefaultEndTime || '')) {
    ElMessage.error('任务默认时间格式必须为 HH:mm')
    return
  }
  if (form.value.taskDefaultStartTime >= form.value.taskDefaultEndTime) {
    ElMessage.error('默认开始时间必须早于默认截止时间')
    return
  }
  saving.value = true
  try {
    const result = await systemApi.updateConfig({
      systemName: form.value.systemName.trim(),
      systemSubName: form.value.systemSubName.trim(),
      browserTitle: form.value.browserTitle.trim(),
      loginHint: form.value.loginHint.trim(),
      taskDefaultStartTime: form.value.taskDefaultStartTime,
      taskDefaultEndTime: form.value.taskDefaultEndTime
    })
    if (result.code === 1) {
      systemConfigStore.applyConfig(form.value)
      systemConfigStore.applyDocumentTitle(route.meta.title)
      ElMessage.success(result.msg)
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>

<template>
  <section class="wk-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">系统信息</h1>
      </div>
      <div class="wk-page-actions">
        <el-button v-if="canSave" type="primary" :icon="Check" :loading="saving" @click="saveConfig">
          保存修改
        </el-button>
      </div>
    </div>

    <div class="general-grid">
      <div class="general-main">
        <section v-loading="loading" class="wk-card form-card">
          <div class="wk-panel-head">
            <div>
              <p class="wk-panel-title">基础信息</p>
              <p class="wk-panel-sub">配置系统对用户可见的名称与说明</p>
            </div>
            <el-icon class="panel-icon" :size="18"><Monitor /></el-icon>
          </div>

          <el-form :model="form" label-position="top" class="config-form" hide-required-asterisk>
            <div class="form-grid">
              <el-form-item label="系统名称">
                <el-input v-model="form.systemName" maxlength="30" show-word-limit placeholder="例如 Wokit" />
              </el-form-item>
              <el-form-item label="系统副标题">
                <el-input v-model="form.systemSubName" maxlength="30" show-word-limit placeholder="例如 工作助手" />
              </el-form-item>
            </div>

            <el-form-item label="浏览器标题后缀">
              <el-input
                v-model="form.browserTitle"
                maxlength="80"
                show-word-limit
                placeholder="页面标题会显示为「当前页面 · 后缀」"
              />
              <p class="field-tip">例如填写「Wokit 工作助手」，首页标签页会显示「首页 · Wokit 工作助手」。</p>
            </el-form-item>

            <el-form-item label="登录页说明">
              <el-input
                v-model="form.loginHint"
                maxlength="80"
                show-word-limit
                placeholder="登录表单上方的说明文字"
              />
            </el-form-item>
          </el-form>
        </section>

        <section v-loading="loading" class="wk-card form-card">
          <div class="wk-panel-head">
            <div>
              <p class="wk-panel-title">任务默认时间</p>
              <p class="wk-panel-sub">只配置新建任务时自动填充的时分，不保存日期</p>
            </div>
            <el-icon class="panel-icon" :size="18"><Timer /></el-icon>
          </div>

          <el-form :model="form" label-position="top" class="config-form" hide-required-asterisk>
            <div class="form-grid">
              <el-form-item label="默认开始时间">
                <el-time-picker
                  v-model="form.taskDefaultStartTime"
                  format="HH:mm"
                  value-format="HH:mm"
                  :clearable="false"
                  placeholder="09:00"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="默认截止时间">
                <el-time-picker
                  v-model="form.taskDefaultEndTime"
                  format="HH:mm"
                  value-format="HH:mm"
                  :clearable="false"
                  placeholder="18:30"
                  style="width: 100%"
                />
              </el-form-item>
            </div>
          </el-form>
        </section>
      </div>

      <aside class="preview-column">
        <section class="wk-card preview-card">
          <div class="preview-head">
            <p class="preview-label">工作台预览</p>
            <span class="preview-tag">实时</span>
          </div>

          <div class="brand-preview">
            <img :src="brandMark" alt="" />
            <div class="brand-preview-text">
              <p class="brand-preview-name">{{ form.systemName || '系统名称' }}</p>
              <p class="brand-preview-sub">{{ form.systemSubName || '系统副标题' }}</p>
            </div>
          </div>

          <div class="preview-row">
            <p class="preview-row-label">浏览器标签</p>
            <p class="preview-title">{{ form.browserTitle || '浏览器标题后缀' }}</p>
          </div>

          <div class="preview-row">
            <p class="preview-row-label">登录页说明</p>
            <p class="preview-desc">{{ form.loginHint || '请输入登录页说明' }}</p>
          </div>
        </section>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.general-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(280px, 0.9fr);
  gap: 16px;
  align-items: start;
}

.general-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.form-card {
  overflow: hidden;
}

.panel-icon {
  color: var(--wk-text-3);
}

.config-form {
  padding: 20px 22px 6px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.field-tip {
  margin-top: 5px;
  color: var(--wk-text-3);
  font-size: 11.5px;
  line-height: 1.6;
}

.preview-column {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-card {
  padding: 18px 18px 16px;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.preview-label {
  color: var(--wk-text-2);
  font-size: 12px;
}

.preview-tag {
  height: 20px;
  padding: 0 7px;
  border-radius: 5px;
  background: var(--wk-primary-soft);
  color: #0b8b4f;
  font-size: 10.5px;
  line-height: 20px;
}

.brand-preview {
  display: flex;
  align-items: center;
  gap: 11px;
  margin: 14px 0 16px;
  padding: 13px 14px;
  border: 1px solid var(--wk-line);
  border-radius: 12px;
  background: #f9fafb;
}

.brand-preview img {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
}

.brand-preview-text {
  min-width: 0;
}

.brand-preview-name {
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-preview-sub {
  overflow: hidden;
  color: var(--wk-text-3);
  font-size: 11px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-row {
  padding-top: 12px;
  border-top: 1px solid var(--wk-line-soft);
}

.preview-row-label {
  color: var(--wk-text-3);
  font-size: 11px;
}

.preview-title {
  margin-top: 4px;
  overflow: hidden;
  color: var(--wk-text);
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-desc {
  margin-top: 4px;
  color: var(--wk-text-2);
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 1100px) {
  .general-grid {
    grid-template-columns: 1fr;
  }

  .preview-column {
    max-width: 520px;
    width: 100%;
  }
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .config-form {
    padding: 16px 16px 4px;
  }

  .preview-column {
    max-width: none;
  }
}
</style>
