<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  Box,
  Delete,
  DocumentChecked,
  Download,
  Edit,
  Plus,
  Warning
} from '@element-plus/icons-vue'
import weeklyApi from '@/api/weekly'
import ProjectPicker from '@/components/ProjectPicker.vue'
import StatCard from '@/components/StatCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useProjectStore } from '@/store/project'
import hasBtnPermission from '@/utils/btnPermission'

const pad = (num) => String(num).padStart(2, '0')
const toDateStr = (date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
const parseDate = (value) => {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}
const addDays = (value, days) => {
  const date = parseDate(value)
  date.setDate(date.getDate() + days)
  return toDateStr(date)
}
const mondayOf = (value) => {
  const date = parseDate(value)
  const day = date.getDay() || 7
  date.setDate(date.getDate() - day + 1)
  return toDateStr(date)
}

// 计算 ISO 周序号，跨年周也能正确显示所属年份
const isoWeekInfo = (value) => {
  const date = parseDate(value)
  const target = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const dayNumber = (date.getDay() + 6) % 7
  target.setDate(target.getDate() - dayNumber + 3)
  const firstThursday = new Date(target.getFullYear(), 0, 4)
  const firstDayNumber = (firstThursday.getDay() + 6) % 7
  firstThursday.setDate(firstThursday.getDate() - firstDayNumber + 3)
  const week = 1 + Math.round((target - firstThursday) / 604800000)
  return { year: target.getFullYear(), week }
}

const todayStr = toDateStr(new Date())
const weekStart = ref(mondayOf(todayStr))
const rows = ref([])
const loading = ref(false)

const weekInfo = computed(() => isoWeekInfo(weekStart.value))
const weekEnd = computed(() => addDays(weekStart.value, 6))
const weekLabel = computed(() => `${weekInfo.value.year} 年第 ${weekInfo.value.week} 周`)
const rangeLabel = computed(() => {
  const start = weekStart.value
  const end = weekEnd.value
  const startShort = `${Number(start.slice(5, 7))}.${Number(start.slice(8, 10))}`
  const endShort = `${Number(end.slice(5, 7))}.${Number(end.slice(8, 10))}`
  return start.slice(0, 4) === end.slice(0, 4)
    ? `${startShort} - ${endShort}`
    : `${start} - ${end}`
})

const stats = computed(() => ({
  total: rows.value.length,
  problem: rows.value.filter((row) => row.problem && row.problem.trim()).length,
  output: rows.value.filter((row) => row.output && row.output.trim()).length
}))

const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)
const formRef = ref()
const projectPickerRef = ref()
const projectBound = ref(false)
const submitting = ref(false)
const form = ref({
  taskTitle: '',
  taskContent: '',
  resolvedProblem: '',
  taskResult: '',
  problem: '',
  output: '',
  extra: '',
  projectId: null
})

const projectStore = useProjectStore()

const rules = {
  taskTitle: [
    { required: true, message: '请填写任务标题', trigger: 'blur' },
    { max: 100, message: '任务标题不能超过 100 个字', trigger: 'blur' }
  ],
  taskContent: [{ max: 500, message: '任务内容不能超过 500 个字', trigger: 'blur' }],
  resolvedProblem: [{ max: 500, message: '已解决问题及解决方式不能超过 500 个字', trigger: 'blur' }],
  taskResult: [{ max: 500, message: '结果不能超过 500 个字', trigger: 'blur' }],
  problem: [{ max: 500, message: '仍存在的问题不能超过 500 个字', trigger: 'blur' }],
  output: [{ max: 500, message: '产出不能超过 500 个字', trigger: 'blur' }],
  extra: [{ max: 500, message: '其他补充不能超过 500 个字', trigger: 'blur' }]
}

let loadToken = 0

