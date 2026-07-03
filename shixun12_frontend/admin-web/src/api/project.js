import request from '@/utils/request'

/** 获取项目详情 GET /m4/projects/{projectId} */
export function getProjectDetail(projectId, options = {}) {
  return request({ url: `/m4/projects/${projectId}`, method: 'get', ...options })
}

/** 归一化项目字段 */
export function normalizeProject(project) {
  if (!project) return null
  const id = project.projectId || project.id
  return {
    id,
    name: project.name || project.title || `项目#${id}`,
  }
}
