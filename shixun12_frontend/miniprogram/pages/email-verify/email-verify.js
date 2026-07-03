// pages/email-verify/email-verify.js
var api = require('../../utils/api-service.js')
var request = api.request
var teamService = require('../../utils/team-service.js')

Page({
  data: {
    email: '',
    code: '',
    sendingCode: false,
    countdown: 0,
    verifying: false,
    errorMsg: '',
    isVerified: false,
    currentEmail: '',
    isRebinding: false
  },

  async onLoad() {
    // 从后端获取当前邮箱状态
    try {
      var res = await request({ url: '/profile/detail', method: 'GET' })
      var data = res.data || {}
      if (data.email && data.emailVerified) {
        wx.setStorageSync('emailVerified', true)
        this.setData({ isVerified: true, currentEmail: data.email })
      } else {
        this.setData({ isVerified: false })
      }
    } catch (e) {
      // 回退到本地缓存
      var cached = wx.getStorageSync('emailVerified')
      if (cached) {
        this.setData({ isVerified: true, currentEmail: wx.getStorageSync('userEmail') || '' })
      }
    }
  },

  // 开启换绑模式
  startRebind: function() {
    this.setData({ isRebinding: true, errorMsg: '' })
  },

  // 取消换绑
  cancelRebind: function() {
    this.setData({ isRebinding: false, email: '', code: '', errorMsg: '' })
  },

  // 邮箱输入
  onEmailInput: function(e) {
    this.setData({ email: e.detail.value, errorMsg: '' })
  },

  // 验证码输入
  onCodeInput: function(e) {
    this.setData({ code: e.detail.value, errorMsg: '' })
  },

  // 发送验证码
  async sendCode() {
    var email = this.data.email
    var countdown = this.data.countdown
    if (!email.trim()) {
      this.setData({ errorMsg: '请先输入邮箱地址' })
      return
    }
    if (!email.endsWith('.edu.cn')) {
      this.setData({ errorMsg: '请使用学校邮箱（.edu.cn）' })
      return
    }
    if (countdown > 0) return

    this.setData({ sendingCode: true, errorMsg: '' })

    try {
      await request({ url: '/auth/email/send', method: 'POST', data: { email: email.trim() } })
      this.setData({ sendingCode: false, countdown: 60 })
      this._countdownTimer = setInterval(() => {
        var r = this.data.countdown - 1
        if (r <= 0) { clearInterval(this._countdownTimer); r = 0 }
        this.setData({ countdown: r })
      }, 1000)
      wx.showToast({ title: '验证码已发送', icon: 'success' })
    } catch (err) {
      this.setData({ sendingCode: false })
      this.setData({ errorMsg: (err && err.message) || '发送失败，请稍后重试' })
    }
  },

  // 提交验证
  async verifyCode() {
    var email = this.data.email
    var code = this.data.code
    var isRebind = this.data.isRebinding

    if (!email.trim() || !code.trim()) {
      this.setData({ errorMsg: '请填写完整信息' })
      return
    }

    this.setData({ verifying: true, errorMsg: '' })

    try {
      await request({ url: '/auth/email/verify', method: 'POST', data: { email: email.trim(), code: code.trim() } })
      this.setData({ verifying: false })
      wx.setStorageSync('emailVerified', true)

      if (isRebind) {
        // 换绑成功，清除用户档案缓存使个人中心刷新
        teamService.clearProfileCache(wx.getStorageSync('userId'))
        this.setData({ isRebinding: false, email: '', code: '', currentEmail: email.trim() })
        wx.showToast({ title: '邮箱换绑成功', icon: 'success' })
      } else {
        // 首次验证成功，清除缓存使各处刷新
        teamService.clearProfileCache(wx.getStorageSync('userId'))
        wx.showToast({ title: '邮箱验证成功', icon: 'success' })
        setTimeout(function() { wx.navigateBack() }, 1000)
      }
    } catch (err) {
      this.setData({ verifying: false })
      this.setData({ errorMsg: (err && err.message) || '验证失败，请重试' })
    }
  },

  onUnload: function() {
    if (this._countdownTimer) clearInterval(this._countdownTimer)
  }
})
