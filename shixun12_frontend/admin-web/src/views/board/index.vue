<template>
  <div class="board-page">
    <!-- ===== 面包屑 (Figma: 13px, 当前 #0060A9 Bold) ===== -->
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/board' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>板块管理</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- ===== 头部: 标题 + 搜索 + 新增按钮 ===== -->
    <div class="board-header">
      <div class="board-header-left">
        <h1 class="page-title">板块管理</h1>
        <p class="page-subtitle">管理及维护平台内的社交板块与权限分配</p>
      </div>
      <div class="board-header-right">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索板块名称、描述、创建人..."
          :prefix-icon="Search"
          clearable
          class="search-bar"
          @input="handleSearch"
        />
        <el-button type="primary" class="btn-add" :icon="Plus" @click="handleAdd">
          新增板块
        </el-button>
      </div>
    </div>

    <!-- ===== 统计卡片 ===== -->
    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-icon stat-icon--blue">
          <el-icon><Grid /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-label">总板块数</span>
          <span class="stat-value">{{ totalBoards }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--green">
          <el-icon><Select /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-label">活跃运行中</span>
          <span class="stat-value">{{ activeCount }}</span>
        </div>
      </div>
    </div>

    <!-- ===== 表格卡片 ===== -->
    <div class="table-card">
      <div class="table-toolbar">
        <h3 class="table-title">板块列表</h3>
      </div>

      <el-table
        :data="pagedList"
        stripe
        border
        class="board-table"
        header-cell-class-name="table-header-cell"
      >
        <!-- 空状态 -->
        <template #empty>
          <el-empty description="暂无数据" :image-size="80" />
        </template>

        <el-table-column label="板块名称" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" class="board-link" @click="router.push({ name:'BoardProjects', params:{ id:row.id }, query:{ name:row.name } })">
              {{ row.name }}
            </el-link>
            <div class="cell-desc">{{ row.description }}</div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span :class="['status-tag', row.status === 'active' ? 'status-active' : 'status-inactive']">
              {{ row.status === 'active' ? '活跃' : '维护中' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />

        <el-table-column label="创建时间" width="160" align="center">
          <template #default="{ row }">
            <span class="cell-date">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" class="action-link" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" class="action-link" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页器 -->
      <div class="table-footer">
        <span class="pagination-info">
          显示 {{ (currentPage - 1) * pageSize + 1 }} 到
          {{ Math.min(currentPage * pageSize, filteredList.length) }} 条，共
          {{ filteredList.length }} 条记录
        </span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="filteredList.length"
          :page-sizes="[10, 20, 50]"
          layout="prev, pager, next"
          background
          small
          class="board-pagination"
        />
      </div>
    </div>

    <!-- ===== 新增 / 编辑弹窗 ===== -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
      class="board-dialog"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
        @submit.prevent
      >
        <el-form-item label="板块名称" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="请输入板块名称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="板块描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入板块描述"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="板块 ICON" prop="icon">
          <el-input
            v-model="formData.icon"
            placeholder="输入 ElementPlus 图标名（如 Monitor）"
            maxlength="30"
          />
          <template #extra>
            <span class="form-extra">默认 Folder，输入错误将使用默认图标</span>
          </template>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!canSave" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Grid, Select, Folder, CirclePlus } from '@element-plus/icons-vue'
import { getBoardList, createBoard, updateBoard, deleteBoard } from '@/api/board'

const router = useRouter()

// ===== 数据 =====
const boardList = ref([])
const loading = ref(false)

const fetchList = async () => {
  loading.value = true
  try { boardList.value = await getBoardList() } finally { loading.value = false }
}
onMounted(fetchList)

// ===== 统计 =====
const activeCount = computed(() => boardList.value.filter(b => b.status === 'active').length)
const totalBoards = computed(() => boardList.value.length)
const recentCount = ref(0)

// ===== 搜索 & 分页 =====
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const filteredList = computed(() => {
  if (!searchKeyword.value.trim()) return boardList.value
  const kw = searchKeyword.value.trim().toLowerCase()
  return boardList.value.filter(b => b.name.toLowerCase().includes(kw) || (b.description||'').toLowerCase().includes(kw))
})

const pagedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const handleSearch = () => { currentPage.value = 1 }

// ===== 格式化 =====
function formatDate(str) {
  if (!str) return ''
  const d = str.slice(0,10)
  const parts = d.split('-')
  return `${parts[1]||'01'}月${parts[2]||'01'}日, ${parts[0]||''}`
}

// ===== 弹窗 =====
const dialogVisible = ref(false)
const dialogTitle = ref('新增板块')
const isEdit = ref(false)
const editingId = ref(null)
const formData = reactive({ name: '', description: '', sortOrder: 0 })
const formRules = { name: [{ required: true, message: '请输入板块名称', trigger: 'blur' }, { min: 1, max: 64, message: '1-64 个字符', trigger: 'blur' }] }
const formRef = ref(null)
const canSave = computed(() => formData.name.trim().length > 0)

const handleAdd = () => {
  isEdit.value = false; dialogTitle.value = '新增板块'
  formData.name = ''; formData.description = ''; formData.sortOrder = 0
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true; editingId.value = row.id; dialogTitle.value = '编辑板块'
  formData.name = row.name; formData.description = row.description || ''; formData.sortOrder = row.sortOrder || 0
  dialogVisible.value = true
}

const handleSave = async () => {
  try { await formRef.value?.validate() } catch { return }
  if (!canSave.value) return
  if (isEdit.value) {
    await updateBoard(editingId.value, { name: formData.name.trim(), description: formData.description.trim(), sortOrder: formData.sortOrder })
    ElMessage.success('板块信息已更新')
  } else {
    await createBoard({ name: formData.name.trim(), description: formData.description.trim(), sortOrder: formData.sortOrder })
    ElMessage.success('新增板块成功')
  }
  dialogVisible.value = false; fetchList()
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除板块「${row.name}」吗？`, '删除确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(async () => { await deleteBoard(row.id); ElMessage.success(`已删除板块「${row.name}」`); fetchList() })
    .catch(() => {})
}
</script>

<style scoped>
/* ================================================================
 * 所有样式从 Figma 3_328 提取 + ElementPlus 扩展保持视觉一致
 * ================================================================ */

.board-page {
  width: 100%;
}

/* ===== 面包屑 (Figma: 13px) ===== */
.board-page :deep(.el-breadcrumb) {
  margin-bottom: 16px;
}
.board-page :deep(.el-breadcrumb__inner) {
  font-size: 13px;
  color: #404752;
  font-weight: 400;
}
.board-page :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #0060A9;
  font-weight: 700;
}

/* ===== 头部 (Figma 标题 30px Bold #191C1E, 副标题 14px #404752) ===== */
.board-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.page-title {
  margin: 0 0 4px;
  font-size: 30px;
  font-weight: 700;
  color: #191C1E;
  line-height: 38px;
}
.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: #404752;
}
.board-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 搜索框 */
.search-bar {
  width: 260px;
}
.search-bar :deep(.el-input__wrapper) {
  height: 38px;
  border-radius: 4px;
  border: 1px solid #C0C7D4;
  box-shadow: none;
}
.search-bar :deep(.el-input__wrapper:hover) {
  border-color: #409EFF;
}
.search-bar :deep(.el-input__inner) {
  font-size: 13px;
}
.search-bar :deep(.el-input__inner::placeholder) {
  color: #6B7280;
}

/* 新增按钮 (Figma: bg #0060A9, 圆角 4px) */
.btn-add {
  height: 32px !important;
  background: #0060A9 !important;
  border: none !important;
  border-radius: 4px !important;
  box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.05);
  font-size: 12px !important;
  font-weight: 500 !important;
  padding: 0 16px !important;
}
.btn-add:hover {
  background: #005299 !important;
}

/* ===== 统计卡片 (Figma: 235x98, border-radius 8px) ===== */
.stat-row {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}
.stat-card {
  flex: 1;
  min-width: 220px;
  height: 98px;
  background: #fff;
  box-shadow: 0px 2px 12px rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(192, 199, 212, 0.30);
  display: flex;
  align-items: center;
  padding: 25px;
  gap: 16px;
  box-sizing: border-box;
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 22px;
}
.stat-icon--blue {
  background: rgba(64, 158, 255, 0.10);
  color: #409EFF;
}
.stat-icon--green {
  background: rgba(85, 175, 40, 0.10);
  color: #55AF28;
}
.stat-icon--orange {
  background: rgba(230, 162, 60, 0.10);
  color: #E6A23C;
}
.stat-icon--light {
  background: rgba(210, 228, 255, 0.10);
  color: #0060A9;
}
.stat-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.stat-label {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.12px;
  color: #404752;
  white-space: nowrap;
}
.stat-value {
  font-size: 24px;
  font-family: Inter, -apple-system, sans-serif;
  font-weight: 600;
  color: #191C1E;
  line-height: 32px;
}

/* ===== 表格卡片 ===== */
.table-card {
  background: #fff;
  box-shadow: 0px 2px 12px rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(192, 199, 212, 0.30);
  overflow: hidden;
}

.table-toolbar {
  padding: 17px 24px;
  border-bottom: 1px solid rgba(192, 199, 212, 0.50);
}
.table-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #191C1E;
}

/* 表头 (Figma: 12px #404752, bg rgba(242,244,247,0.50)) */
.board-table :deep(.table-header-cell) {
  background: rgba(242, 244, 247, 0.50);
  font-size: 12px;
  font-weight: 500;
  color: #404752;
  letter-spacing: 0.6px;
  padding: 14px 0;
}
.board-table :deep(.el-table__body td) {
  font-size: 14px;
  color: #191C1E;
}

/* 名称加粗 */
.cell-name-text {
  font-weight: 600;
}

/* 板块链接 */
.board-link {
  font-size: 14px !important;
  font-weight: 600 !important;
}
.board-link:hover {
  text-decoration: underline !important;
}
.cell-desc {
  font-size: 12px;
  color: #727784;
  margin-top: 4px;
}

/* 项目数量可点击链接 */
.project-link {
  font-weight: 500;
}
.project-link:hover {
  text-decoration: underline !important;
}

/* 日期 */
.cell-date {
  font-size: 14px;
  font-family: Inter, sans-serif;
  color: #404752;
}

/* 热度指数 */
.hot-value {
  font-size: 14px;
  font-family: Inter, sans-serif;
  font-weight: 500;
  color: #404752;
}
.hot-high {
  color: #F56C6C;
  font-weight: 600;
}

/* 状态标签 */
.status-tag { display:inline-block; padding:2px 10px; border-radius:12px; font-size:12px; font-weight:500 }
.status-active { background:rgba(85,175,40,.20); color:#133B00 }
.status-inactive { background:rgba(192,199,212,.30); color:#404752 }

/* 操作按钮 */
.action-link {
  font-size: 12px !important;
  font-weight: 500 !important;
  letter-spacing: 0.12px !important;
  padding: 0 !important;
}

/* ===== 分页 (Figma: 13px #404752) ===== */
.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 17px 24px;
  border-top: 1px solid rgba(192, 199, 212, 0.50);
}
.pagination-info {
  font-size: 13px;
  font-family: Inter, sans-serif;
  color: #404752;
}
.board-pagination :deep(.el-pager li) {
  border-radius: 4px;
  border: 1px solid #C0C7D4;
  background: #fff;
  font-size: 14px;
  font-family: Inter, sans-serif;
  color: #404752;
}
.board-pagination :deep(.el-pager li.is-active) {
  background: #0060A9;
  border-color: #0060A9;
  color: #fff;
  font-size: 12px;
  font-weight: 500;
}

/* ===== 弹窗 ===== */
.board-dialog :deep(.el-dialog) {
  border-radius: 8px;
}
.board-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: #191C1E;
}
.form-extra {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
