// pages/login/login.js
var apiService = require('../../utils/api-service.js')

Page({
  data: { agreed: false, shakeAgree: false },

  // 微信一键登录
  toggleAgreed: function() {
    this.setData({ agreed: !this.data.agreed, shakeAgree: false })
  },

  showUserAgreement: function() {
    wx.navigateTo({ url: '/pages/agreement/user-agreement/user-agreement' })
  },

  showPrivacyPolicy: function() {
    wx.navigateTo({ url: '/pages/agreement/privacy-policy/privacy-policy' })
  },

  goToLogin: function() {
    if (!this.data.agreed) {
      this.setData({ shakeAgree: true })
      setTimeout(() => { this.setData({ shakeAgree: false }) }, 600)
      wx.showToast({ title: '请先勾选同意用户协议和隐私政策', icon: 'none' })
      return
    }
    var that = this

    // Step 1: 获取微信用户头像和昵称
    wx.getUserProfile({
      desc: '用于完善你的个人资料',
      success: function(profileRes) {
        var userInfo = profileRes.userInfo
        console.log('[login] 获取微信用户信息成功', userInfo)

        // Step 2: 微信登录获取临时 code，发给后端换取 token
        wx.login({
          success: function(loginRes) {
            if (!loginRes.code) {
              wx.showToast({ title: '微信登录失败', icon: 'none' })
              return
            }
            console.log('[login] wx.login code:', loginRes.code)

            apiService.login(loginRes.code).then(function(result) {
              // 检查用户状态：封禁用户禁止登录
              if (result && result.status === 'banned') {
                wx.showModal({
                  title: '账号已被封禁',
                  content: '您的账号已被管理员封禁，如有疑问请联系管理员。',
                  showCancel: false
                })
                return
              }

              // token/id/nickname 已由 api-service login() 自动存入 storage
              // 此处仅补充微信头像昵称
              wx.setStorageSync('userInfo', userInfo)
              console.log('[login] 登录成功, userId:', result.id)

              wx.showToast({ title: '欢迎使用', icon: 'success' })
              setTimeout(function() {
                wx.switchTab({ url: '/pages/index/index' })
              }, 500)
            }).catch(function(err) {
              console.error('[login] 后端登录失败:', err)
              wx.showToast({ title: '登录失败，请重试', icon: 'none' })
            })
          },
          fail: function() {
            wx.showToast({ title: '微信登录失败', icon: 'none' })
          }
        })
      },
      fail: function() {
        // 用户拒绝授权头像昵称，仍可继续登录
        console.log('[login] 用户拒绝授权，使用默认登录')
        wx.login({
          success: function(loginRes) {
            if (!loginRes.code) return
            apiService.login(loginRes.code).then(function(result) {
              // token/id/nickname 已由 api-service login() 自动存入 storage
              wx.showToast({ title: '已登录', icon: 'success' })
              setTimeout(function() {
                wx.switchTab({ url: '/pages/index/index' })
              }, 500)
            }).catch(function() {
              wx.showToast({ title: '登录失败', icon: 'none' })
            })
          }
        })
      }
    })
  }
})
