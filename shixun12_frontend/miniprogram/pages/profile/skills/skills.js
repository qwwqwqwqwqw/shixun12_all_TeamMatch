// pages/profile/skills/skills.js
var api = require('../../../utils/api-service.js')
var request = api.request
var USE_MOCK = api.USE_MOCK

const MOCK_TAGS = [
  // 语言
  { id: 1, name: 'Java', category: 'language', status: 'active' },
  { id: 2, name: 'Python', category: 'language', status: 'active' },
  { id: 3, name: 'JavaScript', category: 'language', status: 'active' },
  { id: 4, name: 'TypeScript', category: 'language', status: 'active' },
  { id: 5, name: 'C++', category: 'language', status: 'active' },
  // 框架
  { id: 6, name: 'Spring Boot', category: 'framework', status: 'active' },
  { id: 7, name: 'Vue.js', category: 'framework', status: 'active' },
  { id: 8, name: 'React', category: 'framework', status: 'active' },
  { id: 9, name: 'Flutter', category: 'framework', status: 'active' },
  { id: 10, name: 'Django', category: 'framework', status: 'active' },
  // 工具
  { id: 11, name: 'Git', category: 'tool', status: 'active' },
  { id: 12, name: 'Docker', category: 'tool', status: 'active' },
  { id: 13, name: 'MySQL', category: 'tool', status: 'active' },
  { id: 14, name: 'Redis', category: 'tool', status: 'active' },
  // 软技能
  { id: 15, name: '项目管理', category: 'soft_skill', status: 'active' },
  { id: 16, name: '团队协作', category: 'soft_skill', status: 'active' },
  { id: 17, name: '技术文档编写', category: 'soft_skill', status: 'active' },
  { id: 18, name: '沟通表达', category: 'soft_skill', status: 'active' }
]

const MOCK_SELECTED_IDS = []

Page({
  data: {
    allTags: [],
    selectedIds: [],
    saving: false,
    errorMsg: '',
    loading: true
  },

  async onLoad() {
    if (USE_MOCK) {
      // Mock 模式：直接使用本地数据
      this.setData({
        allTags: MOCK_TAGS,
        loading: false
      })
    } else {
      // 真实模式：调用后端接口获取标签列表
      try {
        const res = await request({
          url: '/profile/skills/tags',
          method: 'GET'
        })
        // 只保留 active 状态，按 id 排序（兼容 WXML 中 allTags[id-1] 的按序取值方式）
        const tags = (res.data || [])
          .filter(function(t) { return t.status === 'active' })
          .sort(function(a, b) { return a.id - b.id })
        this.setData({ allTags: tags })
      } catch (err) {
        this.setData({ errorMsg: '加载标签失败' })
      } finally {
        this.setData({ loading: false })
      }
    }

    // 先从后端获取已选技能（防止重新登录后本地缓存丢失）
    var backendIds = null
    try {
      var skillsRes = await request({ url: '/profile/skills', method: 'GET' })
      var raw = skillsRes.data
      // 兼容后端返回数组 [1,2,3] 或对象 { skillTagIds: [1,2,3] }
      if (Array.isArray(raw)) backendIds = raw
      else if (raw && raw.skillTagIds) backendIds = raw.skillTagIds
      else if (raw && raw.tags) backendIds = raw.tags.map(function(t) { return t.id || t })
    } catch (e) {}
    if (!backendIds || backendIds.length === 0) {
      try {
        var profileRes = await request({ url: '/profile/detail', method: 'GET' })
        var pdata = profileRes.data || {}
        if (pdata.skillTagIds) backendIds = pdata.skillTagIds
        else if (pdata.skills && Array.isArray(pdata.skills)) backendIds = pdata.skills
      } catch (e) {}
    }
    if (backendIds && backendIds.length > 0) {
      this.setData({ selectedIds: backendIds })
      var profile = wx.getStorageSync('profile') || {}
      profile.skillTagIds = backendIds
      wx.setStorageSync('profile', profile)
      wx.setStorageSync('skillTagIds', backendIds)
      return
    }

    // 后端未返回 → 从独立缓存恢复（不依赖 profile，防止被登出清除）
    var savedIds = wx.getStorageSync('skillTagIds')
    if (savedIds && savedIds.length > 0) {
      this.setData({ selectedIds: savedIds })
      return
    }
    var profile = wx.getStorageSync('profile')
    if (profile && profile.skillTagIds) {
      this.setData({ selectedIds: profile.skillTagIds })
    }
  },

  /**
   * 切换技能选择（data-id 绑定的是 item.id，不是数组下标）
   */
  toggleSkill: function(e) {
    var skillId = Number(e.currentTarget.dataset.id)
    var selectedIds = this.data.selectedIds.slice()
    var idx = selectedIds.indexOf(skillId)
    if (idx > -1) {
      selectedIds.splice(idx, 1)
    } else {
      selectedIds.push(skillId)
    }
    this.setData({ selectedIds: selectedIds, errorMsg: '' })
  },

  /**
   * 保存技能标签
   */
  async saveSkills() {
    if (this.data.selectedIds.length === 0) {
      this.setData({ errorMsg: '请至少选择一个技能标签' })
      return
    }

    this.setData({ saving: true, errorMsg: '' })

    if (USE_MOCK) {
      // Mock 模式：本地模拟保存
      var that = this
      setTimeout(function() {
        that.setData({ saving: false })

        var profile = wx.getStorageSync('profile') || {}
        profile.skillTagIds = that.data.selectedIds
        wx.setStorageSync('profile', profile)
        wx.setStorageSync('skillTagIds', that.data.selectedIds)  // 独立 key，登出不丢失

        wx.showToast({ title: '技能保存成功', icon: 'success' })
        setTimeout(function() {
          wx.navigateBack()
        }, 800)
      }, 600)
    } else {
      // 真实模式：调用 PUT /profile/skills
      try {
        await request({
          url: '/profile/skills',
          method: 'PUT',
          data: { skillTagIds: this.data.selectedIds }
        })

        this.setData({ saving: false })

        var profile = wx.getStorageSync('profile') || {}
        profile.skillTagIds = this.data.selectedIds
        wx.setStorageSync('profile', profile)
        wx.setStorageSync('skillTagIds', this.data.selectedIds)  // 独立 key

        wx.showToast({ title: '技能保存成功', icon: 'success' })
        setTimeout(() => {
          wx.navigateBack()
        }, 800)
      } catch (err) {
        this.setData({ saving: false })
        var msg = (err && err.message) ? err.message : '保存失败，请重试'
        this.setData({ errorMsg: msg })
      }
    }
  }
})
