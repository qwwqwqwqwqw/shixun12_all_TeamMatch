// app.js
var apiService = require('./utils/api-service')

App({
  onLaunch() {
    // 全局未捕获 Promise 错误处理
    if (typeof wx.onUnhandledRejection === 'function') {
      wx.onUnhandledRejection(function(res) {
        console.error('[Global] 未捕获的 Promise 错误:', res.reason)
      })
    }

    // 展示本地存储能力
    var logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // 已有 token 则恢复用户信息，无 token 等用户手动登录
    this.restoreSession()
  },

  restoreSession() {
    var token = wx.getStorageSync('token') || ''
    if (!token) {
      console.log('[app] 无 token，待用户登录')
      return
    }

    var userId = wx.getStorageSync('userId') || ''
    var userInfo = wx.getStorageSync('userInfo')
    this.globalData.userId = userId
    this.globalData.userInfo = userInfo || null
    console.log('[app] 会话恢复, userId:', userId)
  },

  globalData: {
    userInfo: null,
    userId: null
  }
})
