<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Download, Link, Lock, Search } from '@element-plus/icons-vue'
import apiDocsApi from '@/api/apiDocs'
import hasBtnPermission from '@/utils/btnPermission'

const loading = ref(false)
const spec = ref(null)
const search = ref('')
const activeGroup = ref('')
const detailVisible = ref(false)
const current = ref({})
const currentGroup = ref('')
const isCompact = ref(false)
let compactMedia
const syncCompact = (event) => {
  isCompact.value = event.matches
}
const drawerSize = computed(() => (isCompact.value ? '100%' : '620px'))

const canExport = computed(() => hasBtnPermission('api:doc:export'))

const groups = computed(() => spec.value?.groups || [])

const visibleGroups = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  const matchedGroups = keyword
    ? groups.value
        .map((group) => ({
          ...group,
          endpoints: (group.endpoints || []).filter((endpoint) =>
            [endpoint.path, endpoint.summary, endpoint.permission, group.name]
              .filter(Boolean)
              .join(' ')
              .toLowerCase()
              .includes(keyword)
          )
        }))
        .filter((group) => group.endpoints.length > 0)
    : groups.value

  if (!keyword && activeGroup.value) {
    return matchedGroups.filter((group) => group.key === activeGroup.value)
  }
  return matchedGroups
})

const endpointCount = computed(() =>
  groups.value.reduce((total, group) => total + (group.endpoints?.length || 0), 0)
)

