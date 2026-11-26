<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, Delete, Key, Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import apiKeyApi from '@/api/apiKey'
import hasBtnPermission from '@/utils/btnPermission'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = ref({ appName: '', keyType: '', status: '', page: 1, limit: 10 })

const isCompact = ref(false)
let compactMedia
const syncCompact = (event) => {
  isCompact.value = event.matches
}

onMounted(() => {
  compactMedia = window.matchMedia('(max-width: 900px)')
  isCompact.value = compactMedia.matches
  compactMedia.addEventListener('change', syncCompact)
  fetchList()
})

onBeforeUnmount(() => {
  compactMedia?.removeEventListener('change', syncCompact)
})

const dialogVisible = ref(false)
const formRef = ref()
const submitting = ref(false)
const form = ref({
  appName: '',
  platform: 'desktop',
  keyType: 'app',
  expiresAt: '',
  ipWhitelist: '',
  remark: ''
})

const rules = {
  appName: [
    { required: true, message: '请输入应用名称', trigger: 'blur' },
    { max: 60, message: '应用名称不能超过 60 个字符', trigger: 'blur' }
  ],
  platform: [{ required: true, message: '请选择平台', trigger: 'change' }],
  keyType: [{ required: true, message: '请选择密钥类型', trigger: 'change' }]
}

const secretVisible = ref(false)
const secretData = ref({})

const canAdd = computed(() => hasBtnPermission('api:key:add'))
const canToggle = computed(() => hasBtnPermission('api:key:toggle'))
const canReset = computed(() => hasBtnPermission('api:key:reset'))
const canDelete = computed(() => hasBtnPermission('api:key:delete'))

