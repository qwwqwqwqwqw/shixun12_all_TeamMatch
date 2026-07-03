// pages/projects/create/create.js
var api = require('../../../utils/api-service.js')
var request = api.request
var USE_MOCK = api.USE_MOCK

// ========== Mock 技能标签 ==========
var MOCK_SKILL_TAGS = [
  { id: 1, name: '微信小程序', category: '', status: 'active' },
  { id: 2, name: 'Spring Boot', category: '', status: 'active' },
  { id: 3, name: 'Vue.js', category: '', status: 'active' },
  { id: 4, name: 'React', category: '', status: 'active' },
  { id: 5, name: 'Python', category: '', status: 'active' },
  { id: 6, name: 'MySQL', category: '', status: 'active' }
]

Page({
  data: {
    form: {
      title: '',
      description: '',
      boardId: '',
      maxMembers: 3,
      skills: [],
      deadline: '',
      deadlineTime: '23:59'
    },
    boardList: [],
    boardIndex: -1,
    skillTagList: [],
    submitting: false,
    errorMsg: '',
    minDate: '',
    minTime: '00:00'
  },

  async onLoad(options) {
    // 计算最早可选日期和最早可选时间
    var now = new Date()
    var y = now.getFullYear()
    var m = String(now.getMonth() + 1).padStart(2, '0')
    var d = String(now.getDate()).padStart(2, '0')
    // 最早时间是当前小时+1 的整点，23点之后推到次日
    var nextHour = now.getHours() + 1
    if (nextHour >= 24) nextHour = 0
    var minTime = String(nextHour).padStart(2, '0') + ':00'
    this.setData({ minDate: y + '-' + m + '-' + d, minTime: minTime })

    if (USE_MOCK) {
      this.setData({
        boardList: [
          { id: 1, name: '小程序开发' },
          { id: 2, name: 'Web应用' },
          { id: 3, name: 'AI应用' },
          { id: 4, name: '校园工具' }
        ],
        skillTagList: MOCK_SKILL_TAGS
      })
    } else {
      try {
        var boardRes = await request({ url: '/boards', method: 'GET' })
        var boards = (boardRes.data || []).filter(function(b) { return b.status === 'active' })
        this.setData({ boardList: boards })
      } catch (err) {
        this.setData({
          boardList: [{ id: 1, name: '小程序开发' }, { id: 2, name: 'Web应用' }]
        })
      }
      try {
        var tagRes = await request({ url: '/profile/skills/tags', method: 'GET' })
        var tags = (tagRes.data || []).filter(function(t) { return t.status === 'active' })
        this.setData({ skillTagList: tags })
      } catch (err) {
        this.setData({ skillTagList: MOCK_SKILL_TAGS })
      }
    }
  },

  // 输入框通用处理
  onInputChange: function(e) {
    var field = e.currentTarget.dataset.field
    this.setData({ ['form.' + field]: e.detail.value, errorMsg: '' })
  },

  // 板块选择
  onBoardChange: function(e) {
    var index = parseInt(e.detail.value)
    var board = this.data.boardList[index]
    this.setData({ boardIndex: index, 'form.boardId': board.id, errorMsg: '' })
  },

  // 技能选择（多选）
  onSkillToggle: function(e) {
    console.log('onSkillToggle 触发', e.currentTarget.dataset.id, typeof e.currentTarget.dataset.id)
    var form = JSON.parse(JSON.stringify(this.data.form))
    var skillId = parseInt(e.currentTarget.dataset.id, 10)
    var idx = form.skills.indexOf(skillId)
    if (idx > -1) {
      form.skills.splice(idx, 1)
    } else {
      form.skills.push(skillId)
    }
    console.log('skills 更新后:', form.skills)
    this.setData({ form: form, errorMsg: '' })
  },

  // 人数选择
  onMemberChange: function(e) {
    var value = parseInt(e.detail.value) || 1
    this.setData({ 'form.maxMembers': value, errorMsg: '' })
  },

  // 截止日期选择
  onDateChange: function(e) {
    var date = e.detail.value
    // 选了今天就把时间也校准到最早可选项
    if (date === this.data.minDate) {
      var form = this.data.form
      if (form.deadlineTime < this.data.minTime) {
        this.setData({ 'form.deadline': date, 'form.deadlineTime': this.data.minTime, errorMsg: '' })
        return
      }
    }
    this.setData({ 'form.deadline': date, errorMsg: '' })
  },

  // 截止时间选择
  onTimeChange: function(e) {
    var time = e.detail.value
    // 选了今天则时间不得早于 minTime
    if (this.data.form.deadline === this.data.minDate && time < this.data.minTime) {
      this.setData({ 'form.deadlineTime': this.data.minTime, errorMsg: '' })
      return
    }
    this.setData({ 'form.deadlineTime': time, errorMsg: '' })
  },

  // 表单提交
  async submitForm() {
    var title = this.data.form.title
    var description = this.data.form.description
    var boardId = this.data.form.boardId
    var skills = this.data.form.skills
    var deadline = this.data.form.deadline
    var deadlineTime = this.data.form.deadlineTime
    var maxMembers = this.data.form.maxMembers

    if (!title.trim()) {
      this.setData({ errorMsg: '请输入项目标题' })
      return
    }
    if (title.length > 30) {
      this.setData({ errorMsg: '项目标题不能超过30个字符' })
      return
    }
    if (!description.trim()) {
      this.setData({ errorMsg: '请输入项目描述' })
      return
    }
    if (!boardId) {
      this.setData({ errorMsg: '请选择项目板块' })
      return
    }
    if (skills.length === 0) {
      this.setData({ errorMsg: '请至少选择一个技能需求' })
      return
    }
    if (!deadline) {
      this.setData({ errorMsg: '请选择截止日期' })
      return
    }
    // 截止时间不能早于当前时间
    var deadlineStr = deadline + 'T' + deadlineTime + ':00'
    var dl = new Date(deadlineStr).getTime()
    if (dl < Date.now()) {
      this.setData({ errorMsg: '截止时间不能早于当前时间，请重新选择' })
      return
    }
    if (maxMembers < 2) {
      this.setData({ errorMsg: '项目人数至少为 2 人' })
      return
    }

    console.log('提交的 skills:', skills, '类型:', typeof skills[0], '数量:', skills.length)

    this.setData({ submitting: true, errorMsg: '' })

    var creatorId = wx.getStorageSync('userId') || 0

    try {
      var postData = {
        title: title.trim(),
        description: description.trim(),
        boardId: boardId,
        maxMembers: maxMembers,
        skillTagIds: skills,
        deadline: deadlineStr,
        creatorId: creatorId
      }
      console.log('[create] 请求体:', JSON.stringify(postData))
      await request({ url: '/m4/projects', method: 'POST', data: postData })

      this.setData({ submitting: false })
      console.log('[create] 发布成功')
      wx.showToast({ title: '项目发布成功', icon: 'success' })
      setTimeout(function() {
        wx.redirectTo({ url: '/pages/projects/list/list' })
      }, 1000)
    } catch (err) {
      this.setData({ submitting: false })
      var msg = (err && err.message) ? err.message : '发布失败，请重试'
      this.setData({ errorMsg: msg })
    }
  }
})