const loadSpec = async () => {
  loading.value = true
  try {
    const result = await apiDocsApi.spec()
    if (result.code === 1) {
      spec.value = result.data
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    loading.value = false
  }
}

const chooseGroup = (group) => {
  activeGroup.value = group.key
  search.value = ''
}

const showAllGroups = () => {
  activeGroup.value = ''
  search.value = ''
}

const openEndpoint = (endpoint, group) => {
  current.value = endpoint
  currentGroup.value = group.name
  detailVisible.value = true
}

const pretty = (value) => {
  if (value === undefined || value === null || value === '') return ''
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

const allParams = computed(() => {
  if (!current.value) return []
  return [...(current.value.params || []), ...(current.value.query || [])]
})

const curlText = computed(() => {
  const endpoint = current.value || {}
  const base = `${window.location.origin}/api/v1${endpoint.path || ''}`
  const lines = [`curl -X ${endpoint.method || 'GET'} '${base}'`]
  if (endpoint.auth && endpoint.auth !== '无') {
    if (endpoint.path === '/auth/login') {
      lines.push(`  -H 'X-Wokit-Client-Key: wk_ak_你的AccessKey'`)
    } else if (endpoint.path === '/oauth/token') {
      lines.push(`  -H 'Content-Type: application/x-www-form-urlencoded'`)
      lines.push(`  --data-urlencode 'grant_type=client_credentials'`)
      lines.push(`  --data-urlencode 'client_id=wk_ak_你的AccessKey'`)
      lines.push(`  --data-urlencode 'client_secret=wk_sk_你的SecretKey'`)
      return lines.join(' \\\n')
    } else if (endpoint.auth.includes('Bearer')) {
      lines.push(`  -H 'Authorization: Bearer 你的Token'`)
    }
  }
  const query = endpoint.query || []
  if (query.length) {
    const params = query.map((item) => `${item.name}=${item.name === 'page' ? '1' : item.name === 'limit' ? '10' : '值'}`)
    lines[0] = lines[0].replace(`'${base}'`, `'${base}?${params.join('&')}'`)
  }
  if (endpoint.body) {
    lines.push(`  -H 'Content-Type: application/json'`)
    lines.push(`  -d '${JSON.stringify(endpoint.body)}'`)
  }
  return lines.join(' \\\n')
})

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

const exportDocs = async () => {
  const blob = await apiDocsApi.export()
  if (!(blob instanceof Blob)) {
    ElMessage.error('文档导出失败')
    return
  }
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `wokit-api-docs-${new Date().toISOString().slice(0, 10)}.json`
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
  ElMessage.success('接口文档已导出')
}

const methodClass = (method) => `is-${(method || 'get').toLowerCase()}`

onMounted(() => {
  compactMedia = window.matchMedia('(max-width: 900px)')
  isCompact.value = compactMedia.matches
  compactMedia.addEventListener('change', syncCompact)
  loadSpec()
})

onBeforeUnmount(() => {
  compactMedia?.removeEventListener('change', syncCompact)
})
</script>

<template>
  <section v-loading="loading" class="wk-page docs-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">接口文档</h1>
        <p class="wk-page-desc">桌面端、手机 App 和服务端对接说明。是否能看到本页面由管理员在角色权限中配置。</p>
      </div>
      <div class="wk-page-actions">
        <el-button v-if="canExport" :icon="Download" @click="exportDocs">导出文档</el-button>
      </div>
    </div>

    <section v-if="spec" class="docs-hero">
      <div class="overview-rules">
        <div class="overview-block">
          <div class="note-head">
            <el-icon><Lock /></el-icon>
            <div>
              <p class="note-title">认证规则</p>
              <p class="note-sub">App 只登录，不注册；账号由管理员在网页端创建</p>
            </div>
          </div>
          <ul class="note-list">
            <li v-for="item in spec.authRules" :key="item">{{ item }}</li>
          </ul>
        </div>

        <div class="overview-block">
          <div class="note-head">
            <el-icon class="note-icon--blue"><Link /></el-icon>
            <div>
              <p class="note-title">响应与状态码</p>
              <p class="note-sub">业务结果以响应体 code 为准</p>
            </div>
          </div>
          <ul class="note-list">
            <li v-for="item in spec.httpRules" :key="item">{{ item }}</li>
          </ul>
        </div>
      </div>

      <div class="hero-meta">
        <div class="meta-item">
          <span class="meta-label">Base URL</span>
          <code>{{ spec.baseUrl }}</code>
        </div>
        <div class="meta-item">
          <span class="meta-label">版本</span>
          <strong>v{{ spec.version }}</strong>
        </div>
        <div class="meta-item">
          <span class="meta-label">接口数</span>
          <strong>{{ endpointCount }}</strong>
        </div>
      </div>
    </section>

    <section v-if="spec" class="wk-card docs-browser">
      <div class="browser-head">
        <div class="group-tabs">
          <button
            type="button"
            class="group-tab"
            :class="{ 'is-active': !activeGroup && !search.trim() }"
            @click="showAllGroups"
          >
            全部
            <span>{{ endpointCount }}</span>
          </button>
          <button
            v-for="group in groups"
            :key="group.key"
            type="button"
            class="group-tab"
            :class="{ 'is-active': activeGroup === group.key && !search.trim() }"
            @click="chooseGroup(group)"
          >
            {{ group.name }}
            <span>{{ group.endpoints?.length || 0 }}</span>
          </button>
        </div>
        <el-input
          v-model="search"
          class="docs-search"
          clearable
          placeholder="搜索接口路径、说明或权限"
          :prefix-icon="Search"
          @input="activeGroup = ''"
        />
      </div>

      <div v-if="!visibleGroups.length" class="empty-docs">没有匹配的接口</div>

      <div v-for="group in visibleGroups" :key="group.key" class="endpoint-group">
        <div class="group-head">
          <div>
            <p class="group-name">{{ group.name }}</p>
            <p class="group-desc">{{ group.description }}</p>
          </div>
          <span class="group-count">{{ group.endpoints.length }} 个接口</span>
        </div>

        <button
          v-for="endpoint in group.endpoints"
          :key="`${endpoint.method}-${endpoint.path}`"
          type="button"
          class="endpoint-row"
          @click="openEndpoint(endpoint, group)"
        >
          <span class="method-badge" :class="methodClass(endpoint.method)">{{ endpoint.method }}</span>
          <span class="endpoint-main">
            <span class="endpoint-path">{{ endpoint.path }}</span>
            <span class="endpoint-summary">{{ endpoint.summary }}</span>
          </span>
          <span class="endpoint-perm">{{ endpoint.permission || '有效 Token' }}</span>
        </button>
      </div>
    </section>

    <el-drawer v-model="detailVisible" :size="drawerSize" :title="current.summary || '接口详情'">
      <div class="endpoint-detail">
        <div class="detail-title-row">
          <span class="method-badge" :class="methodClass(current.method)">{{ current.method }}</span>
          <code class="detail-path">{{ current.path }}</code>
          <el-button size="small" :icon="CopyDocument" @click="copyText(`${current.method} ${current.path}`, '接口路径已复制')">复制</el-button>
        </div>

        <div class="detail-meta">
          <div>
            <span>所属分组</span>
            <strong>{{ currentGroup }}</strong>
          </div>
          <div>
            <span>认证方式</span>
            <strong>{{ current.auth || 'Bearer Token' }}</strong>
          </div>
          <div>
            <span>权限要求</span>
            <strong>{{ current.permission || '有效 Token' }}</strong>
          </div>
        </div>

        <div v-if="current.headers?.length" class="detail-block">
          <p class="block-title">请求头</p>
          <div v-for="header in current.headers" :key="header.name" class="param-row">
            <code>{{ header.name }}</code>
            <span>{{ header.description }}</span>
            <em>{{ header.value }}</em>
          </div>
        </div>

        <div v-if="allParams.length" class="detail-block">
          <p class="block-title">请求参数</p>
          <div v-for="param in allParams" :key="`${param.in}-${param.name}`" class="param-row">
            <code>{{ param.name }}</code>
            <span>{{ param.description }}</span>
            <em>{{ param.type }}{{ param.required ? ' · 必填' : ' · 选填' }}</em>
          </div>
        </div>

        <div v-if="current.body" class="detail-block">
          <div class="block-head">
            <p class="block-title">{{ current.contentType === 'application/x-www-form-urlencoded' ? '表单请求体' : 'JSON 请求体' }}</p>
            <el-button size="small" text :icon="CopyDocument" @click="copyText(pretty(current.body), '请求体已复制')">复制</el-button>
          </div>
          <pre class="code-block">{{ pretty(current.body) }}</pre>
        </div>

        <div v-if="current.response" class="detail-block">
          <div class="block-head">
            <p class="block-title">响应示例</p>
            <el-button size="small" text :icon="CopyDocument" @click="copyText(pretty(current.response), '响应示例已复制')">复制</el-button>
          </div>
          <pre class="code-block">{{ pretty(current.response) }}</pre>
        </div>

        <div v-if="current.notes" class="detail-block">
          <p class="block-title">说明</p>
          <p class="detail-notes">{{ current.notes }}</p>
        </div>

        <div class="detail-block">
          <div class="block-head">
            <p class="block-title">cURL 示例</p>
            <el-button size="small" text :icon="CopyDocument" @click="copyText(curlText, 'cURL 已复制')">复制</el-button>
          </div>
          <pre class="code-block code-block--curl">{{ curlText }}</pre>
        </div>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.docs-page {
  --method-get: #2563eb;
  --method-post: #0b8b4f;
  --method-put: #d97706;
  --method-delete: #d92d20;
}

.docs-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 26px;
  margin-bottom: 16px;
  padding: 18px 22px 20px;
  border: 1px solid #dfe7f3;
  border-radius: 14px;
  background: linear-gradient(135deg, #f7fbff 0%, #f6fbf8 100%);
}

.overview-rules {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(0, 0.88fr);
  gap: 24px;
  min-width: 0;
}

.overview-block {
  min-width: 0;
}

.overview-block + .overview-block {
  padding-left: 24px;
  border-left: 1px solid #e3ebf3;
}

.hero-meta {
  display: flex;
  flex-shrink: 0;
  gap: 22px;
  align-self: center;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  color: #98a2b3;
  font-size: 10.5px;
}

.meta-item code,
.meta-item strong {
  color: #344054;
  font-size: 12px;
}

.meta-item code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.docs-browser {
  overflow: hidden;
}

.note-head {
  display: flex;
  align-items: flex-start;
  gap: 9px;
}

.note-head .el-icon {
  margin-top: 1px;
  color: #0b8b4f;
}

.note-head .note-icon--blue {
  color: #2563eb;
}

.note-title {
  font-size: 13px;
  font-weight: 600;
}

.note-sub {
  margin-top: 2px;
  color: var(--wk-text-3);
  font-size: 10.5px;
  line-height: 1.5;
}

.note-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-top: 11px;
  padding-left: 16px;
  color: #667085;
  font-size: 11.5px;
  line-height: 1.65;
  list-style: disc;
}

.note-list li::marker {
  color: #98a2b3;
}

.browser-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--wk-line-soft);
  background: #fbfcfd;
}

