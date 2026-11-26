<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Box,
  Check,
  Delete,
  Edit,
  FolderOpened,
  Plus,
  RefreshLeft,
  Search
} from '@element-plus/icons-vue'
import projectApi from '@/api/project'
import taskApi from '@/api/task'
import EmptyState from '@/components/EmptyState.vue'
import { useProjectStore } from '@/store/project'
import { useTaskCategoryStore } from '@/store/taskCategory'
import hasBtnPermission from '@/utils/btnPermission'

const router = useRouter()
const projectStore = useProjectStore()
const taskCategoryStore = useTaskCategoryStore()

const keyword = ref('')
const categoryFilter = ref(null)
const statusFilter = ref(null)
const archivedMode = ref(false)
const archivedProjects = ref([])
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
const projectForm = ref({ name: '', categoryId: null, status: 1 })
const projectSubmitting = ref(false)

const archiveDialogVisible = ref(false)
const archiveProject = ref(null)
const archiveReason = ref('')
const archiveSubmitting = ref(false)

const detailVisible = ref(false)
const detailProject = ref(null)
const detailTasks = ref([])
const detailFilter = ref('open')
const detailLoading = ref(false)

const statusOptions = [
  { value: 0, label: '规划中', tone: 'planning' },
  { value: 1, label: '进行中', tone: 'active' },
  { value: 2, label: '维护中', tone: 'maintenance' },
  { value: 3, label: '暂停', tone: 'paused' },
  { value: 4, label: '已完成', tone: 'done' },
  { value: 5, label: '已取消', tone: 'canceled' }
]
const statusMap = Object.fromEntries(statusOptions.map((item) => [item.value, item]))

const taskStatusOptions = [
  { value: 0, label: '待处理' },
  { value: 1, label: '进行中' },
  { value: 2, label: '阻塞中' },
  { value: 3, label: '已完成' }
]

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
  categoryId: [{ required: true, message: '请选择项目分类', trigger: 'change' }],
  status: [{ required: true, message: '请选择项目状态', trigger: 'change' }]
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

const categoryTreeData = computed(() => {
  const nodeMap = new Map(
    categoryTreeRows.value.map((category) => [
      Number(category.id),
      { ...category, children: [] }
    ])
  )
  const roots = []
  categoryTreeRows.value.forEach((category) => {
    const node = nodeMap.get(Number(category.id))
    const parentId = category.parentId == null ? null : Number(category.parentId)
    if (parentId && nodeMap.has(parentId)) {
      nodeMap.get(parentId).children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
})

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

const projects = computed(() => {
  const source = archivedMode.value ? archivedProjects.value : projectStore.projects
  return [...source].sort((a, b) => {
    const timeCompare = String(b.createTime || '').localeCompare(String(a.createTime || ''))
    return timeCompare || Number(b.id) - Number(a.id)
  })
})

const filteredProjects = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return projects.value.filter((project) => {
    const matchCategory = !categoryFilterIds.value || categoryFilterIds.value.has(Number(project.categoryId))
    const matchKeyword = !search || project.name.toLowerCase().includes(search)
    const matchStatus = archivedMode.value || statusFilter.value == null
      || Number(project.status) === Number(statusFilter.value)
    return matchCategory && matchKeyword && matchStatus
  })
})

const categoryName = computed(() => {
  if (!categoryFilter.value) return '全部分类'
  const category = categories.value.find((item) => Number(item.id) === Number(categoryFilter.value))
  return category ? category.path : '全部分类'
})

const detailVisibleTasks = computed(() => {
  if (detailFilter.value === 'open') {
    return detailTasks.value.filter((task) => Number(task.status) !== 3)
  }
  if (detailFilter.value === 'done') {
    return detailTasks.value.filter((task) => Number(task.status) === 3)
  }
  return detailTasks.value
})

const detailCounts = computed(() => ({
  open: Number(detailProject.value?.taskOpen ?? 0),
  done: Number(detailProject.value?.taskDone ?? 0),
  all: Number(detailProject.value?.taskTotal ?? 0)
}))

const formatDateTime = (value) => (value ? value.slice(0, 16) : '--')
const getProjectStatus = (project) => statusMap[Number(project.status)] || statusMap[1]
const getTaskStatus = (task) => taskStatusOptions.find((item) => item.value === Number(task.status)) || taskStatusOptions[0]
const canAddTask = (project) => !project.archived && [0, 1, 2].includes(Number(project.status ?? 1))

const load = async () => {
  loading.value = true
  try {
    const [activeResult, archivedResult] = await Promise.all([
      projectStore.refresh().then(() => ({ code: 1 })),
      projectApi.list({ archived: true })
    ])
    if (activeResult.code !== 1) {
      ElMessage.error('项目列表加载失败')
    }
    if (archivedResult.code === 1) {
      archivedProjects.value = archivedResult.data || []
    }
    syncDetailProject()
  } finally {
    loading.value = false
  }
}

const syncDetailProject = () => {
  if (!detailProject.value) return
  const all = [...projectStore.projects, ...archivedProjects.value]
  const current = all.find((item) => Number(item.id) === Number(detailProject.value.id))
  if (current) detailProject.value = current
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
    await load()
  } else {
    ElMessage.error(result.msg)
  }
}

