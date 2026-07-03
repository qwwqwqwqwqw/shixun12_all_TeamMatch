<template>
  <div class="report-page">
    <!-- ===== 面包屑 ===== -->
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/board' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>举报处理</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- ===== 标题行 ===== -->
    <div class="report-header">
      <h1 class="page-title">举报管理</h1>
      <div class="report-header-actions">
        <el-button class="btn-filter" :icon="Filter">筛选条件</el-button>
        <el-button class="btn-export" :icon="Download">批量导出</el-button>
      </div>
    </div>

    <!-- ===== 状态筛选标签 ===== -->
    <div class="status-tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.key"
        :class="['status-tab', { active: activeTab === tab.key }]"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- ===== 数据表格 ===== -->
    <div class="table-card">
      <el-table
        :data="filteredList"
        stripe
        border
        class="report-table"
        header-cell-class-name="report-table-header"
      >
        <template #empty>
          <el-empty description="暂无举报数据" :image-size="80" />
        </template>

        <!-- 举报编号 -->
        <el-table-column label="举报ID" width="130">
          <template #default="{ row }">
            <span class="cell-id">#RPT-{{ String(row.id).padStart(3,'0') }}</span>
          </template>
        </el-table-column>

        <!-- 举报类型 -->
        <el-table-column label="举报类型" width="120">
          <template #default="{ row }">
            <span :class="['type-tag', row.targetType === 'user' ? 'type-user' : 'type-project']">
              {{ row.targetType === 'user' ? '举报用户' : '举报项目' }}
            </span>
          </template>
        </el-table-column>

        <!-- 被举报对象 -->
        <el-table-column label="被举报对象" min-width="180">
          <template #default="{ row }">
            <UserLabel v-if="row.targetType === 'user'" :user-id="row.targetId" />
            <ProjectLabel v-else :project-id="row.targetId" />
          </template>
        </el-table-column>

        <!-- 举报人 -->
        <el-table-column label="举报人" width="150">
          <template #default="{ row }">
            <UserLabel :user-id="row.reporterId" />
          </template>
        </el-table-column>

        <!-- 举报原因 -->
        <el-table-column prop="reason" label="举报原因" width="140" />

        <!-- 提交时间 -->
        <el-table-column label="时间" width="170">
          <template #default="{ row }">
            <span class="cell-time">{{ row.createdAt }}</span>
          </template>
        </el-table-column>

        <!-- 状态 -->
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span :class="['status-tag-inner', 'status-' + row.status]">
              {{ statusMap[row.status] }}
            </span>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link class="action-view" @click="handleViewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页器 -->
      <div class="table-footer">
        <span class="pagination-info">
          共 {{ filteredList.length }} 条记录
        </span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="filteredList.length"
          :page-sizes="[10, 20, 50]"
          layout="prev, pager, next"
          background
          small
          class="report-pagination"
        />
      </div>
    </div>

    <!-- ===== 举报详情弹窗 ===== -->
    <el-dialog
      v-model="dialogVisible"
      :title="'举报详情 - #RPT-' + String(currentReport?.id || '').padStart(3,'0')"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
      class="detail-dialog"
    >
      <template v-if="currentReport">
        <!-- 基础信息 -->
        <div class="detail-section">
          <div class="section-title">
            <img src="/assets/CodeBuddyAssets/131_2/15.svg" class="section-icon" />
            <span>基础信息</span>
          </div>
          <div class="info-grid">
            <div class="info-cell">
              <span class="info-label">举报类型</span>
              <span class="info-value">{{ currentReport.targetType === 'user' ? '举报用户' : '举报项目' }}（{{ currentReport.reason }}）</span>
            </div>
            <div class="info-cell">
              <span class="info-label">提交时间</span>
              <span class="info-value">{{ currentReport.createdAt }}</span>
            </div>
            <div class="info-cell">
              <span class="info-label">被举报对象</span>
              <span class="info-value">
                <EntityLabel
                  v-if="currentReport.targetId"
                  :type="currentReport.targetType === 'user' ? 'user' : 'project'"
                  :entity-id="currentReport.targetId"
                  stacked
                />
                <span v-else>-</span>
              </span>
            </div>
            <div class="info-cell">
              <span class="info-label">举报人</span>
              <span class="info-value">
                <EntityLabel
                  v-if="currentReport.reporterId"
                  type="user"
                  :entity-id="currentReport.reporterId"
                  stacked
                />
                <span v-else>-</span>
              </span>
            </div>
          </div>
        </div>

        <!-- 举报理由 -->
        <div class="detail-section">
          <div class="section-title">
            <img src="/assets/CodeBuddyAssets/131_2/16.svg" class="section-icon" />
            <span>举报理由</span>
          </div>
          <div class="reason-box">
            <span>"{{ currentReport.reason }}"</span>
          </div>
        </div>

        <!-- 证据截图 -->
        <div class="detail-section">
          <div class="section-title">
            <img src="/assets/CodeBuddyAssets/131_2/17.svg" class="section-icon" />
            <span>证据截图</span>
          </div>
          <div class="evidence-grid">
            <div
              v-for="(img, idx) in (currentReport.evidenceUrls || [])"
              :key="idx"
              class="evidence-img"
              @click="previewImg = img"
            >
              <img :src="img" />
            </div>
            <div class="evidence-empty" v-if="!currentReport.evidenceUrls || currentReport.evidenceUrls.length === 0">
              <img src="/assets/CodeBuddyAssets/131_2/18.svg" class="empty-icon" />
              <span>暂无证据</span>
            </div>
          </div>
        </div>

        <!-- 处理历史（已处理状态） -->
        <div class="detail-section" v-if="currentReport.status !== 'pending'">
          <div class="section-title">
            <img src="/assets/CodeBuddyAssets/131_2/15.svg" class="section-icon" />
            <span>处理记录</span>
          </div>
          <div class="handle-record">
            <span class="handle-label">处理人：</span>
            <span>{{ currentReport.handler }}</span>
            <span class="handle-sep"></span>
            <span class="handle-label">时间：</span>
            <span>{{ currentReport.handledAt }}</span>
            <br />
            <span class="handle-label">结果：</span>
            <span>{{ currentReport.handleResult }}</span>
          </div>
        </div>
      </template>

      <!-- 弹窗底部 -->
      <template #footer>
        <!-- 待处理：显示操作按钮 -->
        <template v-if="currentReport?.status === 'pending'">
          <el-button class="btn-cancel" @click="dialogVisible = false">取消</el-button>
          <el-button class="btn-dismiss" @click="handleDismiss">驳回举报</el-button>
          <el-button class="btn-resolve" @click="handleResolve">确认解决</el-button>
        </template>
        <!-- 已处理：显示关闭按钮 -->
        <template v-else>
          <el-button class="btn-cancel" @click="dialogVisible = false">关闭</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 图片放大预览 -->
    <div v-if="previewImg" class="img-preview-mask" @click="previewImg = null">
      <img :src="previewImg" class="img-preview-full" @click.stop />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Filter, Download } from '@element-plus/icons-vue'
