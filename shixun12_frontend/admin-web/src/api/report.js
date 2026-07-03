import request from '@/utils/request'

/** 获取举报列表 GET /admin/reports?status=xxx */
export function getReportList(params) {
  return request({ url: '/admin/reports', method: 'get', params })
}

/** 获取举报详情 GET /admin/reports/{id} */
export function getReportDetail(id) {
  return request({ url: `/admin/reports/${id}`, method: 'get' })
}

/** 处理举报 PUT /admin/reports/{id}/handle */
export function handleReport(id, data) {
  return request({ url: `/admin/reports/${id}/handle`, method: 'put', data })
}
