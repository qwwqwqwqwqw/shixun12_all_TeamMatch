import request from '@/utils/request'

/** 获取评价复核列表 GET /admin/evaluations?status=&projectId= */
export function getEvaluations(params) {
  return request({ url: '/admin/evaluations', method: 'get', params })
}

/** 执行评价复核 POST /admin/evaluations/{id}/review */
export function reviewEvaluation(id, data) {
  return request({ url: `/admin/evaluations/${id}/review`, method: 'post', data })
}

/** @deprecated 请改用 getEvaluations；仅兼容旧 /pending 路径 */
export function getPendingEvaluations(params) {
  return request({ url: '/admin/evaluations/pending', method: 'get', params })
}
