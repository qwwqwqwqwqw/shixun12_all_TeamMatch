<template>
  <div class="appeal-page">
    <!-- ===== 面包屑 ===== -->
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/board' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>申诉处理</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- ===== 标题行 ===== -->
    <div class="appeal-header">
      <div class="appeal-header-left">
        <h1 class="page-title">申诉处理</h1>
        <p class="page-subtitle">查看、审核及处理用户发起的评价与处罚申诉请求。</p>
      </div>
      <div class="appeal-header-right">
        <el-button class="btn-export" :icon="Download">导出报表</el-button>
        <el-button class="btn-refresh" :icon="Refresh" @click="handleRefresh">刷新数据</el-button>
      </div>
    </div>

    <!-- ===== 筛选条 (Figma: bg #ECEEF1, 搜索 + 状态 tab) ===== -->
    <div class="filter-bar">
      <span class="filter-label">申诉状态：</span>
      <div class="filter-tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.key"
          :class="['filter-tab', { active: activeTab === tab.key }]"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="filter-search">
        <img src="/assets/CodeBuddyAssets/154_215/17.svg" class="search-icon" />
        <input
          v-model="searchKeyword"
          placeholder="搜索编号/用户ID/昵称"
          class="search-input"
          @input="handleSearch"
        />
      </div>
    </div>

    <!-- ===== 数据表格 ===== -->
    <div class="table-card">
      <el-table
        :data="pagedList"
        stripe
        border
        class="appeal-table"
        header-cell-class-name="appeal-table-header"
      >
        <template #empty>
          <el-empty description="暂无申诉数据" :image-size="80" />
        </template>

        <el-table-column label="申诉编号" width="120">
          <template #default="{ row }">
            <span class="cell-id">#APL-{{ String(row.id).padStart(3,'0') }}</span>
          </template>
        </el-table-column>

        <el-table-column label="申诉类型" width="120">
          <template #default="{ row }">
            <span :class="['type-tag', row.targetType === 'evaluation' ? 'type-eval' : 'type-penalty']">
              {{ row.targetType === 'evaluation' ? '评价申诉' : '处罚申诉' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="申诉对象" min-width="180">
          <template #default="{ row }">{{ row.targetType === 'evaluation' ? '评价#' : '处罚#' }}{{ row.targetId }}</template>
        </el-table-column>

        <el-table-column label="申诉人" width="150">
          <template #default="{ row }">
            <UserLabel :user-id="row.userId" />
          </template>
        </el-table-column>

        <el-table-column prop="reason" label="申诉原因" min-width="140" />

        <el-table-column label="提交时间" width="130">
          <template #default="{ row }">
            <span class="cell-time">{{ row.createdAt?.slice(5) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="cell-status">
              <img :src="statusIcon(row.status)" class="status-dot" />
              <span>{{ statusMap[row.status] }}</span>
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link class="action-view" @click="handleViewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="table-footer">
        <span class="pagination-info">显示 {{ pagedList.length }} 条，共 {{ filteredList.length }} 条记录</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="filteredList.length"
          :page-sizes="[10, 20, 50]"
          layout="prev, pager, next"
          background
          small
          class="appeal-pagination"
        />
      </div>
    </div>

    <!-- ===== 详情弹窗 ===== -->
    <el-dialog
      v-model="dialogVisible"
      :title="'申诉详情 - #APL-' + String(currentAppeal?.id || '').padStart(3,'0')"
      width="650px"
      :close-on-click-modal="false"
      destroy-on-close
      class="detail-dialog"
    >
      <template v-if="currentAppeal">
        <!-- 基本信息 -->
        <div class="detail-section">
          <h4 class="section-title">基本信息</h4>
          <div class="info-grid">
            <div class="info-cell">
              <span class="info-label">申诉类型</span>
              <span class="info-value">
                <span :class="['type-tag', currentAppeal.targetType === 'evaluation' ? 'type-eval' : 'type-penalty']">
                  {{ currentAppeal.targetType === 'evaluation' ? '评价申诉' : '处罚申诉' }}
                </span>
              </span>
            </div>
            <div class="info-cell">
              <span class="info-label">申诉对象ID</span>
              <span class="info-value">{{ currentAppeal.targetId || '-' }}</span>
            </div>
            <div class="info-cell">
              <span class="info-label">申诉人</span>
              <span class="info-value">
                <UserLabel v-if="currentAppeal.userId" :user-id="currentAppeal.userId" stacked />
                <span v-else>-</span>
              </span>
            </div>
            <div class="info-cell">
              <span class="info-label">提交时间</span>
              <span class="info-value">{{ currentAppeal.createdAt }}</span>
            </div>
          </div>
        </div>

        <!-- 申诉原因 -->
        <div class="detail-section">
          <h4 class="section-title">申诉原因</h4>
          <div class="reason-box">{{ currentAppeal.reason }}</div>
        </div>

        <!-- 证据截图 -->
        <div class="detail-section" v-if="currentAppeal.evidenceUrls && currentAppeal.evidenceUrls.length">
          <h4 class="section-title">证据截图</h4>
          <div class="evidence-grid">
            <div
              v-for="(img, idx) in currentAppeal.evidenceUrls"
              :key="idx"
              class="evidence-img"
              @click="previewImg = img"
            >
              <img :src="img" />
            </div>
          </div>
        </div>

        <!-- 关联对象详情 -->
        <div class="detail-section" v-if="currentAppeal.relatedDetail">
          <h4 class="section-title">
            {{ currentAppeal.targetType === 'evaluation' ? '原评价详情' : '处罚详情' }}
          </h4>
          <div class="related-card">
            <!-- 评价申诉 -->
            <template v-if="currentAppeal.targetType === 'evaluation'">
              <div class="score-grid">
                <div v-for="(score, key) in currentAppeal.relatedDetail.scores" :key="key" class="score-item">
                  <span class="score-label">{{ scoreLabels[key] }}</span>
                  <div class="score-stars">
                    <span v-for="s in 5" :key="s" :class="['star', { filled: s <= score }]">★</span>
                  </div>
                </div>
              </div>
              <div class="eval-tags" v-if="currentAppeal.relatedDetail.tags?.length">
                <span v-for="tag in currentAppeal.relatedDetail.tags" :key="tag" class="eval-tag">{{ tag }}</span>
              </div>
              <div class="eval-comment" v-if="currentAppeal.relatedDetail.comment">
                <span class="comment-label">评价内容：</span>{{ currentAppeal.relatedDetail.comment }}
              </div>
            </template>
            <!-- 处罚申诉 -->
            <template v-else>
              <div class="penalty-grid">
                <div class="penalty-item">
                  <span class="penalty-label">处罚类型</span>
                  <span class="penalty-value">{{ penaltyTypeMap[currentAppeal.relatedDetail.penaltyType] || currentAppeal.relatedDetail.penaltyType }}</span>
                </div>
                <div class="penalty-item">
                  <span class="penalty-label">处罚详情</span>
                  <span class="penalty-value penalty-highlight">{{ currentAppeal.relatedDetail.value }}</span>
                </div>
                <div class="penalty-item">
                  <span class="penalty-label">处罚原因</span>
                  <span class="penalty-value">{{ currentAppeal.relatedDetail.penaltyReason }}</span>
                </div>
                <div class="penalty-item">
                  <span class="penalty-label">执行人</span>
                  <span class="penalty-value">{{ currentAppeal.relatedDetail.adminName }}</span>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- 处理记录（已处理状态） -->
        <div class="detail-section" v-if="currentAppeal.status !== 'pending'">
          <h4 class="section-title">处理记录</h4>
          <div class="record-box">
            <span>处理人：<UserLabel v-if="currentAppeal.handlerId" :user-id="currentAppeal.handlerId" /><span v-else>-</span></span>
            <span class="record-sep">|</span>
            <span>时间：{{ currentAppeal.handledAt }}</span>
            <br />
            <span class="record-result">结果：{{ currentAppeal.handleResult }}</span>
          </div>
        </div>
      </template>

      <template #footer>
        <template v-if="currentAppeal?.status === 'pending'">
          <el-button class="btn-cancel" @click="dialogVisible = false">取消</el-button>
          <el-button class="btn-reject" @click="handleReject">驳回申诉</el-button>
          <el-button class="btn-approve" @click="handleApprove">通过申诉</el-button>
        </template>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import { getAppealList, handleAppeal } from '@/api/appeal'
import { useEntityResolver } from '@/composables/useEntityResolver'
import UserLabel from '@/components/UserLabel.vue'

const { loadUsers, userName } = useEntityResolver()

// ===== 状态管理 =====
const statusTabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待处理' },
  { key: 'approved', label: '已通过' },
  { key: 'rejected', label: '已驳回' },
]
const activeTab = ref('all')
const statusMap = { pending: '待处理', approved: '已通过', rejected: '已驳回' }
const scoreLabels = { communication: '沟通协作', task: '任务完成', skill: '技术能力', responsibility: '责任心' }
const penaltyTypeMap = { credit_deduct: '信誉扣分', function_limit: '账号封禁' }

const statusIcon = (status) => {
  const map = {
    pending: '/assets/CodeBuddyAssets/154_215/18.svg',
    approved: '/assets/CodeBuddyAssets/154_215/20.svg',
    rejected: '/assets/CodeBuddyAssets/154_215/21.svg',
  }
  return map[status] || map.pending
}

// ===== 数据 =====
const appeals = ref([])
const fetchList = async (status) => {
  try {
    const list = await getAppealList(status ? { status } : {})
    appeals.value = list
    loadUsers(list.flatMap((a) => [a.userId, a.handlerId])).catch(() => {})
  } catch {}
}
onMounted(() => fetchList())

const switchTab = (tab) => { activeTab.value = tab; fetchList(tab === 'all' ? null : tab) }

const handleRefresh = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  fetchList(activeTab.value === 'all' ? null : activeTab.value)
}

// ===== 搜索 & 分页 =====
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const filteredList = computed(() => {
  let list = appeals.value
  const kw = searchKeyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter((a) => {
      const name = userName(a.userId).toLowerCase()
      return String(a.id).includes(kw) || String(a.userId).includes(kw) || name.includes(kw)
    })
  }
  return list
})

const pagedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const handleSearch = () => { currentPage.value = 1 }

// ===== 详情弹窗 + 处理操作 =====
const dialogVisible = ref(false)
const currentAppeal = ref(null)
const previewImg = ref(null)

const handleViewDetail = (row) => { currentAppeal.value = { ...row }; dialogVisible.value = true }

const handleReject = () => {
  ElMessageBox.confirm('确定要驳回该申诉吗？', '确认操作', { type: 'warning' }).then(async () => {
    await handleAppeal(currentAppeal.value.id, { status: 'rejected', handleResult: '申诉驳回' })
    dialogVisible.value = false; ElMessage.success('申诉已驳回'); fetchList(activeTab.value === 'all' ? null : activeTab.value)
  }).catch(() => {})
}

const handleApprove = () => {
  ElMessageBox.confirm('确定要通过该申诉吗？', '确认操作', { type: 'warning' }).then(async () => {
    await handleAppeal(currentAppeal.value.id, { status: 'approved', handleResult: '申诉通过' })
    dialogVisible.value = false; ElMessage.success('申诉已通过'); fetchList(activeTab.value === 'all' ? null : activeTab.value)
  }).catch(() => {})
}
</script>

<style scoped>
.appeal-page { width: 100%; }

.appeal-page :deep(.el-breadcrumb) { margin-bottom: 16px; }
.appeal-page :deep(.el-breadcrumb__inner) { font-size: 13px; color: #404752; font-weight: 400; }
.appeal-page :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) { color: #0060A9; font-weight: 700; }

/* 标题行 */
.appeal-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.page-title { margin: 0 0 4px; font-size: 30px; font-weight: 700; color: #191C1E; }
.page-subtitle { margin: 0; font-size: 14px; color: #404752; }
.appeal-header-right { display: flex; gap: 12px; align-items: center; }
.btn-export { height: 34px !important; border: 1px solid #C0C7D4 !important; border-radius: 4px !important; color: #0060A9 !important; font-size: 12px !important; background: #fff !important; }
.btn-refresh { height: 34px !important; background: #0060A9 !important; border: none !important; border-radius: 4px !important; color: #fff !important; font-size: 12px !important; box-shadow: 0px 1px 2px rgba(0,0,0,0.05); }

/* 筛选条 (Figma: bg #ECEEF1, border-radius 4px) */
.filter-bar { display: flex; align-items: center; background: white; box-shadow: 0px 2px 12px rgba(0,0,0,0.05); border-radius: 8px; padding: 20px; margin-bottom: 16px; gap: 12px; }
.filter-label { font-size: 12px; font-weight: 500; color: #404752; white-space: nowrap; }
.filter-tabs { display: flex; background: #ECEEF1; border-radius: 4px; }
.filter-tab { padding: 4px 16px; border-radius: 4px; border: none; background: transparent; color: #404752; font-size: 12px; font-weight: 500; cursor: pointer; transition: .2s; }
.filter-tab.active { background: #fff; box-shadow: 0px 1px 2px rgba(0,0,0,0.05); color: #0060A9; }
.filter-search { position: relative; margin-left: auto; }
.search-input { width: 256px; height: 40px; border-radius: 4px; border: 1px solid transparent; background: #F2F4F7; padding: 0 12px 0 41px; font-size: 14px; color: #191C1E; outline: none; }
.search-input:focus { border-color: #409EFF; background: #fff; }
.search-input::placeholder { color: #6B7280; }
.search-icon { position: absolute; left: 12px; top: 10px; width: 20px; height: 20px; }

/* 表格 */
.table-card { background: #fff; box-shadow: 0px 2px 12px rgba(0,0,0,0.05); border-radius: 8px; overflow: hidden; }
.appeal-table :deep(.appeal-table-header) { background: #F8F9FB; font-size: 12px; font-weight: 500; color: #404752; border-bottom: 1px solid #C0C7D4; padding: 16px 0; letter-spacing: 0.12px; }
.appeal-table :deep(.el-table__body td) { font-size: 14px; color: #191C1E; padding: 16px 0; }
.cell-id { color: #707784; font-family: Inter, sans-serif; font-weight: 600; font-size: 11px; }
.cell-time { color: #404752; font-family: Inter, sans-serif; font-size: 13px; }
.cell-status { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #4F6076; }
.status-dot { width: 6px; height: 6px; }

/* 类型标签 */
.type-tag { display: inline-block; padding: 3px 9px; border-radius: 2px; font-size: 11px; font-weight: 600; }
.type-eval { background: rgba(64,158,255,0.10); border: 1px solid rgba(64,158,255,0.20); color: #0060A9; }
.type-penalty { background: rgba(255,218,214,0.10); border: 1px solid rgba(255,218,214,0.20); color: #BA1A1A; }
.action-view { font-size: 12px !important; font-weight: 500 !important; }

.table-footer { display: flex; justify-content: space-between; align-items: center; padding: 17px 24px; border-top: 1px solid #C0C7D4; }
.pagination-info { font-size: 12px; font-family: Inter, sans-serif; color: #404752; }

/* ===== 弹窗 ===== */
.detail-dialog :deep(.el-dialog) { border-radius: 8px; }
.detail-dialog :deep(.el-dialog__header) { border-bottom: 1px solid #C0C7D4; padding: 16px 24px; }
.detail-dialog :deep(.el-dialog__title) { font-size: 20px; font-weight: 600; color: #191C1E; }
.detail-dialog :deep(.el-dialog__body) { padding: 24px; max-height: 60vh; overflow-y: auto; }
.detail-dialog :deep(.el-dialog__footer) { background: #F2F4F7; padding: 16px 24px; display: flex; justify-content: center; gap: 12px; }

.detail-section { margin-bottom: 24px; }
.section-title { font-size: 14px; font-weight: 600; color: #0060A9; margin: 0 0 12px; letter-spacing: 0.8px; }

/* 基本信息 */
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px 32px; background: #F2F4F7; border-radius: 4px; padding: 16px; }
.info-cell { display: flex; flex-direction: column; gap: 4px; }
.info-label { font-size: 11px; color: #404752; }
.info-value { font-size: 14px; font-weight: 600; color: #191C1E; }

/* 申诉原因 */
.reason-box { border: 1px solid #C0C7D4; border-radius: 4px; padding: 16px; font-size: 14px; color: #404752; line-height: 1.7; background: #fff; }

/* 关联卡片 */
.related-card { background: #F8F9FB; border-radius: 4px; padding: 16px; }

/* 评分星星 */
.score-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 24px; margin-bottom: 12px; }
.score-item { display: flex; justify-content: space-between; align-items: center; }
.score-label { font-size: 13px; color: #404752; }
.score-stars { display: flex; gap: 2px; }
.star { font-size: 16px; color: #C0C7D4; }
.star.filled { color: #F7BA2A; }
.eval-tags { display: flex; gap: 8px; margin-bottom: 12px; }
.eval-tag { padding: 2px 10px; border-radius: 12px; background: rgba(192,199,212,0.25); font-size: 12px; color: #404752; }
.eval-comment { font-size: 13px; color: #404752; }
.comment-label { color: #707784; }

/* 处罚详情 */
.penalty-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 24px; }
.penalty-item { display: flex; flex-direction: column; gap: 4px; }
.penalty-label { font-size: 12px; color: #707784; }
.penalty-value { font-size: 14px; color: #191C1E; font-weight: 500; }
.penalty-highlight { color: #BA1A1A; font-weight: 600; }

/* 处理记录 */
.record-box { font-size: 14px; color: #191C1E; line-height: 2; }
.record-sep { margin: 0 12px; color: #C0C7D4; }
.record-result { color: #0060A9; }

/* 弹窗按钮 */
.btn-cancel { height: 42px !important; background: #fff !important; border: 1px solid #C0C7D4 !important; border-radius: 4px !important; color: #4F6076 !important; font-size: 14px !important; }
.btn-reject { height: 42px !important; background: #37485D !important; border: none !important; border-radius: 4px !important; color: #fff !important; font-size: 14px !important; }
.btn-approve { height: 42px !important; background: #409EFF !important; border: none !important; border-radius: 4px !important; box-shadow: 0px 2px 4px rgba(0,0,0,0.1) !important; color: #fff !important; font-size: 14px !important; }

/* 证据截图 */
.evidence-grid { display: flex; gap: 12px; flex-wrap: wrap; }
.evidence-img { width: 120px; height: 120px; border-radius: 4px; overflow: hidden; border: 1px solid #C0C7D4; cursor: pointer; }
.evidence-img img { width: 100%; height: 100%; object-fit: cover; transition: transform .2s; }
.evidence-img:hover img { transform: scale(1.05); }

/* 图片放大预览 */
.img-preview-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.8); z-index: 9999; display: flex; align-items: center; justify-content: center; cursor: pointer; }
.img-preview-full { max-width: 90vw; max-height: 90vh; object-fit: contain; border-radius: 8px; }
</style>