const fetchList = async () => {
  loading.value = true
  try {
    const result = await apiKeyApi.list(query.value)
    if (result.code === 1) {
      list.value = result.data.records || []
      total.value = Number(result.data.total || 0)
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.value.page = 1
  fetchList()
}

const resetSearch = () => {
  query.value = { appName: '', keyType: '', status: '', page: 1, limit: query.value.limit }
  fetchList()
}

const openCreate = () => {
  form.value = {
    appName: '',
    platform: 'desktop',
    keyType: 'app',
    expiresAt: '',
    ipWhitelist: '',
    remark: ''
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const submitCreate = async () => {
  if (!formRef.value || submitting.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const result = await apiKeyApi.create({
      appName: form.value.appName.trim(),
      platform: form.value.platform,
      keyType: form.value.keyType,
      expiresAt: form.value.expiresAt || null,
      ipWhitelist: form.value.ipWhitelist.trim() || null,
      remark: form.value.remark.trim() || null
    })
    if (result.code === 1) {
      dialogVisible.value = false
      secretData.value = result.data || {}
      secretVisible.value = true
      fetchList()
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    submitting.value = false
  }
}

const changeStatus = async (row, value) => {
  const result = await apiKeyApi.changeStatus(row.id, value)
  if (result.code === 1) {
    ElMessage.success(result.msg)
  } else {
    row.status = value === 1 ? 0 : 1
    ElMessage.error(result.msg)
  }
}

const resetSecret = async (row) => {
  await ElMessageBox.confirm(
    `重置「${row.appName}」的 Secret Key 后，旧 Secret 和该密钥签发的 Refresh Token 会立即失效。确认继续吗？`,
    '重置 Secret Key',
    { confirmButtonText: '确认重置', cancelButtonText: '取消', type: 'warning' }
  )
  const result = await apiKeyApi.reset(row.id)
  if (result.code === 1) {
    secretData.value = result.data || {}
    secretVisible.value = true
    ElMessage.success(result.msg)
    fetchList()
  } else {
    ElMessage.error(result.msg)
  }
}

const removeKey = async (row) => {
  await ElMessageBox.confirm(
    `确定删除「${row.appName}」的密钥吗？删除后该应用将无法登录，已签发的 Refresh Token 也会撤销。`,
    '删除密钥',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  )
  const result = await apiKeyApi.remove(row.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    if (list.value.length === 1 && query.value.page > 1) {
      query.value.page -= 1
    }
    fetchList()
  } else {
    ElMessage.error(result.msg)
  }
}

const copyText = async (text, label = '已复制') => {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(label)
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    textarea.remove()
    ElMessage.success(label)
  }
}

const typeLabel = (type) => (type === 'service' ? '服务对接' : 'App 应用')
const platformLabel = (platform) => {
  const map = { desktop: '桌面端', mobile: '手机 App', server: '服务端', other: '其他' }
  return map[platform] || platform || '—'
}
const formatTime = (time) => (time ? time.slice(0, 16) : '—')
const formatExpire = (time) => (time ? time.slice(0, 16) : '长期有效')
</script>

<template>
  <section class="wk-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">API Key</h1>
        <p class="wk-page-desc">
          管理桌面端、手机 App 和可信服务端的接入凭证。App 只允许登录，用户账号请在「用户管理」中创建。
        </p>
      </div>
      <div class="wk-page-actions">
        <el-button v-if="canAdd" type="primary" :icon="Plus" @click="openCreate">新增密钥</el-button>
      </div>
    </div>

    <div class="key-tip">
      <el-icon><Key /></el-icon>
      <div>
        <p>App 端只使用 Access Key，Secret Key 仅用于服务端 Client Credentials。</p>
        <p>Secret Key 只在创建或重置时显示一次，不会再次出现在列表中。</p>
      </div>
    </div>

    <section class="wk-card">
      <div class="wk-panel-head key-toolbar">
        <div class="wk-toolbar">
          <el-input
            v-model="query.appName"
            class="search-input"
            clearable
            placeholder="搜索应用名称"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select v-model="query.keyType" class="type-select" clearable placeholder="全部类型" @change="handleSearch">
            <el-option label="App 应用" value="app" />
            <el-option label="服务对接" value="service" />
          </el-select>
          <el-select v-model="query.status" class="status-select" clearable placeholder="全部状态" @change="handleSearch">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button @click="resetSearch">重置</el-button>
        </div>
        <p class="toolbar-count">共 {{ total }} 个密钥</p>
      </div>

      <el-table v-loading="loading" :data="list" class="wk-table" row-key="id">
        <el-table-column label="应用" min-width="200">
          <template #default="{ row }">
            <div class="app-cell">
              <span class="app-name">{{ row.appName }}</span>
              <span class="app-meta">{{ platformLabel(row.platform) }} · {{ typeLabel(row.keyType) }}</span>
              <span v-if="row.remark" class="app-remark">{{ row.remark }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Access Key" min-width="210">
          <template #default="{ row }">
            <div class="mono-cell">
              <code>{{ row.accessKey }}</code>
              <button class="copy-button" type="button" title="复制 Access Key" @click="copyText(row.accessKey, 'Access Key 已复制')">
                <el-icon><CopyDocument /></el-icon>
              </button>
            </div>
          </template>
        </el-table-column>

        <el-table-column v-if="!isCompact" label="Secret" width="150">
          <template #default="{ row }">
            <span class="secret-hint">{{ row.secretHint || '****' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="108">
          <template #default="{ row }">
            <div class="status-cell">
              <el-switch
                v-if="canToggle"
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                @change="(value) => changeStatus(row, value)"
              />
              <span :class="row.status === 1 ? 'status-on' : 'status-off'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column v-if="!isCompact" label="最后使用" width="160">
          <template #default="{ row }">
            <div class="time-cell">
              <span>{{ formatTime(row.lastUsedAt) }}</span>
              <span v-if="row.lastUsedIp" class="ip-text">{{ row.lastUsedIp }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column v-if="!isCompact" label="过期时间" width="150">
          <template #default="{ row }">
            <span class="time-text">{{ formatExpire(row.expiresAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column v-if="canReset || canDelete" label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="canReset" link type="primary" :icon="RefreshRight" @click="resetSecret(row)">重置</el-button>
              <el-button v-if="canDelete" link type="danger" :icon="Delete" @click="removeKey(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.limit"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="fetchList"
        @size-change="handleSearch"
      />
    </section>

    <el-dialog v-model="dialogVisible" title="新增 API Key" width="540px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk>
        <div class="form-grid">
          <el-form-item label="应用名称" prop="appName">
            <el-input v-model="form.appName" maxlength="60" placeholder="例如 Wokit 桌面端" />
          </el-form-item>
          <el-form-item label="平台" prop="platform">
            <el-select v-model="form.platform" style="width: 100%">
              <el-option label="桌面端" value="desktop" />
              <el-option label="手机 App" value="mobile" />
              <el-option label="服务端" value="server" />
              <el-option label="其他" value="other" />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="密钥类型" prop="keyType">
            <el-select v-model="form.keyType" style="width: 100%">
              <el-option label="App 应用（用户登录）" value="app" />
              <el-option label="服务对接（Client Credentials）" value="service" />
            </el-select>
            <p class="form-tip">App 应用只使用 Access Key；服务对接使用 Access Key + Secret Key。</p>
          </el-form-item>
          <el-form-item label="过期时间">
            <el-date-picker
              v-model="form.expiresAt"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="留空表示长期有效"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <el-form-item label="IP 白名单">
          <el-input v-model="form.ipWhitelist" maxlength="300" placeholder="选填，多个 IP 用英文逗号分隔，例如 1.1.1.1,2.2.2.2" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="200" show-word-limit placeholder="选填，便于区分用途" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">创建密钥</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="secretVisible" title="请立即保存密钥" width="560px" :close-on-click-modal="false">
      <div class="secret-alert">
        <p>Secret Key 只显示这一次，关闭后无法再次查看。</p>
        <p>如果丢失，只能重置 Secret Key；重置后旧的 Secret 和 Refresh Token 会立即失效。</p>
      </div>

      <div class="secret-block">
        <label>Access Key</label>
        <div class="secret-row">
          <code>{{ secretData.accessKey }}</code>
          <el-button size="small" :icon="CopyDocument" @click="copyText(secretData.accessKey, 'Access Key 已复制')">复制</el-button>
        </div>
      </div>

      <div class="secret-block">
        <label>Secret Key</label>
        <div class="secret-row">
          <code class="secret-value">{{ secretData.secretKey }}</code>
          <el-button size="small" type="primary" :icon="CopyDocument" @click="copyText(secretData.secretKey, 'Secret Key 已复制')">复制</el-button>
        </div>
      </div>

      <p v-if="secretData.keyType === 'app'" class="secret-note">
        App 安装包只需要 Access Key，不要把 Secret Key 写入安装包。
      </p>
      <p v-else class="secret-note">
        Secret Key 只能保存在服务端环境变量或密钥管理系统中。
      </p>

      <template #footer>
        <el-button type="primary" @click="secretVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.key-tip {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px solid #dbe7ff;
  border-radius: 10px;
  background: #f5f8ff;
  color: #475467;
  font-size: 12px;
  line-height: 1.7;
}

.key-tip .el-icon {
  margin-top: 2px;
  color: #2563eb;
}

.key-tip p {
  margin: 0;
}

.key-tip p + p {
  margin-top: 2px;
  color: #667085;
}

.key-toolbar {
  flex-wrap: wrap;
}

.search-input {
  width: 220px;
}

.type-select {
  width: 138px;
}

.status-select {
  width: 118px;
}

.toolbar-count {
  color: var(--wk-text-3);
  font-size: 12px;
}

.app-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.app-name {
  font-size: 13px;
  font-weight: 500;
}

.app-meta {
  color: var(--wk-text-3);
  font-size: 10.5px;
}

.app-remark {
  overflow: hidden;
  color: var(--wk-text-2);
  font-size: 10.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mono-cell {
  display: flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
}

.mono-cell code {
  overflow: hidden;
  padding: 3px 7px;
  border-radius: 5px;
  background: #f4f6f8;
  color: #475467;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.copy-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: var(--wk-text-3);
  cursor: pointer;
}

.copy-button:hover {
  background: #f2f4f7;
  color: var(--wk-primary);
}

.secret-hint {
  color: #667085;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
}

.status-cell {
  display: flex;
  align-items: center;
  gap: 7px;
}

.status-on {
  color: #0b8b4f;
  font-size: 12px;
}

.status-off {
  color: #98a2b3;
  font-size: 12px;
}

.time-cell {
  display: flex;
  flex-direction: column;
  gap: 1px;
  color: #667085;
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
}

.ip-text {
  color: #98a2b3;
  font-size: 10.5px;
}

.time-text {
  color: #667085;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.table-actions :deep(.el-button) {
  margin-left: 0;
  padding: 4px 2px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-tip {
  margin-top: 3px;
  color: var(--wk-text-3);
  font-size: 11px;
  line-height: 1.5;
}

.secret-alert {
  margin-bottom: 16px;
  padding: 11px 13px;
  border: 1px solid #fde3c6;
  border-radius: 8px;
  background: #fff8ef;
  color: #9a5b13;
  font-size: 12px;
  line-height: 1.7;
}

.secret-alert p {
  margin: 0;
}

.secret-block {
  margin-bottom: 14px;
}

.secret-block label {
  display: block;
  margin-bottom: 5px;
  color: var(--wk-text-2);
  font-size: 12px;
}

.secret-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.secret-row code {
  flex: 1;
  overflow-x: auto;
  padding: 9px 10px;
  border: 1px solid #e4e8ee;
  border-radius: 7px;
  background: #f9fafb;
  color: #344054;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11.5px;
  white-space: nowrap;
}

.secret-value {
  color: #0b8b4f !important;
}

.secret-note {
  margin: 4px 0 0;
  color: var(--wk-text-3);
  font-size: 11.5px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .key-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .wk-toolbar {
    width: 100%;
  }

  .search-input {
    flex: 1;
    width: auto;
    min-width: 130px;
  }

  .type-select,
  .status-select {
    width: 110px;
  }

  .toolbar-count {
    display: none;
  }
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .secret-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
