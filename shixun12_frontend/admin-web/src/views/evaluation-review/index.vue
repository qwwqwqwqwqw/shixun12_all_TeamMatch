<template>
  <div class="eval-page">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/board' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>评价复核</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="eval-header">
      <h1 class="page-title">评价复核</h1>
      <p class="page-subtitle">对全站范围内被标记为异常或由用户反馈的评价信息进行人工核查。</p>
    </div>

    <!-- 筛选条 -->
    <div class="filter-bar">
      <span class="filter-label">审核状态：</span>
      <div class="filter-tabs">
        <button v-for="t in statusTabs" :key="t.key" :class="['filter-tab', { active: activeTab === t.key }]" @click="switchTab(t.key)">{{ t.label }}</button>
      </div>
      <div class="filter-actions">
        <el-button class="btn-filter" :icon="Filter">筛选器</el-button>
        <el-button class="btn-export" :icon="Download">导出列表</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="pagedList" stripe border class="eval-table" header-cell-class-name="eval-th">
        <template #empty><el-empty description="暂无评价数据" :image-size="80" /></template>

        <el-table-column label="评价编号" width="130"><template #default="{ row }"><span class="cell-id">{{ row.evalNo }}</span></template></el-table-column>
        <el-table-column label="所属项目" width="180">
          <template #default="{ row }">
            <ProjectLabel :project-id="row.projectId" />
          </template>
        </el-table-column>
        <el-table-column label="评价对象" width="150">
          <template #default="{ row }">
            <UserLabel :user-id="row.targetId" />
          </template>
        </el-table-column>
        <el-table-column label="四维均分" width="140">
          <template #default="{ row }">
            <span :class="['cell-score', row.avgScore < 2 ? 'score-low' : row.avgScore >= 5 ? 'score-high' : 'score-normal']">{{ row.avgScore.toFixed(1) }}</span>
            <span class="score-stars-mini">{{ '★'.repeat(Math.round(row.avgScore)) }}{{ '☆'.repeat(5 - Math.round(row.avgScore)) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="异常类型" width="120">
          <template #default="{ row }"><span :class="['anomaly-tag', 'anomaly-' + row.anomalyType]">{{ anomalyMap[row.anomalyType] }}</span></template>
        </el-table-column>
        <el-table-column label="提交时间" width="130"><template #default="{ row }"><span class="cell-time">{{ row.createdAt?.slice(5) }}</span></template></el-table-column>
        <el-table-column label="复核状态" width="130">
          <template #default="{ row }">
            <span class="cell-status">
              <img :src="statusIcon(row.reviewStatus)" class="status-dot" />
              <span :class="'status-' + row.reviewStatus">{{ reviewMap[row.reviewStatus] }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.reviewStatus === 'pending'" type="primary" link class="action-review" @click="handleReview(row)">复核</el-button>
            <span v-else class="cell-done">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer"><span class="info-text">共 {{ filteredList.length }} 条记录</span><el-pagination v-model:current-page="pg" v-model:page-size="ps" :total="filteredList.length" :page-sizes="[10,20,50]" layout="prev,pager,next" background small /></div>
    </div>

    <!-- 复核弹窗 -->
    <el-dialog v-model="dlg" :title="'评价复核 - ' + cur?.evalNo" width="650px" :close-on-click-modal="false" destroy-on-close class="review-dialog">
      <template v-if="cur">
        <!-- 被评价人 -->
        <div class="dlg-hero">
          <div class="hero-avatar">{{ (cur.targetId && String(cur.targetId)[0]) || '?' }}</div>
          <div class="hero-info">
            <UserLabel :user-id="cur.targetId" stacked />
            <ProjectLabel :project-id="cur.projectId" class="hero-project" />
          </div>
        </div>

        <!-- 四维评分 -->
        <div class="dlg-section"><h4>四维评分</h4>
          <div class="score-list">
            <div v-for="(v,k) in cur.scores" :key="k" class="score-row"><span class="dim-name">{{ dimLabels[k] }}</span><span class="dim-stars">{{ '★'.repeat(v) }}{{ '☆'.repeat(5-v) }}</span><span class="dim-val">{{ v }}/5</span></div>
          </div>
        </div>

        <!-- 标签 -->
        <div class="dlg-section" v-if="cur.tags"><h4>评价标签</h4>
          <div class="tag-row"><span v-for="t in cur.tags.positive" :key="t" class="tag-pos">{{ t }}</span><span v-for="t in cur.tags.negative" :key="t" class="tag-neg">{{ t }}</span><span v-if="!cur.tags.positive?.length && !cur.tags.negative?.length" class="tag-none">无</span></div>
        </div>

        <!-- 评价内容 -->
        <div class="dlg-section" v-if="cur.comment"><h4>评价内容</h4><div class="comment-box">{{ cur.comment }}</div></div>

        <!-- 异常说明 -->
        <div class="dlg-section"><h4>异常检测说明</h4><div class="anomaly-box">{{ cur.anomalyReason }}</div></div>

        <!-- 处理记录 -->
        <div class="dlg-section" v-if="cur.reviewStatus !== 'pending'"><h4>处理记录</h4><div class="record-box">处理人：{{ cur.handler }} &nbsp;|&nbsp; 时间：{{ cur.handledAt }}<br/>结果：<span class="record-result">{{ cur.handleResult }}</span></div></div>
      </template>

      <template #footer>
        <template v-if="cur?.reviewStatus === 'pending'">
          <el-button class="ft-cancel" @click="dlg=false">取消</el-button>
          <el-button class="ft-void" @click="doAction('void')">作废</el-button>
          <el-button class="ft-keep" @click="doAction('keep_no_credit')">保留但不计分</el-button>
          <el-button class="ft-approve" @click="doAction('approve')">通过复核</el-button>
        </template>
        <template v-else><el-button class="ft-cancel" @click="dlg=false">关闭</el-button></template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Filter, Download } from '@element-plus/icons-vue'
import { getEvaluations, reviewEvaluation } from '@/api/evaluation'
import { useEntityResolver } from '@/composables/useEntityResolver'
import UserLabel from '@/components/UserLabel.vue'
import ProjectLabel from '@/components/ProjectLabel.vue'

const { loadUsers, loadProjects } = useEntityResolver()

const statusTabs = [
  { key:'all', label:'全部' },{ key:'pending', label:'待复核' },{ key:'approved', label:'正常计分' },
  { key:'voided', label:'已作废' },{ key:'kept_no_credit', label:'保留不计分' }
]
const activeTab = ref('all')
const anomalyMap = { extreme_low:'极端低分', extreme_high:'极端满分', none:'无异常' }
const reviewMap = { pending:'待复核', approved:'正常计分', voided:'已作废', kept_no_credit:'保留不计分' }
const dimLabels = { communication:'沟通协作', task:'任务完成', skill:'技术能力', responsibility:'责任心' }

const statusIcon = (s) => {
  const m = { pending:'/assets/CodeBuddyAssets/189_786/9.svg', approved:'/assets/CodeBuddyAssets/189_786/23.svg', voided:'/assets/CodeBuddyAssets/189_786/30.svg', kept_no_credit:'/assets/CodeBuddyAssets/189_786/37.svg' }
  return m[s] || m.pending
}

/** 顶部 tab → 后端 status 参数 */
const TAB_API_STATUS = {
  pending: 'pending_review',
  approved: 'normal',
  voided: 'voided',
  kept_no_credit: 'kept_no_credit',
}

const mapReviewStatus = (status) => {
  if (!status || status === 'pending_review') return 'pending'
  if (status === 'normal') return 'approved'
  if (status === 'voided') return 'voided'
  if (status === 'kept_no_credit') return 'kept_no_credit'
  return status
}

// API 返回 PendingEvaluationVO: evaluationId, projectId, targetId, ..., status, createdAt
const evals = ref([])
const loading = ref(false)

const fetchList = async () => {
  loading.value = true
  try {
    const tab = activeTab.value
    const params = tab === 'all' ? {} : { status: TAB_API_STATUS[tab] }
    const list = await getEvaluations(params)
    evals.value = Array.isArray(list) ? list : []
    if (!evals.value.length) return
    loadUsers(evals.value.flatMap((e) => [e.targetId, e.evaluatorId])).catch(() => {})
    loadProjects(evals.value.map((e) => e.projectId)).catch(() => {})
  } catch {
    evals.value = []
  } finally {
    loading.value = false
  }
}

const switchTab = (key) => {
  activeTab.value = key
  pg.value = 1
  fetchList()
}

onMounted(fetchList)

const displayList = computed(() => evals.value.map((e) => {
  const avg = Number(e.averageScore) || 0
  const scores = {
    communication: Number(e.communicationScore) || 0,
    task: Number(e.taskScore) || 0,
    skill: Number(e.skillScore) || 0,
    responsibility: Number(e.responsibilityScore) || 0,
  }
  return {
    ...e,
    id: e.evaluationId,
    evalNo: 'EVL-' + String(e.evaluationId).padStart(3, '0'),
    projectId: e.projectId,
    targetId: e.targetId,
    avgScore: avg,
    anomalyType: avg <= 2 ? 'extreme_low' : avg >= 5 ? 'extreme_high' : 'none',
    reviewStatus: mapReviewStatus(e.status),
    scores,
    tags: { positive: [], negative: [] },
    anomalyReason: '由系统标记为待复核评价',
  }
}))

const filteredList = computed(() => displayList.value)
const pg = ref(1); const ps = ref(10)
const pagedList = computed(() => { const s = (pg.value-1)*ps.value; return filteredList.value.slice(s, s+ps.value) })

const dlg = ref(false); const cur = ref(null)
const handleReview = (row) => { cur.value = { ...row }; dlg.value = true }

const actions = {
  void: { title:'确定要作废该评价吗？', msg:'作废后评价将不展示且不计入信誉分。', success:'评价已作废' },
  keep_no_credit: { title:'确定要保留该评价但不计分吗？', msg:'评价将展示但不影响信誉分。', success:'已保留但不计分' },
  approve: { title:'确定要通过该评价的复核吗？', msg:'评价将正常展示并计入信誉分。', success:'复核已通过' },
}

const doAction = async (type) => {
  const a = actions[type]
  ElMessageBox.confirm(a.msg, a.title, { type:'warning' }).then(async () => {
    await reviewEvaluation(cur.value.id, { action: type === 'approve' ? 'approve' : type === 'void' ? 'void' : 'kept_no_credit' })
    dlg.value = false
    ElMessage.success(a.success)
    await fetchList()
  }).catch(() => {})
}
</script>

<style scoped>
.eval-page { width:100% }
.eval-page :deep(.el-breadcrumb) { margin-bottom:16px }
.eval-page :deep(.el-breadcrumb__inner) { font-size:13px; color:#404752; font-weight:400 }
.eval-page :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) { color:#0060A9; font-weight:700 }

.eval-header { margin-bottom:16px }
.page-title { margin:0 0 4px; font-size:24px; font-weight:600; color:#191C1E }
.page-subtitle { margin:0; font-size:14px; color:#404752 }

/* 筛选条 */
.filter-bar { display:flex; align-items:center; background:#fff; box-shadow:0 2px 12px rgba(0,0,0,.05); border-radius:4px; padding:20px; margin-bottom:24px; gap:12px }
.filter-label { font-size:14px; color:#191C1E; white-space:nowrap }
.filter-tabs { display:flex; background:#ECEEF1; border-radius:4px }
.filter-tab { padding:4px 16px; border-radius:4px; border:none; background:transparent; color:#404752; font-size:12px; font-weight:500; cursor:pointer; white-space:nowrap }
.filter-tab.active { background:#fff; box-shadow:0 1px 2px rgba(0,0,0,.05); color:#0060A9; font-weight:600 }
.filter-actions { margin-left:auto; display:flex; gap:8px }
.btn-filter { height:40px!important; border:1px solid #C0C7D4!important; border-radius:4px!important; color:#191C1E!important; font-size:14px!important }
.btn-export { height:40px!important; background:#0060A9!important; border:none!important; border-radius:4px!important; color:#fff!important; font-size:14px!important }

/* 表格 */
.table-card { background:#fff; box-shadow:0 2px 12px rgba(0,0,0,.05); border-radius:4px; overflow:hidden }
.eval-table :deep(.eval-th) { background:#F8F9FB; font-size:16px; font-weight:700; color:#404752; letter-spacing:.8px; padding:27px 0; border-bottom:1px solid #C0C7D4 }
.eval-table :deep(.el-table__row--striped) { background:#F9FBFE }
.eval-table :deep(.el-table__body td) { font-size:16px; color:#191C1E; padding:16px 0; border-color:#ECEEF1 }
.cell-id { color:#191C1E; font-size:16px }
.cell-time { color:#404752; font-size:12px }
.cell-score { font-size:16px; font-family:Inter,sans-serif; font-weight:600 }
.score-low { color:#EF4444 } .score-normal { color:#286C00 } .score-high { color:#0060A9 }
.score-stars-mini { color:#F7BA2A; font-size:12px; margin-left:4px }

.anomaly-tag { display:inline-block; padding:4px 10px; border-radius:12px; font-size:12px; font-weight:600 }
.anomaly-extreme_low { background:#FEE2E2; color:#B91C1C }
.anomaly-extreme_high { background:#DBEAFE; color:#1D4ED8 }
.anomaly-none { background:#DCFCE7; color:#15803D }

.cell-status { display:flex; align-items:center; gap:4px; font-size:16px; font-weight:500 }
.status-dot { width:8px; height:8px }
.status-pending { color:#EA580C } .status-approved { color:#16A34A } .status-voided { color:#DC2626 } .status-kept_no_credit { color:#707784 }

.action-review { font-size:14px!important; font-weight:600!important }
.cell-done { color:#C0C7D4 }

.table-footer { display:flex; justify-content:space-between; align-items:center; padding:17px 24px; border-top:1px solid #C0C7D4 }
.info-text { font-size:13px; color:#404752 }

/* 弹窗 */
.review-dialog :deep(.el-dialog) { border-radius:8px }
.review-dialog :deep(.el-dialog__header) { border-bottom:1px solid #C0C7D4; padding:16px 24px }
.review-dialog :deep(.el-dialog__title) { font-size:20px; font-weight:600; color:#191C1E }
.review-dialog :deep(.el-dialog__body) { padding:24px; max-height:60vh; overflow-y:auto }
.review-dialog :deep(.el-dialog__footer) { background:#F2F4F7; padding:16px 24px; display:flex; justify-content:center; gap:12px }

.dlg-hero { display:flex; align-items:center; gap:12px; margin-bottom:24px }
.hero-avatar { width:48px; height:48px; border-radius:50%; background:#D2E4FF; color:#0060A9; display:flex; align-items:center; justify-content:center; font-size:20px; font-weight:700 }
.hero-info { display:flex; flex-direction:column; gap:2px }
.hero-info strong { font-size:16px; color:#191C1E }
.hero-project { margin-top: 4px; }

.dlg-section { margin-bottom:20px }
.dlg-section h4 { font-size:14px; font-weight:600; color:#0060A9; margin:0 0 10px; letter-spacing:.8px }

.score-list { display:grid; grid-template-columns:1fr 1fr; gap:10px 24px }
.score-row { display:flex; align-items:center; gap:8px }
.dim-name { font-size:13px; color:#404752; width:64px }
.dim-stars { color:#F7BA2A; flex:1 }
.dim-val { font-size:13px; color:#191C1E; font-weight:500 }

.tag-row { display:flex; gap:8px; flex-wrap:wrap }
.tag-pos { padding:3px 10px; border-radius:12px; background:rgba(85,175,40,.15); color:#286C00; font-size:12px }
.tag-neg { padding:3px 10px; border-radius:12px; background:rgba(239,68,68,.10); color:#DC2626; font-size:12px }
.tag-none { color:#C0C7D4; font-size:13px }

.comment-box { background:#F8F9FB; border-radius:4px; padding:14px; font-size:14px; color:#404752; line-height:1.6 }
.anomaly-box { background:rgba(234,88,12,.06); border-left:3px solid #EA580C; border-radius:4px; padding:14px; font-size:13px; color:#404752; line-height:1.6 }
.record-box { font-size:14px; color:#191C1E; line-height:2 }
.record-result { color:#0060A9 }

.ft-cancel { height:42px!important; border:1px solid #C0C7D4!important; border-radius:4px!important; color:#4F6076!important }
.ft-void { height:42px!important; background:#DC2626!important; border:none!important; border-radius:4px!important; color:#fff!important }
.ft-keep { height:42px!important; background:#37485D!important; border:none!important; border-radius:4px!important; color:#fff!important }
.ft-approve { height:42px!important; background:#409EFF!important; border:none!important; border-radius:4px!important; box-shadow:0 2px 4px rgba(0,0,0,.1)!important; color:#fff!important }
</style>
