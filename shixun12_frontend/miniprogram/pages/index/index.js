// pages/index/index.js
var request = require('../../utils/api-service.js').request

function parseTopLanguages(val) {
  if (!val) return []
  if (Array.isArray(val)) return val
  try { var arr = JSON.parse(val); return Array.isArray(arr) ? arr : [] }
  catch (e) { return [] }
}

Page({
  data: {
    isLoggedIn: false, isEmailVerified: false, userEmail: '',
    inputUsername: '',
    bindPlatform: '',          // 首次绑定: 'github' | 'gitee' | ''
    updatePlatform: '',        // 换绑: 'github' | 'gitee' | ''
    showBindInput: false,
    showUpdateInput: false,
    showOauthModal: false,     // OAuth 弹窗
    oauthChecking: false,
    boundGithub: '', boundGitee: '', boundPlatform: '',
    hasTechProfile: false,
    techScore: 0, totalRepos: 0, totalStars: 0, totalPrs: 0, totalCommits: 0,
    topLanguages: [],
    boards: [
      { name: '综合技术分', emoji: '🐙', sort: 'techScore' },
      { name: 'Stars 星标', emoji: '⭐', sort: 'totalStars' },
      { name: '开源仓库', emoji: '📦', sort: 'totalRepos' },
      { name: '代码贡献', emoji: '💻', sort: 'totalCommits' }
    ]
  },

  onShow: function() {
    var token = wx.getStorageSync('token')
    var emailVerified = wx.getStorageSync('emailVerified') || false
    this.setData({ isLoggedIn: !!token, isEmailVerified: !!emailVerified })
    if (token) {
      this.fetchTechProfile()
      // 从 detail API 同步 emailVerified
      this.syncEmailStatus()
    }
  },

  async onPullDownRefresh() {
    var token = wx.getStorageSync('token')
    this.setData({ isLoggedIn: !!token })
    if (token) {
      await this.fetchTechProfile()
      await this.syncEmailStatus()
    }
    wx.stopPullDownRefresh()
  },

  async syncEmailStatus() {
    try {
      var res = await request({ url: '/profile/detail', method: 'GET' })
      var data = res.data || {}
      if (data.emailVerified) {
        wx.setStorageSync('emailVerified', true)
        this.setData({ isEmailVerified: true, userEmail: data.email || '' })
      }
    } catch (e) {}
  },

  async fetchTechProfile() {
    try {
      var res = await request({ url: '/profile/tech-profile', method: 'GET' })
      var data = res.data || {}
      var platform = data.source || ''
      if (!platform && data.giteeUsername) platform = 'gitee'
      if (!platform && data.githubUsername) platform = 'github'
      if (!platform) platform = this.data.boundPlatform
      this.setData({
        hasTechProfile: true,
        boundGithub: data.githubUsername || this.data.boundGithub,
        boundGitee: data.giteeUsername || this.data.boundGitee,
        boundPlatform: platform,
        techScore: data.techScore || 0,
        totalRepos: data.totalRepos || 0, totalStars: data.totalStars || 0,
        totalPrs: data.totalPrs || 0, totalCommits: data.totalCommits || 0,
        topLanguages: parseTopLanguages(data.topLanguages)
      })
    } catch (err) {
      this.setData({ hasTechProfile: false })
    }
  },

  onInput: function(e) { this.setData({ inputUsername: e.detail.value }) },

  // ===== 平台选择（首次绑定 & 换绑共用） =====

  choosePlatform: function(e) {
    var type = e.currentTarget.dataset.type // 'bind' | 'update'
    var that = this
    wx.showActionSheet({
      itemList: ['GitHub 用户名', 'Gitee 用户名', 'Gitee OAuth 一键绑定'],
      success: function(res) {
        if (res.tapIndex === 0) {
          // GitHub 手动
          if (type === 'bind') that.setData({ bindPlatform: 'github', showBindInput: true, showUpdateInput: false, updatePlatform: '' })
          else that.setData({ updatePlatform: 'github', showUpdateInput: true, showBindInput: false, bindPlatform: '' })
        } else if (res.tapIndex === 1) {
          // Gitee 手动
          if (type === 'bind') that.setData({ bindPlatform: 'gitee', showBindInput: true, showUpdateInput: false, updatePlatform: '' })
          else that.setData({ updatePlatform: 'gitee', showUpdateInput: true, showBindInput: false, bindPlatform: '' })
        } else {
          // Gitee OAuth
          that.startGiteeOauth()
        }
      }
    })
  },

  cancelBind: function() {
    this.setData({ bindPlatform: '', showBindInput: false, inputUsername: '' })
  },

  cancelUpdate: function() {
    this.setData({ updatePlatform: '', showUpdateInput: false, inputUsername: '' })
  },

  // ===== 手动绑定/换绑 =====

  async doBind() {
    var username = this.data.inputUsername.trim()
    var platform = this.data.bindPlatform
    if (!username) { wx.showToast({ title: '请输入用户名', icon: 'none' }); return }
    wx.showLoading({ title: '绑定中...' })
    try {
      var reqData = platform === 'github' ? { githubUsername: username } : { giteeUsername: username }
      await request({ url: '/profile/' + platform + '/bind', method: 'POST', data: reqData })
      wx.hideLoading()
      wx.showToast({ title: '绑定成功', icon: 'success' })
      this.setData({
        bindPlatform: '', showBindInput: false, inputUsername: '',
        hasTechProfile: true, boundPlatform: platform,
        boundGithub: platform === 'github' ? username : '',
        boundGitee: platform === 'gitee' ? username : ''
      })
    } catch (err) {
      wx.hideLoading()
      var msg = (err && err.message) || '绑定失败'
      wx.showToast({ title: msg, icon: 'none' })
    }
  },

  async doUpdate() {
    var username = this.data.inputUsername.trim()
    var platform = this.data.updatePlatform
    if (!username) { wx.showToast({ title: '请输入用户名', icon: 'none' }); return }
    wx.showLoading({ title: '更新中...' })
    try {
      var samePlatform = platform === this.data.boundPlatform
      var apiPath = samePlatform ? '/profile/' + platform + '/update' : '/profile/' + platform + '/bind'
      var apiMethod = samePlatform ? 'PUT' : 'POST'
      var reqData = platform === 'github' ? { githubUsername: username } : { giteeUsername: username }
      await request({ url: apiPath, method: apiMethod, data: reqData })
      wx.hideLoading()
      wx.showToast({ title: '更新成功', icon: 'success' })
      this.setData({
        updatePlatform: '', showUpdateInput: false, inputUsername: '',
        hasTechProfile: true, boundPlatform: platform,
        boundGithub: platform === 'github' ? username : '',
        boundGitee: platform === 'gitee' ? username : ''
      })
    } catch (err) {
      wx.hideLoading()
      var msg = (err && err.message) || '更新失败'
      wx.showToast({ title: msg, icon: 'none' })
    }
  },

  // ===== Gitee OAuth 一键绑定 =====

  async startGiteeOauth() {
    wx.showLoading({ title: '获取授权链接...' })
    try {
      var res = await request({ url: '/profile/gitee/auth-url', method: 'GET' })
      wx.hideLoading()
      var authUrl = (res.data && res.data.authUrl) || ''
      if (!authUrl) { wx.showToast({ title: '获取授权链接失败', icon: 'none' }); return }
      wx.setClipboardData({
        data: authUrl,
        success: function() {
          // 打开弹窗，用户操作完成后在里面检查
          this.setData({ showOauthModal: true, oauthChecking: false })
        }.bind(this)
      })
    } catch (err) {
      wx.hideLoading()
      var msg = (err && err.message) || '获取失败'
      wx.showToast({ title: msg, icon: 'none' })
    }
  },

  closeOauthModal: function() {
    this.setData({ showOauthModal: false, oauthChecking: false })
  },

  async checkOauthStatus() {
    this.setData({ oauthChecking: true })
    try {
      var res = await request({ url: '/profile/detail', method: 'GET' })
      var data = res.data || {}
      if (data.giteeClaimed) {
        this.setData({ showOauthModal: false, oauthChecking: false })
        wx.showToast({ title: 'Gitee 授权成功', icon: 'success' })
        this.setData({
          hasTechProfile: true, boundPlatform: 'gitee',
          boundGitee: data.giteeUsername || ''
        })
      } else {
        this.setData({ oauthChecking: false })
        wx.showToast({ title: '尚未完成授权，请先在浏览器中完成', icon: 'none' })
      }
    } catch (err) {
      this.setData({ oauthChecking: false })
      wx.showToast({ title: '检查失败，请重试', icon: 'none' })
    }
  },

  // ===== 页面跳转 =====
  goProjectList: function() { wx.navigateTo({ url: '/pages/projects/list/list' }) },
  goCreateProject: function() { wx.navigateTo({ url: '/pages/projects/create/create' }) },
  goMemberMatch: function() { wx.navigateTo({ url: '/pages/member-match/member-match' }) },
  goEmailVerify: function() { wx.navigateTo({ url: '/pages/email-verify/email-verify' }) },
  goLeaderboard: function(e) {
    var sort = e.currentTarget.dataset.sort || ''
    wx.navigateTo({ url: '/pages/leaderboard/leaderboard?sort=' + sort })
  }
})