const loadList = async () => {
  const token = ++loadToken
  loading.value = true
  try {
    const result = await weeklyApi.list(weekStart.value)
    if (token !== loadToken) return
    if (result.code === 1) {
      rows.value = result.data || []
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    if (token === loadToken) {
      loading.value = false
    }
  }
}

const changeWeek = (step) => {
  weekStart.value = addDays(weekStart.value, step * 7)
  loadList()
}

const goCurrentWeek = () => {
  const current = mondayOf(todayStr)
  if (current === weekStart.value) return
  weekStart.value = current
  loadList()
}

const openCreate = () => {
  dialogMode.value = 'create'
  editingId.value = null
  projectBound.value = false
  form.value = { taskTitle: '', taskContent: '', resolvedProblem: '', taskResult: '', problem: '', output: '', extra: '', projectId: null }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openEdit = (row) => {
  dialogMode.value = 'edit'
  editingId.value = row.id
  projectBound.value = Boolean(row.projectId)
  form.value = {
    taskTitle: row.taskTitle || '',
    taskContent: row.taskContent,
    resolvedProblem: row.resolvedProblem || '',
    taskResult: row.taskResult || '',
    problem: row.problem || '',
    output: row.output || '',
    extra: row.extra || '',
    projectId: row.projectId || null
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const submitForm = async () => {
  if (!formRef.value || submitting.value) return
  if (!projectPickerRef.value?.validate()) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...form.value, weekStart: weekStart.value }
    const result = editingId.value
      ? await weeklyApi.update(editingId.value, payload)
      : await weeklyApi.add(payload)
    if (result.code === 1) {
      ElMessage.success(result.msg)
      dialogVisible.value = false
      loadList()
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    submitting.value = false
  }
}

const removeRow = async (row) => {
  const label = (row.taskTitle || '').trim() || row.taskContent
  await ElMessageBox.confirm(
    `确定删除「${label}」这条周报记录吗？`,
    '删除周报记录',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  )
  const result = await weeklyApi.remove(row.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    loadList()
  } else {
    ElMessage.error(result.msg)
  }
}

const hasDetail = (row) => Boolean(row.taskResult || row.resolvedProblem || row.problem || row.output || row.extra)

const buildReportText = () => {
  const now = new Date()
  const lines = [
    'Wokit 工作周报',
    `周期：${weekLabel.value}（${weekStart.value} ~ ${weekEnd.value}）`,
    `任务数：${rows.value.length} 条`,
    `导出时间：${toDateStr(now)} ${pad(now.getHours())}:${pad(now.getMinutes())}`,
    ''
  ]
  rows.value.forEach((row, index) => {
    const title = (row.taskTitle || '').trim()
    lines.push(`${index + 1}. ${title || row.taskContent}`)
    const appendField = (label, value) => {
      if (value && value.trim()) {
        lines.push(`   ${label}：${value.trim()}`)
      }
    }
    appendField('任务内容', title ? row.taskContent : '')
    appendField('项目', projectStore.projectName(row.projectId))
    appendField('结果', row.taskResult)
    appendField('已解决问题及解决方式', row.resolvedProblem)
    appendField('仍存在的问题', row.problem)
    appendField('产出', row.output)
    appendField('其他', row.extra)
    lines.push('')
  })
  return lines.join('\r\n')
}

const exportWeek = () => {
  if (!rows.value.length) {
    ElMessage.warning('本周还没有周报记录，无法导出')
    return
  }
  const blob = new Blob([`\ufeff${buildReportText()}`], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `Wokit-周报-${weekInfo.value.year}年第${weekInfo.value.week}周.txt`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('本周周报已导出')
}

onMounted(() => {
  projectStore.load()
  loadList()
})
</script>

<template>
  <section class="wk-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">周报记录</h1>
        <p class="wk-page-desc">按周记录完成的任务、结果、问题与解决方式，随时导出本周明细。</p>
      </div>
      <div class="wk-page-actions weekly-head-actions">
        <div class="week-switch">
          <button class="week-arrow" type="button" aria-label="上一周" @click="changeWeek(-1)">
            <el-icon :size="14"><ArrowLeft /></el-icon>
          </button>
          <div class="week-current">
            <p class="week-number">第 {{ weekInfo.week }} 周</p>
            <p class="week-range">{{ rangeLabel }}</p>
          </div>
          <button class="week-arrow" type="button" aria-label="下一周" @click="changeWeek(1)">
            <el-icon :size="14"><ArrowRight /></el-icon>
          </button>
          <el-button text class="week-today" @click="goCurrentWeek">本周</el-button>
        </div>
        <el-button v-if="hasBtnPermission('weekly:add')" type="primary" :icon="Plus" @click="openCreate">新建记录</el-button>
      </div>
    </div>

    <div class="stat-grid weekly-stats">
      <StatCard label="本周记录" :value="stats.total" unit="条" tone="green" :icon="DocumentChecked" />
      <StatCard label="有产出" :value="stats.output" unit="条" tone="blue" :icon="Box" />
      <StatCard label="仍存在的问题" :value="stats.problem" unit="条" tone="amber" :icon="Warning" />
    </div>

    <section class="wk-card">
      <div class="wk-panel-head weekly-panel-head">
        <div>
          <p class="wk-panel-title">本周明细</p>
          <p class="wk-panel-sub">{{ weekLabel }} · 共 {{ rows.length }} 条记录</p>
        </div>
        <div class="table-actions">
          <el-button v-if="hasBtnPermission('weekly:export')" :icon="Download" @click="exportWeek">导出本周</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" class="wk-table weekly-table" row-key="id">
        <el-table-column label="任务标题" min-width="220">
          <template #default="{ row }">
            <div class="weekly-task-cell">
              <span v-if="projectStore.projectName(row.projectId)" class="weekly-project-tag">
                {{ projectStore.projectName(row.projectId) }}
              </span>
              <span v-if="(row.taskTitle || '').trim()" class="cell-ellipsis task-text">{{ row.taskTitle }}</span>
              <span v-else class="wk-muted">未填写</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="任务内容" min-width="300">
          <template #default="{ row }">
            <el-tooltip v-if="row.taskContent" :content="row.taskContent" placement="top-start" :show-after="200" popper-class="wk-cell-tooltip">
              <span class="cell-ellipsis">{{ row.taskContent }}</span>
            </el-tooltip>
            <span v-else class="wk-muted">未填写</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" min-width="170">
          <template #default="{ row }">
            <el-tooltip v-if="row.taskResult" :content="row.taskResult" placement="top-start" :show-after="200" popper-class="wk-cell-tooltip">
              <span class="cell-ellipsis">{{ row.taskResult }}</span>
            </el-tooltip>
            <span v-else class="wk-muted">未填写</span>
          </template>
        </el-table-column>
        <el-table-column label="已解决问题及解决方式" min-width="210">
          <template #default="{ row }">
            <el-tooltip v-if="row.resolvedProblem" :content="row.resolvedProblem" placement="top-start" :show-after="200" popper-class="wk-cell-tooltip">
              <span class="cell-ellipsis resolved-text">{{ row.resolvedProblem }}</span>
            </el-tooltip>
            <span v-else class="wk-muted">未填写</span>
          </template>
        </el-table-column>
        <el-table-column label="仍存在的问题" min-width="170">
          <template #default="{ row }">
            <el-tooltip v-if="row.problem" :content="row.problem" placement="top-start" :show-after="200" popper-class="wk-cell-tooltip">
              <span class="cell-ellipsis problem-text">{{ row.problem }}</span>
            </el-tooltip>
            <span v-else class="wk-muted">未填写</span>
          </template>
        </el-table-column>
        <el-table-column label="产出" min-width="160">
          <template #default="{ row }">
            <el-tooltip v-if="row.output" :content="row.output" placement="top-start" :show-after="200" popper-class="wk-cell-tooltip">
              <span class="cell-ellipsis">{{ row.output }}</span>
            </el-tooltip>
            <span v-else class="wk-muted">未填写</span>
          </template>
        </el-table-column>
        <el-table-column label="其他" min-width="140">
          <template #default="{ row }">
            <el-tooltip v-if="row.extra" :content="row.extra" placement="top-start" :show-after="200" popper-class="wk-cell-tooltip">
              <span class="cell-ellipsis">{{ row.extra }}</span>
            </el-tooltip>
            <span v-else class="wk-muted">未填写</span>
          </template>
        </el-table-column>
        <el-table-column v-if="hasBtnPermission('weekly:edit') || hasBtnPermission('weekly:delete')" label="操作" width="124" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="hasBtnPermission('weekly:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="hasBtnPermission('weekly:delete')" link type="danger" :icon="Delete" @click="removeRow(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState compact title="本周还没有周报记录" hint="点击右上角「新建记录」，把本周完成的任务记下来。" />
        </template>
      </el-table>

      <div class="weekly-cards">
        <article v-for="row in rows" :key="row.id" class="weekly-card">
          <div class="weekly-card-head">
            <span v-if="projectStore.projectName(row.projectId)" class="weekly-project-tag weekly-project-mobile">
              {{ projectStore.projectName(row.projectId) }}
            </span>
            <h3 class="weekly-task">{{ (row.taskTitle || '').trim() || row.taskContent }}</h3>
            <div v-if="hasBtnPermission('weekly:edit') || hasBtnPermission('weekly:delete')" class="table-actions">
              <el-button v-if="hasBtnPermission('weekly:edit')" link type="primary" :icon="Edit" aria-label="编辑" @click="openEdit(row)" />
              <el-button v-if="hasBtnPermission('weekly:delete')" link type="danger" :icon="Delete" aria-label="删除" @click="removeRow(row)" />
            </div>
          </div>
          <div class="weekly-fields">
            <div v-if="(row.taskTitle || '').trim() && row.taskContent" class="weekly-field">
              <span class="field-label">任务内容</span>
              <p class="field-value">{{ row.taskContent }}</p>
            </div>
            <div v-if="row.taskResult" class="weekly-field">
              <span class="field-label">结果</span>
              <p class="field-value">{{ row.taskResult }}</p>
            </div>
            <div v-if="row.resolvedProblem" class="weekly-field">
              <span class="field-label">已解决问题及解决方式</span>
              <p class="field-value is-resolved">{{ row.resolvedProblem }}</p>
            </div>
            <div v-if="row.problem" class="weekly-field">
              <span class="field-label">仍存在的问题</span>
              <p class="field-value is-problem">{{ row.problem }}</p>
            </div>
            <div v-if="row.output" class="weekly-field">
              <span class="field-label">产出</span>
              <p class="field-value">{{ row.output }}</p>
            </div>
            <div v-if="row.extra" class="weekly-field">
              <span class="field-label">其他</span>
              <p class="field-value">{{ row.extra }}</p>
            </div>
          </div>
          <p v-if="!hasDetail(row)" class="wk-muted no-detail">未填写补充信息</p>
        </article>
        <EmptyState v-if="!rows.length && !loading" compact title="本周还没有周报记录" hint="点击右上角「新建记录」，把本周完成的任务记下来。" />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建周报记录' : '编辑周报记录'"
      width="680px"
      :close-on-click-modal="false"
    >
      <div class="week-note">
        <el-icon :size="14"><DocumentChecked /></el-icon>
        <span>归属：{{ weekLabel }}（{{ rangeLabel }}）</span>
      </div>

      <ProjectPicker
        ref="projectPickerRef"
        v-model="form.projectId"
        v-model:bound="projectBound"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk>
        <el-form-item label="任务标题" prop="taskTitle">
          <el-input
            v-model="form.taskTitle"
            maxlength="100"
            show-word-limit
            placeholder="本周完成的任务标题"
          />
        </el-form-item>

        <el-form-item label="任务内容" prop="taskContent">
          <el-input
            v-model="form.taskContent"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            resize="vertical"
            placeholder="本周完成了什么任务（选填）"
          />
        </el-form-item>

        <el-form-item label="已解决问题及解决方式" prop="resolvedProblem">
          <el-input
            v-model="form.resolvedProblem"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            resize="vertical"
            placeholder="本周解决了哪些问题，分别采用了什么方式（选填）"
          />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="结果" prop="taskResult">
            <el-input
              v-model="form.taskResult"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              resize="vertical"
              placeholder="完成情况或结论（选填）"
            />
          </el-form-item>
          <el-form-item label="仍存在的问题" prop="problem">
            <el-input
              v-model="form.problem"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              resize="vertical"
              placeholder="遗留问题、风险点（选填）"
            />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="产出" prop="output">
            <el-input
              v-model="form.output"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              resize="vertical"
              placeholder="文档、代码、链接等可交付成果（选填）"
            />
          </el-form-item>
          <el-form-item label="其他" prop="extra">
            <el-input
              v-model="form.extra"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              resize="vertical"
              placeholder="需要补充说明的内容（选填）"
            />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ dialogMode === 'create' ? '保存记录' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.week-switch {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  background: #fff;
  border: 1px solid var(--wk-line);
  border-radius: 10px;
}

.week-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 0;
  color: var(--wk-text-2);
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.week-arrow:hover {
  color: var(--wk-text);
  background: #f4f6f8;
}

.week-current {
  min-width: 92px;
  text-align: center;
}

.week-number {
  color: var(--wk-text);
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1.35;
}

.week-range {
  color: var(--wk-text-3);
  font-size: 11px;
  line-height: 1.35;
  font-variant-numeric: tabular-nums;
}

.week-today {
  height: 26px;
  padding: 0 7px;
  color: #0b8b4f;
  font-size: 12px;
}

.weekly-stats {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.task-text {
  color: var(--wk-text);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.65;
}

.cell-ellipsis {
  display: block;
  width: 100%;
  overflow: hidden;
  color: #475467;
  font-size: 12.5px;
  line-height: 1.7;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.problem-text {
  color: #b54708;
}

.resolved-text {
  color: #0b8b4f;
}

.weekly-task-cell {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.weekly-task-cell :deep(.el-tooltip__trigger) {
  display: block;
  flex: 1 1 auto;
  width: auto;
  min-width: 0;
}

.weekly-project-tag {
  display: inline-block;
  flex: 0 0 auto;
  max-width: 110px;
  overflow: hidden;
  padding: 1px 7px;
  color: #0b8b4f;
  font-size: 10.5px;
  font-weight: 400;
  line-height: 1.6;
  background: #ecfdf3;
  border-radius: 4px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.weekly-project-mobile {
  max-width: 96px;
  margin-top: 0;
}

.weekly-table :deep(td.el-table__cell) {
  padding: 11px 0;
  vertical-align: top;
}

.weekly-table :deep(th.el-table__cell) {
  vertical-align: middle;
}

.weekly-cards {
  display: none;
}

.weekly-card {
  padding: 14px 14px 12px;
  background: #fff;
  border: 1px solid var(--wk-line);
  border-radius: 12px;
}

.weekly-card-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.weekly-task {
  flex: 1;
  min-width: 0;
  color: var(--wk-text);
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.5;
  word-break: break-word;
}

.weekly-fields {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-top: 8px;
}

.weekly-field {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.field-label {
  flex-shrink: 0;
  min-width: 56px;
  color: var(--wk-text-3);
  font-size: 11.5px;
  line-height: 1.7;
}

.field-value {
  flex: 1;
  min-width: 0;
  color: #475467;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.field-value.is-problem {
  color: #b54708;
}

.field-value.is-resolved {
  color: #0b8b4f;
}

.no-detail {
  margin-top: 6px;
  font-size: 11.5px;
}

.weekly-panel-head {
  padding: 18px 22px;
}

.week-note {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: -4px 0 16px;
  padding: 8px 10px;
  color: #0b8b4f;
  font-size: 12px;
  background: var(--wk-primary-soft);
  border: 1px solid var(--wk-primary-line);
  border-radius: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.table-actions :deep(.el-button) {
  margin-left: 0;
  padding: 4px 2px;
}

@media (max-width: 1100px) {
  .weekly-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .weekly-stats > :last-child {
    grid-column: span 2;
  }
}

@media (max-width: 768px) {
  .wk-page-head {
    flex-wrap: wrap;
  }

  .weekly-head-actions {
    justify-content: space-between;
    width: 100%;
    gap: 8px;
  }

  .weekly-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .weekly-table {
    display: none;
  }

  .weekly-cards {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 12px 14px 16px;
  }

  .weekly-panel-head {
    padding: 14px;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .week-note {
    margin-top: 0;
  }
}
</style>
