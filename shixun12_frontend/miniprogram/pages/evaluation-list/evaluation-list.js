var teamService = require('../../utils/team-service')

Page({
  data: {
    projectId: null,
    projectName: '',
    members: [],
    loading: true,
    error: null
  },

  onLoad(options) {
    var projectId = Number(options.projectId) || 0
    var projectName = options.projectName || ''
    this.setData({ projectId: projectId, projectName: projectName, loading: true })
  },

  onReady() {
    this.loadMembers()
  },

  onShow() {
    if (this.data.projectId) {
      this.loadMembers()
    }
  },

  loadMembers() {
    var that = this
    this.setData({ loading: true, error: null })
    teamService.getEvaluatableMembers(this.data.projectId).then(function(members) {
      var list = (members || []).map(function(m) {
        var uid = m.userId || m.id
        return {
          userId: uid,
          nickname: m.nickname || '未知用户',
          major: m.major || '',
          avatarText: (m.nickname || '?').slice(0, 1),
          creditScore: m.creditScore || m.trustScore || 0,
          // 直接用后端返回的 evaluated 字段
          evaluated: m.evaluated === true || m.evaluated === 'true'
        }
      })
      that.setData({ members: list, loading: false })
    }).catch(function(err) {
      console.log('[evaluation-list] 加载失败:', err)
      that.setData({ loading: false, error: '加载可评价成员失败' })
    })
  },

  goEvaluate(e) {
    var targetUserId = e.currentTarget.dataset.userId
    var evaluated = e.currentTarget.dataset.evaluated
    if (evaluated === 'true') return
    wx.navigateTo({
      url: '/pages/evaluation/evaluation?projectId=' + this.data.projectId
        + '&targetUserId=' + targetUserId
        + '&projectName=' + this.data.projectName
    })
  }
})
