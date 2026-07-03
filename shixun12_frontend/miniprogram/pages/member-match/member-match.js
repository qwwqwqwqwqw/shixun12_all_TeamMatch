var apiService = require('../../utils/api-service')
var teamService = require('../../utils/team-service')

Page({
  data: {
    projectId: null,
    projectName: '',
    projectStatus: '',
    isProjectEnded: false,
    isProjectLeader: false,
    searchKeyword: '',
    users: [],
    filteredUsers: [],
    loading: true,
    isSearching: false,
    invitingId: null,
    inviteMsg: '',
    showProjectPicker: false,
    pickTargetUserId: null,
    pickTargetNickname: '',
    myProjects: [],
    activeProjectId: null,
    showProjectBar: false
  },

  onLoad(options) {
    var projectId = Number(options.projectId) || 0
    var projectName = options.projectName || ''
    this.setData({ projectId: projectId, projectName: projectName, loading: true })
  },

  onReady() {
    this.loadRecommendUsers()
  },

  getRecommendations: function(projectId, limit) {
    return apiService.request({
      url: '/m4/projects/' + projectId + '/recommendations?limit=' + (limit || 50),
      method: 'GET'
    })
  },

  // 加载推荐用户
  async loadRecommendUsers() {
    var that = this
    this.setData({ loading: true })

    try {
      // 获取项目信息（仅从项目页进入时有 projectId）
      if (this.data.projectId) {
        try {
          var project = await teamService.getProjectDetail(this.data.projectId)
          var projStatus = (project.status || '').toUpperCase()
          var leaderId = project.creatorId || project.creator_id || 0
          var myId = parseInt(wx.getStorageSync('userId')) || 0
          var ended = projStatus === 'ENDED' || projStatus === 'COMPLETED' || projStatus === 'FINISHED' || project.status === 'ended'
          that.setData({
            projectName: project.name || project.title || that.data.projectName,
            projectStatus: projStatus,
            isProjectEnded: ended,
            isProjectLeader: leaderId === myId
          })
        } catch (e) { /* 非关键 */ }
      }

      // 获取我的招募中项目（邀请选择器上下文）
      var userId = parseInt(wx.getStorageSync('userId')) || 0
      try {
        var allProjects = await teamService.getAllProjects()
        var myCreatedProjects = []
        for (var i = 0; i < allProjects.length; i++) {
          var p = allProjects[i]
          if (p.creatorId === userId || p.creator_id === userId) {
            var pStatus = (p.status || '').toUpperCase()
            if (pStatus === 'RECRUITING') {
              myCreatedProjects.push({
                id: p.id,
                name: p.name || p.title || '',
                status: pStatus,
                members: p.currentMembers || p.memberCount || '--',
                maxMembers: p.maxMembers || '--'
              })
            }
          }
        }
        that.setData({
          myProjects: myCreatedProjects,
          activeProjectId: that.data.activeProjectId || (myCreatedProjects.length > 0 ? myCreatedProjects[0].id : null),
          showProjectBar: !that.data.projectId && myCreatedProjects.length > 1
        })
      } catch (e) { /* 非关键 */ }

      // 调用后端推荐接口
      // 项目页进入用 projectId，首页进入用 activeProjectId
      var effectiveProjectId = this.data.projectId || this.data.activeProjectId
      var userList = []
      if (effectiveProjectId) {
        try {
          var recRes = await this.getRecommendations(effectiveProjectId, 50)
          var items = (recRes && recRes.data) || []
          userList = items.map(function(item) {
            return {
              userId: item.userId,
              nickname: item.nickname || '未知用户',
              avatarUrl: item.avatarUrl || '',
              avatarText: (item.nickname || '?').slice(0, 1),
              creditScore: item.creditScore,
              jaccardSimilarity: item.jaccardSimilarity || 0,
              techAuthority: item.techAuthority || 0,
              trustFactor: item.trustFactor || 0,
              finalScore: item.finalScore || 0,
              matchedSkills: item.matchedSkills || [],
              authorityBreakdown: item.authorityBreakdown || '',
              major: '',
              skills: (item.matchedSkills || []).join('、') || '--',
              matchScore: Math.round((item.jaccardSimilarity || 0) * 100)
            }
          })
        } catch (e) {
          console.log('[member-match] 推荐接口失败:', e)
        }
      }

      that.setData({ users: userList, filteredUsers: userList, loading: false })
    } catch (err) {
      console.log('[member-match] 加载失败:', err)
      that.setData({ loading: false, users: [], filteredUsers: [] })
    }
  },

  // 搜索过滤
  onSearchInput(e) {
    var keyword = (e.detail.value || '').trim()
    this.setData({ searchKeyword: keyword })
    if (!keyword) {
      this.setData({ filteredUsers: this.data.users, isSearching: false })
      return
    }
    var that = this
    this.setData({ isSearching: true })
    teamService.searchUsers(keyword, 1, 20).then(function(results) {
      var myId = parseInt(wx.getStorageSync('userId')) || 0
      var mapped = (results || [])
        .filter(function(u) { return (u.userId || u.id) !== myId })
        .map(function(u) {
          return {
            userId: u.userId || u.id,
            nickname: u.nickname || u.name || '未知用户',
            avatarUrl: u.avatarUrl || u.avatar || u.avatar_url || '',
            avatarText: (u.nickname || u.name || 'U').slice(0, 1),
            major: u.major || u.department || '',
            skills: (u.skills || []).join('、') || '--',
            matchScore: 0,
            matchedSkills: [],
            authorityBreakdown: ''
          }
        })
      that.setData({ filteredUsers: mapped, isSearching: false })
    }).catch(function() {
      var kw = keyword.toLowerCase()
      var filtered = that.data.users.filter(function(u) {
        return u.nickname.toLowerCase().indexOf(kw) > -1
          || (u.skills || '').toLowerCase().indexOf(kw) > -1
      })
      that.setData({ filteredUsers: filtered, isSearching: false })
    })
  },

  switchActiveProject(e) {
    var id = e.currentTarget.dataset.projectId
    this.setData({ activeProjectId: id })
    this.loadRecommendUsers()
  },

  inviteUser(e) {
    if (this.data.isProjectEnded) {
      wx.showToast({ title: '项目已结束，无法邀请新成员', icon: 'none' })
      return
    }
    var targetUserId = e.currentTarget.dataset.userId
    var nickname = e.currentTarget.dataset.nickname || ''
    var myId = parseInt(wx.getStorageSync('userId')) || 0
    if (targetUserId === myId) {
      wx.showToast({ title: '不能邀请自己', icon: 'none' })
      return
    }
    // 项目页进入：固定邀到当前项目，不走多项目切换
    if (this.data.projectId) {
      this.doInvite(this.data.projectId, targetUserId, nickname)
      return
    }
    if (this.data.showProjectBar && this.data.activeProjectId) {
      this.doInvite(this.data.activeProjectId, targetUserId, nickname)
      return
    }
    if (this.data.myProjects.length === 0) {
      wx.showToast({ title: '暂无可用的招募中项目', icon: 'none' })
      return
    }
    if (this.data.myProjects.length === 1) {
      this.doInvite(this.data.myProjects[0].id, targetUserId, nickname)
      return
    }
    this.setData({
      showProjectPicker: true,
      pickTargetUserId: targetUserId,
      pickTargetNickname: nickname
    })
  },

  selectProject(e) {
    var projectId = e.currentTarget.dataset.projectId
    this.setData({ showProjectPicker: false })
    this.doInvite(projectId, this.data.pickTargetUserId, this.data.pickTargetNickname)
  },

  closeProjectPicker() {
    this.setData({ showProjectPicker: false })
  },

  doInvite(projectId, targetUserId, nickname) {
    var that = this
    that.setData({ invitingId: targetUserId })
    wx.showLoading({ title: '发送中' })
    teamService.sendInviteRequest(projectId, targetUserId, '诚邀你加入我们的项目团队').then(function() {
      wx.hideLoading()
      wx.showToast({ title: '邀请已发送', icon: 'success' })
      that.setData({ invitingId: null })
    }).catch(function(err) {
      wx.hideLoading()
      var msg = (err && err.message) || '无法邀请'
      wx.showToast({ title: msg, icon: 'none' })
      that.setData({ invitingId: null })
    })
  }
})
