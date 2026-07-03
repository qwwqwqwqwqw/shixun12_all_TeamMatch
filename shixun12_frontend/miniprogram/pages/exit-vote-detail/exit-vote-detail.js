/*
 * T-038 退出投票详情页
 * 基线约束（V2.1）：
 * 1. 投票选项仅 agree / disagree，无弃权
 * 2. 被投票人不能投票（前端按钮隐藏，后端最终校验）
 * 3. 仅队长可手动关闭投票
 * 4. deadline_at = created_at + 24h，超时后懒检查关闭
 * 5. 投票通过后，成员状态 exited，exit_mode = exit_vote，根据 penalty_level 扣分
 * 6. 统一成功响应码 code: 200
 */

const teamService = require('../../utils/team-service')

Page({
  data: {
    projectId: null,
    voteId: null,
    voteInfo: {
      id: null,
      targetUser: { id: null, nickname: '', avatar: '', major: '', trustScore: 0 },
      initiatorId: null,
      penaltyLevel: 'negotiated',
      status: 'voting',
      result: null,
      totalVoters: 0,
      agreeCount: 0,
      disagreeCount: 0,
      deadlineAt: '',
      createdAt: '',
      myVote: null,
      voters: []
    },
    loading: true,
    error: null,
    submitting: false,
    currentUserId: null,
    isLeader: false,
    countdownText: '--:--:--',
    formatDeadline: ''
  },

  onLoad(options) {
    const voteId = parseInt(options.voteId) || 0
    const projectId = parseInt(options.projectId) || 0
    const app = getApp()
    var currentUserId = (app && app.globalData && app.globalData.userId) || wx.getStorageSync('userId') || null
    if (currentUserId) currentUserId = parseInt(currentUserId)
    this.setData({ voteId, projectId, currentUserId })
  },

  onReady() {
    this.loadVoteDetail()
  },

  onUnload() {
    this.clearTimer()
  },

  clearTimer() {
    if (this._timer) {
      clearInterval(this._timer)
      this._timer = null
    }
  },

  /** 启动每秒倒计时更新 */
  startCountdown(deadlineAt) {
    this.clearTimer()
    var that = this
    var deadline = new Date(deadlineAt).getTime()
    if (isNaN(deadline)) return

    function tick() {
      var diff = deadline - Date.now()
      var text
      if (diff > 0) {
        var hours = Math.floor(diff / 3600000)
        var mins = Math.floor((diff % 3600000) / 60000)
        var secs = Math.floor((diff % 60000) / 1000)
        text = String(hours).padStart(2, '0') + ':' + String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0')
      } else {
        text = '00:00:00'
      }
      that.setData({ countdownText: text })
    }

    tick()
    this._timer = setInterval(tick, 1000)
  },

  /** 通过用户详情接口获取用户信息 */
  fetchUserInfo(userId) {
    var that = this
    return teamService.getUserProfile(userId).then(function(user) {
      return {
        id: user.id || user.userId || userId,
        nickname: user.nickname || '未知用户',
        avatar: user.avatarUrl || user.avatar || '',
        trustScore: user.creditScore || user.trustScore || 0
      }
    }).catch(function() {
      return { id: userId, nickname: '未知用户', avatar: '', trustScore: 0 }
    })
  },

  /** 加载目标用户和投票者信息 */
  enrichTargetUser(voteInfo, projectId) {
    var that = this
    var targetId = voteInfo.targetUserId || voteInfo.targetUser.id
    if (!targetId) return Promise.resolve(voteInfo)

    // 收集所有需要获取信息的 userId
    var userIds = [targetId]
    if (voteInfo.voters && voteInfo.voters.length > 0) {
      for (var v = 0; v < voteInfo.voters.length; v++) {
        var vid = voteInfo.voters[v].userId || voteInfo.voters[v].id
        if (vid && userIds.indexOf(vid) === -1) userIds.push(vid)
      }
    }

    // 并发请求所有用户详情
    var promises = []
    for (var i = 0; i < userIds.length; i++) {
      promises.push(that.fetchUserInfo(userIds[i]))
    }

    return Promise.all(promises).then(function(users) {
      // 构建 userId → userInfo 映射
      var userMap = {}
      for (var u = 0; u < users.length; u++) {
        userMap[users[u].id] = users[u]
      }

      // 填充目标用户
      var userInfo = userMap[targetId]
      if (userInfo) {
        voteInfo.targetUser = voteInfo.targetUser || {}
        voteInfo.targetUser.id = targetId
        voteInfo.targetUser.nickname = userInfo.nickname
        voteInfo.targetUser.avatar = userInfo.avatar
        voteInfo.targetUser.trustScore = userInfo.trustScore
      }

      // 填充投票者信息
      if (voteInfo.voters && voteInfo.voters.length > 0) {
        for (var w = 0; w < voteInfo.voters.length; w++) {
          var voter = voteInfo.voters[w]
          var vid = voter.userId || voter.id
          var vu = userMap[vid]
          if (vu) {
            voter.nickname = vu.nickname
            voter.avatarUrl = vu.avatar
          }
        }
      }

      return voteInfo
    })
  },

  loadVoteDetail() {
    this.setData({ loading: true, error: null })
    var that = this
    teamService.getExitVoteDetail(this.data.projectId, this.data.voteId).then(function(voteInfo) {
      if (!voteInfo) {
        that.setData({ loading: false, error: '数据为空' })
        return
      }

      // 从后端返回的投票数据中提取 projectId（兼容函数入口未传参的情况）
      var realProjectId = voteInfo.projectId || voteInfo.project_id || that.data.projectId
      if (realProjectId && realProjectId !== that.data.projectId) {
        that.setData({ projectId: realProjectId })
      }

      // 格式化截止时间
      var deadlineDate = new Date(voteInfo.deadlineAt)
      var y = deadlineDate.getFullYear()
      var mo = String(deadlineDate.getMonth() + 1).padStart(2, '0')
      var d = String(deadlineDate.getDate()).padStart(2, '0')
      var h = String(deadlineDate.getHours()).padStart(2, '0')
      var mi = String(deadlineDate.getMinutes()).padStart(2, '0')
      var formatDeadline = y + '-' + mo + '-' + d + ' ' + h + ':' + mi

      // 丰富用户信息
      that.enrichTargetUser(voteInfo).then(function(enriched) {
        var uid = that.data.currentUserId || wx.getStorageSync('userId') || 0
        var isLeader = Number(uid) === Number(enriched.initiatorId)
        that.setData({ voteInfo: enriched, isLeader: isLeader, loading: false, formatDeadline: formatDeadline })
        // 启动实时倒计时
        that.startCountdown(voteInfo.deadlineAt)
      })
    }).catch(function(err) {
      var msg = (err && err.message) || '加载失败，请重试'
      that.setData({ loading: false, error: msg })
      wx.showToast({ title: msg, icon: 'none' })
    })
  },

  handleVote(e) {
    const choice = e.currentTarget.dataset.choice
    if (this.data.submitting || this.data.voteInfo.myVote) return

    this.setData({ submitting: true })
    teamService.submitExitVote(this.data.projectId, this.data.voteId, choice).then(() => {
      wx.showToast({ title: '投票成功', icon: 'success' })
      this.loadVoteDetail()
    }).catch(err => {
      var msg = (err && err.message) || '操作失败'
      this.setData({ submitting: false })
      wx.showToast({ title: msg, icon: 'none' })
    })
  },

  handleCloseVote() {
    if (!this.data.isLeader || this.data.voteInfo.status !== 'voting') return

    wx.showModal({
      title: '关闭投票',
      content: '确定要提前关闭此投票吗？关闭后将根据当前票数判定结果',
      success: (res) => {
        if (res.confirm) {
          teamService.closeExitVote(this.data.projectId, this.data.voteId).then(() => {
            wx.showToast({ title: '投票已关闭', icon: 'success' })
            this.loadVoteDetail()
          }).catch(err => {
            var msg = (err && err.message) || '操作失败'
            wx.showToast({ title: msg, icon: 'none' })
          })
        }
      }
    })
  },

  handleCancelVote() {
    if (!this.data.isLeader || this.data.voteInfo.status !== 'voting') return

    wx.showModal({
      title: '撤回投票',
      content: '确定要撤回该投票吗？撤回后投票作废，不会对任何成员产生影响。',
      success: (res) => {
        if (res.confirm) {
          teamService.cancelExitVote(this.data.projectId, this.data.voteId).then(() => {
            wx.showToast({ title: '投票已撤回', icon: 'success' })
            setTimeout(() => { wx.navigateBack() }, 1000)
          }).catch(err => {
            var msg = (err && err.message) || '操作失败'
            wx.showToast({ title: msg, icon: 'none' })
          })
        }
      }
    })
  },

  isLeader() {
    var uid = this.data.currentUserId || wx.getStorageSync('userId') || 0
    var iid = this.data.voteInfo.initiatorId || 0
    return Number(uid) === Number(iid)
  },


})