const openProjectCreate = () => {
  projectDialogMode.value = 'create'
  editingProjectId.value = null
  projectForm.value = {
    name: '',
    categoryId: categoryFilter.value || categories.value[0]?.id || null,
    status: 1
  }
  projectDialogVisible.value = true
  nextTick(() => projectFormRef.value?.clearValidate())
}

const openProjectEdit = (project) => {
  if (project.archived) {
    ElMessage.warning('已归档项目只读，请先恢复项目')
    return
  }
  projectDialogMode.value = 'edit'
  editingProjectId.value = project.id
  projectForm.value = {
    name: project.name,
    categoryId: project.categoryId,
    status: Number(project.status ?? 1)
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
      await load()
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    projectSubmitting.value = false
  }
}

const removeProject = async (project) => {
  await ElMessageBox.confirm(
    `确定删除项目「${project.name}」吗？只有没有任何任务、周报和工时的项目才允许删除，否则请使用归档。`,
    '删除项目',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  )
  const result = await projectApi.remove(project.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    await load()
  } else {
    ElMessage.error(result.msg)
  }
}

const changeProjectStatus = async (project, status) => {
  if (Number(project.status) === Number(status)) return
  const result = await projectApi.changeStatus(project.id, status)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    await load()
  } else {
    ElMessage.error(result.msg)
  }
}

const openArchiveDialog = (project) => {
  archiveProject.value = project
  archiveReason.value = ''
  archiveDialogVisible.value = true
}

