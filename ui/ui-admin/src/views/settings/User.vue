<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import userApi from '@/api/user'
import rolesApi from '@/api/roles'
import EmptyState from '@/components/EmptyState.vue'
import hasBtnPermission from '@/utils/btnPermission'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = ref({ name: '', status: '', page: 1, limit: 10 })
const roles = ref([])

const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)
const formRef = ref()
const submitting = ref(false)
const form = ref({
  name: '',
  realName: '',
  password: '',
  phone: '',
  email: '',
  roleId: null,
  status: 1
})

const rules = {
  name: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { min: 2, max: 20, message: '账号长度需为 2-20 位', trigger: 'blur' }
  ],
  realName: [{ max: 20, message: '姓名不能超过 20 个字', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择用户角色', trigger: 'change' }],
  password: [
    {
      validator: (rule, value, callback) => {
        if (dialogMode.value === 'create' && !value) {
          callback(new Error('请输入初始密码'))
          return
        }
        if (value && (value.length < 6 || value.length > 20)) {
          callback(new Error('密码长度需为 6-20 位'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const fetchList = async () => {
  loading.value = true
  try {
    const result = await userApi.list(query.value)
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

const loadRoles = async () => {
  const result = await rolesApi.listAll()
  if (result.code === 1) {
    roles.value = result.data || []
    const normalRole = roles.value.find((role) => role.code === 'user')
    if (normalRole && !form.value.roleId) {
      form.value.roleId = normalRole.id
    }
  }
}

const handleSearch = () => {
  query.value.page = 1
  fetchList()
}

const resetSearch = () => {
  query.value = { name: '', status: '', page: 1, limit: query.value.limit }
  fetchList()
}

const openCreate = () => {
  dialogMode.value = 'create'
  editingId.value = null
  const normalRole = roles.value.find((role) => role.code === 'user')
  form.value = {
    name: '',
    realName: '',
    password: '',
    phone: '',
    email: '',
    roleId: normalRole ? normalRole.id : null,
    status: 1
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openEdit = (row) => {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.value = {
    name: row.name,
    realName: row.realName || '',
    password: '',
    phone: row.phone || '',
    email: row.email || '',
    roleId: row.roleId || null,
    status: row.status
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
      realName: form.value.realName,
      phone: form.value.phone,
      email: form.value.email,
      password: form.value.password || null,
      roleId: form.value.roleId,
      status: 1
    }
    const result =
      dialogMode.value === 'create'
        ? await userApi.add(payload)
        : await userApi.update(editingId.value, payload)
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

const changeStatus = async (row, value) => {
  const result = await userApi.changeStatus(row.id, value)
  if (result.code === 1) {
    ElMessage.success(result.msg)
  } else {
    row.status = value === 1 ? 0 : 1
    ElMessage.error(result.msg)
  }
}

const removeUser = async (row) => {
  await ElMessageBox.confirm(
    `确定删除账号「${row.name}」吗？删除后该账号无法登录。`,
    '删除账号',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  )
  const result = await userApi.remove(row.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    if (rows.value.length === 1 && query.value.page > 1) {
      query.value.page -= 1
    }
    fetchList()
  } else {
    ElMessage.error(result.msg)
  }
}

const avatarText = (row) => (row.realName || row.name || 'U').slice(0, 1).toUpperCase()

const formatTime = (time) => (time ? time.slice(0, 16) : '—')

onMounted(() => {
  loadRoles()
  fetchList()
})
</script>

<template>
  <section class="wk-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">用户管理</h1>
        <p class="wk-page-desc">管理平台登录账号、所属角色，以及账号的启用与停用。</p>
      </div>
      <div class="wk-page-actions">
        <el-button v-if="hasBtnPermission('user:add')" type="primary" :icon="Plus" @click="openCreate">
          添加用户
        </el-button>
      </div>
    </div>

    <section class="wk-card user-card">
      <div class="wk-panel-head user-toolbar">
        <div class="wk-toolbar">
          <el-input
            v-model="query.name"
            class="search-input"
            clearable
            placeholder="搜索姓名、账号"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select
            v-model="query.status"
            class="status-select"
            placeholder="全部状态"
            clearable
            @change="handleSearch"
          >
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button @click="resetSearch">重置</el-button>
        </div>
        <p class="toolbar-count">共 {{ total }} 个账号</p>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        class="wk-table"
        row-key="id"
        :header-cell-style="{ background: '#f9fafb' }"
      >
        <el-table-column label="姓名" min-width="170">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="table-avatar">{{ avatarText(row) }}</span>
              <div>
                <p class="cell-name">{{ row.realName || '未填写姓名' }}</p>
                <p class="cell-sub">ID {{ row.id }}</p>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="账号" min-width="130" />

        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <span class="role-tag" :class="row.role === 1 ? 'is-admin' : ''">
              {{ row.roleName || (row.role === 1 ? '管理员' : '普通用户') }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="用户状态" width="130">
          <template #default="{ row }">
            <div class="status-cell">
              <el-switch
                v-if="hasBtnPermission('user:status')"
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                :disabled="row.role === 1 && row.id === 1"
                @change="(value) => changeStatus(row, value)"
              />
              <span :class="row.status === 1 ? 'status-on' : 'status-off'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="email" label="邮箱" min-width="200">
          <template #default="{ row }">
            <span :class="row.email ? '' : 'wk-muted'">{{ row.email || '未绑定' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column
          v-if="hasBtnPermission('user:edit') || hasBtnPermission('user:delete')"
          label="操作"
          width="150"
          fixed="right"
        >
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="hasBtnPermission('user:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">
                编辑
              </el-button>
              <el-button
                v-if="hasBtnPermission('user:delete') && row.id !== 1"
                link
                type="danger"
                :icon="Delete"
                @click="removeUser(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState compact title="暂无账号数据" hint="点击右上角「添加用户」创建第一个平台账号。" />
        </template>
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
      :title="dialogMode === 'create' ? '添加用户' : '编辑用户'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk>
        <div class="form-grid">
          <el-form-item label="登录账号" prop="name">
            <el-input
              v-model="form.name"
              :disabled="dialogMode === 'edit'"
              placeholder="用于登录，2-20 位"
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="选填" autocomplete="off" />
          </el-form-item>
        </div>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="dialogMode === 'create' ? '请输入初始密码，6-20 位' : '留空则不修改密码'"
            autocomplete="new-password"
          />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="选填" autocomplete="off" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="选填" autocomplete="off" />
          </el-form-item>
        </div>

        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id">
              <span>{{ role.name }}</span>
              <span class="role-option-code">{{ role.code }}</span>
            </el-option>
          </el-select>
          <p class="form-tip">角色决定该账号可见的菜单、可访问的路由和可用按钮。</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ dialogMode === 'create' ? '创建账号' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.user-toolbar {
  flex-wrap: wrap;
}

.search-input {
  width: 248px;
}

.status-select {
  width: 126px;
}

.toolbar-count {
  color: var(--wk-text-3);
  font-size: 12px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--wk-primary-soft);
  color: #0b8b4f;
  font-size: 12px;
  font-weight: 600;
}

.cell-name {
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
}

.cell-sub {
  color: var(--wk-text-3);
  font-size: 10.5px;
  line-height: 1.4;
}

.role-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 5px;
  background: #f2f4f7;
  color: #667085;
  font-size: 11.5px;
}

.role-tag.is-admin {
  background: var(--wk-primary-soft);
  color: #0b8b4f;
}

.status-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-on {
  color: #0b8b4f;
  font-size: 12px;
}

.status-off {
  color: #98a2b3;
  font-size: 12px;
}

.time-text {
  color: #667085;
  font-size: 12px;
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

.role-option-code {
  float: right;
  color: var(--wk-text-3);
  font-size: 11px;
}

@media (max-width: 768px) {
  .wk-panel-head {
    padding: 14px 14px 12px;
  }

  .user-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .wk-toolbar {
    width: 100%;
  }

  .search-input {
    flex: 1;
    width: auto;
    min-width: 150px;
  }

  .status-select {
    width: 112px;
  }

  .toolbar-count {
    display: none;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
