<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import permissionApi from '@/api/permission'
import EmptyState from '@/components/EmptyState.vue'
import { menuIconMap, menuIconOptions } from '@/utils/menuIcons'
import hasBtnPermission from '@/utils/btnPermission'

const treeData = ref([])
const loading = ref(false)
const total = computed(() => countNodes(treeData.value))

const isCompact = ref(false)
let compactMedia
const syncCompact = (event) => {
  isCompact.value = event.matches
}

onMounted(() => {
  compactMedia = window.matchMedia('(max-width: 900px)')
  isCompact.value = compactMedia.matches
  compactMedia.addEventListener('change', syncCompact)
})

onBeforeUnmount(() => {
  compactMedia?.removeEventListener('change', syncCompact)
})

const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)
const formRef = ref()
const submitting = ref(false)
const form = ref({
  parentId: 0,
  name: '',
  type: 1,
  path: '',
  permissionValue: '',
  icon: '',
  sort: 1,
  status: 1
})

const rules = {
  name: [
    { required: true, message: '请输入权限名称', trigger: 'blur' },
    { max: 50, message: '不能超过 50 个字符', trigger: 'blur' }
  ],
  path: [
    {
      validator: (rule, value, callback) => {
        if (form.value.type !== 2 && !value) {
          callback(new Error('请填写路由地址'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  permissionValue: [
    {
      validator: (rule, value, callback) => {
        if (form.value.type === 2 && !value) {
          callback(new Error('请填写按钮权限标识'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const parentOptions = computed(() => [
  { id: 0, name: '根节点（一级路由）', children: treeData.value }
])

const countNodes = (nodes = []) =>
  nodes.reduce((sum, node) => sum + 1 + countNodes(node.children || []), 0)

const loadTree = async () => {
  loading.value = true
  try {
    const result = await permissionApi.tree()
    if (result.code === 1) {
      treeData.value = result.data || []
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.value = {
    parentId: 0,
    name: '',
    type: 1,
    path: '',
    permissionValue: '',
    icon: '',
    sort: 1,
    status: 1
  }
}

const openRootCreate = () => {
  dialogMode.value = 'create'
  editingId.value = null
  resetForm()
  form.value.type = 0
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openChildCreate = (row) => {
  dialogMode.value = 'create'
  editingId.value = null
  resetForm()
  form.value.parentId = row.id
  form.value.type = row.type === 1 ? 2 : 1
  form.value.sort = (row.children?.length || 0) + 1
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openEdit = async (row) => {
  dialogMode.value = 'edit'
  editingId.value = row.id
  const result = await permissionApi.getById(row.id)
  if (result.code !== 1) {
    ElMessage.error(result.msg)
    return
  }
  const data = result.data
  form.value = {
    parentId: data.parentId ?? 0,
    name: data.name,
    type: data.type,
    path: data.path || '',
    permissionValue: data.permissionValue || '',
    icon: data.icon || '',
    sort: data.sort ?? 1,
    status: data.status ?? 1
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleParentChange = (parentId) => {
  if (!parentId) return
  const parent = findNode(treeData.value, parentId)
  if (!parent) return
  if (parent.type === 1) {
    form.value.type = 2
  } else if (parent.type === 0 && form.value.type === 2) {
    form.value.type = 1
  }
}

const findNode = (nodes, id) => {
  for (const node of nodes) {
    if (node.id === id) return node
    const found = findNode(node.children || [], id)
    if (found) return found
  }
  return null
}

const handleTypeChange = () => {
  if (form.value.type === 2) {
    form.value.path = ''
    form.value.icon = ''
  } else {
    form.value.permissionValue = ''
  }
}

const submitForm = async () => {
  if (!formRef.value || submitting.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      parentId: form.value.parentId || 0,
      name: form.value.name.trim(),
      type: form.value.type,
      path: form.value.type === 2 ? null : form.value.path.trim(),
      permissionValue: form.value.type === 2 ? form.value.permissionValue.trim() : null,
      icon: form.value.type === 2 ? null : form.value.icon.trim(),
      sort: form.value.sort,
      status: form.value.status
    }
    const result =
      dialogMode.value === 'create'
        ? await permissionApi.add(payload)
        : await permissionApi.update(editingId.value, payload)
    if (result.code === 1) {
      ElMessage.success(result.msg)
      dialogVisible.value = false
      loadTree()
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    submitting.value = false
  }
}

const removePermission = async (row) => {
  await ElMessageBox.confirm(
    `确定删除权限「${row.name}」吗？该权限在角色上的授权也会一并移除。`,
    '删除权限',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  )
  const result = await permissionApi.remove(row.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    loadTree()
  } else {
    ElMessage.error(result.msg)
  }
}

const typeLabel = (type) => {
  if (type === 0) return '路由'
  if (type === 1) return '菜单'
  return '按钮'
}

const typeTag = (type) => {
  if (type === 0) return 'is-route'
  if (type === 1) return 'is-menu'
  return 'is-button'
}

loadTree()
</script>

<template>
  <section class="wk-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">权限管理</h1>
        <p class="wk-page-desc">路由控制页面能否进入，菜单控制侧栏是否展示，按钮控制页面内操作是否可用。</p>
      </div>
      <div class="wk-page-actions">
        <span class="total-chip">{{ total }} 项权限</span>
        <el-button v-if="hasBtnPermission('permission:add')" type="primary" :icon="Plus" @click="openRootCreate">
          添加路由
        </el-button>
      </div>
    </div>

    <section class="wk-card permission-card">
      <div class="wk-panel-head">
        <div>
          <p class="wk-panel-title">权限树</p>
          <p class="wk-panel-sub">按层级维护路由目录、菜单页面与页面内按钮</p>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="treeData"
        row-key="id"
        class="wk-table permission-table"
        default-expand-all
        :indent="24"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="权限名称" :min-width="isCompact ? 180 : 230">
          <template #default="{ row }">
            <div class="name-cell">
              <span class="type-tag" :class="typeTag(row.type)">{{ typeLabel(row.type) }}</span>
              <div class="name-wrap">
                <span class="node-name">{{ row.name }}</span>
                <span v-if="isCompact && (row.path || row.permissionValue)" class="node-meta-mobile">
                  {{ row.path || row.permissionValue }}
                </span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="!isCompact"
          prop="path"
          label="路由地址"
          min-width="180"
          class-name="perm-path-col"
          label-class-name="perm-path-col"
        >
          <template #default="{ row }">
            <code v-if="row.path" class="code-text">{{ row.path }}</code>
            <span v-else class="wk-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="!isCompact"
          prop="permissionValue"
          label="按钮标识"
          min-width="180"
          class-name="perm-value-col"
          label-class-name="perm-value-col"
        >
          <template #default="{ row }">
            <code v-if="row.permissionValue" class="code-text">{{ row.permissionValue }}</code>
            <span v-else class="wk-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="!isCompact"
          prop="icon"
          label="图标"
          width="150"
          class-name="perm-icon-col"
          label-class-name="perm-icon-col"
        >
          <template #default="{ row }">
            <span v-if="row.icon" class="icon-cell">
              <el-icon v-if="menuIconMap[row.icon]"><component :is="menuIconMap[row.icon]" /></el-icon>
              <span>{{ row.icon }}</span>
            </span>
            <span v-else class="wk-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="!isCompact"
          prop="sort"
          label="排序"
          width="72"
          align="center"
          class-name="perm-sort-col"
          label-class-name="perm-sort-col"
        />
        <el-table-column
          v-if="!isCompact"
          label="状态"
          width="86"
          class-name="perm-status-col"
          label-class-name="perm-status-col"
        >
          <template #default="{ row }">
            <span class="status-dot" :class="row.status === 1 ? 'is-on' : 'is-off'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column
          v-if="hasBtnPermission('permission:add') || hasBtnPermission('permission:edit') || hasBtnPermission('permission:delete')"
          label="操作"
          :width="isCompact ? 176 : 240"
          fixed="right"
        >
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                v-if="hasBtnPermission('permission:add') && row.type !== 2"
                link
                type="primary"
                :icon="isCompact ? undefined : Plus"
                @click="openChildCreate(row)"
              >
                添加下级
              </el-button>
              <el-button v-if="hasBtnPermission('permission:edit')" link type="primary" :icon="isCompact ? undefined : Edit" @click="openEdit(row)">
                编辑
              </el-button>
              <el-button v-if="hasBtnPermission('permission:delete')" link type="danger" :icon="isCompact ? undefined : Delete" @click="removePermission(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState compact title="暂无权限数据" hint="点击右上角「添加路由」创建第一项权限。" />
        </template>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '添加权限' : '编辑权限'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk>
        <el-form-item label="上级权限">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ children: 'children', label: 'name' }"
            node-key="id"
            check-strictly
            default-expand-all
            :render-after-expand="false"
            style="width: 100%"
            @change="handleParentChange"
          />
          <p class="form-tip">按钮只能挂在菜单下，菜单可以挂在路由目录下。</p>
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="权限名称" prop="name">
            <el-input v-model="form.name" maxlength="50" placeholder="例如 角色管理" />
          </el-form-item>
          <el-form-item label="权限类型">
            <el-select v-model="form.type" style="width: 100%" @change="handleTypeChange">
              <el-option label="路由 / 目录" :value="0" />
              <el-option label="菜单" :value="1" />
              <el-option label="按钮" :value="2" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item v-if="form.type !== 2" label="路由地址" prop="path">
          <el-input v-model="form.path" maxlength="120" placeholder="例如 /settings/roles" />
        </el-form-item>

        <el-form-item v-if="form.type !== 2" label="菜单图标">
          <el-select v-model="form.icon" clearable placeholder="请选择菜单图标" style="width: 100%" popper-class="menu-icon-popper">
            <el-option
              v-for="item in menuIconOptions"
              :key="item.value"
              :label="`${item.label} · ${item.value}`"
              :value="item.value"
            >
              <div class="icon-option">
                <el-icon><component :is="item.component" /></el-icon>
                <span class="icon-option-label">{{ item.label }}</span>
                <span class="icon-option-value">{{ item.value }}</span>
              </div>
            </el-option>
            <template #label="{ label, value }">
              <span class="icon-select-label">
                <el-icon v-if="menuIconMap[value]"><component :is="menuIconMap[value]" /></el-icon>
                <span>{{ label }}</span>
              </span>
            </template>
          </el-select>
          <p class="form-tip">图标用于左侧菜单展示，清空后使用默认图标。</p>
        </el-form-item>

        <el-form-item v-if="form.type === 2" label="按钮权限标识" prop="permissionValue">
          <el-input v-model="form.permissionValue" maxlength="100" placeholder="例如 role:add" />
          <p class="form-tip">前端通过该标识控制按钮显隐，建议按「模块:动作」命名。</p>
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="排序">
            <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ dialogMode === 'create' ? '创建权限' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.total-chip {
  height: 26px;
  padding: 0 9px;
  border-radius: 6px;
  background: #fff;
  border: 1px solid var(--wk-line);
  color: var(--wk-text-2);
  font-size: 11.5px;
  line-height: 24px;
}

.permission-card {
  overflow: hidden;
}

.name-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  vertical-align: middle;
}

.icon-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.icon-cell .el-icon,
.icon-select-label .el-icon {
  color: var(--wk-primary);
}

.icon-select-label {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

:global(.menu-icon-popper .icon-option) {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

:global(.menu-icon-popper .icon-option .el-icon) {
  color: var(--wk-primary);
}

:global(.menu-icon-popper .icon-option-label) {
  flex: 1;
}

:global(.menu-icon-popper .icon-option-value) {
  color: var(--wk-text-3);
  font-size: 12px;
}

.permission-table :deep(.el-table__indent),
.permission-table :deep(.el-table__expand-icon) {
  vertical-align: middle;
}

.type-tag {
  flex-shrink: 0;
  height: 19px;
  padding: 0 6px;
  border-radius: 4px;
  font-size: 10px;
  line-height: 19px;
}

.type-tag.is-route {
  background: #eef4ff;
  color: #2563eb;
}

.type-tag.is-menu {
  background: var(--wk-primary-soft);
  color: #0b8b4f;
}

.type-tag.is-button {
  background: #fff5e8;
  color: #d97706;
}

.name-wrap {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.node-meta-mobile {
  display: block;
  overflow: hidden;
  color: var(--wk-text-2);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-name {
  font-weight: 500;
}

.code-text {
  padding: 2px 6px;
  border-radius: 4px;
  background: #f4f6f8;
  color: #475467;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
}

.status-dot {
  font-size: 11.5px;
}

.status-dot.is-on {
  color: #0b8b4f;
}

.status-dot.is-off {
  color: #98a2b3;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.table-actions :deep(.el-button) {
  margin-left: 0;
  padding: 4px 2px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-tip {
  margin-top: 2px;
  color: var(--wk-text-3);
  font-size: 11.5px;
}

@media (max-width: 900px) {
  .permission-table {
    min-width: 0;
  }

  .permission-card {
    overflow: hidden;
  }
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
