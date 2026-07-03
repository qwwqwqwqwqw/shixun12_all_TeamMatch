// pages/leaderboard/leaderboard.js
var api = require('../../utils/api-service.js')
var request = api.request
var USE_MOCK = api.USE_MOCK

var INITIAL_PAGE_SIZE = 10
var LOAD_MORE_PAGE_SIZE = 10

// ========== Mock 数据 ==========
// 真实 GitHub/Gitee 数据: username|techScore|totalRepos|totalStars|totalCommits|totalPrs
var MOCK_DATA = [
  // ---- GitHub ----
  { userId: 1, username: 'Allenpandas', githubUsername: 'Allenpandas', source: 'github', giteeUsername: '', skills: ['Python', 'JavaScript', 'HTML'], repoCount: 1105, totalStars: 25, totalCommits: 23300, score: 59330 },
  { userId: 2, username: 'Tang1705', githubUsername: 'Tang1705', source: 'github', giteeUsername: '', skills: ['Java', 'Vue.js', 'Spring Boot'], repoCount: 599, totalStars: 53, totalCommits: 13060, score: 33928 },
  { userId: 3, username: 'YXHXianYu', githubUsername: 'YXHXianYu', source: 'github', giteeUsername: '', skills: ['Python', 'Django', 'Flask'], repoCount: 128, totalStars: 61, totalCommits: 2720, score: 8251 },
  { userId: 4, username: 'paulzhn', githubUsername: 'paulzhn', source: 'github', giteeUsername: '', skills: ['Java', 'Python', 'Go'], repoCount: 77, totalStars: 37, totalCommits: 1720, score: 5138 },
  { userId: 5, username: 'fenginsist', githubUsername: 'fenginsist', source: 'github', giteeUsername: '', skills: ['Python', 'C++', 'OpenCV'], repoCount: 61, totalStars: 45, totalCommits: 1400, score: 4506 },
  { userId: 6, username: 'Michael7099', githubUsername: 'Michael7099', source: 'github', giteeUsername: '', skills: ['Java', 'MySQL', 'Redis'], repoCount: 20, totalStars: 4, totalCommits: 450, score: 1212 },
  { userId: 7, username: 'chenzewei01', githubUsername: 'chenzewei01', source: 'github', giteeUsername: '', skills: ['JavaScript', 'Node.js', 'MongoDB'], repoCount: 16, totalStars: 1, totalCommits: 410, score: 1019 },
  { userId: 8, username: 'Coconut00', githubUsername: 'Coconut00', source: 'github', giteeUsername: '', skills: ['Python', '机器学习', 'TensorFlow'], repoCount: 6, totalStars: 8, totalCommits: 150, score: 550 },
  { userId: 9, username: 'Invictus46', githubUsername: 'Invictus46', source: 'github', giteeUsername: '', skills: ['Java', 'Spring', 'Hibernate'], repoCount: 5, totalStars: 1, totalCommits: 190, score: 458 },
  { userId: 10, username: 'Rowsu1', githubUsername: 'Rowsu1', source: 'github', giteeUsername: '', skills: ['C++', 'Qt', 'Linux'], repoCount: 0, totalStars: 3, totalCommits: 30, score: 129 },
  // ---- Gitee ----
  { userId: 11, username: 'jaywcjlove', githubUsername: '', source: 'gitee', giteeUsername: 'jaywcjlove', skills: ['JavaScript', 'CSS', 'Node.js'], repoCount: 2759, totalStars: 100, totalCommits: 55680, score: 144009 },
  { userId: 12, username: 'openharmony_ci', githubUsername: '', source: 'gitee', giteeUsername: 'openharmony_ci', skills: ['C', 'JavaScript', 'HarmonyOS'], repoCount: 0, totalStars: 4640, totalCommits: 1, score: 13922 },
  { userId: 13, username: 'su-youpengeng', githubUsername: '', source: 'gitee', giteeUsername: 'su-youpengeng', skills: ['Java', 'Python', 'SQL'], repoCount: 7, totalStars: 7, totalCommits: 175, score: 588 },
  { userId: 14, username: 'Huoweidujiangji', githubUsername: '', source: 'gitee', giteeUsername: 'Huoweidujiangji', skills: ['Python', 'C++', 'Qt'], repoCount: 2, totalStars: 15, totalCommits: 115, score: 597 },
  { userId: 15, username: 'hitech', githubUsername: '', source: 'gitee', giteeUsername: 'hitech', skills: ['Go', 'Docker', 'Kubernetes'], repoCount: 6, totalStars: 5, totalCommits: 145, score: 471 },
  { userId: 16, username: 'iangzvoi', githubUsername: '', source: 'gitee', giteeUsername: 'iangzvoi', skills: ['Python', 'Flask', 'Redis'], repoCount: 1, totalStars: 9, totalCommits: 65, score: 348 },
  { userId: 17, username: 'ideal', githubUsername: '', source: 'gitee', giteeUsername: 'ideal', skills: ['Java', 'Spring Boot', 'MySQL'], repoCount: 2, totalStars: 2, totalCommits: 50, score: 168 },
  { userId: 18, username: 'huang_yonghui0412', githubUsername: '', source: 'gitee', giteeUsername: 'huang_yonghui0412', skills: ['Python', '数据分析', 'Pandas'], repoCount: 0, totalStars: 2, totalCommits: 10, score: 66 },
  { userId: 19, username: 'tuboyan', githubUsername: '', source: 'gitee', giteeUsername: 'tuboyan', skills: ['Java', 'Android', 'Kotlin'], repoCount: 0, totalStars: 1, totalCommits: 5, score: 33 },
  { userId: 20, username: 'Nolan-Wink', githubUsername: '', source: 'gitee', giteeUsername: 'Nolan-Wink', skills: ['Python', 'C', '嵌入式'], repoCount: 0, totalStars: 1, totalCommits: 5, score: 33 }
]