.group-tabs {
  display: flex;
  gap: 5px;
  overflow-x: auto;
  scrollbar-width: none;
}

.group-tabs::-webkit-scrollbar {
  display: none;
}

.group-tab {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 9px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: #667085;
  font-size: 11.5px;
  cursor: pointer;
}

.group-tab span {
  color: #98a2b3;
  font-size: 10px;
}

.group-tab.is-active {
  border-color: #dce9e2;
  background: var(--wk-primary-soft);
  color: #0b8b4f;
  font-weight: 500;
}

.docs-search {
  width: 228px;
  flex-shrink: 0;
}

.endpoint-group + .endpoint-group {
  border-top: 1px solid var(--wk-line-soft);
}

.group-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 16px 18px 8px;
}

.group-name {
  font-size: 13.5px;
  font-weight: 600;
}

.group-desc {
  margin-top: 3px;
  color: var(--wk-text-3);
  font-size: 11px;
  line-height: 1.55;
}

.group-count {
  flex-shrink: 0;
  color: #98a2b3;
  font-size: 10.5px;
}

.endpoint-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 18px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.endpoint-row:hover {
  background: #f8fbf9;
}

.method-badge {
  flex-shrink: 0;
  min-width: 46px;
  padding: 3px 6px;
  border-radius: 5px;
  background: #f2f4f7;
  color: #475467;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 9.5px;
  font-weight: 600;
  text-align: center;
}

