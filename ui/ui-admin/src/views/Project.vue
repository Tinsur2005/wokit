<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, FolderOpened, Plus, Search } from '@element-plus/icons-vue'
import projectApi from '@/api/project'
import EmptyState from '@/components/EmptyState.vue'
import { useProjectStore } from '@/store/project'

const projectStore = useProjectStore()

const keyword = ref('')
const categoryFilter = ref(null)
const loading = ref(false)

const categoryDialogVisible = ref(false)
const categoryDialogMode = ref('create')
const editingCategoryId = ref(null)
const categoryFormRef = ref()
const categoryForm = ref({ name: '', parentId: null })
const categorySubmitting = ref(false)

const projectDialogVisible = ref(false)
const projectDialogMode = ref('create')
const editingProjectId = ref(null)
const projectFormRef = ref()
const projectForm = ref({ name: '', categoryId: null })
const projectSubmitting = ref(false)

const categoryRules = {
  name: [
    { required: true, message: '请填写分类名称', trigger: 'blur' },
    { max: 20, message: '分类名称不能超过 20 个字', trigger: 'blur' }
  ]
}

const projectRules = {
  name: [
    { required: true, message: '请填写项目名称', trigger: 'blur' },
    { max: 60, message: '项目名称不能超过 60 个字', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择项目分类', trigger: 'change' }]
}

const categories = computed(() => projectStore.categories)

const categoryTreeRows = computed(() => {
  const categoryMap = new Map()
  categories.value.forEach((category) => {
    categoryMap.set(Number(category.id), {
      ...category,
      level: 0,
      totalProjectCount: 0
    })
  })

  const childrenMap = new Map()
  const roots = []
  categories.value.forEach((category) => {
    const item = categoryMap.get(Number(category.id))
    const parentId = category.parentId == null ? null : Number(category.parentId)
    if (parentId && categoryMap.has(parentId)) {
      if (!childrenMap.has(parentId)) childrenMap.set(parentId, [])
      childrenMap.get(parentId).push(item)
    } else {
      roots.push(item)
    }
  })

  const rows = []
  const visited = new Set()
  const walk = (item, level) => {
    if (!item || visited.has(item.id)) return
    visited.add(item.id)
    rows.push({ ...item, level })
    ;(childrenMap.get(item.id) || []).forEach((child) => walk(child, level + 1))
  }
  roots.forEach((item) => walk(item, 0))
  categories.value.forEach((category) => {
    const item = categoryMap.get(Number(category.id))
    if (item && !visited.has(item.id)) walk(item, 0)
  })

  const rowMap = new Map(rows.map((row) => [Number(row.id), row]))
  categories.value.forEach((category) => {
    let currentId = Number(category.id)
    let guard = 0
    while (currentId && guard < 50 && rowMap.has(currentId)) {
      const row = rowMap.get(currentId)
      row.totalProjectCount += Number(category.projectCount || 0)
      currentId = row.parentId == null ? 0 : Number(row.parentId)
      guard += 1
    }
  })

  return rows
})

const collectCategoryAndDescendants = (rootId) => {
  const result = new Set([Number(rootId)])
  const queue = [Number(rootId)]
  while (queue.length) {
    const currentId = queue.shift()
    categories.value.forEach((category) => {
      if (Number(category.parentId) === currentId && !result.has(Number(category.id))) {
        result.add(Number(category.id))
        queue.push(Number(category.id))
      }
    })
  }
  return result
}

const editingCategoryDescendantIds = computed(() => {
  if (!editingCategoryId.value) return new Set()
  return collectCategoryAndDescendants(editingCategoryId.value)
})

const parentCategoryOptions = computed(() =>
  categoryTreeRows.value.filter((category) => !editingCategoryDescendantIds.value.has(Number(category.id)))
)

const categoryFilterIds = computed(() => {
  if (!categoryFilter.value) return null
  return collectCategoryAndDescendants(categoryFilter.value)
})

const projects = computed(() =>
  [...projectStore.projects].sort((a, b) => {
    const timeCompare = String(b.createTime || '').localeCompare(String(a.createTime || ''))
    return timeCompare || Number(b.id) - Number(a.id)
  })
)

const filteredProjects = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return projects.value.filter((project) => {
    const matchCategory = !categoryFilterIds.value || categoryFilterIds.value.has(Number(project.categoryId))
    const matchKeyword = !search || project.name.toLowerCase().includes(search)
    return matchCategory && matchKeyword
  })
})

const categoryName = computed(() => {
  if (!categoryFilter.value) return '全部分类'
  const category = categories.value.find((item) => Number(item.id) === Number(categoryFilter.value))
  return category ? category.path : '全部分类'
})

