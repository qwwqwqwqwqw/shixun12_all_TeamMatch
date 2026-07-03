/*
 * T-040 互评页（联调准备版）
 * 基线约束（V2.1）：
 * 1. 四维评分：沟通协作(communication)、任务完成(task)、技术能力(skill)、责任心(responsibility)
 * 2. 1-2 分必须选负向标签；5 分必须选正向标签
 * 3. 所有维度必须评分方可提交
 * 4. 仅同项目正式成员可互评
 * 5. 项目必须处于 ended 状态且互评窗口未关闭
 * 6. 同一 project_id + evaluator_id + target_id 唯一约束
 * 7. 不能评价自己（入口已过滤）
 * 8. 统一成功响应码 code: 200
 */

const teamService = require('../../utils/team-service')

// 构建标签选中态字典：{ '标签名': true/false }
function buildTagStates(tags, selected) {
  var map = {}
  for (var i = 0; i < tags.length; i++) {
    map[tags[i]] = selected.indexOf(tags[i]) > -1
  }
  return map
}

// 构建技能点赞态字典：{ '技能名': true/false }
function buildSkillStates(skills, endorsed) {
  var map = {}
  for (var i = 0; i < skills.length; i++) {
    map[skills[i].name] = endorsed.indexOf(skills[i].name) > -1
  }
  return map
}

