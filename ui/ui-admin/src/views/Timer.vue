<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AlarmClock, Delete, Edit, Plus } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import timerApi from '@/api/timer'
import hasBtnPermission from '@/utils/btnPermission'

const MODE_META = {
  countdown_day: { label: '倒数日', caption: '距离目标日' },
  countup_day: { label: '正数日', caption: '已经过去' },
  precise: { label: '精准倒计时', caption: '距离目标时间' }
}

const MODE_OPTIONS = [
  { value: 'countdown_day', name: '倒数日', desc: '显示还剩几天' },
  { value: 'countup_day', name: '正数日', desc: '显示已经过了几天' },
  { value: 'precise', name: '精准倒计时', desc: '精确到分钟的时分秒' }
]

const LEGACY_KEY = 'wokit-countdowns'

const pad = (num) => String(num).padStart(2, '0')

const parseDate = (value) => {
  const [year, month, day] = String(value).slice(0, 10).split('-').map(Number)
  return new Date(year, month - 1, day)
}

const parseDateTime = (value) => {
  const [year, month, day] = String(value).slice(0, 10).split('-').map(Number)
  const [hour, minute] = String(value).slice(11, 16).split(':').map(Number)
  return new Date(year, month - 1, day, hour, minute, 0, 0)
}

const startOfDay = (date) => new Date(date.getFullYear(), date.getMonth(), date.getDate())

const items = ref([])
const loading = ref(false)
const now = ref(Date.now())
const dialogVisible = ref(false)
const formRef = ref()
const submitting = ref(false)
const editingId = ref(null)
const form = ref({ name: '', mode: 'countdown_day', targetDate: '', targetDateTime: '' })

let timer = null

const dialogTitle = computed(() => (editingId.value ? '编辑计时' : '新建计时'))

const dayDiff = (date) => {
  const target = startOfDay(date)
  const today = startOfDay(new Date(now.value))
  return Math.round((target.getTime() - today.getTime()) / 86400000)
}

const effectiveMode = (item) => {
  if (item.mode === 'precise') return 'precise'
  if (item.mode === 'countdown_day' && dayDiff(parseDate(item.targetTime)) < 0) return 'countup_day'
  return item.mode
}

const modeLabel = (item) => MODE_META[effectiveMode(item)]?.label || '计时'

const dayNumber = (item) => {
  const diff = dayDiff(parseDate(item.targetTime))
  return effectiveMode(item) === 'countup_day' ? Math.max(0, -diff) : Math.max(0, diff)
}

const preciseRemain = (item) => Math.max(0, parseDateTime(item.targetTime).getTime() - now.value)

const preciseDone = (item) => preciseRemain(item) <= 0

const preciseParts = (item) => {
  const remain = preciseRemain(item)
  const days = Math.floor(remain / 86400000)
  const hours = Math.floor((remain % 86400000) / 3600000)
  const minutes = Math.floor((remain % 3600000) / 60000)
  const seconds = Math.floor((remain % 60000) / 1000)
  return [
    { value: String(days), label: '天' },
    { value: pad(hours), label: '小时' },
    { value: pad(minutes), label: '分' },
    { value: pad(seconds), label: '秒' }
  ]
}

const stateText = (item) => {
  const mode = effectiveMode(item)
  if (mode === 'precise') return preciseDone(item) ? '已结束' : '倒计时中'
  if (mode === 'countdown_day') return dayDiff(parseDate(item.targetTime)) === 0 ? '今天到期' : '进行中'
  return '已过目标日'
}

const targetText = (item) => {
  if (effectiveMode(item) === 'precise') {
    return `目标 ${String(item.targetTime).slice(0, 16)}`
  }
  return `目标日 ${String(item.targetTime).slice(0, 10)}`
}

const sortedItems = computed(() => [...items.value].sort((a, b) => {
  const modeA = effectiveMode(a)
  const modeB = effectiveMode(b)
  const rank = (mode, item) => {
    if (mode === 'precise') return preciseDone(item) ? 3 : 0
    if (mode === 'countdown_day') return 1
    return 2
  }
  const rankA = rank(modeA, a)
  const rankB = rank(modeB, b)
  if (rankA !== rankB) return rankA - rankB
  if (rankA === 0) return preciseRemain(a) - preciseRemain(b)
  if (rankA === 1) return dayDiff(parseDate(a.targetTime)) - dayDiff(parseDate(b.targetTime))
  if (rankA === 2) return dayDiff(parseDate(a.targetTime)) - dayDiff(parseDate(b.targetTime))
  return parseDateTime(b.targetTime).getTime() - parseDateTime(a.targetTime).getTime()
}))

