<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Search, Star, StarFilled } from '@element-plus/icons-vue'
import memoApi from '@/api/memo'
import EmptyState from '@/components/EmptyState.vue'

const memos = ref([])
const keyword = ref('')
const loading = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)
const formRef = ref()
const submitting = ref(false)
const form = ref({ content: '', color: 'blue' })

const colorOptions = [
  { value: 'blue', label: '蓝' },
  { value: 'green', label: '绿' },
  { value: 'amber', label: '琥珀' },
  { value: 'violet', label: '紫' }
]

const rules = {
  content: [
    { required: true, message: '请填写备忘内容', trigger: 'blur' },
    { max: 1000, message: '内容不能超过 1000 个字', trigger: 'blur' }
  ]
}

const pinnedCount = computed(() => memos.value.filter((item) => item.pinned === 1).length)

const loadMemos = async () => {
  loading.value = true
  try {
    const result = await memoApi.list({ keyword: keyword.value || undefined })
    if (result.code === 1) {
      memos.value = result.data || []
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
  form.value = { content: '', color: 'blue' }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openEdit = (memo) => {
  dialogMode.value = 'edit'
  editingId.value = memo.id
  form.value = { content: memo.content, color: memo.color || 'blue' }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const submitMemo = async () => {
  if (!formRef.value || submitting.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const result = editingId.value
      ? await memoApi.update(editingId.value, form.value)
      : await memoApi.add(form.value)
    if (result.code === 1) {
      ElMessage.success(result.msg)
      dialogVisible.value = false
      loadMemos()
    } else {
      ElMessage.error(result.msg)
    }
  } finally {
    submitting.value = false
  }
}

const togglePin = async (memo) => {
  const result = await memoApi.togglePin(memo.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    loadMemos()
  } else {
    ElMessage.error(result.msg)
  }
}

const removeMemo = async (memo) => {
  await ElMessageBox.confirm('确定删除这条备忘吗？删除后不可恢复。', '删除备忘', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const result = await memoApi.remove(memo.id)
  if (result.code === 1) {
    ElMessage.success(result.msg)
    loadMemos()
  } else {
    ElMessage.error(result.msg)
  }
}

const formatTime = (time) => (time ? time.slice(0, 16) : '')

onMounted(loadMemos)
</script>

<template>
  <section class="wk-page">
    <div class="wk-page-head">
      <div>
        <h1 class="wk-page-title">备忘录</h1>
        <p class="wk-page-desc">随手记下想法、待确认事项和临时信息，置顶重要内容。</p>
      </div>
      <div class="wk-page-actions">
        <el-button type="primary" :icon="Plus" @click="openCreate">新建备忘</el-button>
      </div>
    </div>

    <section class="wk-card memo-panel">
      <div class="wk-panel-head">
        <div class="wk-toolbar">
          <el-input
            v-model="keyword"
            class="search-input"
            clearable
            placeholder="搜索备忘内容"
            :prefix-icon="Search"
            @keyup.enter="loadMemos"
            @clear="loadMemos"
          />
          <el-button @click="loadMemos">搜索</el-button>
        </div>
        <p class="memo-count">共 {{ memos.length }} 条 · 置顶 {{ pinnedCount }} 条</p>
      </div>

      <div v-loading="loading" class="memo-body">
        <div v-if="memos.length" class="memo-grid">
          <article
            v-for="memo in memos"
            :key="memo.id"
            class="memo-card"
            :class="`memo-${memo.color || 'blue'}`"
          >
            <div class="memo-top">
              <span class="memo-color"></span>
              <span v-if="memo.pinned === 1" class="memo-pin-tag">置顶</span>
              <div class="memo-actions">
                <el-tooltip :content="memo.pinned === 1 ? '取消置顶' : '置顶'" placement="top">
                  <button class="memo-btn" type="button" @click="togglePin(memo)">
                    <el-icon :size="15">
                      <StarFilled v-if="memo.pinned === 1" />
                      <Star v-else />
                    </el-icon>
                  </button>
                </el-tooltip>
                <el-tooltip content="编辑" placement="top">
                  <button class="memo-btn" type="button" @click="openEdit(memo)">
                    <el-icon :size="15"><Edit /></el-icon>
                  </button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <button class="memo-btn is-danger" type="button" @click="removeMemo(memo)">
                    <el-icon :size="15"><Delete /></el-icon>
                  </button>
                </el-tooltip>
              </div>
            </div>

            <p class="memo-content">{{ memo.content }}</p>

            <div class="memo-foot">
              <span>更新于 {{ formatTime(memo.updateTime) }}</span>
            </div>
          </article>
        </div>

        <EmptyState
          v-else
          title="还没有备忘"
          hint="把临时想到的事情记下来，比记住它更可靠。"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建备忘' : '编辑备忘'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk>
        <el-form-item label="备忘内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            maxlength="1000"
            show-word-limit
            resize="none"
            placeholder="例如：登录页先做账号密码登录，验证码放到二期。"
          />
        </el-form-item>
        <el-form-item label="颜色标记">
          <div class="color-row">
            <button
              v-for="option in colorOptions"
              :key="option.value"
              type="button"
              class="color-dot"
              :class="[`dot-${option.value}`, { active: form.color === option.value }]"
              :title="option.label"
              @click="form.color = option.value"
            ></button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMemo">
          {{ dialogMode === 'create' ? '保存备忘' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.search-input {
  width: 240px;
}

.memo-count {
  color: var(--wk-text-3);
  font-size: 12px;
}

.memo-body {
  padding: 16px 22px 22px;
}

.memo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 12px;
}

.memo-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 152px;
  padding: 15px 16px 12px 18px;
  border: 1px solid var(--wk-line);
  border-radius: 12px;
  background: #fff;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.memo-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 3px;
  border-radius: 0 3px 3px 0;
}

.memo-card:hover {
  border-color: #dfe5ec;
  box-shadow: 0 2px 8px rgba(16, 24, 40, 0.05);
}

.memo-blue::before {
  background: #2563eb;
}

.memo-green::before {
  background: var(--wk-primary);
}

.memo-amber::before {
  background: #f79009;
}

.memo-violet::before {
  background: #7c5cd6;
}

.memo-top {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 6px;
  min-height: 24px;
}

.memo-color {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2563eb;
}

.memo-green .memo-color {
  background: var(--wk-primary);
}

.memo-amber .memo-color {
  background: #f79009;
}

.memo-violet .memo-color {
  background: #7c5cd6;
}

.memo-pin-tag {
  padding: 0 6px;
  border-radius: 4px;
  background: #f2f4f7;
  color: #667085;
  font-size: 10.5px;
  line-height: 18px;
}

.memo-actions {
  display: flex;
  gap: 1px;
  margin-left: auto;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.memo-card:hover .memo-actions,
.memo-card:focus-within .memo-actions {
  opacity: 1;
}

.memo-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #98a2b3;
  cursor: pointer;
}

.memo-btn:hover {
  background: #f2f4f7;
  color: #475467;
}

.memo-btn.is-danger:hover {
  background: #fef0ef;
  color: #d92d20;
}

.memo-content {
  flex: 1;
  color: #344054;
  font-size: 13.5px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.memo-foot {
  margin-top: 10px;
  color: #b0b8c4;
  font-size: 10.5px;
}

.color-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.color-dot {
  width: 24px;
  height: 24px;
  padding: 0;
  border: 2px solid #fff;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: 0 0 0 1px var(--wk-line);
}

.color-dot.active {
  box-shadow: 0 0 0 2px var(--wk-primary);
}

.dot-blue {
  background: #2563eb;
}

.dot-green {
  background: var(--wk-primary);
}

.dot-amber {
  background: #f79009;
}

.dot-violet {
  background: #7c5cd6;
}

@media (max-width: 768px) {
  .wk-panel-head {
    padding: 14px 14px 12px;
    flex-wrap: wrap;
  }

  .search-input {
    flex: 1;
    width: auto;
    min-width: 160px;
  }

  .memo-count {
    display: none;
  }

  .memo-body {
    padding: 12px 14px 16px;
  }

  .memo-grid {
    grid-template-columns: 1fr;
  }

  .memo-actions {
    opacity: 1;
  }
}
</style>
