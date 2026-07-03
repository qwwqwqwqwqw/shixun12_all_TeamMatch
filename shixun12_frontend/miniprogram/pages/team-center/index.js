var teamService = require('../../utils/team-service')
var api = require('../../utils/api-service.js')
var request = api.request

function getUserId() {
  var id = wx.getStorageSync('userId')
  if (id) return parseInt(id)
  var app = getApp()
  return (app && app.globalData && app.globalData.userId) || 0
}

/**
 * 规范化项目状态：后端大写 → 前端小写 triple
 */
function normalizeStatus(raw) {
  var s = (raw || '').toUpperCase().trim()
  if (s === 'EVAL_CLOSED') return 'eval_closed'
  if (s === 'ENDED' || s === 'COMPLETED' || s === 'FINISHED') return 'ended'
  if (s === 'RECRUITING') return 'recruiting'
  // 剩余一律视为进行中
  return 'in_progress'
}

function statusText(status) {
  if (status === 'recruiting') return '招募中'
  if (status === 'in_progress') return '进行中'
  if (status === 'eval_closed') return '互评已截止'
  return '已结束'
}

Page({
  data: {
    activeTab: 'discover',
    joinedProjects: [],
    endedProjects: [],
    showEndedProjects: false,
    boardMap: {},
    pendingVotes: [],
    loadingJoined: false,

    // 卡片展开
    expandedId: null,

    // 确认弹窗
    showStartConfirm: false,
    showEndConfirm: false,
    confirmProjectId: null,
    confirmProjectName: ''
  },

  onShow() {
    // 从个人中心跳转时自动切换到"已加入项目"
    var teamTab = wx.getStorageSync('teamTab')
    if (teamTab === 'joined') {
      wx.removeStorageSync('teamTab')
      this.setData({ activeTab: 'joined' })
    }
    this.loadBoards()
    this.loadData()
  },

  loadBoards() {
    var that = this
    if (this.data.boardMap && Object.keys(this.data.boardMap).length) {
      return Promise.resolve(this.data.boardMap)
    }
    return request({ url: '/boards', method: 'GET' }).then(function(res) {
      var map = {}
      ;(res.data || []).forEach(function(b) { map[b.id] = b.name })
      that.setData({ boardMap: map })
      return map
    }).catch(function() { return {} })
  },

  loadData() {
    this.loadJoinedProjects()
  },

  loadJoinedProjects() {
    var that = this
    var currentUserId = getUserId()
    if (this.data.loadingJoined) return
    this.setData({ loadingJoined: true })

    teamService.getMyJoinedProjects().then(function(projects) {
      // 并行获取每个项目的成员列表，得到真实人数 & 判断是否是队长
      var enrichPromises = projects.map(function(p) {
        var pid = p.projectId || p.id
        return teamService.getMembers(pid).then(function(members) {
          var count = (members && members.length) ? members.length : 0
          // 判断当前用户是否是队长
          var isLeader = false
          if (members && members.length) {
            for (var i = 0; i < members.length; i++) {
              var m = members[i]
              var muid = m.userId || m.id
              if (parseInt(muid) === currentUserId && (m.role === 'leader')) {
                isLeader = true
                break
              }
            }
          }
          return { count: count, isLeader: isLeader }
        }).catch(function() {
          return { count: 0, isLeader: false }
        })
      })

      Promise.all(enrichPromises).then(function(enriched) {
        var mapped = projects.map(function(p, i) {
          var status = normalizeStatus(p.status)
          return {
            id: p.projectId || p.id,
            name: p.name || p.title || ('项目#' + (p.projectId || p.id)),
            leader: p.leader || p.creatorName || '--',
            members: enriched[i].count || p.currentMembers || p.memberCount || '--',
            maxMembers: p.maxMembers || '--',
            boardName: that.data.boardMap[p.boardId] || p.boardName || '',
            createdAt: p.createdAt || p.created_at || '',
            status: status,
            statusText: statusText(status),
            isLeader: enriched[i].isLeader,
            projectId: p.projectId || p.id
          }
        })
        // 按创建时间倒序：最近的项目在上
        mapped.sort(function(a, b) {
          var ta = new Date(a.createdAt).getTime() || 0
          var tb = new Date(b.createdAt).getTime() || 0
          if (ta === tb) return 0
          return tb - ta
        })
        var active = mapped.filter(function(p) { return p.status !== 'ended' && p.status !== 'eval_closed' })
        var ended = mapped.filter(function(p) { return p.status === 'ended' || p.status === 'eval_closed' })
        that.setData({
          joinedProjects: active,
          endedProjects: ended,
          loadingJoined: false
        })
        that.loadPendingVotes(active)
      })
    }).catch(function(err) {
      console.log('[team-center] 加载项目失败:', err)
      that.setData({ loadingJoined: false })
    })
  },

  loadPendingVotes(projects) {
    var that = this
    var votes = []
    var promises = []
    for (var i = 0; i < projects.length; i++) {
      var p = projects[i]
      ;(function(project) {
        promises.push(
          teamService.getProjectPendingVotes(project.id || project.projectId).then(function(res) {
            if (res && res.length > 0) {
              for (var j = 0; j < res.length; j++) {
                res[j].projectName = project.name
                res[j].projectStatus = project.status
                votes.push(res[j])
              }
            }
          }).catch(function() { /* 忽略 */ })
        )
      })(p)
    }
    Promise.all(promises).then(function() {
      that.setData({ pendingVotes: votes })
    })
  },

  // ==================== 卡片展开/收起 ====================

  toggleExpand(e) {
    var id = e.currentTarget.dataset.id
    this.setData({
      expandedId: this.data.expandedId === id ? null : id
    })
  },

  toggleEnded() {
    this.setData({ showEndedProjects: !this.data.showEndedProjects })
  },

  goMyCenter() {
    wx.switchTab({ url: '/pages/my-center/my-center' })
  },

  // ==================== 项目状态流转 ====================

  handleStartProject(e) {
    var id = e.currentTarget.dataset.id
    var name = e.currentTarget.dataset.name
    this.setData({
      showStartConfirm: true,
      confirmProjectId: id,
      confirmProjectName: name
    })
  },

  handleEndProject(e) {
    var id = e.currentTarget.dataset.id
    var name = e.currentTarget.dataset.name
    this.setData({
      showEndConfirm: true,
      confirmProjectId: id,
      confirmProjectName: name
    })
  },

  closeConfirm() {
    this.setData({
      showStartConfirm: false,
      showEndConfirm: false,
      confirmProjectId: null,
      confirmProjectName: ''
    })
  },

  confirmStartProject() {
    var that = this
    var projectId = this.data.confirmProjectId
    var userId = getUserId()
    wx.showLoading({ title: '处理中...', mask: true })
    teamService.startProject(projectId, userId).then(function() {
      wx.hideLoading()
      wx.showToast({ title: '项目已开始', icon: 'success' })
      that.closeConfirm()
      that.setData({ expandedId: null })
      that.loadJoinedProjects()
    }).catch(function(err) {
      wx.hideLoading()
      var msg = (err && err.message) || '操作失败'
      wx.showToast({ title: msg, icon: 'none' })
    })
  },

  confirmEndProject() {
    var that = this
    var projectId = this.data.confirmProjectId
    var userId = getUserId()
    wx.showLoading({ title: '处理中...', mask: true })
    teamService.endProject(projectId, userId).then(function() {
      wx.hideLoading()
      wx.showToast({ title: '项目已结束', icon: 'success' })
      that.closeConfirm()
      that.setData({ expandedId: null })
      that.loadJoinedProjects()
    }).catch(function(err) {
      wx.hideLoading()
      var msg = (err && err.message) || '操作失败'
      wx.showToast({ title: msg, icon: 'none' })
    })
  },

  // ==================== 页面跳转 ====================

  switchTab(e) {
    this.setData({ activeTab: e.currentTarget.dataset.tab, expandedId: null })
  },

  goProjectList() {
    wx.switchTab({ url: '/pages/index/index' })
    setTimeout(function() {
      wx.navigateTo({ url: '/pages/projects/list/list' })
    }, 100)
  },

  goTeamRequests() {
    wx.navigateTo({ url: '/pages/team-requests/team-requests' })
  },

  goProjectDetail(e) {
    var id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/projects/detail/detail?id=' + id })
  },

  goProjectMembers(e) {
    var id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/project-members/project-members?projectId=' + id })
  },

  goEvaluation(e) {
    var id = e.currentTarget.dataset.id
    var name = e.currentTarget.dataset.name || ''
    wx.navigateTo({ url: '/pages/evaluation-list/evaluation-list?projectId=' + id + '&projectName=' + name })
  },

  goExitProject(e) {
    var id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/exit-project/exit-project?projectId=' + id })
  },

  goExitVoteDetail(e) {
    var voteId = e.currentTarget.dataset.voteId
    var projectId = e.currentTarget.dataset.projectId
    wx.navigateTo({ url: '/pages/exit-vote-detail/exit-vote-detail?voteId=' + voteId + '&projectId=' + projectId })
  },

  goVoteManagement(e) {
    var id = e.currentTarget.dataset.id
    var name = e.currentTarget.dataset.name || ''
    wx.navigateTo({ url: '/pages/vote-management/vote-management?projectId=' + id + '&projectName=' + name })
  }
})
