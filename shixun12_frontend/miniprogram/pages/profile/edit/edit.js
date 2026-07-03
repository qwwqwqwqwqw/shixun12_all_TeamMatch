// pages/profile/edit.js
const { request } = require('../../../utils/api-service.js')
const teamService = require('../../../utils/team-service.js')

Page({
  data: {
    form: {
      nickname: '',
      avatarUrl: '',
      school: '',
      major: '',
      grade: '',
      bio: ''
    },
    schoolList: ['北京交通大学','清华大学','北京大学','北京邮电大学','北京航空航天大学','北京理工大学','北京师范大学','中国人民大学','中国农业大学','北京科技大学','北京林业大学','北京工业大学','北京化工大学','中央财经大学','对外经济贸易大学','中国科学院大学','其他'],
    majorList: ['软件工程','计算机科学与技术','人工智能','数据科学与大数据技术','通信工程','电子信息工程','信息安全','网络工程','物联网工程','自动化','电气工程及其自动化','机械工程','土木工程','金融学','会计学','工商管理','其他'],
    gradeList: ['大一','大二','大三','大四','研一','研二','研三','博士','其他'],
    showSchoolOther: false,
    showMajorOther: false,
    showGradeOther: false,
    isLoading: false,
    loadingData: true,
    errorMsg: '',
    avatarStoredUrl: ''
  },

  onLoad() {
    this.loadFromBackend()
  },

  // 从后端获取最新档案，自动填充表单
  async loadFromBackend() {
    this.setData({ loadingData: true })
    try {
      var res = await request({
        url: '/profile/detail',
        method: 'GET'
      })
      var data = res.data || {}
      var rawAv = data.avatarUrl || data.avatar_url || ''
      var form = {
        nickname: data.nickname || '',
        avatarUrl: rawAv.replace('http://', 'https://'),
        school: data.school || '',
        major: data.major || '',
        grade: data.grade || '',
        bio: data.bio || ''
      }
      // 页面加载时默认显示选择器，不清除已有值
      this.setData({ showSchoolOther: false, showMajorOther: false, showGradeOther: false })

      // 微信授权数据优先覆盖（可能比后端新）
      var userInfo = wx.getStorageSync('userInfo')
      if (userInfo) {
        var wxNick = userInfo.nickName || userInfo.nickname
        if (wxNick && wxNick !== '微信用户') form.nickname = wxNick
        var wxAvatar = userInfo.avatarUrl || userInfo.avatar
        if (wxAvatar) form.avatarUrl = wxAvatar
      }

      this.setData({ form: form, loadingData: false, avatarStoredUrl: '' })
    } catch (err) {
      // 后端失败，回退本地缓存
      this.loadFromCache()
    }
  },

  // 回退方案：从本地缓存加载
  loadFromCache() {
    var profile = wx.getStorageSync('profile')
    var userInfo = wx.getStorageSync('userInfo')
    var form = {
      nickname: 'TeamMatch User',
      avatarUrl: '',
      school: '',
      major: '',
      grade: '',
      bio: ''
    }
    var avatarStoredUrl = ''
    if (profile) {
      form = Object.assign(form, profile)
      avatarStoredUrl = profile.avatarStoredUrl || ''
    }
    this.setData({ showSchoolOther: false, showMajorOther: false, showGradeOther: false })
    if (userInfo) {
      var wxNick = userInfo.nickName || userInfo.nickname
      if (wxNick && wxNick !== '微信用户') form.nickname = wxNick
      var wxAvatar = userInfo.avatarUrl || userInfo.avatar
      if (wxAvatar) form.avatarUrl = wxAvatar
    }
    this.setData({ form: form, loadingData: false, avatarStoredUrl: avatarStoredUrl })
  },

  // 选择头像 → 上传 OSS → 后端立即更新档案头像
  onChooseAvatar(e) {
    var that = this
    var tempPath = e.detail.avatarUrl
    wx.showLoading({ title: '上传头像...' })
    teamService.uploadAvatar(tempPath).then(function(uploadResult) {
      wx.hideLoading()
      var displayUrl = uploadResult.displayUrl || uploadResult.accessUrl || uploadResult.storedUrl
      var storedUrl = uploadResult.storedUrl || ''
      that.setData({
        ['form.avatarUrl']: displayUrl,
        avatarStoredUrl: storedUrl
      })

      var userInfo = wx.getStorageSync('userInfo') || {}
      userInfo.avatarUrl = displayUrl
      wx.setStorageSync('userInfo', userInfo)

      var profile = wx.getStorageSync('profile') || {}
      profile.avatarUrl = displayUrl
      profile.avatarStoredUrl = storedUrl
      wx.setStorageSync('profile', profile)

      teamService.clearProfileCache()
      wx.showToast({ title: '头像已更新', icon: 'success', duration: 1000 })
    }).catch(function(err) {
      wx.hideLoading()
      var msg = (err && err.message) || '上传失败'
      if (err && err.code === 'M6024') msg = 'OSS 未配置'
      wx.showToast({ title: msg, icon: 'none' })
    })
  },

  // 学校 picker
  onSchoolChange: function(e) {
    var val = this.data.schoolList[e.detail.value]
    this.setData({ 'form.school': val, showSchoolOther: val === '其他', errorMsg: '' })
  },

  // 专业 picker
  onMajorChange: function(e) {
    var val = this.data.majorList[e.detail.value]
    this.setData({ 'form.major': val, showMajorOther: val === '其他', errorMsg: '' })
  },

  // 手动填写学校 / 取消
  toggleSchoolCustom: function() {
    if (this.data.showSchoolOther) {
      this.setData({ showSchoolOther: false, 'form.school': '' })
    } else {
      this.setData({ showSchoolOther: true, 'form.school': '' })
    }
  },

  // 手动填写专业 / 取消
  toggleMajorCustom: function() {
    if (this.data.showMajorOther) {
      this.setData({ showMajorOther: false, 'form.major': '' })
    } else {
      this.setData({ showMajorOther: true, 'form.major': '' })
    }
  },

  // 年级 picker
  onGradeChange: function(e) {
    var val = this.data.gradeList[e.detail.value]
    this.setData({ 'form.grade': val, showGradeOther: val === '其他', errorMsg: '' })
  },

  // 手动填写年级 / 取消
  toggleGradeCustom: function() {
    if (this.data.showGradeOther) {
      this.setData({ showGradeOther: false, 'form.grade': '' })
    } else {
      this.setData({ showGradeOther: true, 'form.grade': '' })
    }
  },

  // 通用的输入框处理函数
  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value, errorMsg: '' })
  },

  // 保存档案
  async saveProfile() {
    var form = this.data.form
    if (!form.nickname.trim() || !form.school.trim()) {
      this.setData({ errorMsg: '昵称和学校不能为空' })
      return
    }
    if (form.nickname.length > 15) { this.setData({ errorMsg: '昵称不能超过15个字符' }); return }
    if (form.school.length > 15) { this.setData({ errorMsg: '学校不能超过15个字符' }); return }
    if (form.major.length > 15) { this.setData({ errorMsg: '专业不能超过15个字符' }); return }
    if (form.grade.length > 10) { this.setData({ errorMsg: '年级不能超过10个字符' }); return }
    if (form.bio.length > 200) { this.setData({ errorMsg: '个人简介不能超过200个字符' }); return }

    this.setData({ isLoading: true, errorMsg: '' })

    try {
      var payload = {
        nickname: form.nickname,
        school: form.school,
        major: form.major,
        grade: form.grade,
        bio: form.bio
      }
      if (this.data.avatarStoredUrl) payload.avatarUrl = this.data.avatarStoredUrl

      await request({
        url: '/profile/update',
        method: 'PUT',
        data: payload
      })
      // 同步更新缓存
      var userInfo = wx.getStorageSync('userInfo') || {}
      if (form.nickname) userInfo.nickName = form.nickname
      if (form.avatarUrl) userInfo.avatarUrl = form.avatarUrl
      wx.setStorageSync('userInfo', userInfo)
      // 合并旧 profile 保留 skillTagIds 不被覆盖
      var oldProfile = wx.getStorageSync('profile') || {}
      var savedSkills = wx.getStorageSync('skillTagIds') || []
      var merged = Object.assign({}, oldProfile, form)
      if (this.data.avatarStoredUrl) merged.avatarStoredUrl = this.data.avatarStoredUrl
      merged.skillTagIds = oldProfile.skillTagIds || savedSkills
      wx.setStorageSync('profile', merged)
      wx.setStorageSync('skillTagIds', merged.skillTagIds)
      // 清除档案缓存，下次重新拉取
      teamService.clearProfileCache()
      wx.showToast({ title: '档案保存成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    } catch (err) {
      const msg = (err && err.message) ? err.message : '保存失败，请稍后重试'
      this.setData({ errorMsg: msg })
    } finally {
      this.setData({ isLoading: false })
    }
  }
})
