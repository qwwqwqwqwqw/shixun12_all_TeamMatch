import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// ===== 错误码映射表 =====
const ERROR_MAP = {
  400: '参数错误，请检查输入',
  401: '登录已过期，请重新登录',
  403: '您没有权限执行此操作',
  404: '请求的资源不存在',
  409: '操作冲突，可能已被处理',
  422: '当前状态不允许此操作',
  500: '服务器繁忙，请稍后重试',

  // M3
  M3000: '登录已过期，请重新登录',

  // M4 组队与退出
  M1003: '操作冲突，可能已被处理',
  M4002: '仅队长可执行此操作',
  M4004: '该成员已不在项目中',
  M4005: '请求的资源不存在',
  M4006: '操作重复，请勿重复提交',
  M4008: '投票已关闭',
  M4009: '队长不可退出项目',
  M4010: '退出投票已超时',
  TEAM_VOTE_CONFLICT: '项目成员状态已更新，请刷新后重试',

  // M5 互评
  M5001: '互评资格校验失败',
  M5002: '评价内容不符合规范',
  M5003: '已超出互评窗口期',
  M5004: '不能重复评价',
  M5005: '不能评价自己',

  // M6 治理
  M6001: '举报提交失败',
  M6002: '申诉提交失败',
  M6003: '处罚执行失败',
  M6004: '该用户已被处罚',
  M6005: '板块名称已存在',

  // 通用
  M3017: '您的账号已被封禁，请联系管理员',
  M9999: '系统内部错误，请稍后重试',
}

// ===== 请求拦截器：注入 Token =====
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('adminToken')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ===== 响应拦截器：统一错误处理 =====
service.interceptors.response.use(
  (response) => {
    const res = response.data

    // 成功：兼容 code: 200 和 code: '00000'
    if (res.code === 200 || res.code === '00000' || res.success) {
      return res.data !== undefined ? res.data : res
    }

    // 业务错误
    const { code, message } = res
    let errorMsg = (message && message !== 'success' && message !== '操作成功') ? message : ''
    if (!errorMsg) {
      errorMsg = ERROR_MAP[code] || ERROR_MAP[String(code)] || '请求失败，请稍后重试'
    }

    if (!response.config.silent) {
      ElMessage.error(errorMsg)
    }

    // 401 / M3000 → 清 token 跳登录
    if (code === 401 || code === 'M3000') {
      localStorage.removeItem('adminToken')
      router.push('/login')
    }

    return Promise.reject(new Error(errorMsg))
  },
  (error) => {
    // HTTP 错误 / 网络异常
    const silent = error.config?.silent
    if (error.response) {
      const status = error.response.status
      const msg = ERROR_MAP[status] || `请求错误 ${status}`
      if (!silent) ElMessage.error(msg)
      if (status === 401) {
        localStorage.removeItem('adminToken')
        router.push('/login')
      }
    } else if (error.code === 'ECONNABORTED') {
      if (!silent) ElMessage.error('请求超时，请稍后重试')
    } else {
      if (!silent) ElMessage.error('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  }
)

export default service
