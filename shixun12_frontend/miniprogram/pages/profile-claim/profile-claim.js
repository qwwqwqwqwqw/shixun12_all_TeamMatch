// pages/profile-claim/profile-claim.js
var request = require('../../utils/api-service.js').request

Page({
  data: {
    loading: true,
    steps: [
      {
        step: 1,
        title: '绑定邮箱',
        desc: '使用邮箱验证学生身份',
        btnText: '去认证',
        action: 'goEmailVerify'
      },
      {
        step: 2,
        title: '完善个人档案',
        desc: '填写昵称、学校、专业等信息',
        btnText: '去填写',
        action: 'goProfileEdit'
      },
      {
        step: 3,
        title: '认领技术画像',
        desc: '确认并公开你的技术能力图谱',
        btnText: '立即认领',
        action: 'doClaim'
      }
    ]
  },

  onLoad: function() {
    var that = this
    that.setData({ loading: true })
    setTimeout(function() {
      that.setData({ loading: false })
    }, 600)
  },

  goEmailVerify: function() {
    wx.navigateTo({
      url: '/pages/email-verify/email-verify'
    })
  },

  goProfileEdit: function() {
    wx.navigateTo({
      url: '/pages/profile/edit/edit'
    })
  },

  // 认领技术画像 → 绑定 GitHub 账号
  doClaim: function() {
    var that = this
    wx.showModal({
      title: '绑定 GitHub',
      content: '请输入你的 GitHub 用户名',
      editable: true,
      placeholderText: '如：zhangsan-github',
      success: function(modalRes) {
        if (modalRes.confirm && modalRes.content) {
          var username = modalRes.content.trim()
          if (!username) {
            wx.showToast({ title: '请输入 GitHub 用户名', icon: 'none' })
            return
          }
          that.submitGitHubBind(username)
        }
      }
    })
  },

  // 调用后端绑定接口
  async submitGitHubBind(githubUsername) {
    wx.showLoading({ title: '绑定中...' })

    try {
      await request({
        url: '/profile/github/bind',
        method: 'POST',
        data: { githubUsername: githubUsername }
      })

      wx.hideLoading()
      wx.showToast({ title: 'GitHub 账号绑定成功', icon: 'success' })
      setTimeout(function() {
        wx.navigateBack()
      }, 1000)
    } catch (err) {
      wx.hideLoading()
      var msg = (err && err.message) ? err.message : '绑定失败，请重试'
      if (err && err.code === 'M3027') {
        msg = '该 GitHub 账号已被其他用户认领'
      } else if (err && err.code === 'M3000') {
        msg = '登录已过期，请重新登录'
      }
      wx.showToast({ title: msg, icon: 'none', duration: 2000 })
    }
  }
})
