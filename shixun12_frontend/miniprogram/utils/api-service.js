/**
 * TeamMatch 统一 API 服务层
 * 封装 wx.request，自动注入 Token、统一处理响应格式
 * 支持全局 Mock 模式（USE_MOCK = true 时不发真实请求）
 *
 * @module api-service
 * @description 后端基地址: http://localhost:8080/api
 *              成功响应码: "00000"
 *              认证方式: Bearer Token
 */

// var BASE_URL = 'http://localhost:8080/api'
var BASE_URL = 'https://api.teammatch.top/api'

// ========== 全局 Mock 开关 ==========
var USE_MOCK = false

// ========== Mock 数据（仅覆盖 M3 模块接口） ==========
var MOCK_MAP = {
  // POST /auth/login/mock（开发/测试用）
  'POST:/auth/login/mock': function() {
    return {
      code: '00000',
      message: '成功',
      data: {
        id: 1,
        nickname: 'Mock用户',
        avatarUrl: null,
        emailVerified: false,
        formalProfileCompleted: false,
        creditScore: 100,
        token: 'mock-token-' + Date.now()
      }
    }
  },

  // POST /auth/login（真实微信登录）
  'POST:/auth/login': function() {
    return {
      code: '00000',
      message: '成功',
      data: {
        id: 1,
        nickname: 'Mock用户',
        avatarUrl: null,
        emailVerified: false,
        formalProfileCompleted: false,
        creditScore: 100,
        token: 'mock-token-' + Date.now()
      }
    }
  },

  // POST /auth/email/send
  'POST:/auth/email/send': function() {
    return {
      code: '00000',
      message: '验证码已发送',
      data: null
    }
  },

  // POST /auth/email/verify
  'POST:/auth/email/verify': function() {
    return {
      code: '00000',
      message: '邮箱验证成功',
      data: '邮箱验证成功'
    }
  },

  // PUT /profile/update
  'PUT:/profile/update': function() {
    return {
      code: '00000',
      message: '更新成功',
      data: null
    }
  },

  // GET /profile/skills/tags
  'GET:/profile/skills/tags': function() {
    return {
      code: '00000',
      message: '成功',
      data: [
        { id: 1, name: 'Java', category: 'language', status: 'active' },
        { id: 2, name: 'Python', category: 'language', status: 'active' },
        { id: 3, name: 'JavaScript', category: 'language', status: 'active' },
        { id: 4, name: 'TypeScript', category: 'language', status: 'active' },
        { id: 5, name: 'C++', category: 'language', status: 'active' },
        { id: 6, name: 'Spring Boot', category: 'framework', status: 'active' },
        { id: 7, name: 'Vue.js', category: 'framework', status: 'active' },
        { id: 8, name: 'React', category: 'framework', status: 'active' },
        { id: 9, name: 'Flutter', category: 'framework', status: 'active' },
        { id: 10, name: 'Django', category: 'framework', status: 'active' },
        { id: 11, name: 'Git', category: 'tool', status: 'active' },
        { id: 12, name: 'Docker', category: 'tool', status: 'active' },
        { id: 13, name: 'MySQL', category: 'tool', status: 'active' },
        { id: 14, name: 'Redis', category: 'tool', status: 'active' },
        { id: 15, name: '项目管理', category: 'soft_skill', status: 'active' },
        { id: 16, name: '团队协作', category: 'soft_skill', status: 'active' },
        { id: 17, name: '技术文档编写', category: 'soft_skill', status: 'active' },
        { id: 18, name: '沟通表达', category: 'soft_skill', status: 'active' }
      ]
    }
  },

  // PUT /profile/skills
  'PUT:/profile/skills': function() {
    return {
      code: '00000',
      message: '保存成功',
      data: null
    }
  }
}

function getMockKey(method, url) {
  return method + ':' + url
}

/**
 * 通用网络请求
 * USE_MOCK=true 时走本地 Mock，模拟 400ms 延迟
 * USE_MOCK=false 时发真实 wx.request
 *
 * @param {Object} options
 * @param {string} options.url
 * @param {string} [options.method='GET']
 * @param {Object} [options.data]
 * @param {Object} [options.header]
 * @param {number} [options.timeout=10000]
 * @returns {Promise<Object>} resolve { code, message, data }
 */
function request(options) {
  var url = options.url || ''
  var method = (options.method || 'GET').toUpperCase()
  var data = options.data || {}
  var timeout = options.timeout || 10000

  // ===== Mock 分支 =====
  if (USE_MOCK) {
    return new Promise(function(resolve, reject) {
      var key = getMockKey(method, url)
      var mockFn = MOCK_MAP[key]
      setTimeout(function() {
        if (mockFn) {
          resolve(mockFn(data))
        } else {
          console.warn('[api-service] Mock 未匹配: ' + key)
          resolve({ code: '00000', message: 'Mock OK', data: null })
        }
      }, 400)
    })
  }

  // ===== 真实请求分支 =====
  var token = wx.getStorageSync('token')
  var headers = { 'Content-Type': 'application/json' }
  if (token) {
    headers['Authorization'] = 'Bearer ' + token
  }
  if (options.header) {
    var keys = Object.keys(options.header)
    for (var i = 0; i < keys.length; i++) {
      headers[keys[i]] = options.header[keys[i]]
    }
  }

  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + url,
      method: method,
      data: data,
      header: headers,
      timeout: timeout,
      success: function(res) {
        if (res.statusCode === 200 && res.data && res.data.code === '00000') {
          resolve(res.data)
        } else {
          // 账号被封禁/认证失效/未登录，清除登录态并跳转
          var code = res.data && res.data.code
          if (code === 'M3017' || code === 'M3000' || code === 'A0401' || code === 'A0403' || res.statusCode === 401 || res.statusCode === 403) {
            wx.removeStorageSync('token')
            wx.removeStorageSync('userId')
            wx.removeStorageSync('userInfo')
            wx.removeStorageSync('emailVerified')
            wx.showToast({ title: '账号异常，请重新登录', icon: 'none', duration: 2000 })
            setTimeout(function() { wx.reLaunch({ url: '/pages/login/login' }) }, 1500)
          }
          reject(res.data || { code: 'M9999', message: '未知错误' })
        }
      },
      fail: function(err) {
        reject({ code: 'M9999', message: err.errMsg || '网络异常' })
      }
    })
  })
}

/**
 * 统一登录入口
 * USE_MOCK=true  → POST /auth/login/mock  (开发测试用)
 * USE_MOCK=false → POST /auth/login       (真实微信登录)
 *
 * @param {string} code - 微信 wx.login() 返回的临时 code
 * @returns {Promise<Object>} { id, nickname, token, avatarUrl, creditScore, ... }
 */
function login(code) {
  var url = USE_MOCK ? '/auth/login/mock' : '/auth/login'
  var body = USE_MOCK ? { code: 'test_openid_123' } : { code: code }
  return request({
    url: url,
    method: 'POST',
    data: body
  }).then(function(res) {
    // 成功则自动保存 token 和基础信息
    if (res.data && res.data.token) {
      wx.setStorageSync('token', res.data.token)
      wx.setStorageSync('userId', res.data.id)
      wx.setStorageSync('nickname', res.data.nickname || '')
    }
    return res.data
  })
}

module.exports = {
  request: request,
  login: login,
  USE_MOCK: USE_MOCK,
  BASE_URL: BASE_URL
}
