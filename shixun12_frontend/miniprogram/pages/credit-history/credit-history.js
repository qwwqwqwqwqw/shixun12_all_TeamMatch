/*
 * T-042 信誉分变化历史页
 * 基线约束（V2.1）：
 * 1. 信誉分初始值 100
 * 2. 当前信誉分 = 100 + SUM(change_value WHERE effective=1)
 * 3. 流水类型：evaluation / exit_vote / self_exit / penalty / appeal_restore
 * 4. effective=1 生效，effective=0 挂起或不计分
 * 5. 统一成功响应码 code: 200
 */

const teamService = require('../../utils/team-service.js')

// 变化类型 → 来源描述映射
var SOURCE_DESC_MAP = {
  evaluation: '互评',
  exit_vote: '投票退出',
  self_exit: '主动退出',
  penalty: '处罚扣分',
  penalty_restore: '处罚撤销',
  appeal_restore: '申诉恢复'
}

function getSourceDesc(changeType) {
  return SOURCE_DESC_MAP[changeType] || changeType || '其他'
}

// 清理旧格式 description 中的类型前缀，避免与 sourceDesc 重复
// 旧数据可能有嵌套前缀：如 "处罚撤销恢复: 申诉通过: 申诉通过"
function cleanDesc(changeType, rawDesc) {
  if (!rawDesc) return '信誉分变化'
  var d = rawDesc
  switch (changeType) {
    case 'penalty':
      d = d.replace(/^处罚扣分:\s*/, '')
      // 旧数据中管理员可能误输入纯数字，转为可读格式
      if (/^\d+$/.test(d)) d = '扣' + d + '分'
      break
    case 'penalty_restore':
      // 外层前缀
      d = d.replace(/^处罚撤销恢复:\s*/, '')
      // 内层嵌套前缀（旧 AppealServiceImpl 拼接的 "申诉通过: "）
      d = d.replace(/^申诉通过:\s*/, '')
      break
    case 'self_exit':
      d = d.replace(/^主动退出项目，信誉分/, '退出项目，')
      break
    case 'exit_vote':
      d = d.replace(/^被投票退出项目，信誉分/, '退出项目，')
      break
    case 'appeal_restore':
      d = d.replace(/^申诉恢复\s*[-:]\s*/, '')
      break
  }
  return d.trim() || '信誉分变化'
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  // 后端返回格式: "2026-06-05T12:00:00" 或 "2026-06-05 12:00:00"
  var d = dateStr.replace('T', ' ')
  // 只取到分钟
  if (d.length >= 16) {
    return d.substring(0, 16)
  }
  return d
}

Page({
  data: {
    currentScore: 0,
    initialScore: 100,
    records: [],
    loading: true,
    error: null
  },

  onLoad() {
    // 只做同步赋值
  },

  onReady() {
    this.loadData()
  },

  async loadData() {
    this.setData({ loading: true, error: null })
    try {
      const [score, records] = await Promise.all([
        teamService.getCurrentCreditScore(),
        teamService.getCreditHistory()
      ])

      var sourceMap = {
        evaluation: '互评',
        exit_vote: '投票退出',
        self_exit: '主动退出',
        penalty: '处罚',
        penalty_restore: '处罚撤销',
        appeal_restore: '申诉恢复'
      }

      var mapped = (records || []).map(function(r) {
        return {
          id: r.id,
          changeType: r.changeType,
          changeValue: r.changeValue,
          effective: r.effective !== false,
          desc: r.description || r.reason || (r.changeValue > 0 ? '信誉分恢复' : '信誉分扣除'),
          sourceDesc: sourceMap[r.changeType] || r.changeType || '',
          time: r.createdAt || ''
        }
      })

      this.setData({
        currentScore: score,
        records: mapped,
        loading: false
      })
    } catch (err) {
      this.setData({
        loading: false,
        error: '加载失败，请重试'
      })
    }
  },

  handleRecordTap(e) {
    const id = e.currentTarget.dataset.id
    // 暂不跳转详情页，预留
  }
})
