// pages/projects/detail/detail.js
var api = require('../../../utils/api-service.js')
var request = api.request
var teamService = require('../../../utils/team-service.js')
var USE_MOCK = api.USE_MOCK

// ========== Mock 项目详情 ==========
var MOCK_DETAIL = {
  id: 1,
  title: '校园二手交易小程序',
  description: '开发一个面向全校师生的二手物品交易平台，支持商品发布、搜索、在线沟通、订单管理等功能。前端采用微信小程序，后端使用 Spring Boot + MySQL。',
  boardName: '小程序开发',
  maxMembers: 5,
  currentMembers: 3,
  status: 'recruiting',
  statusText: '招募中',
  skills: ['微信小程序', 'Spring Boot', 'MySQL', 'Vue.js'],
  deadline: '2026-06-15',
  creatorName: '张三',
  creatorAvatar: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
  members: [
    { id: 1, name: '张三', role: '队长', avatar: '/assets/CodeBuddyAssets/114_182/2.svg' },
    { id: 2, name: '李四', role: '成员', avatar: '/assets/CodeBuddyAssets/114_182/2.svg' },
    { id: 3, name: '王五', role: '成员', avatar: '/assets/CodeBuddyAssets/114_182/2.svg' }
  ],
  createdAt: '2026-04-01'
}

// status 值 → 展示文案
function getStatusText(status) {
  var map = {
    'recruiting': '招募中',
    'in_progress': '进行中',
    'ended': '已结束'
  }
  return map[status] || status
}

Page({
  data: {
    project: null,
    loading: false
  },

  onLoad: function(options) {
    var projectId = options.id || 1
    this.setData({ projectId: projectId })
    this.loadProjectDetail(projectId)
  },

  onShow: function() {
    // 从其他页面返回时刷新成员列表
    if (this.data.project) {
      this.refreshMembers()
    }
  },

  async refreshMembers() {
    try {
      var memberRes = await request({
        url: '/m4/projects/' + this.data.projectId + '/members',
        method: 'GET'
      })
      var rawMembers = memberRes.data || []
      var enriched = await Promise.all(rawMembers.map(async function(m) {
        var uid = m.userId || m.id
        var avatar = ''
        var name = m.nickname || m.name || ''
        try {
          var profile = await teamService.getUserProfile(uid)
          avatar = profile.avatarUrl || profile.avatar || profile.avatar_url || ''
          name = profile.nickname || profile.nickName || name || ''
        } catch (e) {}
        return {
          id: uid,
          name: name || '未知用户',
          role: m.role === 'leader' ? '队长' : '成员',
          avatar: avatar,
          userId: uid
        }
      }))
      this.setData({
        'project.members': enriched,
        'project.currentMembers': enriched.length
      })
    } catch (e) {}
  },

  async loadProjectDetail(projectId) {
    this.setData({ loading: true })

    if (USE_MOCK) {
      // Mock 模式
      var that = this
      setTimeout(function() {
        that.setData({
          project: MOCK_DETAIL,
          loading: false
        })
      }, 600)
    } else {
      // 真实模式：调用 GET /m4/projects/{projectId}
      try {
        var res = await request({
          url: '/m4/projects/' + projectId,
          method: 'GET'
        })
        // 检查项目是否存在
        if (!res.data) {
          var msg = '项目不存在'
          wx.showToast({ title: msg, icon: 'none', duration: 2000 })
          this.setData({ loading: false })
          setTimeout(function() { wx.navigateBack() }, 2000)
          return
        }

        var data = res.data
        var creatorId = data.creatorId || data.creator_id || 0
        var creatorName = data.creatorName || data.creator_name || ''
        var creatorAvatar = data.creatorAvatar || data.creator_avatar || ''

        // 获取真实成员列表，并拉取每个用户的头像
        var memberList = data.members || []
        var memberCount = data.currentMembers || data.current_members || 0
        try {
          var memberRes = await request({
            url: '/m4/projects/' + projectId + '/members',
            method: 'GET'
          })
          var rawMembers = memberRes.data || []

          // 并发获取每个成员的详细信息（头像等）
          var enrichedMembers = await Promise.all(rawMembers.map(async function(m) {
            var uid = m.userId || m.id
            var avatar = ''
            var name = m.nickname || m.name || ''
            try {
              var profile = await teamService.getUserProfile(uid)
              avatar = profile.avatarUrl || profile.avatar || profile.avatar_url || ''
              name = profile.nickname || profile.nickName || name || ''
            } catch (e) {
              // 获取失败，使用默认值
            }
            return {
              id: uid,
              name: name || '未知用户',
              role: m.role === 'leader' ? '队长' : '成员',
              avatar: avatar,
              userId: uid
            }
          }))

          memberList = enrichedMembers
          memberCount = memberList.length
        } catch (e) {
          // 获取成员列表失败，使用项目详情中的 members 数据
        }

        // 后端没返回创建者信息时，主动拉取用户详情
        if (!creatorName && creatorId) {
          try {
            var profile = await teamService.getUserProfile(creatorId)
            creatorName = profile.nickname || '未知用户'
            creatorAvatar = profile.avatarUrl || profile.avatar || ''
          } catch (e) {
            creatorName = '未知用户'
          }
        }

        this.setData({
          project: {
            id: data.id || projectId,
            title: data.title || '',
            description: data.description || '',
            boardName: data.boardName || data.board_name || '',
            maxMembers: data.maxMembers || data.max_members || 0,
            currentMembers: memberCount,
            status: data.status || '',
            statusText: getStatusText(data.status),
            skills: (data.skills || []).map(function(s) { return typeof s === 'object' ? (s.name || '') : s }),
            deadline: data.deadline || '',
            creatorId: creatorId,
            isCreator: parseInt(creatorId) === parseInt(wx.getStorageSync('userId')),
            creatorName: creatorName || '未知用户',
            creatorAvatar: creatorAvatar || '',
            members: memberList,
            createdAt: data.createdAt || data.created_at || ''
          },
          loading: false
        })
      } catch (err) {
        this.setData({ loading: false })
        wx.showToast({ title: '加载失败', icon: 'none' })
      }
    }
  },

  // 申请加入
  applyJoin: function() {
    var that = this
    var projectId = this.data.project.id
    var toUserId = this.data.project.creatorId  // 项目队长
    var fromUserId = parseInt(wx.getStorageSync('userId')) || 0
    wx.showModal({
      title: '申请加入',
      content: '确定要申请加入「' + (this.data.project.title || '') + '」吗？',
      success: function(res) {
        if (!res.confirm) return
        wx.showLoading({ title: '申请中' })
        teamService.sendApplyRequest(projectId, fromUserId, toUserId, '我对这个项目很感兴趣，希望能加入团队').then(function() {
          wx.hideLoading()
          wx.showToast({ title: '申请已发送', icon: 'success' })
        }).catch(function(err) {
          wx.hideLoading()
          var msg = (err && err.message) || '无法申请'
          wx.showToast({ title: msg, icon: 'none' })
        })
      }
    })
  },

  // 查看成员详情
  viewMember: function(e) {
    var userId = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/profile/profile?userId=' + userId })
  }
})
