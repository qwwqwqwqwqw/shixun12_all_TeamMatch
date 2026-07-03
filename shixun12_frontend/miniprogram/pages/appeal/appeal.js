/*
 * T-044 申诉入口页
 * 基线约束（V2.1）：
 * 1. 申诉对象：evaluation（对互评不满）、penalty（对管理员处罚不满）
 * 2. P0 不支持对 exit_vote 投票结果申诉
 * 3. 同一事件申诉有唯一性约束（后端校验）
 * 4. 申诉通过后新增 appeal_restore 流水，不篡改原流水
 * 5. 统一成功响应码 code: 200
 */

const teamService = require('../../utils/team-service.js')

Page({
  data: {
    appealType: 'evaluation',
    targets: [],
    selectedTargetId: null,

    reason: '',
    reasonLength: 0,
    maxReasonLength: 500,

    images: [],
    storedUrls: [],
    maxImages: 5,
    uploading: false,

    loading: true,
    error: null,
    submitting: false,
    canSubmit: false
  },

  onLoad() {
    // 无异步
  },

  onReady() {
    this.loadTargets()
  },

  async loadTargets() {
    this.setData({ loading: true, error: null })
    try {
      var rawTargets = await teamService.getAppealTargets(this.data.appealType)
      var mapped
      if (this.data.appealType === 'evaluation') {
        mapped = (rawTargets || []).map(function(e) {
          return {
            id: e.id || e.evaluationId,
            displayName: '被评价 #' + (e.id || e.evaluationId),
            desc: (e.comment || '无评语').slice(0, 50),
            meta: '评分 ' + (e.communicationScore || 0) + '/' + (e.taskScore || 0) + '/' + (e.skillScore || 0) + '/' + (e.responsibilityScore || 0),
            time: e.createdAt || ''
          }
        })
      } else {
        mapped = (rawTargets || []).map(function(p) {
          return {
            id: p.id,
            displayName: p.type === 'credit_deduct' ? '扣分 ' + (p.creditDeductValue || 0) + '分' : p.type,
            desc: (p.reason || '无说明').slice(0, 50),
            meta: '处罚 · ' + (p.status === 'active' ? '生效中' : p.status),
            time: p.createdAt || ''
          }
        })
      }
      this.setData({ targets: mapped, loading: false })
    } catch (err) {
      this.setData({ loading: false, error: '加载失败，请重试' })
    }
  },

  switchType(e) {
    var type = e.currentTarget.dataset.type
    if (type !== this.data.appealType) {
      this.setData({
        appealType: type,
        selectedTargetId: null,
        targets: []
      })
      this.loadTargets()
      this.checkCanSubmit()
    }
  },

  selectTarget(e) {
    var id = e.currentTarget.dataset.id
    this.setData({ selectedTargetId: id })
    this.checkCanSubmit()
  },

  onReasonInput(e) {
    var value = e.detail.value
    this.setData({
      reason: value,
      reasonLength: value.length
    })
    this.checkCanSubmit()
  },

  chooseImage() {
    var that = this
    var remaining = this.data.maxImages - this.data.images.length
    if (remaining <= 0) {
      wx.showToast({ title: '最多上传' + this.data.maxImages + '张', icon: 'none' })
      return
    }
    wx.chooseMedia({
      count: remaining,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: function(res) {
        var files = res.tempFiles || []
        if (files.length === 0) return
        that.setData({ uploading: true })
        wx.showLoading({ title: '上传中...' })
        var uploads = files.map(function(f) {
          return teamService.uploadFile(f.tempFilePath, 'appeal_evidence').then(function(storedUrl) {
            return { tempPath: f.tempFilePath, storedUrl: storedUrl }
          })
        })
        Promise.all(uploads).then(function(results) {
          wx.hideLoading()
          var images = that.data.images.concat(results)
          var storedUrls = images.map(function(img) { return img.storedUrl })
          that.setData({ images: images, storedUrls: storedUrls, uploading: false })
        }).catch(function(err) {
          wx.hideLoading()
          var msg = (err && err.message) || '上传失败'
          if (err && err.code === 'M6024') msg = 'OSS 未配置，请联系后端'
          wx.showToast({ title: msg, icon: 'none' })
          that.setData({ uploading: false })
        })
      }
    })
  },

  deleteImage(e) {
    var index = e.currentTarget.dataset.index
    var images = this.data.images.slice()
    images.splice(index, 1)
    var storedUrls = images.map(function(img) { return img.storedUrl })
    this.setData({ images: images, storedUrls: storedUrls })
  },

  checkCanSubmit() {
    var can = this.data.selectedTargetId && this.data.reason.trim().length > 0
    this.setData({ canSubmit: can })
  },

  async handleSubmit() {
    if (!this.data.canSubmit || this.data.submitting) return
    this.setData({ submitting: true })
    try {
      await teamService.submitAppeal({
        appealType: this.data.appealType,
        targetId: this.data.selectedTargetId,
        reason: this.data.reason,
        evidenceUrls: this.data.storedUrls.length > 0 ? this.data.storedUrls : undefined
      })
      wx.showToast({ title: '申诉已提交', icon: 'success' })
      setTimeout(function() { wx.navigateBack() }, 1500)
    } catch (err) {
      var code = err && err.code
      var msg = '提交失败，请重试'
      if (code === 409) msg = '该事件已有申诉在处理'
      else if (code === 422) msg = '申诉理由不符合要求'
      wx.showToast({ title: msg, icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
