// pages/projects/list/list.js
var teamService = require('../../../utils/team-service.js')
var api = require('../../../utils/api-service.js')
var request = api.request

var PAGE_SIZE = 12

var STATUS_MAP = {
  'recruiting': '招募中',
  'in_progress': '进行中',
  'ended': '已结束',
  'eval_closed': '已关闭'
}
function getStatusText(status) {
  return STATUS_MAP[status] || status || ''
}

Page({
  data: {
    projects: [],
    allProjects: [],
    boardMap: {},
    searchKeyword: '',
    loading: false,
    refreshing: false,
    displayCount: PAGE_SIZE,
    hasMore: true
  },

  onLoad: function(options) {
    var myProjects = (options && options.myProjects === '1')
    this.loadBoards().then(() => this.loadProjects(myProjects))
  },

  async loadBoards() {
    try {
      var res = await request({ url: '/boards', method: 'GET' })
      var map = {}
      ;(res.data || []).forEach(function(b) { map[b.id] = b.name })
      this.setData({ boardMap: map })
    } catch (e) {}
  },

  loadProjects: function(myProjects) {
    var that = this
    this.setData({ loading: true })

    var req = myProjects
      ? teamService.getMyJoinedProjects()
      : teamService.getAllProjects()

    req.then(async function(list) {
      var mapped = (list || []).map(function(p) {
        var pid = p.id || p.projectId
        var status = p.status || 'recruiting'
        return {
          id: pid,
          title: p.name || p.title || ('项目#' + pid),
          description: p.description || '',
          boardName: that.data.boardMap[p.boardId] || p.boardName || '',
          maxMembers: p.maxMembers || 0,
          currentMembers: p.currentMembers || p.memberCount || 0,
          status: status,
          statusText: getStatusText(status),
          skills: p.skills || [],
          deadline: p.deadline || '',
          creatorName: p.creatorName || p.leader || ''
        }
      })

      // 并发拉取每个项目的成员数
      await Promise.all(mapped.map(function(proj) {
        return teamService.getMembers(proj.id).then(function(members) {
          proj.currentMembers = (members && members.length) || 0
        }).catch(function() {})
      }))

      that.setData({ allProjects: mapped, loading: false, displayCount: PAGE_SIZE })
      that.renderPage()
    }).catch(function() {
      that.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  onSearchInput: function(e) {
    this.setData({ searchKeyword: e.detail.value, displayCount: PAGE_SIZE })
    this.renderPage()
  },

  renderPage: function() {
    var keyword = (this.data.searchKeyword || '').trim().toLowerCase()
    var source = this.data.allProjects
    if (keyword) {
      source = source.filter(function(p) {
        return (p.title || '').toLowerCase().indexOf(keyword) > -1
          || (p.description || '').toLowerCase().indexOf(keyword) > -1
      })
    }
    var count = Math.min(this.data.displayCount, source.length)
    this.setData({
      projects: source.slice(0, count),
      hasMore: count < source.length
    })
  },

  onScrollToLower: function() {
    if (!this.data.hasMore) return
    this.setData({ displayCount: this.data.displayCount + PAGE_SIZE })
    this.renderPage()
  },

  onRefresherRefresh: function() {
    var that = this
    this.setData({ refreshing: true, displayCount: PAGE_SIZE })
    teamService.getAllProjects().then(function(list) {
      var mapped = (list || []).map(function(p) {
        var pid = p.id || p.projectId
        var status = p.status || 'recruiting'
        return {
          id: pid,
          title: p.name || p.title || ('项目#' + pid),
          description: p.description || '',
          boardName: that.data.boardMap[p.boardId] || p.boardName || '',
          maxMembers: p.maxMembers || 0,
          currentMembers: p.currentMembers || p.memberCount || 0,
          status: status,
          statusText: getStatusText(status),
          skills: p.skills || [],
          deadline: p.deadline || '',
          creatorName: p.creatorName || p.leader || ''
        }
      })
      that.setData({ allProjects: mapped, refreshing: false })
      that.renderPage()
    }).catch(function() {
      wx.showToast({ title: '刷新失败', icon: 'none' })
      that.setData({ refreshing: false })
    })
  },

  goToDetail: function(e) {
    var projectId = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/projects/detail/detail?id=' + projectId })
  }
})
