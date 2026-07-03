/*
 * T-037 成员主动退出页
 * 基线约束（V2.1 详细设计文档）：
 * 1. self_exit 不经过投票，直接生效，扣 10 分
 * 2. 仅 in_progress 阶段可触发正式退出；recruiting 阶段轻量退出不扣分
 * 3. 队长 P0 不能 self_exit（不支持队长转让）
 * 4. 不使用"有事退出/协商退出/无故退出"等旧名称
 * 5. 统一成功响应码 code: 200
 * 6. 退出成功后 team_member.status → exited，exit_mode → self_exit
 */

const teamService = require('../../utils/team-service')
const LOGIN_PAGE_URL = '/pages/index/index'
const MEMBERS_PAGE_URL = '/pages/project-members/project-members'
const TOKEN_KEYS = ['token', 'accessToken', 'userToken', 'sessionToken']

Page({
  data: {
    isAuthenticated: false,
    isRedirectingToLogin: false,
    projectId: 0,
    projectTitle: '',
    projectSubtitle: '',
    projectStatus: '',
    projectStatusText: '',
    reason: '',
    maxLength: 200,
    submitting: false,
    showConfirm: false,
    loading: true,
    error: null
  },

  onLoad(options) {
    if (!this.ensureLogin()) return

    const projectId = Number(options.projectId) || 1
    this.setData({ projectId })
    this.loadProjectDetail()
  },

  async loadProjectDetail() {
    this.setData({ loading: true, error: null })
    try {
      const project = await teamService.getProjectDetail(this.data.projectId)
      console.log('[T-037] project detail', project)
      var statusMap = { recruiting: '招募中', in_progress: '进行中', ended: '已结束' }
      this.setData({
        projectTitle: project.name || project.title || '未知项目',
        projectSubtitle: project.description || project.subtitle || '',
        projectStatus: project.status || '',
        projectStatusText: statusMap[project.status] || project.status || '',
        loading: false
      })
    } catch (err) {
      console.log('[T-037] loadProjectDetail failed', err)
      this.setData({
        loading: false,
        error: '加载项目信息失败'
      })
    }
  },

  onReasonInput(e) {
    const value = e.detail.value || ''
    if (value.length > this.data.maxLength) return
    this.setData({ reason: value })
  },

  onConfirmTap() {
    if (this.data.submitting || this.data.isLeader || this.data.projectStatus === 'ended') return
    this.setData({ showConfirm: true })
  },

  onCancelConfirm() {
    this.setData({ showConfirm: false })
  },

  async onDoExit() {
    if (this.data.submitting) return

    this.setData({ submitting: true, showConfirm: false })

    try {
      const result = await teamService.selfExit(this.data.projectId, this.data.reason)
      console.log('[T-037] selfExit success', result)
      wx.showToast({ title: '已退出项目', icon: 'success' })
      setTimeout(() => {
        wx.redirectTo({
          url: `${MEMBERS_PAGE_URL}?projectId=${this.data.projectId}`
        })
      }, 1500)
    } catch (err) {
      const msg = (err && err.message) || '退出失败，请重试'
      console.log('[T-037] selfExit failed', err)
      wx.showToast({ title: msg, icon: 'none' })
      this.setData({ submitting: false })
    }
  },



  ensureLogin() {
    const token = this.getLoginToken()
    if (token) {
      if (!this.data.isAuthenticated || this.data.isRedirectingToLogin) {
        this.setData({
          isAuthenticated: true,
          isRedirectingToLogin: false
        })
      }
      return true
    }
    this.redirectToLogin()
    return false
  },

  getLoginToken() {
    for (let index = 0; index < TOKEN_KEYS.length; index += 1) {
      const token = wx.getStorageSync(TOKEN_KEYS[index])
      if (token) return token
    }
    return ''
  },

  redirectToLogin() {
    if (this.data.isRedirectingToLogin) return
    this.setData({ isAuthenticated: false, isRedirectingToLogin: true })
    wx.showToast({ title: '请先登录后使用', icon: 'none' })
    setTimeout(() => {
      wx.redirectTo({ url: LOGIN_PAGE_URL })
    }, 300)
  }
})
