// pages/profile-auth/profile-auth.js
var api = require('../../utils/api-service.js')
var request = api.request
var teamService = require('../../utils/team-service.js')

Page({
  data: {
    avatarUrl: '',
    nickName: '',
    canSubmit: false,
    submitting: false
  },

  onChooseAvatar: function(e) {
    this.setData({ avatarUrl: e.detail.avatarUrl })
    this.checkCanSubmit()
  },

  onInputChange: function(e) {
    this.setData({ nickName: e.detail.value })
    this.checkCanSubmit()
  },

  checkCanSubmit: function() {
    this.setData({ canSubmit: this.data.nickName.trim().length > 0 })
  },

  submitProfile: function() {
    var that = this
    var nickName = this.data.nickName.trim()
    var rawAvatar = this.data.avatarUrl

    if (!nickName) {
      wx.showToast({ title: '请输入昵称', icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    wx.showLoading({ title: '保存中...' })

    // 如果选了头像且是临时路径，先用专用头像接口上传并落库
    if (rawAvatar && rawAvatar.indexOf('http') !== 0) {
      teamService.uploadAvatar(rawAvatar).then(function(uploadResult) {
        var displayUrl = uploadResult.displayUrl || uploadResult.accessUrl || uploadResult.storedUrl
        that.doSave(nickName, displayUrl, uploadResult.storedUrl || '')
      }).catch(function(err) {
        wx.hideLoading()
        that.setData({ submitting: false })
        var msg = (err && err.message) || '头像上传失败，请重试'
        if (err && err.code === 'M6024') msg = 'OSS 未配置'
        wx.showToast({ title: msg, icon: 'none' })
      })
    } else {
      // 无头像或已是 http URL，直接保存
      this.doSave(nickName, rawAvatar || '', '')
    }
  },

  doSave: function(nickName, avatarUrl, avatarStoredUrl) {
    var that = this
    console.log('[profile-auth] 最终保存 avatarUrl:', avatarUrl)

    // 保存到本地缓存
    wx.setStorageSync('userInfo', {
      avatarUrl: avatarUrl,
      nickName: nickName
    })
    if (avatarStoredUrl) {
      var profile = wx.getStorageSync('profile') || {}
      profile.avatarUrl = avatarUrl
      profile.avatarStoredUrl = avatarStoredUrl
      profile.nickname = nickName
      wx.setStorageSync('profile', profile)
    }

    // 同步写入后端
    var payload = { nickname: nickName }
    if (avatarStoredUrl) payload.avatarUrl = avatarStoredUrl
    else if (avatarUrl && avatarUrl.indexOf('http') === 0) payload.avatarUrl = avatarUrl

    request({
      url: '/profile/update',
      method: 'PUT',
      data: payload
    }).then(function() {
      wx.hideLoading()
      that.setData({ submitting: false })
      teamService.clearProfileCache()
      wx.showToast({ title: '授权成功', icon: 'success' })
      setTimeout(function() { wx.navigateBack() }, 800)
    }).catch(function(err) {
      wx.hideLoading()
      that.setData({ submitting: false })
      console.warn('[profile-auth] 后台同步失败:', err)
      wx.showToast({ title: '已授权', icon: 'success' })
      setTimeout(function() { wx.navigateBack() }, 800)
    })
  }
})
