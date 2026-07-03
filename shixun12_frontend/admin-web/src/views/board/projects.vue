<template>
  <div class="projects-page">
    <!-- 面包屑 -->
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/board' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/board' }">板块管理</el-breadcrumb-item>
      <el-breadcrumb-item>{{ boardName }} 的项目</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="projects-header">
      <div class="projects-header-left">
        <h1 class="page-title">{{ boardName }}</h1>
        <p class="page-subtitle">板块下的所有项目，共 {{ projectList.length }} 个</p>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="projectList" stripe border class="projects-table" header-cell-class-name="table-header-cell" v-loading="loading">
        <template #empty>
          <el-empty description="该板块暂无项目" :image-size="80" />
        </template>
        <el-table-column prop="name" label="项目名称" min-width="200" />
        <el-table-column label="队长" width="160">
          <template #default="{ row }">
            <UserLabel v-if="row.creatorId" :user-id="row.creatorId" />
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column prop="memberCount" label="人数上限" width="100" align="center" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <span :class="['status-tag', row.status === 'active' ? 'status-active' : 'status-done']">
              {{ row.status === 'active' ? '进行中' : '已结束' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="140" align="center">
          <template #default="{ row }">
            <span class="cell-date">{{ row.createdDate }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getBoardProjects } from '@/api/board'
import { useEntityResolver } from '@/composables/useEntityResolver'
import UserLabel from '@/components/UserLabel.vue'

const { loadUsers } = useEntityResolver()

const route = useRoute()
const loading = ref(false)
const rawProjects = ref([])

const boardName = computed(() => route.query.name || '未知板块')
const boardId = computed(() => parseInt(route.params.id) || 0)

// BoardProjectSummaryVO: { id, creatorId, title, status, maxMembers, createdAt }
const projectList = computed(() => {
  return rawProjects.value.map(p => ({
    id: p.id,
    name: p.title || ('项目#' + p.id),
    creatorId: p.creatorId,
    memberCount: p.maxMembers !== undefined ? p.maxMembers : '--',
    status: p.status || 'active',
    createdDate: (p.createdAt || '').slice(0, 10)
  }))
})

const fetchProjects = async () => {
  loading.value = true
  try {
    const data = await getBoardProjects(boardId.value)
    rawProjects.value = Array.isArray(data) ? data : (data.records || data.list || [])
    loadUsers(rawProjects.value.map((p) => p.creatorId)).catch(() => {})
  } catch (e) {
    rawProjects.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchProjects)
</script>

<style scoped>
.projects-page {
  width: 100%;
}

/* 面包屑 */
.projects-page :deep(.el-breadcrumb) {
  margin-bottom: 16px;
}
.projects-page :deep(.el-breadcrumb__inner) {
  font-size: 13px;
  color: #404752;
  font-weight: 400;
}
.projects-page :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #0060A9;
  font-weight: 700;
}

/* 头部 */
.projects-header {
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

/* 表格 */
.table-card {
  background: #fff;
  box-shadow: 0px 2px 12px rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(192, 199, 212, 0.30);
  overflow: hidden;
}
.projects-table :deep(.table-header-cell) {
  background: rgba(242, 244, 247, 0.50);
  font-size: 12px;
  font-weight: 500;
  color: #404752;
  letter-spacing: 0.6px;
  padding: 14px 0;
}
.projects-table :deep(.el-table__body td) {
  font-size: 14px;
  color: #191C1E;
}

/* 状态标签 */
.status-tag {
  display: inline-block;
  padding: 2px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}
.status-active {
  background: rgba(85, 175, 40, 0.20);
  color: #133B00;
}
.status-done {
  background: rgba(192, 199, 212, 0.30);
  color: #404752;
}

/* 日期 */
.cell-date {
  font-size: 14px;
  font-family: Inter, sans-serif;
  color: #404752;
}
</style>
