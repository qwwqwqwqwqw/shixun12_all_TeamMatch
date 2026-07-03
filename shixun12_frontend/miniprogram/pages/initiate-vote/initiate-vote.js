/*
 * 队长发起退出投票页
 * 基线约束：
 * - negotiated = -5 分，malicious = -10 分（文档 5.3.4）
 * - 投票有效期 24h，多数通过生效，被投票人不能投票
 * - 成功创建后跳转已有 exit-vote-detail 页面
 */
var teamService = require('../../utils/team-service')
var TOKEN_KEYS = ['token', 'accessToken', 'userToken', 'sessionToken']
var LOGIN_PAGE_URL = '/pages/index/index'

Page({
  data: {
    isAuthenticated: false,
    isRedirectingToLogin: false,
    projectId: 0,
    targetUserId: 0,
    targetNickname: '',
    targetCreditScore: 0,
    reason: '',
    maxLength: 200,
    penaltyLevel: 'negotiated', // 'negotiated' | 'malicious'
    penaltyOptions: [
      { value: 'negotiated', label: '协商退出', score: '-5 分', desc: '成员配合退出，协商处理' },
      { value: 'malicious', label: '恶意退出', score: '-10 分', desc: '恶意行为 / 严重违规 / 违规退出' }
    ],
    submitting: false,
    showConfirm: false
  },

  onLoad: function(options) {
    if (!this.ensureLogin()) return

    var projectId = Number(options.projectId) || 0
    var targetUserId = Number(options.targetUserId) || 0
    var targetNickname = decodeURIComponent(options.targetNickname || '')
    var targetCreditScore = Number(options.targetCreditScore) || 0

    this.setData({
      projectId: projectId,
      targetUserId: targetUserId,
      targetNickname: targetNickname,
      targetCreditScore: targetCreditScore
    })
  },

  // ========== 表单输入 ==========

  onReasonInput: function(e) {
    var val = e.detail.value || ''
    if (val.length > this.data.maxLength) {
      val = val.slice(0, this.data.maxLength)
    }
    this.setData({ reason: val })
  },

  onSelectPenalty: function(e) {
    var level = e.currentTarget.dataset.level
    this.setData({ penaltyLevel: level })
  },

  // ========== 确认发起 ==========

  onConfirmTap: function() {
    if (!this.data.reason.trim()) {
      wx.showToast({ title: '请输入投票原因', icon: 'none' })
      return
    }
    this.setData({ showConfirm: true })
  },

  onCancelConfirm: function() {
    this.setData({ showConfirm: false })
  },

  onDoSubmit: function() {
    var that = this
    this.setData({ submitting: true, showConfirm: false })

    teamService.createExitVote(
      this.data.projectId,
      this.data.targetUserId,
      this.data.reason.trim(),
      this.data.penaltyLevel
    ).then(function(res) {
      that.setData({ submitting: false })
      // 获取新建投票的 id，跳转到已有投票详情页
      var data = res.data || res
      var voteId = data.id || (data.data && data.data.id)
      if (!voteId) {
        wx.showToast({ title: '投票已创建', icon: 'success' })
        wx.navigateBack()
        return
      }
      wx.redirectTo({
        url: '/pages/exit-vote-detail/exit-vote-detail?voteId=' + voteId + '&projectId=' + that.data.projectId
      })
    }).catch(function(err) {
      that.setData({ submitting: false })
      var msg = (err && err.message) || '发起投票失败'
      // 后端常见错误码映射
      if (err && err.code) {
        if (err.code === 'M4002') msg = '仅队长可发起投票'
        else if (err.code === 'M4003') msg = '项目当前状态不允许发起投票'
        else if (err.code === 'M4004') msg = '该成员不是活跃成员'
        else if (err.code === 'M4006') msg = '该成员已有进行中的投票'
      }
      wx.showToast({ title: msg, icon: 'none', duration: 2500 })
    })
  },

  // ========== 登录 ==========

  ensureLogin: function() {
    var token = this.getLoginToken()
    if (token) {
      this.setData({ isAuthenticated: true, isRedirectingToLogin: false })
      return true
    }
    this.redirectToLogin()
    return false
  },

  getLoginToken: function() {
    for (var i = 0; i < TOKEN_KEYS.length; i++) {
      var t = wx.getStorageSync(TOKEN_KEYS[i])
      if (t) return t
    }
    return ''
  },

  redirectToLogin: function() {
    if (this.data.isRedirectingToLogin) return
    this.setData({ isAuthenticated: false, isRedirectingToLogin: true })
    wx.showToast({ title: '请先登录后使用', icon: 'none' })
    setTimeout(function() {
      wx.switchTab({ url: LOGIN_PAGE_URL })
    }, 300)
  }
})
