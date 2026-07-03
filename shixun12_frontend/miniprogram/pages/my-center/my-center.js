// pages/my-center/my-center.js
var api = require('../../utils/api-service.js')
var USE_MOCK = api.USE_MOCK
var teamService = require('../../utils/team-service.js')

Page({
  data: {
    loading: true,
    hasUserProfile: false,
    userInfo: {
      nickname: 'TeamMatch 用户',
      avatarUrl: '',
      creditScore: 100,
      emailVerified: false,
      email: '',
      school: ''
    },
    menuList: [
      {
        id: 'projects',
        icon: '📁',
        title: '我的项目',
        desc: '查看已加入和管理的项目',
        action: 'goProjectList'
      },
      {
        id: 'profile',
        icon: '👤',
        title: '我的档案',
        desc: '编辑个人基本信息和简介',
        action: 'goProfileEdit'
      },
      {
        id: 'skills',
        icon: '🏷️',
        title: '技能标签',
        desc: '管理你的技术技能标签',
        action: 'goSkills'
      },
      {
        id: 'email',
        icon: '📧',
        title: '邮箱认证',
        desc: '绑定邮箱完成身份验证',
        action: 'goEmailVerify'
      }
    ]
  },

  onLoad: function() {
    this.loadUserProfile()
    this.setData({ loading: false })
    this.loadProfileDetail()
  },

  onShow: function() {
    this.loadUserProfile()
    this.loadProfileDetail()
  },

  loadUserProfile: function() {
    var userInfo = wx.getStorageSync('userInfo')

    // 清理无效的旧缓存（昵称无效才清）
    if (userInfo && typeof userInfo === 'object') {
      var nickOk = userInfo.nickName &&
                   typeof userInfo.nickName === 'string' &&
                   userInfo.nickName !== '微信用户'
      if (!nickOk) {
        wx.removeStorageSync('userInfo')
        userInfo = null
      }
    }

    // 头像和昵称都是 http 有效才视为已授权
    if (userInfo && userInfo.avatarUrl && userInfo.nickName) {
      var raw = userInfo.avatarUrl
      if (raw.length > 10 && raw.indexOf('http') === 0) {
        this.setData({
          hasUserProfile: true,
          'userInfo.avatarUrl': raw,
          'userInfo.nickname': userInfo.nickName
        })
        return
      }
    }

    // 没有微信授权时，后端数据在 loadProfileDetail 中异步填充
    this.setData({ hasUserProfile: false })
  },

  // 拉取后端档案数据（昵称、头像、信誉分等）
  async loadProfileDetail() {
    if (USE_MOCK) return

    try {
      // 通过 teamService 获取档案（统一归一化 avatar_url → avatarUrl）
      var userId = wx.getStorageSync('userId')
      var data = await teamService.getUserProfile(userId)
      if (!data) return

      var backendAvatar = data.avatarUrl || data.avatar || ''
      var backendNick = data.nickname || ''
      var update = {
        'userInfo.creditScore': data.creditScore != null ? data.creditScore : this.data.userInfo.creditScore,
        'userInfo.emailVerified': data.emailVerified || false,
        'userInfo.email': data.email || '',
        'userInfo.school': data.school || ''
      }

      // 微信授权优先，后端数据兜底
      if (this.data.hasUserProfile) {
        if (this.data.userInfo.nickname === 'TeamMatch 用户' && backendNick) {
          update['userInfo.nickname'] = backendNick
        }
      } else {
        if (backendNick && backendNick !== 'TeamMatch User') {
          update['userInfo.nickname'] = backendNick
          update['hasUserProfile'] = true
        }
        // 不用后端裸链接覆盖已有的本地头像（签名 URL 存在时后端返回的是无签名链接会 403）
        if (backendAvatar && !this.data.userInfo.avatarUrl) {
          update['userInfo.avatarUrl'] = backendAvatar
        }
      }

      if (data.emailVerified) wx.setStorageSync('emailVerified', true)
      this.setData(update)
    } catch (err) {
      console.log('[my-center] 档案数据拉取失败，使用默认值')
    }
  },

  applyAvatarResult: function(uploadResult) {
    var avatarUrl = uploadResult.displayUrl || uploadResult.accessUrl || uploadResult.storedUrl
    var storedUrl = uploadResult.storedUrl || ''
    if (!avatarUrl) return

    this.setData({
      'userInfo.avatarUrl': avatarUrl,
      hasUserProfile: true
    })

    var userInfo = wx.getStorageSync('userInfo') || {}
    userInfo.avatarUrl = avatarUrl
    wx.setStorageSync('userInfo', userInfo)

    var profile = wx.getStorageSync('profile') || {}
    profile.avatarUrl = avatarUrl
    profile.avatarStoredUrl = storedUrl
    wx.setStorageSync('profile', profile)

    teamService.clearProfileCache()
  },

  onChooseAvatar: function(e) {
    var that = this
    var tempPath = e.detail.avatarUrl
    wx.showLoading({ title: '上传头像...' })
    teamService.uploadAvatar(tempPath).then(function(uploadResult) {
      wx.hideLoading()
      that.applyAvatarResult(uploadResult)
      wx.showToast({ title: '头像已更新', icon: 'success', duration: 1200 })
    }).catch(function(err) {
      wx.hideLoading()
      var msg = (err && err.message) || '上传失败'
      if (err && err.code === 'M6024') msg = 'OSS 未配置'
      wx.showToast({ title: msg, icon: 'none' })
    })
  },

  changeAvatar: function() {
    var that = this
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: function(res) {
        var tempFilePath = res.tempFiles[0].tempFilePath
        wx.showLoading({ title: '上传中...' })
        teamService.uploadAvatar(tempFilePath)
          .then(function(uploadResult) {
            wx.hideLoading()
            that.applyAvatarResult(uploadResult)
            wx.showToast({ title: '头像更新成功', icon: 'success' })
          })
          .catch(function(err) {
            wx.hideLoading()
            var msg = (err && err.message) || '头像上传失败，请重试'
            wx.showToast({ title: msg, icon: 'none' })
          })
      }
    })
  },

  goProfileAuth: function() {
    wx.navigateTo({
      url: '/pages/profile-auth/profile-auth'
    })
  },

  goProjectList: function() {
    // 跳转组队中心的"已加入项目" Tab
    wx.setStorageSync('teamTab', 'joined')
    wx.switchTab({ url: '/pages/team-center/index' })
  },

  goProfileEdit: function() {
    wx.navigateTo({
      url: '/pages/profile/edit/edit'
    })
  },

  goSkills: function() {
    wx.navigateTo({
      url: '/pages/profile/skills/skills'
    })
  },

  goEmailVerify: function() {
    wx.navigateTo({
      url: '/pages/email-verify/email-verify'
    })
  },

  doLogout: function() {
    var that = this
    wx.showModal({
      title: '退出登录',
      content: '确定要退出登录吗？',
      success: function(res) {
        if (res.confirm) {
          // 清除登录 token
          wx.removeStorageSync('token')
          wx.removeStorageSync('accessToken')
          wx.removeStorageSync('userToken')
          wx.removeStorageSync('sessionToken')
          wx.removeStorageSync('profile')
          wx.removeStorageSync('userInfo')

          wx.showToast({
            title: '已退出登录',
            icon: 'success',
            duration: 1200
          })

          setTimeout(function() {
            wx.redirectTo({
              url: '/pages/login/login'
            })
          }, 1200)
        }
      }
    })
  }
})