const runningCount = computed(() => items.value.filter((item) => {
  const mode = effectiveMode(item)
  if (mode === 'countdown_day') return true
  return mode === 'precise' && !preciseDone(item)
}).length)

const loadItems = async () => {
  loading.value = true
  try {
    const result = await timerApi.list()
    if (result.code === 1) {
      items.value = result.data || []
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    loading.value = false
  }
}

const migrateLegacyItems = async () => {
  let legacyItems = []
  try {
    const stored = JSON.parse(localStorage.getItem(LEGACY_KEY) || '[]')
    legacyItems = Array.isArray(stored) ? stored : []
  } catch (error) {
    return
  }
  const validItems = legacyItems.filter((item) => item && item.name && item.date)
  if (!validItems.length) {
    if (legacyItems.length) localStorage.removeItem(LEGACY_KEY)
    return
  }
  let migrated = 0
  for (const item of validItems) {
    const mode = dayDiff(parseDate(item.date)) < 0 ? 'countup_day' : 'countdown_day'
    const result = await timerApi.add({
      name: String(item.name).slice(0, 30),
      mode,
      targetTime: `${item.date} 00:00:00`
    })
    if (result.code === 1) migrated += 1
  }
  if (migrated === validItems.length) {
    localStorage.removeItem(LEGACY_KEY)
    ElMessage.success(`已迁移 ${migrated} 条本地计时记录`)
  }
}

const openCreate = () => {
  editingId.value = null
  form.value = { name: '', mode: 'countdown_day', targetDate: '', targetDateTime: '' }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openEdit = (item) => {
  const mode = effectiveMode(item)
  editingId.value = item.id
  form.value = {
    name: item.name,
    mode,
    targetDate: mode === 'precise' ? '' : String(item.targetTime).slice(0, 10),
    targetDateTime: mode === 'precise' ? String(item.targetTime).slice(0, 16) : ''
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const selectMode = (mode) => {
  if (form.value.mode === mode) return
  form.value.mode = mode
  form.value.targetDate = ''
  form.value.targetDateTime = ''
  nextTick(() => formRef.value?.clearValidate())
}

const disabledDayDate = (date) => {
  const day = startOfDay(date).getTime()
  const today = startOfDay(new Date()).getTime()
  if (form.value.mode === 'countdown_day') return day < today
  if (form.value.mode === 'countup_day') return day > today
  return false
}

const disabledPreciseDate = (date) => startOfDay(date).getTime() < startOfDay(new Date()).getTime()

const validateDayTarget = (rule, value, callback) => {
  if (form.value.mode === 'precise') {
    callback()
    return
  }
  if (!form.value.targetDate) {
    callback(new Error('请选择目标日期'))
    return
  }
  callback()
}

const validatePreciseTarget = (rule, value, callback) => {
  if (form.value.mode !== 'precise') {
    callback()
    return
  }
  if (!form.value.targetDateTime) {
    callback(new Error('请选择目标时间'))
    return
  }
  callback()
}

const rules = {
  name: [
    { required: true, message: '请填写计时名称', trigger: 'blur' },
    { max: 30, message: '名称不能超过 30 个字', trigger: 'blur' }
  ],
  targetDate: [{ validator: validateDayTarget, trigger: 'change' }],
  targetDateTime: [{ validator: validatePreciseTarget, trigger: 'change' }]
}

const submitForm = async () => {
  if (!formRef.value || submitting.value) return
  await formRef.value.validate()
  const targetTime = form.value.mode === 'precise'
    ? `${form.value.targetDateTime.slice(0, 16)}:00`
    : `${form.value.targetDate} 00:00:00`
  const payload = {
    name: form.value.name.trim(),
    mode: form.value.mode,
    targetTime
  }
  submitting.value = true
  try {
    const result = editingId.value
      ? await timerApi.update(editingId.value, payload)
      : await timerApi.add(payload)
    if (result.code === 1) {
      ElMessage.success(result.msg)
      dialogVisible.value = false
      await loadItems()
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    submitting.value = false
  }
}

const removeItem = async (item) => {
  await ElMessageBox.confirm(`确定删除「${item.name}」这个计时吗？删除后不可恢复。`, '删除计时', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const result = await timerApi.remove(item.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    await loadItems()
  } else {
    ElMessage.error(result.msg)
  }
}

onMounted(async () => {
  await migrateLegacyItems()
  loadItems()
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <section class="wk-page timer-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">计时</h1>
        <p class="wk-page-desc">倒数日、正数日与精准倒计时，盯住每一个重要节点。</p>
      </div>
      <div class="wk-page-actions">
        <el-button v-if="hasBtnPermission('timer:add')" type="primary" :icon="Plus" @click="openCreate">新建计时</el-button>
      </div>
    </div>

    <section class="wk-card timer-panel">
      <div class="wk-panel-head">
        <div class="head-left">
          <span class="head-icon"><el-icon :size="17"><AlarmClock /></el-icon></span>
          <div>
            <p class="wk-panel-title">我的计时</p>
            <p class="wk-panel-sub">数据保存在平台账号中</p>
          </div>
        </div>
        <p class="head-count">进行中 {{ runningCount }} 个 · 共 {{ items.length }} 个</p>
      </div>

      <div v-loading="loading" class="timer-body">
        <div v-if="sortedItems.length" class="timer-grid">
          <article
            v-for="item in sortedItems"
            :key="item.id"
            class="timer-card"
            :class="[`mode-${effectiveMode(item)}`, { 'is-done': preciseDone(item) }]"
          >
            <div class="cd-top">
              <span class="mode-tag">{{ modeLabel(item) }}</span>
              <div v-if="hasBtnPermission('timer:edit') || hasBtnPermission('timer:delete')" class="cd-actions">
                <button v-if="hasBtnPermission('timer:edit')" class="cd-icon-btn" type="button" aria-label="编辑" @click="openEdit(item)">
                  <el-icon :size="14"><Edit /></el-icon>
                </button>
                <button v-if="hasBtnPermission('timer:delete')" class="cd-icon-btn is-danger" type="button" aria-label="删除" @click="removeItem(item)">
                  <el-icon :size="14"><Delete /></el-icon>
                </button>
              </div>
            </div>

            <template v-if="effectiveMode(item) === 'precise'">
              <p v-if="preciseDone(item)" class="cd-done">已到时间</p>
              <div v-else class="precise-parts">
                <div v-for="part in preciseParts(item)" :key="part.label" class="precise-part">
                  <span class="precise-num">{{ part.value }}</span>
                  <span class="precise-unit">{{ part.label }}</span>
                </div>
              </div>
              <p class="cd-caption">
                {{ preciseDone(item) ? '目标时间已到' : MODE_META.precise.caption }}
              </p>
            </template>

            <template v-else>
              <p class="cd-value">
                {{ dayNumber(item) }}<span class="cd-unit">天</span>
              </p>
              <p class="cd-caption">
                {{ effectiveMode(item) === 'countup_day' ? MODE_META.countup_day.caption : MODE_META.countdown_day.caption }}
              </p>
            </template>

            <p class="cd-name" :title="item.name">{{ item.name }}</p>
            <div class="cd-foot">
              <span class="cd-date">{{ targetText(item) }}</span>
              <span class="cd-state">{{ stateText(item) }}</span>
            </div>
          </article>
        </div>

        <EmptyState
          v-else
          title="还没有计时"
          hint="新建一个倒数日、正数日或精准倒计时，让重要节点清晰可见。"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk>
        <el-form-item label="计时名称" prop="name">
          <el-input v-model="form.name" maxlength="30" show-word-limit placeholder="例如：项目上线、入职纪念日" />
        </el-form-item>

        <el-form-item label="模式">
          <div class="mode-switch">
            <button
              v-for="option in MODE_OPTIONS"
              :key="option.value"
              type="button"
              class="mode-option"
              :class="{ 'is-active': form.mode === option.value }"
              @click="selectMode(option.value)"
            >
              <span class="mode-option-name">{{ option.name }}</span>
              <span class="mode-option-desc">{{ option.desc }}</span>
            </button>
          </div>
        </el-form-item>

        <el-form-item v-if="form.mode !== 'precise'" label="目标日期" prop="targetDate">
          <el-date-picker
            v-model="form.targetDate"
            type="date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :placeholder="form.mode === 'countup_day' ? '选择已经过去的日期' : '选择未来的目标日期'"
            :disabled-date="disabledDayDate"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item v-else label="目标时间（精确到分钟）" prop="targetDateTime">
          <el-date-picker
            v-model="form.targetDateTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            placeholder="选择目标日期和时间"
            :disabled-date="disabledPreciseDate"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ editingId ? '保存修改' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.timer-panel {
  min-height: 320px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.head-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--wk-primary-soft);
  color: #0b8b4f;
}

.head-count {
  color: var(--wk-text-3);
  font-size: 12px;
}

.timer-body {
  min-height: 220px;
  padding: 16px 22px 22px;
}

.timer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 12px;
}

.timer-card {
  position: relative;
  padding: 14px 16px 15px;
  border: 1px solid var(--wk-line);
  border-radius: 12px;
  background: #fff;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.timer-card:hover {
  border-color: #d5e3da;
  box-shadow: 0 4px 14px rgba(16, 24, 40, 0.05);
}

.cd-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 26px;
}

.mode-tag {
  padding: 0 8px;
  border-radius: 5px;
  font-size: 11px;
  line-height: 21px;
}

.timer-card.mode-countdown_day .mode-tag {
  background: var(--wk-primary-soft);
  color: #0b8b4f;
}

.timer-card.mode-countup_day .mode-tag {
  background: #eff8ff;
  color: #175cd3;
}

.timer-card.mode-precise .mode-tag {
  background: #fffaeb;
  color: #b54708;
}

.cd-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.timer-card:hover .cd-actions,
.timer-card:focus-within .cd-actions {
  opacity: 1;
}

.cd-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #c5ccd6;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.cd-icon-btn:hover {
  background: #f2f4f7;
  color: #475467;
}

.cd-icon-btn.is-danger:hover {
  background: #fef0ef;
  color: #d92d20;
}

.cd-value {
  margin-top: 8px;
  color: #0b8b4f;
  font-size: 40px;
  font-weight: 600;
  line-height: 1.05;
  letter-spacing: -1.2px;
  font-variant-numeric: tabular-nums;
}

.timer-card.mode-countup_day .cd-value {
  color: #175cd3;
}

.cd-unit {
  margin-left: 4px;
  color: #667085;
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0;
}

.cd-caption {
  margin-top: 2px;
  color: #98a2b3;
  font-size: 11px;
}

.precise-parts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 4px;
  margin-top: 10px;
  padding: 9px 0 7px;
  border-radius: 8px;
  background: #fffcf5;
}

.precise-part {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 0;
}

.precise-num {
  color: #b54708;
  font-size: 21px;
  font-weight: 600;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.precise-unit {
  margin-top: 2px;
  color: #98a2b3;
  font-size: 10px;
  white-space: nowrap;
}

.cd-done {
  margin: 14px 0 10px;
  color: #98a2b3;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0;
}

.cd-name {
  margin-top: 10px;
  overflow: hidden;
  color: #344054;
  font-size: 13.5px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cd-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 3px;
}

.cd-date {
  overflow: hidden;
  color: #98a2b3;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cd-state {
  flex-shrink: 0;
  color: #667085;
  font-size: 10.5px;
}

.timer-card.mode-precise.is-done .cd-state {
  color: #98a2b3;
}

.mode-switch {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.mode-option {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 9px 10px;
  border: 1px solid var(--wk-border);
  border-radius: 8px;
  background: #fff;
  color: #475467;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.mode-option:hover {
  border-color: var(--wk-primary-line);
}

.mode-option.is-active {
  border-color: var(--wk-primary);
  background: var(--wk-primary-soft);
}

.mode-option-name {
  color: #1d2939;
  font-size: 12.5px;
  font-weight: 500;
}

.mode-option-desc {
  color: #98a2b3;
  font-size: 10.5px;
  line-height: 1.35;
}

.mode-option.is-active .mode-option-name {
  color: #0b8b4f;
}

@media (max-width: 768px) {
  .wk-panel-head {
    padding: 14px 14px 12px;
  }

  .timer-body {
    padding: 12px 14px 16px;
  }

  .timer-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .timer-card {
    padding: 11px 12px 12px;
  }

  .cd-value {
    font-size: 34px;
  }

  .cd-actions {
    opacity: 1;
  }

  .precise-num {
    font-size: 18px;
  }

  .precise-unit {
    font-size: 9.5px;
  }

  .head-count {
    font-size: 11px;
  }
}

@media (max-width: 560px) {
  .timer-grid {
    grid-template-columns: 1fr;
  }

  .mode-switch {
    grid-template-columns: 1fr;
  }

  .mode-option {
    flex-direction: row;
    align-items: baseline;
    justify-content: space-between;
    gap: 10px;
  }
}
</style>
