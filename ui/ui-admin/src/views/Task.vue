<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  Calendar,
  Check,
  CircleCheck,
  Delete,
  Edit,
  List,
  Plus,
  TrendCharts
} from '@element-plus/icons-vue'
import taskApi from '@/api/task'
import ProjectPicker from '@/components/ProjectPicker.vue'
import StatCard from '@/components/StatCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useProjectStore } from '@/store/project'
import hasBtnPermission from '@/utils/btnPermission'

const pad = (num) => String(num).padStart(2, '0')
const formatDate = (date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
const parseDate = (str) => {
  const [year, month, day] = str.split('-').map(Number)
  return new Date(year, month - 1, day)
}

const todayStr = formatDate(new Date())

const selectedDate = ref(todayStr)
const viewMonth = ref(todayStr.slice(0, 7))
const tasks = ref([])
const marks = ref([])
const stat = ref({
  todayPending: 0,
  todayTotal: 0,
  weekCompleted: 0,
  monthTotal: 0,
  monthCompleted: 0
})
const loadingTasks = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)
const formRef = ref()
const projectPickerRef = ref()
const projectBound = ref(false)
const submitting = ref(false)
const form = ref({
  title: '',
  description: '',
  priority: 2,
  status: 0,
  startTime: '',
  deadline: '',
  projectId: null
})

const projectStore = useProjectStore()

const priorityOptions = [
  { value: 0, label: '紧急', tone: 'urgent' },
  { value: 1, label: '重要', tone: 'important' },
  { value: 2, label: '普通', tone: 'normal' },
  { value: 3, label: '低优先级', tone: 'low' }
]

const statusOptions = [
  { value: 0, label: '待处理', tagType: 'info' },
  { value: 1, label: '进行中', tagType: 'primary' },
  { value: 2, label: '阻塞中', tagType: 'warning' },
  { value: 3, label: '已完成', tagType: 'success' }
]

const priorityMap = Object.fromEntries(priorityOptions.map((item) => [item.value, item]))
const statusMap = Object.fromEntries(statusOptions.map((item) => [item.value, item]))
const getPriority = (value) => priorityMap[value] || priorityMap[2]
const getStatus = (value) => statusMap[value] || statusMap[0]

const weekLabels = ['一', '二', '三', '四', '五', '六', '日']

const markMap = computed(() => {
  const map = {}
  marks.value.forEach((item) => {
    map[item.date] = item
  })
  return map
})

const calendarCells = computed(() => {
  const [year, month] = viewMonth.value.split('-').map(Number)
  const firstDay = new Date(year, month - 1, 1)
  const offset = (firstDay.getDay() + 6) % 7
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(year, month - 1, 1 - offset + index)
    const dateStr = formatDate(date)
    const mark = markMap.value[dateStr]
    return {
      date: dateStr,
      day: date.getDate(),
      inMonth: date.getMonth() === month - 1,
      isToday: dateStr === todayStr,
      total: mark ? mark.total : 0,
      pending: mark ? mark.pending : 0
    }
  })
})

const monthLabel = computed(() => `${viewMonth.value.slice(0, 4)} 年 ${Number(viewMonth.value.slice(5, 7))} 月`)

const selectedDayLabel = computed(() => {
  const date = parseDate(selectedDate.value)
  return `${date.getMonth() + 1} 月 ${date.getDate()} 日`
})

const selectedWeekday = computed(() => `周${'日一二三四五六'[parseDate(selectedDate.value).getDay()]}`)

const isDone = (task) => task.status === 3
const isDueToday = (task) => Boolean(task.deadline && task.deadline.slice(0, 10) === todayStr)

const pendingCount = computed(() => tasks.value.filter((task) => !isDone(task)).length)

const completionRate = computed(() => {
  if (!stat.value.monthTotal) return 0
  return Math.round((stat.value.monthCompleted / stat.value.monthTotal) * 100)
})

