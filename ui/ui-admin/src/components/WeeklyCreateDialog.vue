<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { DocumentChecked } from '@element-plus/icons-vue'
import ProjectPicker from '@/components/ProjectPicker.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  weekStart: { type: String, default: '' },
  initialTaskContent: { type: String, default: '' },
  initialProjectId: { type: [Number, String], default: null },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'submit', 'closed'])

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

const currentWeekStart = computed(() => props.weekStart || mondayOf(toDateStr(new Date())))
const weekInfo = computed(() => isoWeekInfo(currentWeekStart.value))
const weekEnd = computed(() => addDays(currentWeekStart.value, 6))
const weekLabel = computed(() => `${weekInfo.value.year} 年第 ${weekInfo.value.week} 周`)

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const formRef = ref()
const projectPickerRef = ref()
const projectBound = ref(false)
const form = ref({
  taskContent: '',
  resolvedProblem: '',
  taskResult: '',
  problem: '',
  output: '',
  extra: '',
  projectId: null
})

const rules = {
  taskContent: [
    { required: true, message: '请填写任务内容', trigger: 'blur' },
    { max: 200, message: '任务内容不能超过 200 个字', trigger: 'blur' }
  ],
  resolvedProblem: [{ max: 500, message: '已解决问题及解决方式不能超过 500 个字', trigger: 'blur' }],
  taskResult: [{ max: 500, message: '结果不能超过 500 个字', trigger: 'blur' }],
  problem: [{ max: 500, message: '仍存在的问题不能超过 500 个字', trigger: 'blur' }],
  output: [{ max: 500, message: '产出不能超过 500 个字', trigger: 'blur' }],
  extra: [{ max: 500, message: '其他补充不能超过 500 个字', trigger: 'blur' }]
}

const resetForm = () => {
  projectBound.value = Boolean(props.initialProjectId)
  form.value = {
    taskContent: (props.initialTaskContent || '').slice(0, 200),
    resolvedProblem: '',
    taskResult: '',
    problem: '',
    output: '',
    extra: '',
    projectId: props.initialProjectId || null
  }
  nextTick(() => formRef.value?.clearValidate())
}

const submitForm = async () => {
  if (!formRef.value || props.loading) return
  if (!projectPickerRef.value?.validate()) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  emit('submit', { ...form.value, weekStart: currentWeekStart.value })
}

watch(
  () => props.modelValue,
  (value) => {
    if (value) resetForm()
  }
)
</script>

<template>
  <el-dialog
    v-model="visible"
    title="新建周报记录"
    width="680px"
    :close-on-click-modal="false"
    :close-on-press-escape="!loading"
    :show-close="!loading"
    @closed="emit('closed')"
  >
    <div class="week-note">
      <el-icon :size="14"><DocumentChecked /></el-icon>
      <span>归属：{{ weekLabel }}（{{ currentWeekStart }} ~ {{ weekEnd }}）</span>
    </div>

    <ProjectPicker
      ref="projectPickerRef"
      v-model="form.projectId"
      v-model:bound="projectBound"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk>
      <el-form-item label="任务内容" prop="taskContent">
        <el-input
          v-model="form.taskContent"
          type="textarea"
          :rows="2"
          maxlength="200"
          show-word-limit
          resize="vertical"
          placeholder="本周完成了什么任务"
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
      <el-button :disabled="loading" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submitForm">保存记录</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
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

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .week-note {
    margin-top: 0;
  }
}
</style>
