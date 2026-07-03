<template>
  <div class="penalty-page">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/board' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>处罚管理</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 标题行 -->
    <div class="penalty-header">
      <div class="header-left">
        <h1 class="page-title">处罚管理</h1>
        <p class="page-subtitle">管理平台违规行为及用户处罚状态</p>
      </div>
      <el-button type="danger" class="btn-execute" :icon="WarningFilled" @click="handleExecute">执行处罚</el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-icon stat-icon--red"><el-icon :size="20"><WarningFilled /></el-icon></div>
        <div class="stat-info"><span class="stat-label">处罚总数</span><span class="stat-value">{{ penalties.length }}</span></div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon--orange"><el-icon :size="20"><CircleClose /></el-icon></div>
        <div class="stat-info"><span class="stat-label">活跃处罚</span><span class="stat-value">{{ activeCount }}</span></div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <div class="table-toolbar">
        <span class="table-title">处罚记录列表</span>
      </div>
      <el-table :data="pagedList" stripe border class="penalty-table" header-cell-class-name="penalty-th">
        <template #empty><el-empty description="暂无处罚记录" :image-size="80" /></template>

        <el-table-column label="处罚ID" width="120"><template #default="{ row }"><span class="cell-id">{{ row.penaltyNo }}</span></template></el-table-column>
        <el-table-column label="处罚用户" width="180">
          <template #default="{ row }">
            <div class="cell-user">
              <UserLabel :user-id="row.userId" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="处罚类型" width="120"><template #default="{ row }"><span :class="['type-tag', row.penaltyType==='credit_deduct'?'type-deduct':'type-ban']">{{ row.penaltyType==='credit_deduct'?'扣分':'封禁' }}</span></template></el-table-column>
        <el-table-column label="处罚详情" width="170"><template #default="{ row }"><span :class="['cell-detail', row.penaltyType==='credit_deduct'?'detail-deduct':'detail-ban']">{{ row.detail }}</span></template></el-table-column>
        <el-table-column prop="reason" label="处罚原因" width="110" />
        <el-table-column label="处罚时间" width="130"><template #default="{ row }"><span class="cell-time">{{ row.createdAt?.slice(5) }}</span></template></el-table-column>
        <el-table-column label="当前状态" width="110"><template #default="{ row }"><span :class="['status-tag', row.status==='active'?'status-active':'status-revoked']">{{ row.status==='active'?'已生效':'已撤销' }}</span></template></el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status==='active'" type="danger" link class="action-revoke" @click="handleRevoke(row)">撤销处罚</el-button>
            <span v-else class="cell-none">无操作</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer"><span class="info-text">显示 {{ pagedList.length }} 条，共 {{ penalties.length }} 条记录</span><el-pagination v-model:current-page="pg" v-model:page-size="ps" :total="penalties.length" :page-sizes="[10,20,50]" layout="prev,pager,next" background small /></div>
    </div>

    <!-- 执行处罚弹窗 -->
    <el-dialog v-model="dlg" title="执行处罚" width="520px" :close-on-click-modal="false" destroy-on-close class="execute-dialog">
      <el-form :model="form" label-position="top">
        <el-form-item label="被处罚用户" required>
          <el-select
            v-model="form.userId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchUserOptions"
            :loading="userSearchLoading"
            placeholder="输入用户 ID 或昵称搜索"
            style="width:100%"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="formatUserOptionLabel(u)"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="处罚类型" required>
          <el-select v-model="form.penaltyType" placeholder="请选择处罚类型" style="width:100%">
            <el-option label="扣分" value="credit_deduct" />
            <el-option label="封禁" value="function_limit" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.penaltyType==='credit_deduct'" label="扣分值">
          <el-input-number v-model="form.deductScore" :min="1" :max="100" :step="5" style="width:100%" />
        </el-form-item>
        <el-form-item label="处罚原因" required>
          <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请输入处罚原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg=false">取消</el-button>
        <el-button type="danger" :disabled="!canExecute" @click="handleConfirm">确认执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled, CircleClose } from '@element-plus/icons-vue'