import { getReportList, handleReport } from '@/api/report'
import { useEntityResolver } from '@/composables/useEntityResolver'
import UserLabel from '@/components/UserLabel.vue'
import ProjectLabel from '@/components/ProjectLabel.vue'
import EntityLabel from '@/components/EntityLabel.vue'

const { loadUsers, loadProjects } = useEntityResolver()

// ===== 筛选状态 =====
const statusTabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待处理' },
  { key: 'resolved', label: '已解决' },
  { key: 'dismissed', label: '已驳回' },
]
const activeTab = ref('all')

const statusMap = { pending: '待处理', resolved: '已解决', dismissed: '已驳回' }

// ===== 数据 =====
const reports = ref([])
const loading = ref(false)

const enrichReports = async (list) => {
  loadUsers([
    ...list.map((r) => r.reporterId),
    ...list.filter((r) => r.targetType === 'user').map((r) => r.targetId),
  ]).catch(() => {})
  loadProjects(list.filter((r) => r.targetType === 'project').map((r) => r.targetId)).catch(() => {})
}

const fetchList = async (status) => {
  loading.value = true
  try {
    const list = await getReportList(status ? { status } : {})
    reports.value = list
    enrichReports(list)
  } finally { loading.value = false }
}
onMounted(() => fetchList())

// 切换筛选时重新请求
const switchTab = (tab) => {
  activeTab.value = tab
  fetchList(tab === 'all' ? null : tab)
}

// ===== 筛选 =====
const filteredList = computed(() => reports.value)

