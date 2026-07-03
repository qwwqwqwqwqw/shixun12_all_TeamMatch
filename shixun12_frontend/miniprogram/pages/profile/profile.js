// pages/profile/profile.js
var teamService = require('../../utils/team-service.js')
var apiService = require('../../utils/api-service.js')

Page({
  data: {
    loading: true,
    user: null,
    notFound: false
  },

  onLoad: function(options) {
    var userId = options.userId || ''
    this.loadUserProfile(userId)
  },

  async loadUserProfile(userId) {
    // Mock 模式：使用假数据
    if (apiService.USE_MOCK) {
      var that = this
      setTimeout(function() {
        that.setData({
          user: {
            nickname: '林乔',
            avatarUrl: '/assets/CodeBuddyAssets/114_182/2.svg',
            school: '元宇宙大学',
            major: '软件工程 22级',
            grade: '2022级',
            bio: 'Vue.js 重度用户，追求代码优雅。热爱前端技术，喜欢参与开源项目。',
            creditScore: 96,
            githubUsername: 'linqiao-vue',
            email: 'linqiao@bjtu.edu.cn'
          },
          loading: false
        })
      }, 400)
      return
    }

    // 真实模式
    try {
      var profile = await teamService.getUserProfile(userId)
      // 后端返回空数据表示用户未入驻
      if (!profile || (!profile.nickname && !profile.nickName)) {
        this.setData({ notFound: true, loading: false })
        return
      }
      this.setData({
        user: {
          nickname: profile.nickname || profile.nickName || '未知用户',
          avatarUrl: profile.avatarUrl || profile.avatar || '',
          school: profile.school || '',
          major: profile.major || '',
          grade: profile.grade || '',
          bio: profile.bio || '',
          creditScore: profile.creditScore || profile.trustScore || 0,
          githubUsername: profile.githubUsername || profile.giteeUsername || '',
          email: profile.email || ''
        },
        loading: false
      })
    } catch (err) {
      this.setData({ notFound: true, loading: false })
    }
  }
})
