const teamService = require('../../utils/team-service.js')

Page({
  data: { reports: [], loading: true, error: null },
  onReady() { this.loadReports() },
  async loadReports() {
    this.setData({ loading: true, error: null })
    try {
      var list = await teamService.getMyReports()
      // 映射后端字段
      var reports = []
      for (var i = 0; i < list.length; i++) {
        var item = list[i]
        var report = {
          id: item.id,
          targetType: item.targetType || item.target_type || '',
          targetId: item.targetId || item.target_id,
          reason: item.reason || '',
          status: item.status || '',
          createdAt: item.createdAt || item.created_at || '',
          // targetName 需要额外获取，暂时显示类型+ID
          targetName: (item.targetType || item.target_type) === 'user' ? '用户' : '项目' + '#' + (item.targetId || item.target_id)
        }
        // 尝试获取目标名称
        var targetId = item.targetId || item.target_id
        if ((item.targetType || item.target_type) === 'user' && targetId) {
          try {
            var profile = await teamService.getUserProfile(targetId)
            report.targetName = profile.nickname || '未知用户'
          } catch (e) {
            report.targetName = '未知用户'
          }
        }
        reports.push(report)
      }
      this.setData({ reports: reports, loading: false })
    } catch (err) {
      this.setData({ loading: false, error: '加载失败' })
    }
  }
})