import { getPenaltyList, createPenalty, revokePenalty } from '@/api/penalty'
import { lookupUsers, formatUserOptionLabel } from '@/api/user'
import { useEntityResolver } from '@/composables/useEntityResolver'
import UserLabel from '@/components/UserLabel.vue'

const { loadUsers, userName } = useEntityResolver()

// API Penalty: id, userId, type, creditDeductValue, reason, adminId, status, revokedAt, createdAt
const penalties = ref([])

const fetchList = async () => {
  try {
    penalties.value = await getPenaltyList()
    loadUsers(penalties.value.map((p) => p.userId)).catch(() => {})
  } catch {}
}
onMounted(fetchList)

// 映射展示字段
const displayPenalties = computed(() => penalties.value.map(p => {
  const detail = p.type === 'credit_deduct'
    ? `扣除信誉分 ${p.creditDeductValue || 0} 分`
    : '账号封禁'
  const username = userName(p.userId)
  return {
    ...p,
    penaltyNo: 'PNL-' + String(p.id).padStart(3, '0'),
    username,
    penaltyType: p.type,
    detail,
    createdAt: p.createdAt,
    status: p.status
  }
}))

const activeCount = computed(() => penalties.value.filter(p => p.status === 'active').length)
const pg = ref(1); const ps = ref(10)
const pagedList = computed(() => { const s = (pg.value - 1) * ps.value; return displayPenalties.value.slice(s, s + ps.value) })

const handleRevoke = (row) => {
  ElMessageBox.confirm(`确定要撤销对「${row.username}」的处罚吗？撤销后将恢复相关信誉分或解除封禁。`, `撤销处罚`, { type: 'warning' }).then(async () => {
    await revokePenalty(row.id, { reason: '管理员撤销' })
    ElMessage.success('处罚已撤销'); fetchList()
  }).catch(() => {})
}

// 弹窗
const dlg = ref(false)
const userOptions = ref([])
const userSearchLoading = ref(false)
let searchSeq = 0
const form = ref({ userId: '', penaltyType: '', deductScore: 5, reason: '' })
const canExecute = computed(() => form.value.userId && form.value.penaltyType && form.value.reason.trim())

const searchUserOptions = async (keyword) => {
  const q = keyword?.trim()
  if (!q) {
    userOptions.value = []
    return
  }
  const seq = ++searchSeq
  userSearchLoading.value = true
  try {
    const list = await lookupUsers(q)
    if (seq === searchSeq) userOptions.value = list
  } catch {
    if (seq === searchSeq) userOptions.value = []
  } finally {
    if (seq === searchSeq) userSearchLoading.value = false
  }
}

const handleExecute = () => {
  form.value = { userId: '', penaltyType: '', deductScore: 5, reason: '' }
  userOptions.value = []
  dlg.value = true
}

const handleConfirm = async () => {
  if (!canExecute.value) return
  await createPenalty({
    userId: form.value.userId,
    type: form.value.penaltyType,
    creditDeductValue: form.value.penaltyType === 'credit_deduct' ? form.value.deductScore : undefined,
    reason: form.value.reason.trim()
  })
  dlg.value = false; ElMessage.success('处罚已执行'); fetchList()
}
</script>

