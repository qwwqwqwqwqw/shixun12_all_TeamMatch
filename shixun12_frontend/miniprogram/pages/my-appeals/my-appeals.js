const teamService = require('../../utils/team-service.js')

Page({
  data: { appeals: [], loading: true, error: null },
  onReady() { this.loadAppeals() },
  async loadAppeals() {
    this.setData({ loading: true, error: null })
    try {
      var appeals = await teamService.getMyAppeals()
      this.setData({ appeals: appeals, loading: false })
    } catch (err) {
      this.setData({ loading: false, error: '加载失败' })
    }
  }
})
