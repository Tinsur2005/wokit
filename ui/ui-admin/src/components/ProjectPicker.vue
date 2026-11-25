<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useProjectStore } from '@/store/project'

const props = defineProps({
  modelValue: { type: [Number, String], default: null },
  bound: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'update:bound'])

const projectStore = useProjectStore()
const selectError = ref(false)

const bound = computed({
  get: () => props.bound,
  set: (value) => {
    emit('update:bound', value)
    if (!value) {
      selectError.value = false
      emit('update:modelValue', null)
    }
  }
})

const projectOptions = computed(() =>
  [...projectStore.projects].sort((a, b) => {
    const timeCompare = String(b.createTime || '').localeCompare(String(a.createTime || ''))
    return timeCompare || Number(b.id) - Number(a.id)
  })
)

const selectedProjectId = computed({
  get: () => props.modelValue ?? null,
  set: (value) => {
    selectError.value = false
    emit('update:modelValue', value ?? null)
  }
})

watch(
  () => props.modelValue,
  (value) => {
    if (value != null) {
      selectError.value = false
      emit('update:bound', true)
    }
  }
)

const validate = () => {
  if (!props.bound || props.modelValue != null) return true
  selectError.value = true
  ElMessage.warning('已勾选绑定项目，请选择项目；如不需要绑定，请取消勾选')
  return false
}

defineExpose({ validate })

onMounted(() => {
  projectStore.load()
})
</script>

<template>
  <div class="project-picker">
    <div class="project-picker-head">
      <el-checkbox v-model="bound" :disabled="!projectOptions.length">绑定项目</el-checkbox>
      <span v-if="!projectOptions.length" class="project-picker-hint">暂无项目，请先到项目管理创建</span>
      <span v-else class="project-picker-hint" :class="{ 'is-required': bound }">
        {{ bound ? '已勾选，保存前必须选择项目' : '可选，不绑定不影响保存' }}
      </span>
    </div>

    <el-select
      v-if="bound"
      v-model="selectedProjectId"
      class="project-select"
      :class="{ 'is-error': selectError }"
      filterable
      clearable
      placeholder="搜索或选择项目"
    >
      <el-option
        v-for="project in projectOptions"
        :key="project.id"
        :label="project.name"
        :value="project.id"
      >
        <span class="project-option-name">{{ project.name }}</span>
        <span class="project-option-category">{{ project.categoryName }}</span>
      </el-option>
    </el-select>
    <p v-if="selectError" class="project-picker-error">请选择一个项目，或取消勾选绑定项目</p>
  </div>
</template>

<style scoped>
.project-picker {
  margin-bottom: 18px;
}

.project-picker-head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 28px;
}

.project-picker-head :deep(.el-checkbox__label) {
  color: var(--wk-text-2);
  font-size: 13px;
}

.project-picker-hint {
  color: var(--wk-text-3);
  font-size: 11.5px;
}

.project-picker-hint.is-required {
  color: var(--wk-text-2);
}

.project-select {
  width: 100%;
  margin-top: 8px;
}

.project-select.is-error :deep(.el-select__wrapper) {
  box-shadow: 0 0 0 1px #f04438 inset;
}

.project-picker-error {
  margin: 6px 0 0;
  color: #f04438;
  font-size: 12px;
  line-height: 1.4;
}

.project-option-name {
  color: var(--wk-text);
}
.project-option-category {
  margin-left: 8px;
}
</style>
