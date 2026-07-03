/*
 * T-035 项目成员列表（接口联调版）
 * 基线约束（V2.1 详细设计文档）：
 * 1. team_member.status 仅用 active / exited，不使用 removed 等旧名称
 * 2. team_member.exit_mode 仅用 self_exit / exit_vote，对应 UI 文案"主动退出"/"被投票移除"
 * 3. 被移除成员识别方式：status='exited' AND exit_mode='exit_vote'
 * 4. 队长不能直接移除成员，P0 阶段移除必须走 exit_vote 投票流程
 * 5. 统一成功响应码 code: 200（不是 0）
 * 6. 本页面操作图标 (...) 仅作为退出投票入口占位，不直接执行移除
 */

const teamService = require('../../utils/team-service')
const apiService = require('../../utils/api-service.js')
const USE_MOCK = apiService.USE_MOCK
const LOGIN_PAGE_URL = '/pages/index/index'
const TOKEN_KEYS = ['token', 'accessToken', 'userToken', 'sessionToken']

// ========== Mock 成员数据 ==========
var MOCK_MEMBERS = [
  { id: 1, userId: 1, nickname: '江一舟', avatarUrl: '/assets/CodeBuddyAssets/114_182/2.svg', creditScore: 98, major: '计算机科学 21级', school: '元宇宙大学', bio: '全栈开发者，热爱开源项目', role: 'leader', status: 'active', exitMode: null, exitDate: null },
  { id: 2, userId: 2, nickname: '林乔', avatarUrl: '/assets/CodeBuddyAssets/114_182/2.svg', creditScore: 96, major: '软件工程 22级', school: '元宇宙大学', bio: 'Vue.js 重度用户，追求代码优雅', role: 'member', status: 'active', exitMode: null, exitDate: null },
  { id: 3, userId: 3, nickname: '周予安', avatarUrl: '/assets/CodeBuddyAssets/114_182/2.svg', creditScore: 91, major: '计算机科学 21级', school: '元宇宙大学', bio: '喜欢研究分布式系统', role: 'member', status: 'active', exitMode: null, exitDate: null },
  { id: 4, userId: 4, nickname: '陈知远', avatarUrl: '/assets/CodeBuddyAssets/114_182/2.svg', creditScore: 85, major: '软件工程 22级', school: '元宇宙大学', bio: '前端初学者，勤奋好学', role: 'member', status: 'exited', exitMode: 'self_exit', exitDate: '05-02' },
  { id: 5, userId: 5, nickname: '许之恒', avatarUrl: '/assets/CodeBuddyAssets/114_182/2.svg', creditScore: 72, major: '软件工程 21级', school: '元宇宙大学', bio: '', role: 'member', status: 'exited', exitMode: 'exit_vote', exitDate: '04-28' }
]

