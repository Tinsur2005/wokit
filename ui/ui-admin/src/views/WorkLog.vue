<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Coin, Delete, Edit, Plus, Search, TrendCharts } from '@element-plus/icons-vue'
import workLogApi from '@/api/worklog'
import ProjectPicker from '@/components/ProjectPicker.vue'
import StatCard from '@/components/StatCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useProjectStore } from '@/store/project'

const todayStr = (() => {
  const now = new Date()
  const pad = (num) => String(num).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
})()

const month = ref(todayStr.slice(0, 7))
const keyword = ref('')
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = ref({ page: 1, limit: 10 })
const stat = ref({ monthHours: 0, days: 0, avgHours: 0, records: 0 })

const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)
const formRef = ref()
const projectPickerRef = ref()
const projectBound = ref(false)
const submitting = ref(false)
const form = ref({
  workDate: todayStr,
  project: '',
  content: '',
  hours: 8,
  projectId: null
})

const projectStore = useProjectStore()

const workLogName = (row) => projectStore.projectName(row.projectId) || row.project || '未命名事项'

const rules = {
  workDate: [{ required: true, message: '请选择工作日期', trigger: 'change' }],
  project: [
    {
      validator: (rule, value, callback) => {
        if (projectBound.value) {
          callback()
          return
        }
        const name = String(value || '').trim()
        if (!name) {
          callback(new Error('请填写事项名称'))
          return
        }
        if (name.length > 100) {
          callback(new Error('不能超过 100 个字'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  content: [{ max: 500, message: '工作内容不能超过 500 个字', trigger: 'blur' }],
  hours: [
    { required: true, message: '请填写工时', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        const num = Number(value)
        if (!num || num <= 0 || num > 24) {
          callback(new Error('工时需大于 0 且不超过 24 小时'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

const loadStat = async () => {
  const result = await workLogApi.stat(month.value)
  if (result.code === 1) {
    stat.value = result.data || stat.value
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const result = await workLogApi.list({
      month: month.value,
      keyword: keyword.value || undefined,
      page: query.value.page,
      limit: query.value.limit
    })
    if (result.code === 1) {
      rows.value = result.data.records || []
      total.value = Number(result.data.total || 0)
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    loading.value = false
  }
}

const loadAll = async () => {
  await Promise.all([loadStat(), loadList()])
}

const handleMonthChange = () => {
  query.value.page = 1
  loadAll()
}

const handleSearch = () => {
  query.value.page = 1
  loadList()
}

const openCreate = () => {
  dialogMode.value = 'create'
  editingId.value = null
  projectBound.value = false
  form.value = { workDate: todayStr, project: '', content: '', hours: 8, projectId: null }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openEdit = (row) => {
  dialogMode.value = 'edit'
  editingId.value = row.id
  projectBound.value = Boolean(row.projectId)
  form.value = {
    workDate: row.workDate,
    project: row.project,
    content: row.content || '',
    hours: Number(row.hours),
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
    const selectedProjectName = projectStore.projectName(form.value.projectId)
    const payload = {
      ...form.value,
      project: projectBound.value ? selectedProjectName : form.value.project
    }
    const result = editingId.value
      ? await workLogApi.update(editingId.value, payload)
      : await workLogApi.add(payload)
    if (result.code === 1) {
      ElMessage.success(result.msg)
      dialogVisible.value = false
      month.value = form.value.workDate.slice(0, 7)
      query.value.page = 1
      loadAll()
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    submitting.value = false
  }
}

const removeRow = async (row) => {
  await ElMessageBox.confirm(
    `确定删除 ${row.workDate} 的「${workLogName(row)}」工时记录吗？`,
    '删除工时',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  )
  const result = await workLogApi.remove(row.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    if (rows.value.length === 1 && query.value.page > 1) {
      query.value.page -= 1
    }
    loadAll()
  } else {
    ElMessage.error(result.msg)
  }
}

const formatHours = (hours) => Number(hours || 0).toFixed(1)

onMounted(() => {
  projectStore.load()
  loadAll()
})
</script>

<template>
  <section class="wk-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">工时登记</h1>
        <p class="wk-page-desc">按天记录投入的工时，月底回看时间花在了哪里。</p>
      </div>
      <div class="wk-page-actions">
        <el-date-picker
          v-model="month"
          class="month-picker"
          type="month"
          format="YYYY 年 M 月"
          value-format="YYYY-MM"
          :clearable="false"
          @change="handleMonthChange"
        />
        <el-button type="primary" :icon="Plus" @click="openCreate">登记工时</el-button>
      </div>
    </div>

    <div class="stat-grid worklog-stats">
      <StatCard label="本月工时" :value="formatHours(stat.monthHours)" unit="h" tone="green" :icon="Coin" />
      <StatCard label="登记天数" :value="stat.days" unit="天" tone="blue" :icon="Calendar" />
      <StatCard label="日均工时" :value="formatHours(stat.avgHours)" unit="h" tone="amber" :icon="TrendCharts" />
    </div>

    <section class="wk-card">
      <div class="wk-panel-head">
        <div class="wk-toolbar">
          <el-input
            v-model="keyword"
            class="search-input"
            clearable
            placeholder="搜索事项或工作内容"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button @click="handleSearch">搜索</el-button>
          <el-button @click="loadAll">刷新</el-button>
        </div>
        <p class="list-count">当前月共 {{ total }} 条记录</p>
      </div>

      <el-table v-loading="loading" :data="rows" class="wk-table worklog-table" row-key="id">
        <el-table-column label="日期" width="118">
          <template #default="{ row }">
            <span class="date-text">{{ row.workDate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="事项" min-width="160">
          <template #default="{ row }">
            <div v-if="projectStore.projectName(row.projectId)" class="worklog-item-cell">
              <span class="worklog-item-name">{{ projectStore.projectName(row.projectId) }}</span>
              <span class="worklog-project-category">
                {{ projectStore.projectCategoryName(row.projectId) || '未分类' }}
              </span>
            </div>
            <div v-else class="worklog-item-cell">
              <span class="worklog-item-name">{{ row.project }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="工作内容" min-width="240">
          <template #default="{ row }">
            <span :class="row.content ? 'content-text' : 'wk-muted'">{{ row.content || '未填写' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="工时" width="100" align="right">
          <template #default="{ row }">
            <span class="hours-text">{{ formatHours(row.hours) }}<em>h</em></span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" :icon="Delete" @click="removeRow(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState compact title="本月还没有工时记录" hint="点击右上角「登记工时」，记录今天投入的时间。" />
        </template>
      </el-table>

      <div v-loading="loading" class="worklog-cards">
        <article v-for="row in rows" :key="row.id" class="worklog-card">
          <div class="log-card-head">
            <span class="log-card-date">{{ row.workDate }}</span>
            <span class="hours-text">{{ formatHours(row.hours) }}<em>h</em></span>
            <div class="table-actions">
              <el-button link type="primary" :icon="Edit" aria-label="编辑" @click="openEdit(row)" />
              <el-button link type="danger" :icon="Delete" aria-label="删除" @click="removeRow(row)" />
            </div>
          </div>
          <div v-if="projectStore.projectName(row.projectId)" class="log-card-project">
            <p class="log-card-project-name">{{ projectStore.projectName(row.projectId) }}</p>
            <span class="log-card-category">
              {{ projectStore.projectCategoryName(row.projectId) || '未分类' }}
            </span>
          </div>
          <p v-else class="log-card-project">{{ row.project }}</p>
          <p v-if="row.content" class="log-card-content">{{ row.content }}</p>
          <p v-else class="wk-muted log-card-content">未填写工作内容</p>
        </article>
        <EmptyState v-if="!rows.length && !loading" compact title="本月还没有工时记录" hint="点击右上角「登记工时」，记录今天投入的时间。" />
      </div>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.limit"
        class="worklog-pagination"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="loadList"
        @size-change="handleSearch"
      />
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '登记工时' : '编辑工时'"
      width="540px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk>
        <div class="form-grid">
          <el-form-item label="工作日期" prop="workDate">
            <el-date-picker
              v-model="form.workDate"
              type="date"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="工时（小时）" prop="hours">
            <el-input-number
              v-model="form.hours"
              :min="0.5"
              :max="24"
              :step="0.5"
              :precision="1"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <ProjectPicker
          ref="projectPickerRef"
          v-model="form.projectId"
          v-model:bound="projectBound"
        />

        <el-form-item v-if="!projectBound" label="事项名称" prop="project">
          <el-input v-model="form.project" maxlength="100" placeholder="例如：插件开发、需求评审" />
        </el-form-item>

        <el-form-item label="工作内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            resize="none"
            placeholder="今天具体做了什么（选填）"
          />
        </el-form-item>
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
.month-picker {
  width: 136px;
}

.search-input {
  width: 236px;
}

.list-count {
  color: var(--wk-text-3);
  font-size: 12px;
}

.date-text {
  color: #475467;
  font-size: 12.5px;
  font-variant-numeric: tabular-nums;
}

.content-text {
  color: #475467;
  font-size: 12.5px;
}

.worklog-item-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  min-width: 0;
}

.worklog-item-name {
  max-width: 100%;
  overflow: hidden;
  color: var(--wk-text);
  font-size: 12.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.worklog-project-category {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  padding: 1px 7px;
  color: #0b8b4f;
  font-size: 10.5px;
  line-height: 1.6;
  background: #ecfdf3;
  border-radius: 4px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hours-text {
  color: #0b8b4f;
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
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

.worklog-stats {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.hours-text em {
  margin-left: 1px;
  color: #98a2b3;
  font-size: 11px;
  font-style: normal;
  font-weight: 400;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.worklog-cards {
  display: none;
}

.worklog-card {
  padding: 13px 14px 12px;
  background: #fff;
  border: 1px solid var(--wk-line);
  border-radius: 12px;
}

.log-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.log-card-date {
  flex: 1;
  color: var(--wk-text-3);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.log-card-project {
  margin-top: 4px;
  color: var(--wk-text);
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.5;
}

.log-card-project-name {
  margin: 4px 0 0;
  color: var(--wk-text);
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.5;
}

.log-card-category {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  margin-top: 5px;
  padding: 1px 7px;
  color: #0b8b4f;
  font-size: 10.5px;
  line-height: 1.6;
  background: #ecfdf3;
  border-radius: 4px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-card-content {
  margin-top: 4px;
  color: var(--wk-text-2);
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1100px) {
  .worklog-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .worklog-stats > :last-child {
    grid-column: span 2;
  }
}

@media (max-width: 768px) {
  .wk-page-head {
    flex-wrap: wrap;
  }

  .wk-page-actions {
    justify-content: space-between;
    width: 100%;
  }

  .worklog-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .worklog-stats > :last-child {
    grid-column: span 2;
  }

  .month-picker {
    flex: 1;
    width: auto;
    min-width: 0;
  }

  .wk-panel-head {
    padding: 14px 14px 12px;
  }

  .search-input {
    flex: 1;
    width: auto;
    min-width: 150px;
  }

  .list-count {
    display: none;
  }

  .worklog-table {
    display: none;
  }

  .worklog-cards {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 12px 14px 6px;
  }

  .worklog-pagination :deep(.el-pagination__sizes) {
    display: none;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
