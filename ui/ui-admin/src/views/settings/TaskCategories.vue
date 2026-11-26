<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import taskCategoryApi from '@/api/taskCategory'
import EmptyState from '@/components/EmptyState.vue'
import hasBtnPermission from '@/utils/btnPermission'

const loading = ref(false)
const categories = ref([])
const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)
const formRef = ref()
const submitting = ref(false)
const form = ref({ name: '', color: '#1677ff', sort: 0 })

const presetColors = [
  '#1677ff', '#f04438', '#f79009', '#7a5af8',
  '#0e9384', '#667085', '#067647', '#98a2b3'
]

const rules = {
  name: [
    { required: true, message: '请填写分类名称', trigger: 'blur' },
    { max: 30, message: '分类名称不能超过 30 个字', trigger: 'blur' }
  ],
  color: [{ required: true, message: '请选择分类颜色', trigger: 'change' }],
  sort: [{ required: true, message: '请填写排序值', trigger: 'change' }]
}

const sortedCategories = computed(() =>
  [...categories.value].sort((a, b) => Number(a.sort || 0) - Number(b.sort || 0) || Number(a.id) - Number(b.id))
)

const load = async () => {
  loading.value = true
  try {
    const result = await taskCategoryApi.list()
    if (result.code === 1) {
      categories.value = result.data || []
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  dialogMode.value = 'create'
  editingId.value = null
  form.value = { name: '', color: '#1677ff', sort: nextSort() }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openEdit = (category) => {
  dialogMode.value = 'edit'
  editingId.value = category.id
  form.value = {
    name: category.name,
    color: category.color || '#1677ff',
    sort: Number(category.sort || 0)
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const nextSort = () => {
  if (!categories.value.length) return 0
  return Math.max(...categories.value.map((item) => Number(item.sort || 0))) + 1
}

const submit = async () => {
  if (!formRef.value || submitting.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const result = editingId.value
      ? await taskCategoryApi.update(editingId.value, form.value)
      : await taskCategoryApi.add(form.value)
    if (result.code === 1) {
      ElMessage.success(result.msg)
      dialogVisible.value = false
      await load()
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    submitting.value = false
  }
}

const remove = async (category) => {
  if (Number(category.usageCount || 0) > 0) {
    ElMessage.warning(`「${category.name}」已被 ${category.usageCount} 条任务使用，只能修改不能删除`)
    return
  }
  await ElMessageBox.confirm(`确定删除任务分类「${category.name}」吗？`, '删除任务分类', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const result = await taskCategoryApi.remove(category.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    await load()
  } else {
    ElMessage.error(result.msg)
  }
}

onMounted(load)
</script>

<template>
  <section class="settings-page task-category-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">任务分类</h1>
        <p class="wk-page-desc">用于归类项目任务，例如新需求、BUG 修复、性能优化。分类由管理员统一维护。</p>
      </div>
      <div class="wk-page-actions">
        <el-button v-if="hasBtnPermission('task:category:add')" type="primary" :icon="Plus" @click="openCreate">新建分类</el-button>
      </div>
    </div>

    <section class="wk-card category-table-card">
      <el-table v-loading="loading" :data="sortedCategories" class="wk-table">
        <el-table-column label="分类名称" min-width="180">
          <template #default="{ row }">
            <div class="category-name-cell">
              <span class="category-color-dot" :style="{ backgroundColor: row.color }"></span>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="颜色" width="140">
          <template #default="{ row }">
            <div class="color-cell">
              <span class="color-preview" :style="{ backgroundColor: row.color }"></span>
              <span>{{ row.color }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="90">
          <template #default="{ row }">{{ row.sort }}</template>
        </el-table-column>
        <el-table-column label="使用情况" min-width="160">
          <template #default="{ row }">
            <span v-if="row.usageCount > 0" class="usage-text is-used">已被 {{ row.usageCount }} 条任务使用</span>
            <span v-else class="usage-text">暂未被使用，可删除</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="hasBtnPermission('task:category:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
              <el-button
                v-if="hasBtnPermission('task:category:delete')"
                link
                type="danger"
                :icon="Delete"
                :disabled="row.usageCount > 0"
                @click="remove(row)"
              >删除</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState compact title="还没有任务分类" hint="新建第一个分类，项目任务创建时就能选择它。" />
        </template>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建任务分类' : '编辑任务分类'"
      width="460px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" maxlength="30" show-word-limit placeholder="例如：新需求、BUG 修复、性能优化" />
        </el-form-item>
        <el-form-item label="标签颜色" prop="color">
          <div class="color-field">
            <el-color-picker v-model="form.color" :predefine="presetColors" />
            <div class="color-presets">
              <button
                v-for="color in presetColors"
                :key="color"
                type="button"
                class="color-swatch"
                :class="{ 'is-active': form.color === color }"
                :style="{ backgroundColor: color }"
                :aria-label="color"
                @click="form.color = color"
              ></button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" style="width: 100%" />
          <p class="dialog-hint-soft">数字越小越靠前，任务创建时的分类下拉按此排序。</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ dialogMode === 'create' ? '创建分类' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.category-table-card {
  overflow: hidden;
}

.category-name-cell,
.color-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-color-dot,
.color-preview {
  display: inline-block;
  width: 9px;
  height: 9px;
  flex-shrink: 0;
  border-radius: 50%;
}

.color-preview {
  width: 15px;
  height: 15px;
  border-radius: 4px;
}

.color-cell span:last-child {
  color: var(--wk-text-3);
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
}

.usage-text {
  color: var(--wk-text-3);
  font-size: 11.5px;
}

.usage-text.is-used {
  color: #b54708;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.table-actions :deep(.el-button) {
  margin-left: 0;
  padding: 3px 4px;
}

.color-field {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.color-presets {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.color-swatch {
  width: 21px;
  height: 21px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 5px;
  cursor: pointer;
}

.color-swatch.is-active {
  border-color: #101828;
  box-shadow: 0 0 0 2px #fff inset;
}

.dialog-hint-soft {
  margin-top: 3px;
  color: var(--wk-text-3);
  font-size: 11.5px;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .color-field {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
