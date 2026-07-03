/*
 * T-034 组队请求页（操作闭环版）
 * 基线约束（V2.1）：
 * 1. 请求状态仅用 pending/accepted/rejected/cancelled/expired
 * 2. 统一成功响应码 code: 200（不是 0）
 * 3. 操作权限由后端校验，前端只需根据返回成功切换 UI，不本地模拟权限
 * 4. 同一请求不能重复操作，后端返回 409 时前端提示"操作已失效，请刷新"
 * 5. 名词冻结：不使用 removed、remove_vote 等旧名称
 */

const teamService = require('../../utils/team-service')
const apiService = require('../../utils/api-service.js')
const USE_MOCK = apiService.USE_MOCK
const LOGIN_PAGE_URL = '/pages/index/index'
const TOKEN_KEYS = ['token', 'accessToken', 'userToken', 'sessionToken']

const statusMetaMap = {
  pending: {
    label: '待处理',
    class: 'pending'
  },
  accepted: {
    label: '已通过',
    class: 'accepted'
  },
  rejected: {
    label: '已拒绝',
    class: 'rejected'
  },
  cancelled: {
    label: '已取消',
    class: 'cancelled'
  },
  expired: {
    label: '已过期',
    class: 'expired'
  }
}

// ========== Mock 请求数据 ==========
function getMockRequests(tab) {
  // 收到/发出两套数据
  var allReceived = [
    { requestId: 'REQ-001', projectName: '智能课表小程序', projectId: 1,
      requestType: 'invite', status: 'pending', createdAt: '05-10 14:20',
      fromUserId: 1, toUserId: 202, message: '你好，我们正在组建一个小程序开发团队，诚邀你加入！',
      counterpart: { nickname: '江一舟', avatarUrl: '', major: '计算机科学 21级', creditScore: 98 } },
    { requestId: 'REQ-002', projectName: '校园跑腿平台', projectId: 2,
      requestType: 'invite', status: 'pending', createdAt: '05-09 09:30',
      fromUserId: 3, toUserId: 202, message: '项目需要后端开发，看到你技能匹配',
      counterpart: { nickname: '周予安', avatarUrl: '', major: '计算机科学 21级', creditScore: 91 } },
    { requestId: 'REQ-003', projectName: '二手交易市场', projectId: 3,
      requestType: 'apply', status: 'accepted', createdAt: '05-08 16:00',
      fromUserId: 4, toUserId: 202, message: '我对机器学习方向很感兴趣，希望能加入',
      counterpart: { nickname: '陈知远', avatarUrl: '', major: '软件工程 22级', creditScore: 85 } },
    { requestId: 'REQ-004', projectName: '图书漂流平台', projectId: 4,
      requestType: 'invite', status: 'rejected', createdAt: '05-06 10:00',
      fromUserId: 5, toUserId: 202,
      counterpart: { nickname: '许之恒', avatarUrl: '', major: '软件工程 21级', creditScore: 72 } }
  ]
  var allSent = [
    { requestId: 'REQ-S01', projectName: '智能课表小程序', projectId: 1,
      requestType: 'apply', status: 'pending', createdAt: '05-11 08:00',
      fromUserId: 202, toUserId: 2, message: '希望能加入项目，贡献前端经验',
      counterpart: { nickname: '林乔', avatarUrl: '', major: '软件工程 22级', creditScore: 96 } },
    { requestId: 'REQ-S02', projectName: '机器学习实践', projectId: 5,
      requestType: 'apply', status: 'rejected', createdAt: '05-07 12:18',
      fromUserId: 202, toUserId: 1,
      counterpart: { nickname: '江一舟', avatarUrl: '', major: '计算机科学 21级', creditScore: 98 } },
    { requestId: 'REQ-S03', projectName: '校园跑腿平台', projectId: 2,
      requestType: 'apply', status: 'expired', createdAt: '05-04 08:15',
      fromUserId: 202, toUserId: 3,
      counterpart: { nickname: '周予安', avatarUrl: '', major: '计算机科学 21级', creditScore: 91 } }
  ]

  var raw = tab === 'received' ? allReceived : allSent

  return raw.map(function(item) {
    var meta = statusMetaMap[item.status] || statusMetaMap.pending
    var isReceived = tab === 'received'
    var cp = item.counterpart
    return {
      requestId: item.requestId,
      projectName: item.projectName,
      counterpartId: isReceived ? item.fromUserId : item.toUserId,
      counterpartNickname: cp.nickname,
      counterpartLabel: isReceived ? '来自' : '发送给',
      avatarUrl: cp.avatarUrl,
      avatarText: cp.nickname.slice(0, 1),
      major: cp.major,
      creditScore: cp.creditScore,
      message: item.message || '',
      requestType: item.requestType,
      requestTypeLabel: item.requestType === 'invite' ? '邀请' : '申请',
      status: item.status,
      createdAt: item.createdAt,
      isReceived: isReceived,
      hint: item.status !== 'pending' ? (item.requestType === 'invite' ? (item.status === 'accepted' ? '已接受' : '已拒绝') : (item.status === 'accepted' ? '已通过' : (item.status === 'rejected' ? '已拒绝' : '已过期'))) : '',
      label: meta.label,
      class: meta.class
    }
  })
}

