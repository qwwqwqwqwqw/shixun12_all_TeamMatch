import request from '@/utils/request'

/** 搜索用户 GET /users?keyword=xxx&page=1&size=20 */
export function searchUsers(params) {
  return request({ url: '/users', method: 'get', params })
}

/** 获取用户详情 GET /profile/detail/{userId} */
export function getUserProfile(userId, options = {}) {
  return request({ url: `/profile/detail/${userId}`, method: 'get', ...options })
}

/** 将分页/数组响应统一为用户列表 */
export function normalizeUserList(data) {
  if (!data) return []
  const list = Array.isArray(data) ? data : (data.records || data.list || [])
  return list.map(normalizeUser)
}

/** 归一化用户字段 */
export function normalizeUser(user) {
  if (!user) return null
  const id = user.userId || user.id
  return {
    id,
    nickname: user.nickname || user.nickName || user.name || `用户#${id}`,
    avatarUrl: user.avatarUrl || user.avatar || user.avatar_url || '',
    school: user.school || user.department || '',
  }
}

/** 下拉选项展示：昵称 + ID + 学校，便于区分重名 */
export function formatUserOptionLabel(user) {
  if (!user) return ''
  const parts = [`${user.nickname} (ID: ${user.id})`]
  if (user.school) parts.push(user.school)
  return parts.join(' · ')
}

/**
 * 管理端用户查找：支持用户 ID 精确查 + 昵称模糊搜
 * - 输入纯数字时优先 GET /profile/detail/{id}
 * - 同时走 GET /users?keyword= 做昵称匹配
 */
export async function lookupUsers(query, { page = 1, size = 20 } = {}) {
  const q = String(query || '').trim()
  if (!q) return []

  const results = []
  const seen = new Set()

  const addUser = (user) => {
    const normalized = normalizeUser(user)
    if (!normalized?.id || seen.has(normalized.id)) return
    seen.add(normalized.id)
    results.push(normalized)
  }

  const tasks = []

  if (/^\d+$/.test(q)) {
    tasks.push(
      getUserProfile(Number(q), { silent: true })
        .then(addUser)
        .catch(() => {})
    )
  }

  tasks.push(
    searchUsers({ keyword: q, page, size })
      .then((data) => normalizeUserList(data).forEach(addUser))
      .catch(() => {})
  )

  await Promise.all(tasks)
  return results
}
