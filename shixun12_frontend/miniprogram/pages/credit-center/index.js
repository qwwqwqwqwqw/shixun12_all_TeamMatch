var teamService = require('../../utils/team-service')

Page({
  data: {
    creditScore: 0,
    scoreLevel: 'green',
    loading: true
  },

  onShow() {
    this.loadScore()
  },

  loadScore() {
    var that = this
    this.setData({ loading: true })
    teamService.getCurrentCreditScore().then(function(score) {
      var level = score >= 80 ? 'green' : score >= 50 ? 'orange' : 'red'
      wx.setStorageSync('creditScore', score)
      that.setData({ creditScore: score, scoreLevel: level, loading: false })
    }).catch(function() {
      // 失败时回退本地缓存
      var score = wx.getStorageSync('creditScore') || 100
      var level = score >= 80 ? 'green' : score >= 50 ? 'orange' : 'red'
      that.setData({ creditScore: score, scoreLevel: level, loading: false })
    })
  },

  goCreditHistory() {
    wx.navigateTo({ url: '/pages/credit-history/credit-history' })
  },

  goReport() {
    wx.navigateTo({ url: '/pages/report/report' })
  },

  goAppeal() {
    wx.navigateTo({ url: '/pages/appeal/appeal' })
  },

  goMyReports() {
    wx.navigateTo({ url: '/pages/my-reports/my-reports' })
  },

  goMyAppeals() {
    wx.navigateTo({ url: '/pages/my-appeals/my-appeals' })
  }
})
