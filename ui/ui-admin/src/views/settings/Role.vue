<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Search, Stamp } from '@element-plus/icons-vue'
import rolesApi from '@/api/roles'
import permissionApi from '@/api/permission'
import hasBtnPermission from '@/utils/btnPermission'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = ref({ name: '', code: '', page: 1, limit: 10 })

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
const form = ref({ name: '', code: '', description: '', sort: 10 })

const rules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/, message: '编码以字母开头，只能包含字母、数字、下划线和短横线', trigger: 'blur' }
  ]
}

const fetchList = async () => {
  loading.value = true
  try {
    const result = await rolesApi.page(query.value)
    if (result.code === 1) {
      list.value = result.data.records || []
      total.value = Number(result.data.total || 0)
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.value.page = 1
  fetchList()
}

const resetSearch = () => {
  query.value = { name: '', code: '', page: 1, limit: query.value.limit }
  fetchList()
}

const openCreate = () => {
  dialogMode.value = 'create'
  editingId.value = null
  form.value = { name: '', code: '', description: '', sort: 10 }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openEdit = (row) => {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.value = {
    name: row.name,
    code: row.code,
    description: row.description || '',
    sort: row.sort ?? 10
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const submitForm = async () => {
  if (!formRef.value || submitting.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      name: form.value.name,
      code: form.value.code,
      description: form.value.description,
      sort: form.value.sort
    }
    const result =
      dialogMode.value === 'create'
        ? await rolesApi.add(payload)
        : await rolesApi.update(editingId.value, payload)
    if (result.code === 1) {
      ElMessage.success(result.msg)
      dialogVisible.value = false
      fetchList()
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    submitting.value = false
  }
}

const removeRole = async (row) => {
  await ElMessageBox.confirm(
    `确定删除角色「${row.name}」吗？该角色下的用户会失去对应权限。`,
    '删除角色',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  )
  const result = await rolesApi.remove(row.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    fetchList()
  } else {
    ElMessage.error(result.msg)
  }
}

// ============ 权限分配 ============
const drawerVisible = ref(false)
const currentRole = ref({})
const treeData = ref([])
const treeRef = ref()
const treeLoading = ref(false)
const savingPermission = ref(false)
const defaultProps = { children: 'children', label: 'name' }

const isAdminRole = computed(() => currentRole.value.code === 'admin')
const drawerSize = computed(() => (isCompact.value ? '100%' : '560px'))

const showPermissionDrawer = (row) => {
  currentRole.value = row
  drawerVisible.value = true
  treeLoading.value = true
  treeData.value = []
  permissionApi.tree()
    .then((treeResult) => {
      if (treeResult.code !== 1) {
        ElMessage.error(treeResult.msg)
        return null
      }
      treeData.value = treeResult.data || []
      return rolesApi.selectPermissionById(row.id)
    })
    .then((permissionResult) => {
      if (!permissionResult || permissionResult.code !== 1) return
      const assignedIds = permissionResult.data || []
      nextTick(() => treeRef.value?.setCheckedKeys(assignedIds))
    })
    .finally(() => {
      treeLoading.value = false
    })
}

const assignPermission = async () => {
  if (savingPermission.value) return
  const ids = treeRef.value?.getCheckedNodes(false, true).map((node) => node.id) || []
  savingPermission.value = true
  try {
    const result = await rolesApi.updatePermission(currentRole.value.id, ids)
    if (result.code === 1) {
      ElMessage.success(result.msg)
      drawerVisible.value = false
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    savingPermission.value = false
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

fetchList()
</script>

<template>
  <section class="wk-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">角色管理</h1>
        <p class="wk-page-desc">维护系统角色，并为角色分配路由、菜单和按钮权限。</p>
      </div>
      <div class="wk-page-actions">
        <el-button v-if="hasBtnPermission('role:add')" type="primary" :icon="Plus" @click="openCreate">
          添加角色
        </el-button>
      </div>
    </div>

    <section class="wk-card">
      <div class="wk-panel-head role-toolbar">
        <div class="wk-toolbar">
          <el-input
            v-model="query.name"
            class="search-input"
            clearable
            placeholder="搜索角色名称"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-input
            v-model="query.code"
            class="search-input"
            clearable
            placeholder="搜索角色编码"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button @click="resetSearch">重置</el-button>
        </div>
        <p class="toolbar-count">共 {{ total }} 个角色</p>
      </div>

      <el-table v-loading="loading" :data="list" class="wk-table" row-key="id">
        <el-table-column label="角色" min-width="190">
          <template #default="{ row }">
            <div class="role-cell">
              <span class="role-name">{{ row.name }}</span>
              <span v-if="row.builtIn === 1" class="built-in-tag">内置</span>
              <span v-if="isCompact" class="role-meta-mobile">{{ row.code }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="!isCompact"
          prop="code"
          label="角色编码"
          min-width="140"
          class-name="role-code-col"
          label-class-name="role-code-col"
        >
          <template #default="{ row }">
            <code class="code-text">{{ row.code }}</code>
          </template>
        </el-table-column>
        <el-table-column
          v-if="!isCompact"
          prop="description"
          label="角色描述"
          min-width="260"
          class-name="role-desc-col"
          label-class-name="role-desc-col"
        >
          <template #default="{ row }">
            <span :class="row.description ? '' : 'wk-muted'">{{ row.description || '未填写描述' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="!isCompact"
          prop="sort"
          label="排序"
          width="80"
          align="center"
          class-name="role-sort-col"
          label-class-name="role-sort-col"
        />
        <el-table-column
          v-if="!isCompact"
          label="创建时间"
          width="170"
          class-name="role-time-col"
          label-class-name="role-time-col"
        >
          <template #default="{ row }">
            <span class="time-text">{{ row.createTime ? row.createTime.slice(0, 16) : '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column
          v-if="hasBtnPermission('role:edit') || hasBtnPermission('role:delete') || hasBtnPermission('role:assign')"
          label="操作"
          width="230"
          fixed="right"
        >
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="hasBtnPermission('role:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">
                编辑
              </el-button>
              <el-button v-if="hasBtnPermission('role:assign')" link type="success" :icon="Stamp" @click="showPermissionDrawer(row)">
                权限
              </el-button>
              <el-button
                v-if="hasBtnPermission('role:delete') && row.builtIn !== 1"
                link
                type="danger"
                :icon="Delete"
                @click="removeRole(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.limit"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="fetchList"
        @size-change="handleSearch"
      />
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '添加角色' : '编辑角色'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk>
        <div class="form-grid">
          <el-form-item label="角色名称" prop="name">
            <el-input v-model="form.name" maxlength="50" placeholder="例如 项目成员" />
          </el-form-item>
          <el-form-item label="角色编码" prop="code">
            <el-input v-model="form.code" maxlength="50" placeholder="例如 member" :disabled="dialogMode === 'edit' && form.code === 'admin'" />
          </el-form-item>
        </div>
        <el-form-item label="角色描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="说明该角色可以使用哪些功能" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ dialogMode === 'create' ? '创建角色' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="drawerVisible"
      :size="drawerSize"
      class="role-permission-drawer"
      :title="`分配权限 · ${currentRole.name || ''}`"
    >
      <div v-loading="treeLoading" class="permission-drawer">
        <div v-if="isAdminRole" class="admin-permission-tip">
          管理员角色固定拥有全部权限，保存时也会自动补全，无法被裁剪。
        </div>
        <el-tree
          ref="treeRef"
          :data="treeData"
          :props="defaultProps"
          node-key="id"
          show-checkbox
          check-strictly
          default-expand-all
          class="permission-tree"
        >
          <template #default="{ data }">
            <div class="tree-node">
              <span class="tree-node-name">{{ data.name }}</span>
              <span class="type-tag" :class="typeTag(data.type)">{{ typeLabel(data.type) }}</span>
              <code v-if="data.path" class="tree-node-path">{{ data.path }}</code>
              <code v-if="data.permissionValue" class="tree-node-value">{{ data.permissionValue }}</code>
            </div>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button v-if="!isAdminRole" type="primary" :loading="savingPermission" @click="assignPermission">
            保存权限
          </el-button>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<style scoped>
.role-toolbar {
  flex-wrap: wrap;
}

.search-input {
  width: 200px;
}

.toolbar-count {
  color: var(--wk-text-3);
  font-size: 12px;
}

.role-cell {
  display: flex;
  align-items: center;
  gap: 7px;
}

.role-name {
  font-weight: 500;
}

.role-meta-mobile {
  display: block;
  flex-basis: 100%;
  color: var(--wk-text-2);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
}

.built-in-tag {
  height: 20px;
  padding: 0 6px;
  border-radius: 4px;
  background: #eef4ff;
  color: #2563eb;
  font-size: 10.5px;
  line-height: 20px;
}

.code-text {
  padding: 2px 7px;
  border-radius: 5px;
  background: #f4f6f8;
  color: #475467;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11.5px;
}

.time-text {
  color: #667085;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
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

.permission-drawer {
  min-height: 260px;
}

.admin-permission-tip {
  margin-bottom: 12px;
  padding: 9px 11px;
  border: 1px solid #dbe7ff;
  border-radius: 8px;
  background: #f5f8ff;
  color: #3b5b96;
  font-size: 12px;
  line-height: 1.6;
}

.permission-tree {
  --el-tree-node-hover-bg-color: #f9fafb;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
  padding: 2px 0;
}

.tree-node-name {
  flex-shrink: 0;
  font-size: 13px;
}

.type-tag {
  flex-shrink: 0;
  height: 18px;
  padding: 0 6px;
  border-radius: 4px;
  font-size: 10px;
  line-height: 18px;
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

.tree-node-path,
.tree-node-value {
  overflow: hidden;
  color: #98a2b3;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 10.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 0 16px 16px;
}

@media (max-width: 900px) {
  .role-cell {
    flex-wrap: wrap;
  }

  :global(.role-permission-drawer .el-drawer__body) {
    padding: 0 16px 12px;
    overflow-x: hidden;
  }

  :global(.role-permission-drawer .el-drawer__header) {
    margin-bottom: 14px;
    padding: 16px 16px 0;
  }

  :global(.role-permission-drawer .el-drawer__title) {
    overflow: hidden;
    font-size: 16px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .permission-tree :deep(.el-tree-node__content) {
    height: auto;
    min-height: 34px;
    padding-top: 5px;
    padding-bottom: 5px;
    align-items: flex-start;
  }

  .tree-node {
    flex-wrap: wrap;
    gap: 4px 6px;
    padding: 1px 0;
  }

  .tree-node-name {
    flex: 1 1 calc(100% - 70px);
    min-width: 0;
    line-height: 20px;
    white-space: normal;
    word-break: break-word;
  }

  .tree-node-path,
  .tree-node-value {
    flex: 1 1 100%;
    padding-left: 0;
    white-space: normal;
    word-break: break-all;
  }

  .drawer-footer {
    padding: 0 16px 8px;
  }
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .search-input {
    width: 100%;
  }
}
</style>
