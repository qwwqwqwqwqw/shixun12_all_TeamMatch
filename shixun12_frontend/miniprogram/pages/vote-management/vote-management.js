var teamService = require('../../utils/team-service')

Page({
  data: {
    projectId: null,
    projectName: '',
    votes: [],
    loading: true,
    error: null
  },

  onLoad(options) {
    var projectId = parseInt(options.projectId) || 0
    var projectName = options.projectName || ''
    this.setData({ projectId: projectId, projectName: projectName, loading: true })
  },

  onReady() {
    this.loadVotes()
  },

  loadVotes() {
    var that = this
    this.setData({ loading: true, error: null })
    teamService.getProjectExitVotes(this.data.projectId).then(function(votes) {
      var list = votes.map(function(v) {
        return {
          id: v.id,
          targetUserId: v.targetUserId,
          initiatorId: v.initiatorId,
          status: v.status,
          statusLabel: v.status === 'voting' ? '投票中' : (v.status === 'closed' ? '已关闭' : v.status),
          result: v.result,
          resultLabel: v.result === 'pass' ? '通过' : (v.result === 'reject' ? '驳回' : '--'),
          penaltyLevel: v.penaltyLevel,
          penaltyLabel: v.penaltyLevel === 'malicious' ? '恶意退出' : '协商退出',
          agreeCount: v.agreeCount || 0,
          disagreeCount: v.disagreeCount || 0,
          totalVoters: v.totalVoters || 0,
          deadlineAt: v.deadlineAt || '',
          createdAt: v.createdAt || ''
        }
      })
      that.setData({ votes: list, loading: false })
    }).catch(function(err) {
      console.log('[vote-management] 加载失败', err)
      that.setData({ loading: false, error: '加载投票列表失败', votes: [] })
    })
  },

  goVoteDetail(e) {
    var voteId = e.currentTarget.dataset.voteId
    wx.navigateTo({
      url: '/pages/exit-vote-detail/exit-vote-detail?voteId=' + voteId + '&projectId=' + this.data.projectId
    })
  },


})
