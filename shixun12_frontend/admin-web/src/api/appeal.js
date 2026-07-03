import request from '@/utils/request'

/** 获取申诉列表 GET /admin/appeals?status=xxx&targetType=xxx */
export function getAppealList(params) {
  return request({ url: '/admin/appeals', method: 'get', params })
}

/** 获取申诉详情 GET /admin/appeals/{id} */
export function getAppealDetail(id) {
  return request({ url: `/admin/appeals/${id}`, method: 'get' })
}

/** 处理申诉 PUT /admin/appeals/{id}/handle */
export function handleAppeal(id, data) {
  return request({ url: `/admin/appeals/${id}/handle`, method: 'put', data })
}