// ===== 分页 =====
const currentPage = ref(1)
const pageSize = ref(10)

// ===== 详情弹窗 =====
const dialogVisible = ref(false)
const currentReport = ref(null)
const previewImg = ref(null) // 点击放大

const handleViewDetail = (row) => {
  currentReport.value = { ...row }
  dialogVisible.value = true
}

// ===== 处理操作 =====
const handleDismiss = async () => {
  await handleReport(currentReport.value.id, { status: 'dismissed', handleResult: '举报不成立，已驳回' })
  dialogVisible.value = false; ElMessage.success('已驳回举报'); fetchList(activeTab.value === 'all' ? null : activeTab.value)
}

const handleResolve = async () => {
  await handleReport(currentReport.value.id, { status: 'resolved', handleResult: '已确认违规，已处理' })
  dialogVisible.value = false; ElMessage.success('已解决举报'); fetchList(activeTab.value === 'all' ? null : activeTab.value)
}
</script>

<style scoped>
/* ================================================================
 * 所有样式从 Figma 131_2 提取
 * 列表页 + 详情弹窗
 * ================================================================ */

/* ===== 面包屑 ===== */
.report-page { width: 100%; }

.report-page :deep(.el-breadcrumb) {
  margin-bottom: 16px;
}
.report-page :deep(.el-breadcrumb__inner) {
  font-size: 13px;
  color: #404752;
  font-weight: 400;
}
.report-page :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #0060A9;
  font-weight: 700;
}

/* ===== 标题行 (Figma: 20px Bold #191C1E) ===== */
.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #191C1E;
}
.report-header-actions {
  display: flex;
  gap: 8px;
}

/* 筛选按钮 (Figma: white, outline #C0C7D4, border-radius 4px) */
.btn-filter {
  height: 42px !important;
  background: #fff !important;
  border: 1px solid #C0C7D4 !important;
  border-radius: 4px !important;
  color: #191C1E !important;
  font-size: 16px !important;
  font-weight: 400 !important;
}
.btn-filter:hover {
  border-color: #409EFF !important;
}

/* 导出按钮 (Figma: bg #0060A9, border-radius 4px, white 16px) */
.btn-export {
  height: 42px !important;
  background: #0060A9 !important;
  border: none !important;
  border-radius: 4px !important;
  color: #fff !important;
  font-size: 16px !important;
  font-weight: 400 !important;
}
.btn-export:hover {
  background: #005299 !important;
}

/* ===== 状态筛选标签 ===== */
.status-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.status-tab {
  padding: 8px 20px;
  border-radius: 20px;
  border: 1px solid #C0C7D4;
  background: #fff;
  color: #404752;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.status-tab:hover {
  border-color: #409EFF;
  color: #409EFF;
}
.status-tab.active {
  background: #0060A9;
  border-color: #0060A9;
  color: #fff;
}

/* ===== 表格卡片 (Figma: white, border-radius 8px, outline #C0C7D4) ===== */
.table-card {
  background: #fff;
  box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  border: 1px solid #C0C7D4;
  overflow: hidden;
}

/* 表头 (Figma: bg #F8F9FB, 16px Bold #404752) */
.report-table :deep(.report-table-header) {
  background: #F8F9FB;
  font-size: 16px;
  font-weight: 700;
  color: #404752;
  border-bottom: 1px solid #C0C7D4;
  padding: 16px 0;
}
.report-table :deep(.el-table__body td) {
  font-size: 16px;
  color: #191C1E;
  padding: 16px 0;
}

/* 举报编号 (Figma: #0060A9, 16px Inter Bold) */
.cell-id {
  color: #0060A9;
  font-family: Inter, sans-serif;
  font-weight: 700;
  font-size: 16px;
}

/* 时间 */
.cell-time {
  font-family: Inter, sans-serif;
  font-size: 16px;
  color: #191C1E;
}

/* 类型标签 */
.type-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}
.type-user {
  background: rgba(64, 158, 255, 0.15);
  color: #005299;
}
.type-project {
  background: rgba(155, 89, 182, 0.15);
  color: #6C3483;
}

/* 状态标签 (Figma: 待处理 bg #FFDAD6 #93000A, 已解决 bg #55AF28 white) */
.status-tag-inner {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
}
.status-pending {
  background: #FFDAD6;
  color: #93000A;
}
.status-resolved {
  background: #55AF28;
  color: #fff;
}
.status-dismissed {
  background: #C0C7D4;
  color: #404752;
}