Page({
  data: {
    isAuthenticated: false,
    isRedirectingToLogin: false,
    projectTitle: 'AI创新实践队',
    projectId: 1,
    members: [],
    currentUserId: null,
    isLeader: false,
    projectStatus: '',
    canInvite: true,
    inviteDisabledReason: '',

    // 新增：页面状态
    loading: false,
    error: null
  },

  onLoad(options) {
    if (!this.ensureLogin()) return

    var app = getApp()
    var currentUserId = (app && app.globalData && app.globalData.userId) || parseInt(wx.getStorageSync('userId')) || null
    const projectId = Number(options.projectId) || 0
    this.setData({ projectId, currentUserId })
    this.checkProjectStatus(projectId)
    this.checkLeader(projectId, currentUserId)
  },

  async checkProjectStatus(projectId) {
    try {
      var project = await teamService.getProjectDetail(projectId)
      var status = (project.status || '').toLowerCase()
      var canInvite = status === 'recruiting'
      var reason = status === 'in_progress' ? '项目已开始' : (status === 'ended' ? '项目已结束' : '项目状态不支持邀请')
      this.setData({ projectStatus: status, canInvite: canInvite, inviteDisabledReason: reason })
    } catch (e) {
      this.setData({ canInvite: true })
    }
  },

  async checkLeader(projectId, currentUserId) {
    try {
      var members = await teamService.getMembers(projectId)
      var isLeader = false
      if (members && members.length) {
        for (var i = 0; i < members.length; i++) {
          var m = members[i]
          var muid = m.userId || m.id
          if (parseInt(muid) === parseInt(currentUserId) && m.role === 'leader') {
            isLeader = true
            break
          }
        }
      }
      this.setData({ isLeader: isLeader })
    } catch (e) { this.setData({ isLeader: false }) }
  },

  onReady() {
    this.loadMembers()
  },

  onShow() {
    // 从子页面（如发起投票）返回时，刷新成员列表
    if (this.data.projectId && this.data.isAuthenticated) {
      this.loadMembers()
    }
  },

  async loadMembers() {
    this.setData({ loading: true, error: null })

    // Mock 模式：直接返回本地数据
    if (USE_MOCK) {
      var that = this
      setTimeout(function() {
        that.setData({ members: MOCK_MEMBERS, loading: false })
        console.log('[T-035] loadMembers Mock', { count: MOCK_MEMBERS.length })
      }, 400)
      return
    }

    try {
      var list = await teamService.getMembers(this.data.projectId)
      console.log('[T-035] loadMembers success', { count: list.length })

      // 并发获取每个用户的详情（昵称、信誉分等）
      var enriched = []
      for (var i = 0; i < list.length; i++) {
        var member = list[i]
        var userId = member.userId || member.id
        var profile = null
        try {
          profile = await teamService.getUserProfile(userId)
        } catch (e) {
          // 获取详情失败，使用默认值
        }
        enriched.push({
          id: member.id,
          userId: userId,
          nickname: (profile && profile.nickname) || '未知用户',
          avatarUrl: (profile && (profile.avatarUrl || profile.avatar || profile.avatar_url)) || '',
          creditScore: (profile && (profile.creditScore || profile.trustScore)) || 0,
          major: (profile && profile.major) || '',
          school: (profile && profile.school) || '',
          bio: (profile && profile.bio) || '',
          role: member.role || '',
          status: member.status || 'active',
          exitMode: member.exitMode || '',
          exitDate: member.exitDate || ''
        })
      }

      this.setData({ members: enriched, loading: false })
    } catch (err) {
      const errorMsg = (err && err.message) || '加载失败，请重试'
      console.log('[T-035] loadMembers failed', err)
      this.setData({ loading: false, error: errorMsg, members: [] })
    }
  },

  handleMemberAction(e) {
    var that = this
    var dataset = e.currentTarget.dataset
    var userId = dataset.userId
    var nickname = dataset.nickname || '未知用户'
    var creditScore = dataset.creditScore || 0

    if (!that.data.isLeader) {
      wx.showToast({ title: '仅队长可操作', icon: 'none' })
      return
    }

    wx.showActionSheet({
      itemList: ['发起移除投票'],
      itemColor: '#EF4444',
      success: function(res) {
        if (res.tapIndex === 0) {
          wx.navigateTo({
            url: '/pages/initiate-vote/initiate-vote?projectId=' + that.data.projectId +
                 '&targetUserId=' + userId +
                 '&targetNickname=' + encodeURIComponent(nickname) +
                 '&targetCreditScore=' + creditScore
          })
        }
      }
    })
  },



  viewMember: function(e) {
    var userId = e.currentTarget.dataset.userId
    wx.navigateTo({ url: '/pages/profile/profile?userId=' + userId })
  },

  goInviteMember() {
    if (!this.data.canInvite) {
      wx.showToast({ title: this.data.inviteDisabledReason || '无法邀请', icon: 'none' })
      return
    }
    var name = this.data.projectTitle || ''
    wx.navigateTo({
      url: '/pages/member-match/member-match?projectId=' + this.data.projectId + '&projectName=' + name
    })
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

    this.setData({
      isAuthenticated: false,
      isRedirectingToLogin: true
    })

    wx.showToast({ title: '请先登录后使用', icon: 'none' })
    setTimeout(() => {
      wx.switchTab({ url: LOGIN_PAGE_URL })
    }, 300)
  }
})
