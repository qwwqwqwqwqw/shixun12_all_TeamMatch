/*
 * T-043 举报入口页
 * 基线约束（V2.1）：
 * 1. 举报对象范围 P0 仅支持 user / project
 * 2. 不支持将 evaluation 作为 target_type
 * 3. 举报提交后状态 pending，后台处理
 * 4. 统一成功响应码 code: 200
 */

const teamService = require('../../utils/team-service.js')

function buildTagStates(tags, selected) {
  var map = {}
  for (var i = 0; i < tags.length; i++) {
    map[tags[i]] = selected.indexOf(tags[i]) > -1
  }
  return map
}

Page({
  data: {
    reportType: 'user',
    searchKeyword: '',
    searchResults: [],
    selectedTarget: null,

    reasons: ['虚假信息', '恶意行为', '骚扰', '侵权内容', '其他'],
    selectedReasons: [],

    description: '',
    descriptionLength: 0,
    maxDescriptionLength: 500,

    images: [],
    storedUrls: [],
    maxImages: 5,
    uploading: false,

    submitting: false,
    canSubmit: false,

    _tagStates: {},
    _allProjects: []
  },

  onLoad() {
    // 预加载全量项目列表用于搜索
    this.preloadProjects()
  },

  onReady() {
    this.setData({
      _tagStates: buildTagStates(this.data.reasons, this.data.selectedReasons)
    })
  },

  preloadProjects() {
    var that = this
    teamService.getAllProjects().then(function(projects) {
      that.setData({ _allProjects: projects || [] })
    }).catch(function() {
      that.setData({ _allProjects: [] })
    })
  },

  // 切换举报类型
  switchType(e) {
    var type = e.currentTarget.dataset.type
    if (type !== this.data.reportType) {
      this.setData({
        reportType: type,
        searchKeyword: '',
        searchResults: [],
        selectedTarget: null
      })
      this.checkCanSubmit()
    }
  },

  // 搜索输入
  onSearchInput(e) {
    var keyword = e.detail.value
    this.setData({ searchKeyword: keyword })
    if (keyword.trim()) {
      this.doSearch(keyword)
    } else {
      this.setData({ searchResults: [] })
    }
  },

  // 搜索（项目用真实API，用户用新接口 GET /api/users?keyword=）
  doSearch(keyword) {
    var that = this
    if (this.data.reportType === 'project') {
      // 真实项目搜索（本地过滤）
      var projects = this.data._allProjects
      var results = projects.filter(function(p) {
        var name = p.name || p.title || ''
        return name.indexOf(keyword) > -1
      }).map(function(p) {
        return {
          id: p.id || p.projectId,
          name: p.name || p.title || '项目#' + (p.id || p.projectId),
          avatarText: (p.name || 'P').slice(0, 1),
          desc: (p.description || '').slice(0, 30)
        }
      })
      that.setData({ searchResults: results })
    } else {
      // 用户搜索：对接新接口 GET /api/users?keyword=xxx
      teamService.searchUsers(keyword, 1, 20).then(function(users) {
        var results = (users || []).map(function(u) {
          return {
            id: u.userId || u.id,
            name: u.nickname || u.name || '未知用户',
            avatarUrl: u.avatarUrl || u.avatar || u.avatar_url || '',
            avatarText: (u.nickname || u.name || 'U').slice(0, 1),
            desc: (u.school || u.department || u.bio || '').slice(0, 30)
          }
        })
        that.setData({ searchResults: results })
      }).catch(function() {
        that.setData({ searchResults: [] })
      })
    }
  },

  // 选中搜索结果
  selectTarget(e) {
    var item = e.currentTarget.dataset.item
    this.setData({
      selectedTarget: item,
      searchKeyword: '',
      searchResults: []
    })
    this.checkCanSubmit()
  },

  // 移除已选对象
  removeTarget() {
    this.setData({ selectedTarget: null })
    this.checkCanSubmit()
  },

  // 切换原因
  toggleReason(e) {
    var reason = e.currentTarget.dataset.reason
    var arr = this.data.selectedReasons.slice()
    var idx = arr.indexOf(reason)
    if (idx > -1) arr.splice(idx, 1)
    else arr.push(reason)
    this.setData({
      selectedReasons: arr,
      _tagStates: buildTagStates(this.data.reasons, arr)
    })
    this.checkCanSubmit()
  },

  // 详细说明输入
  onDescriptionInput(e) {
    var value = e.detail.value
    this.setData({
      description: value,
      descriptionLength: value.length
    })
    this.checkCanSubmit()
  },

  // 选择图片 → 上传 OSS → 收集 storedUrl
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
          return teamService.uploadFile(f.tempFilePath, 'report_evidence').then(function(storedUrl) {
            // 同时保存本地路径用于预览、storedUrl 用于提交
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

  // 删除图片
  deleteImage(e) {
    var index = e.currentTarget.dataset.index
    var images = this.data.images.slice()
    images.splice(index, 1)
    var storedUrls = images.map(function(img) { return img.storedUrl })
    this.setData({ images: images, storedUrls: storedUrls })
  },

  // 校验提交
  checkCanSubmit() {
    var selected = this.data.selectedTarget
    var reasons = this.data.selectedReasons
    var desc = this.data.description
    var can = selected && reasons.length > 0 && desc.trim().length > 0
    this.setData({ canSubmit: can })
  },

  // 提交举报
  async handleSubmit() {
    if (!this.data.canSubmit || this.data.submitting) return
    this.setData({ submitting: true })
    try {
      await teamService.submitReport({
        targetType: this.data.reportType,
        targetId: this.data.selectedTarget.id,
        reasons: this.data.selectedReasons,
        description: this.data.description,
        evidenceUrls: this.data.storedUrls.length > 0 ? this.data.storedUrls : undefined
      })
      wx.showToast({ title: '举报已提交', icon: 'success' })
      setTimeout(function() { wx.navigateBack() }, 1500)
    } catch (err) {
      var code = err && err.code
      var msg = '提交失败，请重试'
      if (code === 409) msg = '您已提交过相同举报'
      else if (code === 422) msg = '举报内容不符合要求'
      wx.showToast({ title: msg, icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