/**
 * 根据请求类型和状态生成辅助提示文字
 */
function getResultHint(requestType, status) {
  const hints = {
    invite: {
      accepted: '邀请已接受，成员状态已同步',
      rejected: '邀请已被拒绝',
      cancelled: '邀请已撤回',
      expired: '邀请已过期'
    },
    apply: {
      accepted: '申请已接受，成员状态已同步',
      rejected: '申请已被拒绝',
      cancelled: '申请已撤回',
      expired: '申请已过期'
    }
  }
  return (hints[requestType] && hints[requestType][status]) || ''
}

Page({
  data: {
    isAuthenticated: false,
    isRedirectingToLogin: false,
    activeTab: 'received',
    requests: [],
    loading: false,
    error: '',
    operatingId: null
  },

  onLoad() {
    if (!this.ensureLogin()) return
  },

  onReady() {
    this.loadRequests()
  },



  switchTab(event) {
    if (!this.ensureLogin()) return

    const { tab } = event.currentTarget.dataset
    if (!tab || tab === this.data.activeTab) return

    this.setData({ activeTab: tab }, () => {
      this.loadRequests()
    })
  },

  loadRequests() {
    const { activeTab } = this.data

    this.setData({ loading: true, error: '' })

    // Mock 模式：直接返回本地数据
    if (USE_MOCK) {
      var that = this
      var mockList = getMockRequests(activeTab)
      setTimeout(function() {
        that.setData({ requests: mockList, loading: false })
        console.log('[T-034] loadRequests Mock', { tab: activeTab, count: mockList.length })
      }, 400)
      return
    }

    teamService.getRequests(activeTab).then(async res => {
      var rawData = (res.data && res.data.list) || []
      if (rawData.length === 0 && res.code !== 200 && res.code !== '00000') {
        this.setData({
          loading: false,
          error: res.message || '加载失败，请稍后重试'
        })
        return
      }

      var cancelledIds = wx.getStorageSync('cancelled_requests') || []

      // 收集所有需要查档案的 userId（去重）
      var needProfile = []
      rawData.forEach(function(item) {
        var uid = activeTab === 'received' ? item.fromUserId : item.toUserId
        if (uid && needProfile.indexOf(uid) === -1) needProfile.push(uid)
      })

      // 批量拉取用户档案
      var profileMap = {}
      var profilePromises = needProfile.map(function(uid) {
        return teamService.getUserProfile(uid).then(function(profile) {
          profileMap[uid] = {
            nickname: (profile && profile.nickname) || '未知用户',
            avatarUrl: (profile && (profile.avatarUrl || profile.avatar)) || '',
            major: (profile && profile.major) || '',
            creditScore: (profile && (profile.creditScore || profile.trustScore)) || 0
          }
        }).catch(function() {
          profileMap[uid] = { nickname: '未知用户', avatarUrl: '', major: '' }
        })
      })
      await Promise.all(profilePromises)

      var list = rawData.map(function(item) {
        if (cancelledIds.indexOf(item.id) !== -1) item.status = 'cancelled'
        var meta = statusMetaMap[item.status] || statusMetaMap.pending
        var isReceived = activeTab === 'received'
        var counterpartId = isReceived ? item.fromUserId : item.toUserId
        var profile = profileMap[counterpartId] || { nickname: '未知用户', avatarUrl: '', major: '', creditScore: 0 }
        var counterpartLabel = isReceived ? '来自' : '发送给'
        var requestTypeLabel = item.requestType === 'invite' ? '邀请' : '申请'
        var hint = item.status !== 'pending' ? getResultHint(item.requestType, item.status) : ''

        return {
          requestId: item.id,
          projectName: item.projectName || ('项目#' + item.projectId),
          counterpartId: counterpartId,
          counterpartNickname: profile.nickname,
          counterpartLabel: counterpartLabel,
          avatarUrl: profile.avatarUrl,
          avatarText: profile.nickname ? profile.nickname.slice(0, 1) : '?',
          major: profile.major,
          creditScore: profile.creditScore,
          message: item.message || '',
          requestType: item.requestType || '',
          requestTypeLabel: requestTypeLabel,
          status: item.status || '',
          createdAt: item.createdAt || '',
          isReceived: isReceived,
          hint: hint,
          label: meta.label,
          class: meta.class
        }
      })

      this.setData({
        loading: false,
        requests: list
      })
    }).catch(err => {
      this.setData({
        loading: false,
        error: err.message || '网络异常，请稍后重试'
      })
    })
  },

  handleAction(e) {
    if (!this.ensureLogin()) return

    const { id, action } = e.currentTarget.dataset

    // 防重复：如果该请求正在操作中，直接返回
    if (this.data.operatingId === id) {
      wx.showToast({ title: '操作处理中，请稍后', icon: 'none' })
      return
    }

    this.setData({ operatingId: id })

    this.doAction(id, action).finally(() => {
      if (this.data.operatingId === id) {
        this.setData({ operatingId: null })
      }
    })
  },

  async doAction(requestId, action) {
    // Mock 模式：直接模拟操作
    if (USE_MOCK) {
      var status = action === 'accept' ? 'accepted' : (action === 'reject' ? 'rejected' : 'cancelled')
      this.updateRequestStatus(requestId, status)
      if (action === 'cancel') {
        var cancelledIds = wx.getStorageSync('cancelled_requests') || []
        if (cancelledIds.indexOf(requestId) === -1) cancelledIds.push(requestId)
        wx.setStorageSync('cancelled_requests', cancelledIds)
      }
      wx.showToast({ title: action === 'accept' ? '已接受' : (action === 'reject' ? '已拒绝' : '已取消'), icon: 'success' })
      return
    }

    try {
      let result
      if (action === 'accept') {
        result = await teamService.acceptRequest(requestId)
        this.updateRequestStatus(requestId, 'accepted')
        wx.showToast({ title: '已接受', icon: 'success' })
      } else if (action === 'reject') {
        result = await teamService.rejectRequest(requestId)
        this.updateRequestStatus(requestId, 'rejected')
        wx.showToast({ title: '已拒绝', icon: 'success' })
      } else if (action === 'cancel') {
        result = await teamService.cancelRequest(requestId)
        this.updateRequestStatus(requestId, 'cancelled')
        // 持久化已取消的请求 ID
        var cancelledIds = wx.getStorageSync('cancelled_requests') || []
        if (cancelledIds.indexOf(requestId) === -1) {
          cancelledIds.push(requestId)
          wx.setStorageSync('cancelled_requests', cancelledIds)
        }
        wx.showToast({ title: '已取消', icon: 'success' })
      }
      console.log('[T-034] doAction success', { requestId, action, result })
    } catch (err) {
      var code = err && err.code
      // M4005：取消时后端已执行但返回非成功码，视为已生效
      if (code === 'M4005' && action === 'cancel') {
        this.updateRequestStatus(requestId, 'cancelled')
        var cancelledIds = wx.getStorageSync('cancelled_requests') || []
        if (cancelledIds.indexOf(requestId) === -1) {
          cancelledIds.push(requestId)
          wx.setStorageSync('cancelled_requests', cancelledIds)
        }
        return
      }
      var msg = (err && err.message) || '操作失败'
      // 映射新后端错误码
      if (code === 'M4007') msg = '项目已满员'
      else if (code === 'M4003') msg = '项目不在招募中'
      else if (code === 'M4006') msg = '已有待处理的请求'
      else if (err && err.message === 'request:fail') msg = '网络异常，请重试'
      console.log('[T-034] doAction failed', { requestId, action, err })
      wx.showToast({ title: msg, icon: 'none' })
    }
  },

  // 更新单个请求的状态
  updateRequestStatus(requestId, newStatus) {
    const requests = this.data.requests.map(req => {
      if (req.requestId === requestId) {
        const meta = statusMetaMap[newStatus] || statusMetaMap.pending
        return { ...req, status: newStatus, ...meta, hint: getResultHint(req.requestType, newStatus) }
      }
      return req
    })
    this.setData({ requests })
  },

  // 从列表中移除请求（取消用）
  removeRequestFromList(requestId) {
    const requests = this.data.requests.filter(req => req.requestId !== requestId)
    this.setData({ requests })
  },

  ensureLogin() {
    const token = this.getLoginToken()
    if (token) {
      if (!this.data.isAuthenticated || this.data.isRedirectingToLogin) {
        this.setData({
          isAuthenticated: true,
          isRedirectingToLogin: false
        })
      }
      return true
    }

    this.redirectToLogin()
    return false
  },

  getLoginToken() {
    for (let index = 0; index < TOKEN_KEYS.length; index += 1) {
      const token = wx.getStorageSync(TOKEN_KEYS[index])
      if (token) return token
    }
    return ''
  },

  redirectToLogin() {
    if (this.data.isRedirectingToLogin) return

    this.setData({
      isAuthenticated: false,
      isRedirectingToLogin: true
    })

    wx.showToast({ title: '请先登录后使用', icon: 'none' })
    setTimeout(() => {
      wx.switchTab({ url: LOGIN_PAGE_URL })
    }, 300)
  }
})
