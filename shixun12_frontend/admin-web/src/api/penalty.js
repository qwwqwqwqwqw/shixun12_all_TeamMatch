import request from '@/utils/request'

/** 创建处罚 POST /admin/penalties */
export function createPenalty(data) {
  return request({ url: '/admin/penalties', method: 'post', data })
}

/** 获取处罚列表 GET /admin/penalties?status=xxx&type=xxx */
export function getPenaltyList(params) {
  return request({ url: '/admin/penalties', method: 'get', params })
}

/** 获取处罚详情 GET /admin/penalties/{id} */
export function getPenaltyDetail(id) {
  return request({ url: `/admin/penalties/${id}`, method: 'get' })
}

/** 撤销处罚 PUT /admin/penalties/{id}/revoke */
export function revokePenalty(id, data) {
  return request({ url: `/admin/penalties/${id}/revoke`, method: 'put', data })
}