const submitArchive = async () => {
  if (!archiveProject.value || archiveSubmitting.value) return
  archiveSubmitting.value = true
  try {
    const result = await projectApi.archive(archiveProject.value.id, archiveReason.value.trim())
    if (result.code === 1) {
      ElMessage.success(result.msg)
      archiveDialogVisible.value = false
      await load()
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    archiveSubmitting.value = false
  }
}

const restoreProject = async (project) => {
  await ElMessageBox.confirm(
    `确定恢复项目「${project.name}」吗？恢复后会回到归档前的项目状态。`,
    '恢复项目',
    { confirmButtonText: '恢复', cancelButtonText: '取消', type: 'info' }
  )
  const result = await projectApi.restore(project.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    await load()
  } else {
    ElMessage.error(result.msg)
  }
}

const switchArchivedMode = (value) => {
  archivedMode.value = Boolean(value)
  statusFilter.value = null
  detailVisible.value = false
}

const openDetail = async (project) => {
  detailProject.value = project
  detailFilter.value = 'open'
  detailVisible.value = true
  await loadDetailTasks()
}

const loadDetailTasks = async () => {
  if (!detailProject.value) return
  detailLoading.value = true
  try {
    await taskCategoryStore.load()
    const result = await taskApi.list({
      projectId: detailProject.value.id,
      taskKind: 1,
      page: 1,
      limit: 200
    })
    if (result.code === 1) {
      detailTasks.value = result.data.records || []
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    detailLoading.value = false
  }
}

const goCreateProjectTask = () => {
  if (!detailProject.value || !canAddTask(detailProject.value)) return
  router.push({
    path: '/tasks',
    query: { projectId: detailProject.value.id, create: '1' }
  })
}

onMounted(load)
</script>

<template>
  <section class="wk-page project-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">项目管理</h1>
        <p class="wk-page-desc">先按分类整理项目，再把项目任务归到具体项目上；项目数量直接来自任务表。</p>
      </div>
      <div class="wk-page-actions">
        <el-button v-if="hasBtnPermission('project:add')" type="primary" :icon="Plus" @click="openProjectCreate">新建项目</el-button>
      </div>
    </div>

    <div class="project-layout">
      <section class="wk-card category-card">
        <div class="category-head">
          <div>
            <h2 class="panel-title">项目分类</h2>
            <p class="panel-sub">{{ categories.length }} 个分类</p>
          </div>
          <el-button v-if="hasBtnPermission('project:category:add')" link type="primary" :icon="Plus" @click="openCategoryCreate">新建</el-button>
        </div>

        <div v-loading="loading" class="category-list">
          <el-tree
            v-if="categories.length"
            :data="categoryTreeData"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            default-expand-all
            :expand-on-click-node="false"
            :indent="16"
            class="category-tree"
          >
            <template #default="{ data }">
              <div class="category-node" :title="data.path">
                <div class="category-info">
                  <p class="category-name">{{ data.name }}</p>
                  <p class="category-count">{{ data.totalProjectCount }} 个项目</p>
                </div>
                <div v-if="hasBtnPermission('project:category:edit') || hasBtnPermission('project:category:delete')" class="category-actions" @click.stop>
                  <el-button v-if="hasBtnPermission('project:category:edit')" link type="primary" :icon="Edit" aria-label="编辑分类" @click="openCategoryEdit(data)" />
                  <el-button v-if="hasBtnPermission('project:category:delete')" link type="danger" :icon="Delete" aria-label="删除分类" @click="removeCategory(data)" />
                </div>
              </div>
            </template>
          </el-tree>

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
            <el-radio-group :model-value="archivedMode" size="small" @change="switchArchivedMode">
              <el-radio-button :value="false">当前项目</el-radio-button>
              <el-radio-button :value="true">已归档</el-radio-button>
            </el-radio-group>
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
            <el-select
              v-model="statusFilter"
              class="status-filter"
              clearable
              placeholder="全部状态"
              :disabled="archivedMode"
            >
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
        </div>

        <el-table v-loading="loading" :data="filteredProjects" class="wk-table project-table" row-key="id">
          <el-table-column label="项目名称" min-width="210">
            <template #default="{ row }">
              <button class="project-name-cell project-name-button" type="button" @click="openDetail(row)">
                <span class="project-icon"><el-icon :size="15"><FolderOpened /></el-icon></span>
                <span class="project-name">{{ row.name }}</span>
              </button>
            </template>
          </el-table-column>
          <el-table-column label="分类" min-width="120">
            <template #default="{ row }">
              <span class="project-category-tag">{{ row.categoryName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="115">
            <template #default="{ row }">
              <el-dropdown
                v-if="!row.archived && hasBtnPermission('project:status')"
                trigger="click"
                @command="(status) => changeProjectStatus(row, status)"
              >
                <el-tag
                  size="small"
                  effect="plain"
                  class="project-status-tag is-clickable"
                  :class="'status-' + getProjectStatus(row).tone"
                >
                  {{ getProjectStatus(row).label }}
                  <el-icon class="status-caret" :size="10"><ArrowDown /></el-icon>
                </el-tag>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="item in statusOptions"
                      :key="item.value"
                      :command="item.value"
                    >
                      <el-icon v-if="Number(row.status) === item.value" class="status-check">
                        <Check />
                      </el-icon>
                      <span :class="{ 'status-option-active': Number(row.status) === item.value }">
                        {{ item.label }}
                      </span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-tag
                v-else
                size="small"
                effect="plain"
                class="project-status-tag"
                :class="'status-' + getProjectStatus(row).tone"
              >
                {{ row.archived ? '已归档' : getProjectStatus(row).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="项目任务" width="120">
            <template #default="{ row }">
              <button class="task-count-button" type="button" @click="openDetail(row)">
                <strong>{{ row.taskOpen }}</strong>
                <span>/ {{ row.taskTotal }}</span>
              </button>
              <p class="task-count-hint">未完成 / 总数</p>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <div class="table-actions project-actions">
                <template v-if="!row.archived">
                  <el-button v-if="hasBtnPermission('project:edit')" link type="primary" :icon="Edit" @click="openProjectEdit(row)">编辑</el-button>
                  <el-button v-if="hasBtnPermission('project:archive')" link type="warning" :icon="Box" @click="openArchiveDialog(row)">归档</el-button>
                  <el-button v-if="hasBtnPermission('project:delete')" link type="danger" :icon="Delete" @click="removeProject(row)">删除</el-button>
                </template>
                <el-button
                  v-else-if="hasBtnPermission('project:restore')"
                  link
                  type="primary"
                  :icon="RefreshLeft"
                  @click="restoreProject(row)"
                >恢复</el-button>
              </div>
            </template>
          </el-table-column>

          <template #empty>
            <EmptyState
              compact
              :title="archivedMode ? '没有已归档项目' : '还没有项目'"
              :hint="archivedMode ? '归档后的项目会出现在这里。' : (categories.length ? '点击右上角「新建项目」，把第一项工作立起来。' : '先创建一个项目分类，再新建项目。')"
            />
          </template>
        </el-table>

        <div v-loading="loading" class="project-cards">
          <article v-for="project in filteredProjects" :key="project.id" class="project-mobile-card">
            <button class="project-mobile-main project-mobile-main-button" type="button" @click="openDetail(project)">
              <span class="project-icon"><el-icon :size="15"><FolderOpened /></el-icon></span>
              <div>
                <h3 class="project-name">{{ project.name }}</h3>
                <div class="project-mobile-tags">
                  <span class="project-category-tag">{{ project.categoryName }}</span>
                  <el-tag size="small" effect="plain" :class="['project-status-tag', 'status-' + getProjectStatus(project).tone]">
                    {{ project.archived ? '已归档' : getProjectStatus(project).label }}
                  </el-tag>
                </div>
                <p class="project-mobile-count">
                  未完成 <strong>{{ project.taskOpen }}</strong> / 共 {{ project.taskTotal }} 个项目任务
                </p>
              </div>
            </button>
            <div class="project-mobile-foot">
              <div class="table-actions">
                <template v-if="!project.archived">
                  <el-button v-if="hasBtnPermission('project:edit')" link type="primary" :icon="Edit" aria-label="编辑项目" @click="openProjectEdit(project)" />
                  <el-button v-if="hasBtnPermission('project:archive')" link type="warning" aria-label="归档项目" @click="openArchiveDialog(project)">归档</el-button>
                  <el-button v-if="hasBtnPermission('project:delete')" link type="danger" :icon="Delete" aria-label="删除项目" @click="removeProject(project)" />
                </template>
                <el-button
                  v-else-if="hasBtnPermission('project:restore')"
                  link
                  type="primary"
                  :icon="RefreshLeft"
                  aria-label="恢复项目"
                  @click="restoreProject(project)"
                />
              </div>
            </div>
          </article>

          <EmptyState
            v-if="!filteredProjects.length && !loading"
            compact
            :title="archivedMode ? '没有已归档项目' : '没有匹配的项目'"
            :hint="archivedMode ? '归档后的项目会出现在这里。' : (projects.length ? '换个关键词、分类或状态试试。' : '先创建分类，再新建项目。')"
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
        <el-form-item v-if="projectDialogMode === 'edit'" label="项目状态" prop="status">
          <el-select v-model="projectForm.status" style="width: 100%" :disabled="!hasBtnPermission('project:status')">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            >
              <span>{{ item.label }}</span>
              <span v-if="item.value === 2" class="status-option-hint">已发布但未结束，持续维护</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="projectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="projectSubmitting" @click="submitProject">
          {{ projectDialogMode === 'create' ? '创建项目' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="archiveDialogVisible" title="归档项目" width="460px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="项目">{{ archiveProject?.name }}</el-form-item>
        <el-form-item label="归档原因（选填）">
          <el-input
            v-model="archiveReason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            resize="none"
            placeholder="例如：已交付，进入长期维护；或需求结束，不再新增任务"
          />
        </el-form-item>
        <p class="dialog-hint-soft">归档后项目变为只读，不能编辑或新增任务，但可以随时恢复。</p>
      </el-form>
      <template #footer>
        <el-button @click="archiveDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="archiveSubmitting" @click="submitArchive">确认归档</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      :title="detailProject?.name || '项目详情'"
      size="520px"
      class="project-detail-drawer"
    >
      <div v-if="detailProject" class="detail-content">
        <div class="detail-summary">
          <div>
            <span class="project-category-tag">{{ detailProject.categoryName }}</span>
            <el-tag size="small" effect="plain" class="project-status-tag" :class="'status-' + getProjectStatus(detailProject).tone">
              {{ detailProject.archived ? '已归档' : getProjectStatus(detailProject).label }}
            </el-tag>
          </div>
          <p class="detail-created">创建于 {{ formatDateTime(detailProject.createTime) }}</p>
          <div class="detail-counts">
            <div>
              <strong>{{ detailProject.taskOpen }}</strong>
              <span>未完成</span>
            </div>
            <div>
              <strong>{{ detailProject.taskDone }}</strong>
              <span>已完成</span>
            </div>
            <div>
              <strong>{{ detailProject.taskTotal }}</strong>
              <span>总任务</span>
            </div>
          </div>
          <p v-if="detailProject.archived" class="detail-archive-note">
            归档于 {{ formatDateTime(detailProject.archivedAt) }}<span v-if="detailProject.archiveReason"> · {{ detailProject.archiveReason }}</span>
          </p>
          <p v-else-if="detailProject.status === 2 && detailProject.releasedAt" class="detail-archive-note">
            发布于 {{ formatDateTime(detailProject.releasedAt) }}，当前处于持续维护阶段。
          </p>
        </div>

        <div class="detail-toolbar">
          <div class="detail-tabs">
            <button type="button" :class="{ 'is-active': detailFilter === 'open' }" @click="detailFilter = 'open'">未完成 {{ detailCounts.open }}</button>
            <button type="button" :class="{ 'is-active': detailFilter === 'done' }" @click="detailFilter = 'done'">已完成 {{ detailCounts.done }}</button>
            <button type="button" :class="{ 'is-active': detailFilter === 'all' }" @click="detailFilter = 'all'">全部 {{ detailCounts.all }}</button>
          </div>
          <el-button
            v-if="hasBtnPermission('task:add') && canAddTask(detailProject)"
            type="primary"
            size="small"
            :icon="Plus"
            @click="goCreateProjectTask"
          >新增项目任务</el-button>
        </div>

        <div v-loading="detailLoading" class="detail-task-list">
          <article v-for="task in detailVisibleTasks" :key="task.id" class="detail-task-row" :class="{ 'is-done': task.status === 3 }">
            <div class="detail-task-head">
              <p class="detail-task-title">{{ task.title }}</p>
              <el-tag size="small" effect="plain" class="detail-task-status" :class="'task-status-' + task.status">
                {{ getTaskStatus(task).label }}
              </el-tag>
            </div>
            <p v-if="task.description" class="detail-task-desc">{{ task.description }}</p>
            <div class="detail-task-meta">
              <span
                class="task-category-tag"
                :style="{
                  color: taskCategoryStore.categoryColor(task.taskCategoryId),
                  borderColor: taskCategoryStore.categoryColor(task.taskCategoryId)
                }"
              >{{ taskCategoryStore.categoryName(task.taskCategoryId) || '未分类' }}</span>
              <span>截止 {{ formatDateTime(task.deadline) }}</span>
            </div>
          </article>

          <EmptyState
            v-if="!detailVisibleTasks.length && !detailLoading"
            compact
            :title="detailFilter === 'done' ? '暂无已完成任务' : '暂无项目任务'"
            hint="项目任务统一在每日任务中创建，完成后会自动计入这里。"
          />
        </div>
      </div>
    </el-drawer>
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

.category-tree {
  background: transparent;
}

.category-tree :deep(.el-tree-node__content) {
  height: auto;
  min-height: 44px;
  padding: 3px 6px;
  border-radius: 9px;
}

.category-tree :deep(.el-tree-node__content:hover) {
  background: #f8faf9;
}

.category-tree :deep(.el-tree-node__expand-icon) {
  color: #98a2b3;
  font-size: 12px;
}

.category-tree :deep(.el-tree-node__expand-icon.is-leaf) {
  color: transparent;
}

.category-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
  padding: 5px 2px;
}

.category-info {
  flex: 1;
  min-width: 0;
}

.category-name {
  overflow: hidden;
  color: var(--wk-text);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  flex-wrap: wrap;
  gap: 2px;
  flex-shrink: 0;
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
  width: 190px;
}

.category-filter,
.status-filter {
  width: 130px;
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

.project-name-button,
.project-mobile-main-button {
  width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.project-name-button:hover .project-name {
  color: #0b8b4f;
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

.project-status-tag {
  height: 22px;
  margin-left: 5px;
  border-radius: 5px;
  font-size: 11px;
}

.project-status-tag.is-clickable {
  cursor: pointer;
  user-select: none;
}

.project-status-tag.is-clickable:hover {
  filter: brightness(0.96);
}

.status-caret {
  margin-left: 2px;
  vertical-align: -1px;
}

.status-check {
  margin-right: 5px;
  color: var(--wk-primary);
}

.status-option-active {
  color: var(--wk-text);
  font-weight: 500;
}

.project-status-tag.status-planning {
  border-color: #d0d5dd;
  background: #f2f4f7;
  color: #667085;
}

.project-status-tag.status-active {
  border-color: #abefc6;
  background: #ecfdf3;
  color: #067647;
}

.project-status-tag.status-maintenance {
  border-color: #b9e6fe;
  background: #f0f9ff;
  color: #026aa2;
}

.project-status-tag.status-paused {
  border-color: #fedf89;
  background: #fffaeb;
  color: #b54708;
}

.project-status-tag.status-done {
  border-color: #d9d6fe;
  background: #f4f3ff;
  color: #5925dc;
}

.project-status-tag.status-canceled {
  border-color: #e4e7ec;
  background: #f9fafb;
  color: #98a2b3;
}

.task-count-button {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  padding: 0;
  border: none;
  background: transparent;
  color: #475467;
  cursor: pointer;
}

.task-count-button strong {
  color: var(--wk-text);
  font-size: 14px;
  font-weight: 600;
}

.task-count-button span {
  color: var(--wk-text-3);
  font-size: 11.5px;
}

.task-count-hint {
  margin-top: 1px;
  color: #b0b8c4;
  font-size: 10.5px;
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

.status-option-hint {
  margin-left: 8px;
  color: var(--wk-text-3);
  font-size: 11.5px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-summary {
  padding: 14px;
  border: 1px solid var(--wk-line);
  border-radius: 10px;
  background: #fbfcfd;
}

.detail-summary > div:first-child {
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-created {
  margin-top: 8px;
  color: var(--wk-text-3);
  font-size: 12px;
}

.detail-counts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 13px;
}

.detail-counts > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.detail-counts strong {
  color: var(--wk-text);
  font-size: 22px;
  font-weight: 600;
  line-height: 1.1;
}

.detail-counts span {
  color: var(--wk-text-3);
  font-size: 11.5px;
}

.detail-archive-note {
  margin-top: 11px;
  color: #b54708;
  font-size: 11.5px;
  line-height: 1.6;
}

.detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.detail-tabs {
  display: flex;
  gap: 3px;
  padding: 3px;
  border-radius: 8px;
  background: #f4f6f8;
}

.detail-tabs button {
  height: 28px;
  padding: 0 9px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #667085;
  font-size: 11.5px;
  cursor: pointer;
}

.detail-tabs button.is-active {
  background: #fff;
  color: var(--wk-text);
  font-weight: 500;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.06);
}

.detail-task-list {
  min-height: 160px;
}

.detail-task-row {
  padding: 11px 0;
  border-bottom: 1px solid var(--wk-line-soft);
}

.detail-task-row.is-done .detail-task-title {
  color: var(--wk-text-3);
  text-decoration: line-through;
}

.detail-task-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.detail-task-title {
  flex: 1;
  min-width: 0;
  color: var(--wk-text);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.5;
}

.detail-task-status {
  flex-shrink: 0;
  height: 20px;
  border-radius: 5px;
  font-size: 10.5px;
}

.detail-task-status.task-status-0 {
  border-color: #e4e7ec;
  background: #f9fafb;
  color: #667085;
}

.detail-task-status.task-status-1 {
  border-color: #abefc6;
  background: #ecfdf3;
  color: #067647;
}

.detail-task-status.task-status-2 {
  border-color: #fedf89;
  background: #fffaeb;
  color: #b54708;
}

.detail-task-status.task-status-3 {
  border-color: #d9d6fe;
  background: #f4f3ff;
  color: #5925dc;
}

.detail-task-desc {
  margin-top: 4px;
  color: var(--wk-text-3);
  font-size: 11.5px;
  line-height: 1.55;
}

.detail-task-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 7px;
  color: var(--wk-text-3);
  font-size: 11px;
}

.task-category-tag {
  padding: 1px 6px;
  border: 1px solid;
  border-radius: 4px;
  font-size: 10.5px;
  line-height: 1.5;
}

@media (max-width: 960px) {
  .project-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .category-list {
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

  .category-filter,
  .status-filter {
    width: 130px;
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

  .project-mobile-tags {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 5px;
  }

  .project-mobile-tags .project-status-tag {
    margin-left: 0;
  }

  .project-mobile-count {
    margin-top: 6px;
    color: var(--wk-text-3);
    font-size: 11.5px;
  }

  .project-mobile-count strong {
    color: var(--wk-text);
    font-size: 13px;
  }

  .project-mobile-foot {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 9px;
    padding-top: 9px;
    border-top: 1px solid var(--wk-line-soft);
  }

  .project-actions :deep(.el-button) {
    padding: 2px 3px;
    font-size: 11.5px;
  }

  .detail-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 420px) {
  .detail-tabs button {
    padding: 0 7px;
    font-size: 11px;
  }

  .detail-counts strong {
    font-size: 19px;
  }
}
</style>