.method-badge.is-get {
  background: #eff4ff;
  color: var(--method-get);
}

.method-badge.is-post {
  background: #ecfdf3;
  color: var(--method-post);
}

.method-badge.is-put {
  background: #fff6e8;
  color: var(--method-put);
}

.method-badge.is-delete {
  background: #fff0ef;
  color: var(--method-delete);
}

.endpoint-main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.endpoint-path {
  overflow: hidden;
  flex-shrink: 0;
  color: #344054;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.endpoint-summary {
  overflow: hidden;
  color: #667085;
  font-size: 11.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.endpoint-perm {
  flex-shrink: 0;
  max-width: 180px;
  overflow: hidden;
  padding: 3px 6px;
  border-radius: 4px;
  background: #f7f8fa;
  color: #98a2b3;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 9.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.endpoint-row:last-child {
  margin-bottom: 8px;
}

.empty-docs {
  padding: 44px 20px;
  color: #98a2b3;
  font-size: 12px;
  text-align: center;
}

.endpoint-detail {
  padding: 0 4px 20px;
}

.detail-title-row {
  display: flex;
  align-items: center;
  gap: 9px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--wk-line-soft);
}

.detail-path {
  flex: 1;
  overflow-x: auto;
  color: #344054;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  white-space: nowrap;
}

.detail-meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 18px;
  padding: 15px 0 4px;
}

.detail-meta div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.detail-meta span {
  color: #98a2b3;
  font-size: 10.5px;
}

.detail-meta strong {
  overflow-wrap: anywhere;
  color: #475467;
  font-size: 11.5px;
  font-weight: 500;
  line-height: 1.5;
}

.detail-block {
  margin-top: 16px;
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.block-title {
  margin-bottom: 6px;
  color: #344054;
  font-size: 12px;
  font-weight: 600;
}

.param-row {
  display: flex;
  align-items: baseline;
  gap: 9px;
  padding: 7px 0;
  border-bottom: 1px dashed #eceff3;
  font-size: 11px;
}

.param-row code {
  flex-shrink: 0;
  color: #2563eb;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.param-row span {
  flex: 1;
  color: #667085;
  line-height: 1.55;
}

.param-row em {
  flex-shrink: 0;
  color: #98a2b3;
  font-size: 10px;
  font-style: normal;
}

.code-block {
  max-height: 280px;
  overflow: auto;
  margin: 0;
  padding: 12px 14px;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  background: #f9fafb;
  color: #344054;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.code-block--curl {
  background: #1f2937;
  border-color: #1f2937;
  color: #d1fadf;
}

.detail-notes {
  padding: 10px 12px;
  border-radius: 7px;
  background: #f8fafc;
  color: #667085;
  font-size: 11.5px;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .docs-hero {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .hero-meta {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .docs-hero {
    padding: 16px;
  }

  .overview-rules {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .overview-block + .overview-block {
    padding-top: 16px;
    padding-left: 0;
    border-top: 1px solid #e3ebf3;
    border-left: 0;
  }

  .hero-meta {
    width: 100%;
    justify-content: space-between;
  }

  .browser-head {
    flex-direction: column;
    align-items: stretch;
    gap: 9px;
    padding: 12px 14px;
  }

  .docs-search {
    width: 100%;
  }

  .endpoint-row {
    flex-wrap: wrap;
    padding: 10px 14px;
  }

  .endpoint-main {
    flex-basis: calc(100% - 58px);
  }

  .endpoint-perm {
    display: none;
  }

  .endpoint-summary {
    display: none;
  }

  .group-head {
    padding: 14px 14px 7px;
  }

  .detail-meta {
    grid-template-columns: 1fr;
  }
}
</style>