const rules = {
  title: [
    { required: true, message: '请填写任务标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过 100 个字', trigger: 'blur' }
  ],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  status: [{ required: true, message: '请选择任务状态', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  deadline: [
    { required: true, message: '请选择截止时间', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (!value || !form.value.startTime) {
          callback()
          return
        }
        const start = new Date(form.value.startTime.replace(/-/g, '/')).getTime()
        const end = new Date(value.replace(/-/g, '/')).getTime()
        if (end < start) {
          callback(new Error('截止时间不能早于开始时间'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

const loadCalendar = async () => {
  const result = await taskApi.calendar(viewMonth.value)
  if (result.code === 1) {
    marks.value = result.data || []
  }
}

const loadStat = async () => {
  const result = await taskApi.stat()
  if (result.code === 1) {
    stat.value = result.data || stat.value
  }
}

const loadTasks = async () => {
  loadingTasks.value = true
  try {
    const result = await taskApi.listByDate(selectedDate.value)
    if (result.code === 1) {
      tasks.value = result.data || []
    }
  } finally {
    loadingTasks.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([loadCalendar(), loadStat(), loadTasks()])
}

const selectDay = (cell) => {
  if (cell.date === selectedDate.value) return
  selectedDate.value = cell.date
  if (!cell.inMonth) {
    viewMonth.value = cell.date.slice(0, 7)
    loadCalendar()
  }
  loadTasks()
}

const shiftMonth = (delta) => {
  const [year, month] = viewMonth.value.split('-').map(Number)
  const next = new Date(year, month - 1 + delta, 1)
  viewMonth.value = `${next.getFullYear()}-${pad(next.getMonth() + 1)}`
  selectedDate.value = viewMonth.value === todayStr.slice(0, 7) ? todayStr : `${viewMonth.value}-01`
  refreshAll()
}

const goToday = () => {
  viewMonth.value = todayStr.slice(0, 7)
  selectedDate.value = todayStr
  refreshAll()
}

const handleStartChange = () => {
  if (!form.value.deadline || !formRef.value) return
  formRef.value.validateField('deadline').catch(() => {})
}

const openCreate = () => {  dialogMode.value = 'create'
  editingId.value = null
  projectBound.value = false
  form.value = {
    title: '',
    description: '',
    priority: 2,
    status: 0,
    startTime: `${selectedDate.value} 09:00:00`,
    deadline: `${selectedDate.value} 18:00:00`,
    projectId: null
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openEdit = (task) => {
  dialogMode.value = 'edit'
  editingId.value = task.id
  projectBound.value = Boolean(task.projectId)
  form.value = {
    title: task.title,
    description: task.description || '',
    priority: task.priority ?? 2,
    status: task.status ?? 0,
    startTime: task.startTime,
    deadline: task.deadline,
    projectId: task.projectId || null
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const submitTask = async () => {
  if (!formRef.value || submitting.value) return
  if (!projectPickerRef.value?.validate()) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      title: form.value.title,
      description: form.value.description,
      priority: form.value.priority,
      status: form.value.status,
      startTime: form.value.startTime,
      deadline: form.value.deadline,
      projectId: form.value.projectId || null
    }
    const result = editingId.value
      ? await taskApi.update(editingId.value, payload)
      : await taskApi.add(payload)
    if (result.code === 1) {
      ElMessage.success(result.msg)
      dialogVisible.value = false
      const targetDate = payload.deadline.slice(0, 10)
      selectedDate.value = targetDate
      viewMonth.value = targetDate.slice(0, 7)
      await refreshAll()
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    submitting.value = false
  }
}

const toggleTask = async (task) => {
  const nextStatus = isDone(task) ? 0 : 3
  const result = await taskApi.changeStatus(task.id, nextStatus)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    await refreshAll()
  } else {
    ElMessage.error(result.msg)
  }
}

const removeTask = async (task) => {
  await ElMessageBox.confirm(`确定删除「${task.title}」吗？删除后不可恢复。`, '删除任务', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const result = await taskApi.remove(task.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    await refreshAll()
  } else {
    ElMessage.error(result.msg)
  }
}

const formatDateTime = (value) => (value ? value.slice(0, 16) : '--')

const timeRange = (task) => {
  const start = formatDateTime(task.startTime)
  const end = formatDateTime(task.deadline)
  return `${start} - ${end}`
}

const isOverdue = (task) => {
  if (isDone(task) || !task.deadline) return false
  return new Date(task.deadline.replace(/-/g, '/')).getTime() < Date.now()
}

onMounted(() => {
  projectStore.load()
  refreshAll()
})
</script>

<template>
  <section class="wk-page task-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">每日任务</h1>
        <p class="wk-page-desc">按时间范围安排任务，范围内每天都会展示，用截止时间收口。</p>
      </div>
      <div class="wk-page-actions">
        <el-button v-if="hasBtnPermission('task:add')" type="primary" :icon="Plus" @click="openCreate">新建任务</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <StatCard
        label="今日待办"
        :value="stat.todayPending"
        unit="项"
        tone="green"
        :icon="List"
      />
      <StatCard
        label="本周完成"
        :value="stat.weekCompleted"
        unit="项"
        tone="blue"
        :icon="CircleCheck"
      />
      <StatCard
        label="本月任务"
        :value="stat.monthTotal"
        unit="项"
        tone="amber"
        :icon="Calendar"
      />
      <StatCard
        label="本月完成率"
        :value="completionRate"
        unit="%"
        tone="violet"
        :icon="TrendCharts"
      />
    </div>

    <div class="task-split">
      <section class="wk-card calendar-card">
        <div class="cal-head">
          <div>
            <p class="cal-month">{{ monthLabel }}</p>
            <p class="cal-sub">{{ marks.length }} 天有任务</p>
          </div>
          <div class="cal-actions">
            <button class="cal-today" type="button" @click="goToday">今天</button>
            <button class="cal-nav" type="button" aria-label="上个月" @click="shiftMonth(-1)">
              <el-icon :size="14"><ArrowLeft /></el-icon>
            </button>
            <button class="cal-nav" type="button" aria-label="下个月" @click="shiftMonth(1)">
              <el-icon :size="14"><ArrowRight /></el-icon>
            </button>
          </div>
        </div>

        <div class="week-row">
          <span v-for="label in weekLabels" :key="label">{{ label }}</span>
        </div>

        <div class="calendar-grid">
          <button
            v-for="cell in calendarCells"
            :key="cell.date"
            type="button"
            class="day-cell"
            :class="{
              'is-outside': !cell.inMonth,
              'is-today': cell.isToday,
              'is-selected': cell.date === selectedDate
            }"
            :title="cell.total ? `${cell.date} 共 ${cell.total} 项任务` : cell.date"
            @click="selectDay(cell)"
          >
            <span class="day-num">{{ cell.day }}</span>
            <span v-if="cell.inMonth && cell.total" class="day-mark">
              <i class="dot" :class="cell.pending > 0 ? 'dot-pending' : 'dot-done'"></i>
            </span>
          </button>
        </div>

        <div class="cal-legend">
          <span><i class="dot dot-pending"></i>有未完成</span>
          <span><i class="dot dot-done"></i>全部完成</span>
        </div>
      </section>

      <section class="wk-card day-card">
        <div class="day-head">
          <div class="day-head-left">
            <div class="day-title-line">
              <h2>{{ selectedDayLabel }}</h2>
              <span class="day-week">{{ selectedWeekday }}</span>
              <span
                v-if="tasks.length"
                class="wk-badge"
                :class="{ 'is-muted': pendingCount === 0 }"
              >
                {{ pendingCount > 0 ? `${pendingCount} 项未完成` : '已全部完成' }}
              </span>
            </div>
            <p class="day-sub">当日任务 · 共 {{ tasks.length }} 项</p>
          </div>
          <el-tooltip v-if="hasBtnPermission('task:add')" content="添加当日任务" placement="top">
            <button class="day-add" type="button" aria-label="添加任务" @click="openCreate">
              <el-icon :size="18"><Plus /></el-icon>
            </button>
          </el-tooltip>
        </div>

        <div class="task-list" v-loading="loadingTasks">
          <template v-if="tasks.length">
            <article
              v-for="task in tasks"
              :key="task.id"
              class="task-row"
              :class="[isDone(task) ? 'is-done' : '', 'priority-' + getPriority(task.priority).tone]"
            >
              <button
                class="task-check"
                :class="{ checked: isDone(task) }"
                type="button"
                :aria-label="isDone(task) ? '标记为待处理' : '标记为已完成'"
                :disabled="!hasBtnPermission('task:toggle')"
                @click="toggleTask(task)"
              >
                <el-icon v-if="isDone(task)" :size="13"><Check /></el-icon>
              </button>

              <div class="task-body">
                <div class="task-title-line">
                  <span class="task-priority" :class="'is-' + getPriority(task.priority).tone">
                    {{ getPriority(task.priority).label }}
                  </span>
                  <p class="task-title">{{ task.title }}</p>
                  <span v-if="isDueToday(task)" class="due-today">今日截止</span>
                  <span v-if="isOverdue(task)" class="overdue">已逾期</span>
                </div>
                <p v-if="task.description" class="task-desc">{{ task.description }}</p>
                <div class="task-meta">
                  <span v-if="projectStore.projectName(task.projectId)" class="task-project">
                    {{ projectStore.projectName(task.projectId) }}
                  </span>
                  <span v-if="projectStore.projectName(task.projectId)" class="meta-sep">·</span>
                  <span class="time">{{ timeRange(task) }}</span>
                  <span class="meta-sep">·</span>
                  <el-tag
                    size="small"
                    effect="plain"
                    :type="getStatus(task.status).tagType"
                    class="task-status-tag"
                  >
                    {{ getStatus(task.status).label }}
                  </el-tag>
                </div>
              </div>

              <div v-if="hasBtnPermission('task:edit') || hasBtnPermission('task:delete')" class="task-ops">
                <el-tooltip v-if="hasBtnPermission('task:edit')" content="编辑" placement="top">
                  <button class="row-btn" type="button" @click="openEdit(task)">
                    <el-icon :size="15"><Edit /></el-icon>
                  </button>
                </el-tooltip>
                <el-tooltip v-if="hasBtnPermission('task:delete')" content="删除" placement="top">
                  <button class="row-btn is-danger" type="button" @click="removeTask(task)">
                    <el-icon :size="15"><Delete /></el-icon>
                  </button>
                </el-tooltip>
              </div>
            </article>
          </template>

          <EmptyState
            v-else
            title="当日暂无任务"
            hint="点击右上角的加号，安排这一天的第一件事。"
          />
        </div>
      </section>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建任务' : '编辑任务'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk>
        <el-form-item label="任务标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="100"
            show-word-limit
            placeholder="例如：完成每日任务模块联调"
          />
        </el-form-item>
        <el-form-item label="任务描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            resize="none"
            placeholder="补充任务背景、验收标准或注意事项（选填）"
          />
        </el-form-item>
        <ProjectPicker
          ref="projectPickerRef"
          v-model="form.projectId"
          v-model:bound="projectBound"
        />
        <div class="task-attr-grid">
          <el-form-item label="优先级" prop="priority">
            <el-select v-model="form.priority" placeholder="选择优先级" style="width: 100%">
              <el-option
                v-for="item in priorityOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" placeholder="选择任务状态" style="width: 100%">
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </div>
        <div class="time-grid">
          <el-form-item label="开始时间" prop="startTime">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择开始时间"
              style="width: 100%"
              @change="handleStartChange"
            />
          </el-form-item>
          <el-form-item label="截止时间" prop="deadline">
            <el-date-picker
              v-model="form.deadline"
              type="datetime"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择截止时间"
              style="width: 100%"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTask">
          {{ dialogMode === 'create' ? '创建任务' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.task-split {
  display: grid;
  grid-template-columns: minmax(330px, 0.86fr) minmax(0, 1.4fr);
  gap: 14px;
  align-items: start;
}

/* ---------- 日历 ---------- */
.calendar-card {
  padding: 18px 18px 14px;
}

.cal-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.cal-month {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.cal-sub {
  margin-top: 2px;
  color: var(--wk-text-3);
  font-size: 11.5px;
}

.cal-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cal-today,
.cal-nav {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 29px;
  padding: 0 10px;
  border: 1px solid var(--wk-line);
  border-radius: 7px;
  background: #fff;
  color: #475467;
  font-size: 12px;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease, background-color 0.15s ease;
}

.cal-nav {
  width: 29px;
  padding: 0;
  color: #667085;
}

.cal-today:hover,
.cal-nav:hover {
  border-color: var(--wk-primary-line);
  background: var(--wk-primary-soft);
  color: #0b8b4f;
}

.week-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-top: 16px;
  padding-bottom: 6px;
  color: #98a2b3;
  font-size: 12px;
  text-align: center;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.day-cell {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  height: 53px;
  padding: 0;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: #344054;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.day-cell:hover {
  background: #f5f8f6;
}

.day-cell.is-outside {
  color: #c3cad4;
}

.day-cell.is-today {
  background: var(--wk-primary-soft);
  color: #0b8b4f;
  font-weight: 600;
}

.day-cell.is-selected {
  background: var(--wk-primary);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(18, 183, 106, 0.24);
}

.day-cell.is-selected:hover {
  background: var(--wk-primary-hover);
}

.day-num {
  font-size: 15px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.day-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 5px;
}

.dot {
  display: inline-block;
  width: 4px;
  height: 4px;
  border-radius: 50%;
}

.dot-pending {
  background: var(--wk-primary);
}

.dot-done {
  background: #98a2b3;
}

.day-cell.is-selected .dot {
  background: rgba(255, 255, 255, 0.85);
}

.cal-legend {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--wk-line-soft);
  color: #98a2b3;
  font-size: 11px;
}

.cal-legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

/* ---------- 当日任务 ---------- */
.day-card {
  min-height: 496px;
  display: flex;
  flex-direction: column;
}

.day-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 19px 22px 16px;
  border-bottom: 1px solid var(--wk-line-soft);
}

.day-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.day-title-line h2 {
  font-size: 25px;
  font-weight: 600;
  letter-spacing: -0.5px;
  line-height: 1.2;
}

.day-week {
  color: #667085;
  font-size: 12.5px;
}

.day-sub {
  margin-top: 5px;
  color: var(--wk-text-3);
  font-size: 11.5px;
}

.day-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border: none;
  border-radius: 9px;
  background: var(--wk-primary);
  color: #fff;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.day-add:hover {
  background: var(--wk-primary-hover);
}

.task-list {
  flex: 1;
  padding: 6px 22px 18px;
}

.task-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--wk-line-soft);
  border-left: 3px solid transparent;
}

.task-row.priority-urgent {
  border-left-color: #f04438;
}

.task-row.priority-important {
  border-left-color: #f79009;
}

.task-row.priority-normal {
  border-left-color: #84caff;
}

.task-row.priority-low {
  border-left-color: #d0d5dd;
}

.task-row:last-child {
  border-bottom: none;
}

.task-row.is-done {
  opacity: 0.82;
}

.task-check {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 19px;
  height: 19px;
  margin-top: 2px;
  flex-shrink: 0;
  padding: 0;
  border: 1.5px solid #d0d5dd;
  border-radius: 50%;
  background: #fff;
  color: #fff;
  cursor: pointer;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.task-check:hover {
  border-color: var(--wk-primary);
}

.task-check.checked {
  border-color: var(--wk-primary);
  background: var(--wk-primary);
}

.task-body {
  flex: 1;
  min-width: 0;
}

.task-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.task-title {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
  word-break: break-word;
}

.task-priority {
  flex-shrink: 0;
  padding: 0 6px;
  border: 1px solid transparent;
  border-radius: 4px;
  font-size: 10.5px;
  font-weight: 500;
  line-height: 18px;
}

.task-priority.is-urgent {
  color: #b42318;
  background: #fef3f2;
  border-color: #fecdca;
}

.task-priority.is-important {
  color: #b54708;
  background: #fffaeb;
  border-color: #fedf89;
}

.task-priority.is-normal {
  color: #175cd3;
  background: #eff8ff;
  border-color: #b2ddff;
}

.task-priority.is-low {
  color: #667085;
  background: #f2f4f7;
  border-color: #e4e7ec;
}

.due-today {
  padding: 0 6px;
  color: #b54708;
  font-size: 10.5px;
  line-height: 18px;
  background: #fffaeb;
  border: 1px solid #fedf89;
  border-radius: 4px;
}

.task-row.is-done .task-title {
  color: var(--wk-text-3);
  text-decoration: line-through;
  text-decoration-color: #c5ccd6;
}

.overdue {
  padding: 0 6px;
  border-radius: 4px;
  background: #fef0ef;
  color: #d92d20;
  font-size: 10.5px;
  line-height: 18px;
}

.task-desc {
  display: -webkit-box;
  margin-top: 3px;
  overflow: hidden;
  color: var(--wk-text-2);
  font-size: 12.5px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 5px;
  color: #98a2b3;
  font-size: 11.5px;
  flex-wrap: wrap;
}

.task-project {
  max-width: 170px;
  overflow: hidden;
  padding: 1px 6px;
  color: #0b8b4f;
  font-size: 11px;
  line-height: 1.5;
  background: #ecfdf3;
  border-radius: 4px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time {
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.1px;
}

.meta-sep {
  color: #d0d5dd;
}

.task-status-tag {
  flex-shrink: 0;
  height: 20px;
  padding: 0 6px;
}

.task-ops {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.task-row:hover .task-ops,
.task-row:focus-within .task-ops {
  opacity: 1;
}

.row-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #98a2b3;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.row-btn:hover {
  background: #f2f4f7;
  color: #475467;
}

.row-btn.is-danger:hover {
  background: #fef0ef;
  color: #d92d20;
}

.time-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.task-attr-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

/* ---------- 响应式 ---------- */
@media (max-width: 1040px) {
  .task-split {
    grid-template-columns: 1fr;
  }

  .day-card {
    min-height: 360px;
  }
}

@media (max-width: 768px) {
  .calendar-card {
    padding: 14px 14px 12px;
  }

  .cal-month {
    font-size: 18px;
  }

  .day-cell {
    height: 44px;
  }

  .day-num {
    font-size: 13.5px;
  }

  .week-row {
    margin-top: 12px;
  }

  .day-head {
    padding: 14px 16px 12px;
  }

  .day-title-line h2 {
    font-size: 21px;
  }

  .task-list {
    padding: 2px 16px 14px;
  }

  .task-ops {
    opacity: 1;
  }

  .task-row {
    padding: 12px 0;
  }

  .time-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .task-attr-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .wk-page-actions .el-button {
    height: 34px;
    padding: 0 12px;
  }
}
</style>