<style scoped>
.penalty-page { width:100% }
.penalty-page :deep(.el-breadcrumb) { margin-bottom:16px }
.penalty-page :deep(.el-breadcrumb__inner) { font-size:13px; color:#404752; font-weight:400 }
.penalty-page :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) { color:#0060A9; font-weight:700 }

.penalty-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:24px }
.page-title { margin:0 0 4px; font-size:24px; font-weight:600; color:#191C1E }
.page-subtitle { margin:0; font-size:14px; color:#404752 }
.btn-execute { height:39px!important; background:#BA1A1A!important; border:none!important; border-radius:4px!important; box-shadow:0 1px 2px rgba(0,0,0,.05)!important; font-size:12px!important; font-weight:500!important; letter-spacing:.12px }

.stat-row { display:flex; gap:24px; margin-bottom:24px }
.stat-card { flex:1; height:126px; background:#fff; box-shadow:0 2px 12px rgba(0,0,0,.05); border-radius:8px; border:1px solid rgba(192,199,212,.30); display:flex; align-items:center; padding:0 25px; gap:16px }
.stat-icon { width:56px; height:56px; border-radius:4px; display:flex; align-items:center; justify-content:center; flex-shrink:0 }
.stat-icon--red { background:rgba(186,26,26,.10); color:#BA1A1A }
.stat-icon--orange { background:rgba(230,162,60,.10); color:#E6A23C }
.stat-info { display:flex; flex-direction:column }
.stat-label { font-size:12px; font-weight:500; letter-spacing:.6px; color:#404752 }
.stat-value { font-size:30px; font-family:Inter,sans-serif; font-weight:700; color:#191C1E }

.table-card { background:#fff; box-shadow:0 2px 12px rgba(0,0,0,.05); border-radius:8px; border:1px solid rgba(192,199,212,.30); overflow:hidden }
.table-toolbar { padding:18px 24px; background:#F2F4F7; border-bottom:1px solid rgba(192,199,212,.30) }
.table-title { font-size:20px; font-weight:600; color:#191C1E }
.penalty-table :deep(.penalty-th) { background:rgba(230,232,235,.50); font-size:12px; font-weight:600; color:#404752; letter-spacing:.6px; padding:16px 0 }
.penalty-table :deep(.el-table__body td) { font-size:14px; color:#191C1E; padding:16px 0 }
.cell-id { color:#707784; font-family:"Liberation Mono",monospace; font-size:14px }
.cell-user { display:flex; align-items:center; gap:10px }
.user-avatar { width:28px; height:28px; border-radius:50%; background:#D2E4FF; color:#0060A9; display:flex; align-items:center; justify-content:center; font-size:12px; font-weight:600; flex-shrink:0 }
.cell-time { color:#404752; font-size:14px; font-family:Inter,sans-serif }
.cell-detail { font-size:14px; font-weight:600 }
.detail-deduct { color:#BA1A1A; font-family:Inter,sans-serif }
.detail-ban { color:#191C1E }
.cell-none { color:#707784; font-size:13px; font-style:italic }

.type-tag { display:inline-block; padding:2px 10px; border-radius:12px; font-size:12px; font-weight:500 }
.type-deduct { background:#FFDAD6; color:#93000A }
.type-ban { background:#D2E4FF; color:#0A1C30 }

.status-tag { display:inline-block; padding:3px 11px; border-radius:12px; font-size:12px; font-weight:500 }
.status-active { background:#FFDAD6; border:1px solid rgba(186,26,26,.20); color:#BA1A1A }
.status-revoked { background:#E0E3E6; color:#404752 }

.action-revoke { font-size:14px!important; font-weight:600!important }

.table-footer { display:flex; justify-content:space-between; align-items:center; padding:17px 24px; background:#F2F4F7; border-top:1px solid rgba(192,199,212,.30) }
.info-text { font-size:12px; font-family:Inter,sans-serif; color:#404752 }

.execute-dialog :deep(.el-dialog) { border-radius:8px }
.execute-dialog :deep(.el-dialog__header) { border-bottom:1px solid #C0C7D4; padding:16px 24px }
.execute-dialog :deep(.el-dialog__title) { font-size:20px; font-weight:600; color:#191C1E }
.execute-dialog :deep(.el-dialog__footer) { padding:16px 24px; display:flex; justify-content:flex-end; gap:12px }
</style>