/* 查看详情 */
.action-view {
  font-size: 16px !important;
  font-weight: 700 !important;
}

/* 分页 */
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

/* ================================================================
 * 详情弹窗 (Figma: 600px, border-radius 8px, backdrop blur)
 * ================================================================ */

.detail-dialog :deep(.el-dialog) {
  border-radius: 8px;
  box-shadow: 0px 25px 50px -12px rgba(0, 0, 0, 0.25);
}
.detail-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #C0C7D4;
  padding: 16px 24px;
  margin: 0;
}
.detail-dialog :deep(.el-dialog__title) {
  font-size: 20px;
  font-family: Inter, sans-serif;
  font-weight: 600;
  color: #191C1E;
}
.detail-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}
.detail-dialog :deep(.el-dialog__footer) {
  background: #F2F4F7;
  padding: 16px 24px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

/* 弹窗 section */
.detail-section {
  margin-bottom: 32px;
}
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 16px;
  color: #0060A9;
  font-weight: 400;
  letter-spacing: 0.80px;
}
.section-icon {
  width: 20px;
  height: 20px;
}

/* 基础信息网格 (Figma: bg #F2F4F7, border-radius 4px) */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px 32px;
  background: #F2F4F7;
  border-radius: 4px;
  padding: 16px;
}
.info-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.info-label {
  font-size: 11px;
  color: #404752;
}
.info-value {
  font-size: 16px;
  font-weight: 600;
  color: #191C1E;
}
.info-value-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
}
.info-value-inline strong {
  color: #191C1E;
}
.id-badge {
  padding: 1px 8px;
  border-radius: 2px;
  background: #D2E4FF;
  color: #55667C;
  font-size: 12px;
  font-family: Inter, sans-serif;
}
.id-badge--light {
  background: rgba(192, 199, 212, 0.30);
  color: #404752;
}

/* 举报理由 (Figma: outline #C0C7D4, italic) */
.reason-box {
  border: 1px solid #C0C7D4;
  border-radius: 4px;
  padding: 17px;
  background: #fff;
}
.reason-box span {
  font-size: 16px;
  color: #404752;
  font-style: italic;
  line-height: 1.6;
}

/* 证据截图 */
.evidence-grid {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.evidence-img {
  width: 173px;
  height: 173px;
  border-radius: 4px;
  overflow: hidden;
  border: 2px solid #C0C7D4;
}
.evidence-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.evidence-empty {
  width: 173px;
  height: 173px;
  background: #F2F4F7;
  border-radius: 4px;
  border: 2px solid #C0C7D4;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.evidence-empty span {
  font-size: 11px;
  color: #C0C7D4;
}
.empty-icon {
  width: 21px;
  height: 21px;
}

/* 处理记录 */
.handle-record {
  font-size: 16px;
  color: #191C1E;
  line-height: 2;
}
.handle-label {
  color: #404752;
}
.handle-sep {
  display: inline-block;
  width: 24px;
}

/* ===== 弹窗按钮 (Figma 规格) ===== */
.btn-cancel {
  height: 42px !important;
  background: #fff !important;
  border: 1px solid #C0C7D4 !important;
  border-radius: 4px !important;
  color: #4F6076 !important;
  font-size: 16px !important;
  padding: 0 24px !important;
}
.btn-dismiss {
  height: 42px !important;
  background: #37485D !important;
  border: none !important;
  border-radius: 4px !important;
  color: #fff !important;
  font-size: 16px !important;
  padding: 0 24px !important;
}
.btn-resolve {
  height: 42px !important;
  background: #409EFF !important;
  border: none !important;
  border-radius: 4px !important;
  box-shadow: 0px 2px 4px -2px rgba(0,0,0,0.10), 0px 4px 6px -1px rgba(0,0,0,0.10) !important;
  color: #fff !important;
  font-size: 16px !important;
  padding: 0 24px !important;
}
.btn-dismiss:hover,
.btn-resolve:hover {
  opacity: 0.9;
}

/* 图片放大预览 */
.img-preview-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.8); z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
}
.img-preview-full {
  max-width: 90vw; max-height: 90vh;
  object-fit: contain; border-radius: 8px;
}
</style>