const formatDate = (value) => (value ? value.slice(0, 10) : '--')

const load = async () => {
  loading.value = true
  try {
    await projectStore.refresh()
  } finally {
    loading.value = false
  }
}

const openCategoryCreate = () => {
  categoryDialogMode.value = 'create'
  editingCategoryId.value = null
  categoryForm.value = { name: '', parentId: null }
  categoryDialogVisible.value = true
  nextTick(() => categoryFormRef.value?.clearValidate())
}

const openCategoryEdit = (category) => {
  categoryDialogMode.value = 'edit'
  editingCategoryId.value = category.id
  categoryForm.value = { name: category.name, parentId: category.parentId ?? null }
  categoryDialogVisible.value = true
  nextTick(() => categoryFormRef.value?.clearValidate())
}

const submitCategory = async () => {
  if (!categoryFormRef.value || categorySubmitting.value) return
  await categoryFormRef.value.validate()
  categorySubmitting.value = true
  try {
    const result = editingCategoryId.value
      ? await projectApi.updateCategory(editingCategoryId.value, categoryForm.value)
      : await projectApi.addCategory(categoryForm.value)
    if (result.code === 1) {
      ElMessage.success(result.msg)
      categoryDialogVisible.value = false
      await projectStore.refresh()
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    categorySubmitting.value = false
  }
}

const removeCategory = async (category) => {
  const childCount = categories.value.filter(
    (item) => Number(item.parentId) === Number(category.id)
  ).length
  if (childCount > 0) {
    ElMessage.warning(`「${category.name}」下还有 ${childCount} 个子分类，请先移动或删除这些子分类`)
    return
  }
  if (category.projectCount > 0) {
    ElMessage.warning(`「${category.name}」下还有 ${category.projectCount} 个项目，请先移动或删除这些项目`)
    return
  }
  await ElMessageBox.confirm(`确定删除分类「${category.name}」吗？`, '删除项目分类', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const result = await projectApi.removeCategory(category.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    if (Number(categoryFilter.value) === Number(category.id)) {
      categoryFilter.value = null
    }
    await projectStore.refresh()
  } else {
    ElMessage.error(result.msg)
  }
}

const openProjectCreate = () => {
  projectDialogMode.value = 'create'
  editingProjectId.value = null
  projectForm.value = {
    name: '',
    categoryId: categoryFilter.value || categories.value[0]?.id || null
  }
  projectDialogVisible.value = true
  nextTick(() => projectFormRef.value?.clearValidate())
}

const openProjectEdit = (project) => {
  projectDialogMode.value = 'edit'
  editingProjectId.value = project.id
  projectForm.value = {
    name: project.name,
    categoryId: project.categoryId
  }
  projectDialogVisible.value = true
  nextTick(() => projectFormRef.value?.clearValidate())
}

const submitProject = async () => {
  if (!projectFormRef.value || projectSubmitting.value) return
  await projectFormRef.value.validate()
  projectSubmitting.value = true
  try {
    const result = editingProjectId.value
      ? await projectApi.update(editingProjectId.value, projectForm.value)
      : await projectApi.add(projectForm.value)
    if (result.code === 1) {
      ElMessage.success(result.msg)
      projectDialogVisible.value = false
      await projectStore.refresh()
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    projectSubmitting.value = false
  }
}

const removeProject = async (project) => {
  await ElMessageBox.confirm(
    `确定删除项目「${project.name}」吗？已绑定该项目的任务、周报和工时会自动解除绑定。`,
    '删除项目',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  )
  const result = await projectApi.remove(project.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    await projectStore.refresh()
  } else {
    ElMessage.error(result.msg)
  }
}

onMounted(load)
</script>

<template>
  <section class="wk-page project-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">项目管理</h1>
        <p class="wk-page-desc">先按分类整理项目，再把任务、周报和工时归到具体项目上。</p>
      </div>
      <div class="wk-page-actions">
        <el-button type="primary" :icon="Plus" @click="openProjectCreate">新建项目</el-button>
      </div>
    </div>

    <div class="project-layout">
      <section class="wk-card category-card">
        <div class="category-head">
          <div>
            <h2 class="panel-title">项目分类</h2>
            <p class="panel-sub">{{ categories.length }} 个分类</p>
          </div>
          <el-button link type="primary" :icon="Plus" @click="openCategoryCreate">新建</el-button>
        </div>

        <div v-loading="loading" class="category-list">
          <div
            v-for="category in categoryTreeRows"
            :key="category.id"
            class="category-row"
            :style="{ paddingLeft: `${10 + category.level * 16}px` }"
            :title="category.path"
          >
            <span v-if="category.level" class="category-tree-line"></span>
            <span class="category-badge">{{ category.name.slice(0, 1) }}</span>
            <div class="category-info">
              <p class="category-name">{{ category.name }}</p>
              <p class="category-count">{{ category.totalProjectCount }} 个项目</p>
            </div>
            <div class="category-actions">
              <el-button link type="primary" :icon="Edit" aria-label="编辑分类" @click="openCategoryEdit(category)" />
              <el-button link type="danger" :icon="Delete" aria-label="删除分类" @click="removeCategory(category)" />
            </div>
          </div>

          <EmptyState
            v-if="!categories.length && !loading"
            compact
            title="还没有项目分类"
            hint="先创建一个分类，例如「工作」「学习」，还可以在分类下继续建子分类。"
          />
        </div>
      </section>

      <section class="wk-card project-list-card">
        <div class="project-list-head">
          <div class="project-title-block">
            <h2 class="panel-title">项目列表</h2>
            <p class="panel-sub">{{ categoryName }} · {{ filteredProjects.length }} 个项目</p>
          </div>
          <div class="project-toolbar">
            <el-input
              v-model="keyword"
              class="project-search"
              clearable
              placeholder="搜索项目名称"
              :prefix-icon="Search"
            />
            <el-select
              v-model="categoryFilter"
              class="category-filter"
              clearable
              filterable
              placeholder="全部分类"
            >
              <el-option
                v-for="category in categoryTreeRows"
                :key="category.id"
                :label="category.path || category.name"
                :value="category.id"
              />
            </el-select>
          </div>
        </div>

        <el-table v-loading="loading" :data="filteredProjects" class="wk-table project-table" row-key="id">
          <el-table-column label="项目名称" min-width="220">
            <template #default="{ row }">
              <div class="project-name-cell">
                <span class="project-icon"><el-icon :size="15"><FolderOpened /></el-icon></span>
                <span class="project-name">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="分类" min-width="130">
            <template #default="{ row }">
              <span class="project-category-tag">{{ row.categoryName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="140">
            <template #default="{ row }">
              <span class="project-date">{{ formatDate(row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" :icon="Edit" @click="openProjectEdit(row)">编辑</el-button>
                <el-button link type="danger" :icon="Delete" @click="removeProject(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>

          <template #empty>
            <EmptyState
              compact
              title="还没有项目"
              :hint="categories.length ? '点击右上角「新建项目」，把第一项工作立起来。' : '先创建一个项目分类，再新建项目。'"
            />
          </template>
        </el-table>

        <div v-loading="loading" class="project-cards">
          <article v-for="project in filteredProjects" :key="project.id" class="project-mobile-card">
            <div class="project-mobile-main">
              <span class="project-icon"><el-icon :size="15"><FolderOpened /></el-icon></span>
              <div>
                <h3 class="project-name">{{ project.name }}</h3>
                <span class="project-category-tag">{{ project.categoryName }}</span>
              </div>
            </div>
            <div class="project-mobile-foot">
              <span class="project-date">创建于 {{ formatDate(project.createTime) }}</span>
              <div class="table-actions">
                <el-button link type="primary" :icon="Edit" aria-label="编辑项目" @click="openProjectEdit(project)" />
                <el-button link type="danger" :icon="Delete" aria-label="删除项目" @click="removeProject(project)" />
              </div>
            </div>
          </article>

          <EmptyState
            v-if="!filteredProjects.length && !loading"
            compact
            title="没有匹配的项目"
            :hint="projects.length ? '换个关键词或分类试试。' : '先创建分类，再新建项目。'"
          />
        </div>
      </section>
    </div>

    <el-dialog
      v-model="categoryDialogVisible"
      :title="categoryDialogMode === 'create' ? '新建项目分类' : '编辑项目分类'"
      width="460px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="categoryFormRef"
        :model="categoryForm"
        :rules="categoryRules"
        label-position="top"
        hide-required-asterisk
      >
        <el-form-item label="分类名称" prop="name">
          <el-input
            v-model="categoryForm.name"
            maxlength="20"
            show-word-limit
            placeholder="例如：工作、学习、个人"
          />
        </el-form-item>
        <el-form-item label="父分类" prop="parentId">
          <el-select
            v-model="categoryForm.parentId"
            filterable
            clearable
            placeholder="不选则作为一级分类"
            style="width: 100%"
            no-data-text="暂无可选父分类"
          >
            <el-option
              v-for="category in parentCategoryOptions"
              :key="category.id"
              :label="category.path || category.name"
              :value="category.id"
            />
          </el-select>
          <p class="dialog-hint-soft">可输入分类名搜索；不选择父分类时，当前分类作为一级分类。</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="categorySubmitting" @click="submitCategory">
          {{ categoryDialogMode === 'create' ? '创建分类' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="projectDialogVisible"
      :title="projectDialogMode === 'create' ? '新建项目' : '编辑项目'"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="projectFormRef"
        :model="projectForm"
        :rules="projectRules"
        label-position="top"
        hide-required-asterisk
      >
        <el-form-item label="项目名称" prop="name">
          <el-input
            v-model="projectForm.name"
            maxlength="60"
            show-word-limit
            placeholder="例如：Wokit 工作助手"
          />
        </el-form-item>
        <el-form-item label="项目分类" prop="categoryId">
          <el-select
            v-model="projectForm.categoryId"
            placeholder="选择项目分类"
            style="width: 100%"
            filterable
            :disabled="!categories.length"
          >
            <el-option
              v-for="category in categoryTreeRows"
              :key="category.id"
              :label="category.path || category.name"
              :value="category.id"
            />
          </el-select>
          <p v-if="!categories.length" class="dialog-hint">还没有分类，请先创建一个项目分类。</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="projectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="projectSubmitting" @click="submitProject">
          {{ projectDialogMode === 'create' ? '创建项目' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.project-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.category-card {
  overflow: hidden;
}

.category-head,
.project-list-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 18px 14px;
}

.panel-title {
  color: var(--wk-text);
  font-size: 15px;
  font-weight: 600;
}

.panel-sub {
  margin-top: 2px;
  color: var(--wk-text-3);
  font-size: 12px;
}

.category-list {
  min-height: 180px;
  padding: 0 8px 10px;
}

.category-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  transition: background-color 0.15s ease;
}

.category-row:hover {
  background: #f8faf9;
}

.category-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  color: #0b8b4f;
  font-size: 13px;
  font-weight: 600;
  background: #e9f8f1;
  border-radius: 8px;
}

.category-tree-line {
  width: 11px;
  height: 1px;
  flex-shrink: 0;
  background: #cfdad5;
}

.category-info {
  flex: 1;
  min-width: 0;
}

.category-name {
  color: var(--wk-text);
  font-size: 13px;
  font-weight: 500;
}

.category-count {
  margin-top: 1px;
  color: var(--wk-text-3);
  font-size: 11.5px;
}

.category-actions,
.table-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.category-actions :deep(.el-button),
.table-actions :deep(.el-button) {
  margin-left: 0;
  padding: 3px 4px;
}

.project-list-card {
  min-width: 0;
  overflow: hidden;
}

.project-list-head {
  align-items: center;
  padding-bottom: 12px;
}

.project-title-block {
  min-width: 140px;
}

.project-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.project-search {
  width: 210px;
}

.category-filter {
  width: 170px;
}

.project-table :deep(td.el-table__cell) {
  padding-top: 12px;
  padding-bottom: 12px;
}

.project-name-cell {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.project-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  color: #0b8b4f;
  background: #eefaf4;
  border-radius: 8px;
}

.project-name {
  overflow: hidden;
  color: var(--wk-text);
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-category-tag {
  display: inline-block;
  max-width: 220px;
  overflow: hidden;
  padding: 2px 8px;
  color: #475467;
  font-size: 11.5px;
  line-height: 1.5;
  background: #f2f4f7;
  border-radius: 5px;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.project-date {
  color: var(--wk-text-3);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.project-cards {
  display: none;
}

.dialog-hint {
  margin-top: 6px;
  color: #b54708;
  font-size: 11.5px;
  line-height: 1.6;
}

.dialog-hint-soft {
  margin-top: 2px;
  color: var(--wk-text-3);
  font-size: 11.5px;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .project-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .category-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    min-height: 80px;
  }
}

@media (max-width: 768px) {
  .wk-page-head {
    flex-wrap: wrap;
  }

  .project-list-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
    padding: 15px 14px 10px;
  }

  .project-toolbar {
    width: 100%;
    justify-content: flex-start;
  }

  .project-search {
    flex: 1;
    width: auto;
    min-width: 0;
  }

  .category-filter {
    width: 140px;
  }

  .category-list {
    display: block;
    padding-bottom: 8px;
  }

  .project-table {
    display: none;
  }

  .project-cards {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 10px 12px 14px;
  }

  .project-mobile-card {
    padding: 12px;
    border: 1px solid var(--wk-line);
    border-radius: 10px;
  }

  .project-mobile-main {
    display: flex;
    align-items: flex-start;
    gap: 10px;
  }

  .project-mobile-main > div {
    min-width: 0;
  }

  .project-mobile-main .project-name {
    margin-bottom: 5px;
    font-size: 13.5px;
  }

  .project-mobile-foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-top: 9px;
    padding-top: 9px;
    border-top: 1px solid var(--wk-line-soft);
  }
}
</style>