/**
 * 解析 topLanguages 字段
 * 后端返回可能是 JSON 字符串 "[\"Java\",\"Python\"]" 或已是数组
 */
function parseLanguages(val) {
  if (!val) return []
  if (Array.isArray(val)) return val
  try {
    var parsed = JSON.parse(val)
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    // 不是 JSON 字符串，尝试按逗号分割
    return String(val).split(',').map(function(s) { return s.trim() }).filter(Boolean)
  }
}

/**
 * 将后端字段映射为前端 WXML 需要的字段名
 * @param {Object} item  - 后端返回的单条数据
 * @param {number} index - 数组索引，用于生成 rank
 */
function mapItem(item, index) {
  return {
    rank: item.rank != null ? item.rank : (index + 1),
    userId: item.userId,
    username: item.nickname || '未知用户',
    avatarUrl: item.avatarUrl || '',
    githubUsername: item.githubUsername || '',
    giteeUsername: item.giteeUsername || '',
    source: item.source || '',
    skills: item.skills || parseLanguages(item.topLanguages),
    repoCount: item.totalRepos || item.repoCount || 0,
    totalStars: item.totalStars || 0,
    totalCommits: item.totalCommits || 0,
    score: item.techScore || item.score || 0,
    school: item.school || ''
  }
}

Page({
  data: {
    loading: true,
    leaderboardData: [],
    sortType: '',
    hasGithub: false,
    currentUserId: 0,
    myRank: null,
    displayCount: INITIAL_PAGE_SIZE,
    hasMore: true,
    loadingMore: false
  },

  onLoad: function(options) {
    var sortType = (options && options.sort) || ''
    var uid = parseInt(wx.getStorageSync('userId')) || 0
    this.setData({ sortType: sortType, currentUserId: uid })
    this.loadLeaderboard()
    this.checkGithubBind()
  },

  // 检测用户是否已绑定 GitHub
  async checkGithubBind() {
    try {
      var res = await request({
        url: '/profile/tech-profile',
        method: 'GET'
      })
      var data = res.data || {}
      this.setData({ hasGithub: !!(data.githubUsername || data.giteeUsername) })
    } catch (e) {
      this.setData({ hasGithub: false })
    }
  },

  async loadLeaderboard() {
    var that = this
    this.setData({ loading: true })

    // Mock 数据永驻，深拷贝后偏移 userId 避免与真实用户冲突
    var data = JSON.parse(JSON.stringify(MOCK_DATA)).map(function(d, i) {
      d.userId = 10001 + i
      d.isMock = true
      return d
    })

    // 尝试拉取真实 API 数据并合并
    try {
      var sort = that.data.sortType
      var url = '/leaderboard?page=1&size=100'
      if (sort) url += '&sort=' + sort
      var res = await request({ url: url, method: 'GET' })
      var apiList = (res.data || []).map(mapItem)
      for (var i = 0; i < apiList.length; i++) {
        var item = apiList[i]
        if (item) {
          data.push(item)
        }
      }
    } catch (err) {
      // 后端不可用，仅用 Mock
    }

    // 按类型排序 + 重新排名
    var s = that.data.sortType
    if (s === 'totalStars') data.sort(function(a, b) { return (b.totalStars || 0) - (a.totalStars || 0) })
    else if (s === 'totalRepos') data.sort(function(a, b) { return (b.repoCount || 0) - (a.repoCount || 0) })
    else if (s === 'totalCommits') data.sort(function(a, b) { return (b.totalCommits || 0) - (a.totalCommits || 0) })
    else data.sort(function(a, b) { return (b.score || 0) - (a.score || 0) })
    data.forEach(function(item, i) { item.rank = i + 1 })

    // 找出"我的排名"
    var uid = that.data.currentUserId
    var mine = null
    for (var j = 0; j < data.length; j++) {
      if (data[j].userId === uid) { mine = data[j]; break }
    }

    that.setData({
      leaderboardData: data,
      myRank: mine,
      loading: false,
      displayCount: INITIAL_PAGE_SIZE,
      hasMore: data.length > INITIAL_PAGE_SIZE
    })
  },

  onScrollToLower: function() {
    if (this.data.loadingMore || !this.data.hasMore) return
    var newCount = this.data.displayCount + LOAD_MORE_PAGE_SIZE
    this.setData({
      loadingMore: true,
      displayCount: newCount,
      hasMore: newCount < this.data.leaderboardData.length
    })
    var that = this
    setTimeout(function() { that.setData({ loadingMore: false }) }, 300)
  },

  viewProfile: function(e) {
    var userId = e.currentTarget.dataset.userId || ''
    wx.navigateTo({ url: '/pages/profile/profile?userId=' + userId })
  },

  goGitHubBind: function() {
    wx.switchTab({ url: '/pages/index/index' })
  }
})
