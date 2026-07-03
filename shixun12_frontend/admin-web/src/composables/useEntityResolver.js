import { ref } from 'vue'
import { getUserProfile, normalizeUser } from '@/api/user'
import { getProjectDetail, normalizeProject } from '@/api/project'

// 会话级缓存，跨页面复用
const userCache = ref({})
const projectCache = ref({})

const toKey = (id) => String(id)

export function useEntityResolver() {
  const loadUsers = async (userIds) => {
    const ids = [...new Set(userIds.map(toKey))].filter((id) => id && !userCache.value[id])
    if (!ids.length) return

    const results = await Promise.allSettled(
      ids.map((id) => getUserProfile(id, { silent: true }))
    )
    const next = { ...userCache.value }
    results.forEach((result, index) => {
      if (result.status !== 'fulfilled' || !result.value) return
      const user = normalizeUser(result.value)
      if (user?.id) next[toKey(user.id)] = user
    })
    userCache.value = next
  }

  const loadProjects = async (projectIds) => {
    const ids = [...new Set(projectIds.map(toKey))].filter((id) => id && !projectCache.value[id])
    if (!ids.length) return

    const results = await Promise.allSettled(
      ids.map((id) => getProjectDetail(id, { silent: true }))
    )
    const next = { ...projectCache.value }
    results.forEach((result) => {
      if (result.status !== 'fulfilled' || !result.value) return
      const project = normalizeProject(result.value)
      if (project?.id) next[toKey(project.id)] = project
    })
    projectCache.value = next
  }

  const userName = (userId) => {
    if (userId == null || userId === '') return '-'
    return userCache.value[toKey(userId)]?.nickname || `用户#${userId}`
  }
  const projectName = (projectId) => {
    if (projectId == null || projectId === '') return '-'
    return projectCache.value[toKey(projectId)]?.name || `项目#${projectId}`
  }

  return { userCache, projectCache, loadUsers, loadProjects, userName, projectName }
}