Page({
  data: {
    projectId: null,
    targetUserId: null,
    targetUser: null,
    dims: [
      { key: 'communication', name: '沟通协作', score: 0 },
      { key: 'task', name: '任务完成', score: 0 },
      { key: 'skill', name: '技术能力', score: 0 },
      { key: 'responsibility', name: '责任心', score: 0 }
    ],
    positiveTags: ['沟通积极', '按时交付', '技术可靠', '责任心强'],
    negativeTags: ['沟通差', '延期', '质量低', '失联'],
    selectedPositiveTags: [],
    selectedNegativeTags: [],
    comment: '',
    commentLength: 0,
    maxCommentLength: 300,
    skills: [
      { name: '微信小程序' },
      { name: 'JavaScript' },
      { name: 'UI设计' }
    ],
    endorsedSkills: [],
    loading: true,
    error: null,
    submitting: false,
    canSubmit: false,
    submitted: false,
    _tagStates: {},
    _skillStates: {}
  },

  onLoad(options) {
    const projectId = Number(options.projectId) || 0
    const targetUserId = Number(options.targetUserId) || 0
    this.setData({ projectId, targetUserId, loading: true })
  },

  onReady() {
    this.setData({
      _tagStates: buildTagStates(this.data.positiveTags.concat(this.data.negativeTags),
        this.data.selectedPositiveTags.concat(this.data.selectedNegativeTags)),
      _skillStates: buildSkillStates(this.data.skills, this.data.endorsedSkills)
    })
    this.checkEligibility()
  },

  async checkEligibility() {
    try {
      const result = await teamService.checkEvaluationEligibility(
        this.data.projectId,
        this.data.targetUserId
      )
      if (result.eligible) {
        await this.loadTargetUser()
      } else {
        wx.showModal({
          title: '无法评价',
          content: result.reason || '您当前没有评价该成员的资格',
          showCancel: false,
          success: function() { wx.navigateBack() }
        })
      }
    } catch (err) {
      this.setData({ loading: false, error: '资格校验失败' })
      wx.showToast({ title: '资格校验失败', icon: 'none' })
    }
  },

  async loadTargetUser() {
    try {
      const members = await teamService.getEvaluatableMembers(this.data.projectId)
      var member = null
      for (var i = 0; i < members.length; i++) {
        if (members[i].userId === this.data.targetUserId || members[i].id === this.data.targetUserId) {
          member = members[i]
          break
        }
      }
      if (member) {
        var targetUser = {
          id: member.userId || member.id,
          nickname: member.nickname || '未知用户',
          major: member.major || '',
          avatar: member.avatarUrl || '',
          avatarText: (member.nickname || '?').slice(0, 1)
        }
        this.setData({ targetUser, loading: false })
      } else {
        this.setData({
          targetUser: {
            id: this.data.targetUserId,
            nickname: '未知用户',
            avatarText: '?',
            major: ''
          },
          loading: false
        })
      }
    } catch (err) {
      console.log('[T-040] loadTargetUser failed', err)
      // 加载失败时使用默认值
      this.setData({
        targetUser: {
        id: this.data.targetUserId || 0,
        nickname: '加载失败',
        major: '',
        avatar: '',
        avatarText: '?'
        },
        loading: false
      })
    }
  },

  handleStarTap(e) {
    const dim = e.currentTarget.dataset.dim
    const val = Number(e.currentTarget.dataset.val)
    const dims = this.data.dims.map(item => {
      if (item.key === dim) {
        // 点击同一星星 → 归零；点击不同星星 → 设为该值
        const newScore = item.score === val ? 0 : val
        return { ...item, score: newScore }
      }
      return item
    })
    this.setData({ dims })
    this.checkCanSubmit()
  },

  handleTagTap(e) {
    const tag = e.currentTarget.dataset.tag
    const type = e.currentTarget.dataset.type
    var arr, tagStates, allTags, allSelected

    if (type === 'positive') {
      arr = this.data.selectedPositiveTags.slice()
      var idx = arr.indexOf(tag)
      if (idx > -1) arr.splice(idx, 1)
      else arr.push(tag)
      // 构建全部标签（正向+负向）的状态字典
      allTags = this.data.positiveTags.concat(this.data.negativeTags)
      allSelected = arr.concat(this.data.selectedNegativeTags)
      tagStates = buildTagStates(allTags, allSelected)
      this.setData({
        selectedPositiveTags: arr,
        _tagStates: tagStates
      })
    } else {
      arr = this.data.selectedNegativeTags.slice()
      var idx = arr.indexOf(tag)
      if (idx > -1) arr.splice(idx, 1)
      else arr.push(tag)
      allTags = this.data.positiveTags.concat(this.data.negativeTags)
      allSelected = this.data.selectedPositiveTags.concat(arr)
      tagStates = buildTagStates(allTags, allSelected)
      this.setData({
        selectedNegativeTags: arr,
        _tagStates: tagStates
      })
    }
    this.checkCanSubmit()
  },

  onCommentInput(e) {
    const comment = e.detail.value
    this.setData({ comment, commentLength: comment.length })
  },

  handleEndorseTap(e) {
    const skill = e.currentTarget.dataset.skill
    const arr = this.data.endorsedSkills.slice()
    const idx = arr.indexOf(skill)
    if (idx > -1) arr.splice(idx, 1)
    else arr.push(skill)
    const skillStates = buildSkillStates(this.data.skills, arr)
    this.setData({
      endorsedSkills: arr,
      _skillStates: skillStates
    })
  },

  checkCanSubmit() {
    const dims = this.data.dims
    const posLen = this.data.selectedPositiveTags.length
    const negLen = this.data.selectedNegativeTags.length
    const commentOk = this.data.comment.length >= 20

    let hasZero = false
    let hasLowWithNoNeg = false
    let hasLowWithNoComment = false
    let hasHighWithNoPos = false

    dims.forEach(d => {
      if (d.score === 0) hasZero = true
      if (d.score <= 2 && d.score > 0) {
        if (negLen === 0) hasLowWithNoNeg = true
        if (!commentOk) hasLowWithNoComment = true
      }
      if (d.score === 5 && posLen === 0) hasHighWithNoPos = true
    })

    this.setData({ canSubmit: !hasZero && !hasLowWithNoNeg && !hasLowWithNoComment && !hasHighWithNoPos })
  },

  async handleSubmit() {
    if (!this.data.canSubmit || this.data.submitting || this.data.submitted) return

    this.setData({ submitting: true })
    try {
      const scores = {}
      this.data.dims.forEach(d => { scores[d.key] = d.score })

      await teamService.submitEvaluation({
        projectId: this.data.projectId,
        targetUserId: this.data.targetUserId,
        scores,
        positiveTags: this.data.selectedPositiveTags,
        negativeTags: this.data.selectedNegativeTags,
        comment: this.data.comment,
        endorsedSkills: this.data.endorsedSkills
      })

      this.setData({ submitted: true, submitting: false })
      wx.showToast({ title: '评价已提交', icon: 'success' })
      setTimeout(function() { wx.navigateBack() }, 1500)
    } catch (err) {
      var code = err && err.code
      var msg = (err && err.message) || '提交失败'
      if (code === 409) msg = '您已评价过该成员'
      else if (code === 422) msg = '评价窗口已关闭或项目状态不正确'
      else if (code === 400 || code === 'M5206' || code === 'M5201') msg = (err && err.message) || '参数错误'
      wx.showToast({ title: msg, icon: 'none' })
      this.setData({ submitting: false })
    }
  }
})
