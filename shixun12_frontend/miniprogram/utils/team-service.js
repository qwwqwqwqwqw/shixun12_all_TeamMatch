/*
 * TeamMatch 组队/互评/治理 Service 层
 * M2 联调配对清单：docs/M2后端接口对照.md
 * 统一响应格式：{ code: 200, message: "success", data: ... }
 */

var USE_MOCK = false
//var BASE_URL = 'http://localhost:8080/api'
var BASE_URL = 'https://api.teammatch.top/api'

// ========== 用户档案内存缓存（5分钟有效）==========
var profileCache = {}

function getCachedProfile(userId) {
  var entry = profileCache[userId]
  if (entry && Date.now() - entry.time < 5 * 60 * 1000) {
    // 补齐归一化（兼容修复前的旧缓存）
    var d = entry.data
    d.avatarUrl = d.avatarUrl || d.avatar || d.avatar_url || ''
    d.nickname = d.nickname || d.nickName || ''
    return d
  }
  return null
}

function setCachedProfile(userId, data) {
  profileCache[userId] = { data: data, time: Date.now() }
}

// ========== 辅助函数 ==========

var TOKEN_KEYS = ['token', 'accessToken', 'userToken', 'sessionToken']

function getToken() {
  for (var i = 0; i < TOKEN_KEYS.length; i++) {
    var t = wx.getStorageSync(TOKEN_KEYS[i])
    if (t) return t
  }
  return ''
}

function getUserId() {
  var app = getApp()
  // 优先从 localStorage 读取真实登录 userId，回退到 globalData
  var id = wx.getStorageSync('userId')
  if (id) return id
  return (app && app.globalData && app.globalData.userId) || 0
}

function authHeader() {
  return {
    'Authorization': 'Bearer ' + getToken(),
    'Content-Type': 'application/json'
  }
}

/**
 * 通用成功码判断（兼容后端 "00000" 或 200）
 */
function isSuccessCode(code) {
  return code === 200 || code === '00000' || code === 0
}

// 检测是否为封禁/认证失效响应，是则清登录态并跳转
function handleBanResponse(resData) {
  var code = resData && resData.code
  if (code === 'M3017' || code === 'M3000') {
    wx.removeStorageSync('token')
    wx.removeStorageSync('userId')
    wx.removeStorageSync('userInfo')
    wx.removeStorageSync('emailVerified')
    wx.showToast({ title: '账号异常，请重新登录', icon: 'none', duration: 2000 })
    setTimeout(function() { wx.reLaunch({ url: '/pages/login/login' }) }, 1500)
  }
}

// ========== 方法 0：发起组队请求 ==========

/**
 * 用户申请加入项目
 * POST /api/m4/team-requests/apply
 */
function sendApplyRequest(projectId, fromUserId, toUserId, message) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/team-requests/apply',
      method: 'POST',
      header: authHeader(),
      data: { projectId: projectId, fromUserId: fromUserId, toUserId: toUserId, message: message || '' },
      timeout: 15000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 队长邀请用户
 * POST /api/m4/team-requests/invite
 */
function sendInviteRequest(projectId, targetUserId, message) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/team-requests/invite',
      method: 'POST',
      header: authHeader(),
      data: { projectId: projectId, fromUserId: getUserId(), toUserId: targetUserId, message: message || '' },
      timeout: 15000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

// ========== 方法 1-4：组队请求 ==========

/**
 * 获取组队请求列表
 * @param {'received'|'sent'} direction
 */
function getRequests(direction) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/team-requests?userId=' + getUserId() + '&direction=' + direction,
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        console.log('[getRequests] 原始响应:', JSON.stringify(res.data))
        if (res.data) {
          var code = res.data.code
          // 兼容 code: 200（数字）或 code: "00000"（字符串）或 code: 0
          var ok = (code === 200 || code === '00000' || code === 0)
          if (ok) {
            var body = res.data
            // 统一归一化，兼容后端 data 为数组格式
            if (Array.isArray(body.data)) {
              resolve({ code: 200, data: { list: body.data } })
            } else {
              resolve({ code: 200, data: body.data })
            }
            return
          }
        }
        reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 接受请求
 */
function acceptRequest(requestId) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/team-requests/' + requestId + '/accept?operatorId=' + getUserId(),
      method: 'POST',
      header: authHeader(),
      timeout: 15000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 拒绝请求
 */
function rejectRequest(requestId) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/team-requests/' + requestId + '/reject?operatorId=' + getUserId(),
      method: 'POST',
      header: authHeader(),
      timeout: 15000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 取消请求
 */
function cancelRequest(requestId) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/team-requests/' + requestId + '/cancel?operatorId=' + getUserId(),
      method: 'POST',
      header: authHeader(),
      timeout: 15000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

// ========== 方法 5：项目成员 ==========

/**
 * 获取项目成员列表
 */
function getMembers(projectId) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/projects/' + projectId + '/members',
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        handleBanResponse(res.data)
        if (res.data && isSuccessCode(res.data.code)) {
          // 兼容 data 为数组格式
          if (Array.isArray(res.data.data)) {
            resolve(res.data.data.list || res.data.data)
          } else {
            resolve(res.data.data.list)
          }
        } else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 获取项目详情
 * 完整路径：GET /api/m4/projects/{projectId}
 */
function getProjectDetail(projectId) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/projects/' + projectId,
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        handleBanResponse(res.data)
        if (res.data && isSuccessCode(res.data.code)) {
          resolve(res.data.data || res.data)
        } else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 获取用户详情
 * 完整路径：GET /api/profile/detail/{userId}
 */
function getUserProfile(userId) {
  return new Promise(function(resolve, reject) {
    // 内存缓存命中 → 直接返回
    var cached = getCachedProfile(userId)
    if (cached) {
      resolve(cached)
      return
    }
    wx.request({
      url: BASE_URL + '/profile/detail/' + userId,
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        handleBanResponse(res.data)
        if (res.data && isSuccessCode(res.data.code)) {
          var data = res.data.data || res.data
          // 归一化：后端可能返回下划线或驼峰
          data.avatarUrl = data.avatarUrl || data.avatar || data.avatar_url || ''
          data.nickname = data.nickname || data.nickName || ''
          setCachedProfile(userId, data)
          resolve(data)
        } else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/** 清除用户档案缓存（例如用户修改资料后调用） */
function clearProfileCache(userId) {
  if (userId) delete profileCache[userId]
  else profileCache = {}
}

// ========== 方法：项目状态流转（队长专属） ==========

/**
 * 开始项目：recruiting → in_progress
 * POST /api/m4/projects/{id}/start?operatorId=队长ID
 */
function startProject(projectId, operatorId) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/projects/' + projectId + '/start?operatorId=' + operatorId,
      method: 'POST',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 结束项目：in_progress → ended
 * POST /api/m4/projects/{id}/end?operatorId=队长ID
 */
function endProject(projectId, operatorId) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/projects/' + projectId + '/end?operatorId=' + operatorId,
      method: 'POST',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

// ========== 方法 6：主动退出 ==========

/**
 * 成员主动退出项目（后端不接收 reason）
 */
function selfExit(projectId) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/projects/' + projectId + '/exit/self?userId=' + getUserId(),
      method: 'POST',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

// ========== 方法 7-9：退出投票 ==========

/**
 * 获取退出投票详情（新增 projectId 参数）
 */
function getExitVoteDetail(projectId, voteId) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/projects/' + projectId + '/exit/votes/' + voteId,
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 提交投票
 */
function submitExitVote(projectId, voteId, choice) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/projects/' + projectId + '/exit/votes/' + voteId + '/submit',
      method: 'POST',
      header: authHeader(),
      data: { voterId: getUserId(), choice: choice },
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 队长发起退出投票
 * POST /api/m4/projects/{projectId}/exit/votes
 * Body: { initiatorId, targetUserId, reason, penaltyLevel: "negotiated" | "malicious" }
 * 校验：仅队长、项目 in_progress、目标为活跃成员、不对队长发起、无进行中投票
 */
function createExitVote(projectId, targetUserId, reason, penaltyLevel) {
  var initiatorId = getUserId()
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/projects/' + projectId + '/exit/votes',
      method: 'POST',
      header: authHeader(),
      data: {
        initiatorId: initiatorId,
        targetUserId: targetUserId,
        reason: reason || '',
        penaltyLevel: penaltyLevel || 'negotiated'
      },
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 关闭投票
 */
function closeExitVote(projectId, voteId) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/projects/' + projectId + '/exit/votes/' + voteId + '/close?operatorId=' + getUserId(),
      method: 'POST',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 撤回投票（队长取消已发起的投票，不作废不扣分）
 */
function cancelExitVote(projectId, voteId) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/projects/' + projectId + '/exit/votes/' + voteId + '/cancel?operatorId=' + getUserId(),
      method: 'POST',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}



// ========== 方法 10-11：互评 ==========

/**
 * 校验评价资格
 */
function checkEvaluationEligibility(projectId, targetUserId) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m5/projects/' + projectId + '/members/' + targetUserId + '/evaluation-eligibility',
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) {
          // 后端返回 Boolean（true/false），包装为 { eligible, reason } 对象给页面使用
          var eligible = res.data.data === true
          resolve({ eligible: eligible, reason: eligible ? '' : '您当前没有评价该成员的资格' })
        }
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 获取可评价成员列表
 * 完整路径：GET /api/m5/projects/{projectId}/evaluatable-members
 */
function getEvaluatableMembers(projectId) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m5/projects/' + projectId + '/evaluatable-members',
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) {
          if (Array.isArray(res.data.data)) {
            resolve(res.data.data.list || res.data.data)
          } else {
            resolve(res.data.data.list)
          }
        } else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 提交评价（评分字段扁平化）
 * @param {Object} params - { projectId, targetId, communicationScore, taskScore, skillScore, responsibilityScore, comment, positiveTags, negativeTags }
 */
function submitEvaluation(params) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m5/evaluations',
      method: 'POST',
      header: authHeader(),
      data: {
        projectId: params.projectId,
        targetId: params.targetUserId || params.targetId,
        communicationScore: params.scores ? params.scores.communication : params.communicationScore,
        taskScore: params.scores ? params.scores.task : params.taskScore,
        skillScore: params.scores ? params.scores.skill : params.skillScore,
        responsibilityScore: params.scores ? params.scores.responsibility : params.responsibilityScore,
        comment: params.comment || '',
        positiveTags: params.positiveTags || [],
        negativeTags: params.negativeTags || []
      },
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

// ========== 方法 12-13：信誉分 ==========

/**
 * 获取当前信誉分
 * @returns {Promise<number>}
 */
function getCurrentCreditScore() {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m5/credit/score',
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data.data.creditScore)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 获取信誉分变化历史
 * @returns {Promise<Array>}
 */
function getCreditHistory() {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m5/credit/changes?page=1&pageSize=20',
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) {
          if (Array.isArray(res.data.data)) {
            resolve(res.data.data.list || res.data.data)
          } else {
            resolve(res.data.data.list)
          }
        } else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

// ========== 方法 14-15：举报 ==========

/**
 * 上传图片到 OSS（举报/申诉证据、头像）
 * @param {string} filePath - 本地临时路径
 * @param {string} category - 'report_evidence' | 'appeal_evidence' | 'avatar'
 * @returns {Promise<string>} - storedUrl（用于提交业务接口）
 */
function uploadFile(filePath, category) {
  return new Promise(function(resolve, reject) {
    var token = getToken()
    wx.uploadFile({
      url: BASE_URL + '/files/upload?category=' + encodeURIComponent(category),
      filePath: filePath,
      name: 'file',
      header: { 'Authorization': 'Bearer ' + token },
      timeout: 30000,
      success: function(res) {
        try {
          var body = JSON.parse(res.data)
          if (body && isSuccessCode(body.code)) {
            var uploadResult = normalizeUploadResult(body.data)
            resolve(uploadResult.storedUrl || uploadResult.accessUrl)
          } else {
            reject(body)
          }
        } catch (e) {
          reject({ message: '上传响应解析失败' })
        }
      },
      fail: function(err) { reject(err) }
    })
  })
}

function normalizeUploadResult(data) {
  data = data || {}
  var accessUrl = data.accessUrl || data.storedUrl || ''
  if (accessUrl && accessUrl.indexOf('http://') === 0) {
    accessUrl = accessUrl.replace('http://', 'https://')
  }
  var storedUrl = data.storedUrl || ''
  return {
    objectKey: data.objectKey || '',
    storedUrl: storedUrl,
    accessUrl: accessUrl,
    displayUrl: accessUrl || storedUrl
  }
}

/**
 * 上传头像并立即更新当前用户档案
 * POST /api/profile/avatar
 * @returns {Promise<{objectKey:string, storedUrl:string, accessUrl:string, displayUrl:string}>}
 */
function uploadAvatar(filePath) {
  return new Promise(function(resolve, reject) {
    var token = getToken()
    wx.uploadFile({
      url: BASE_URL + '/profile/avatar',
      filePath: filePath,
      name: 'file',
      header: { 'Authorization': 'Bearer ' + token },
      timeout: 30000,
      success: function(res) {
        try {
          var body = JSON.parse(res.data)
          if (body && isSuccessCode(body.code)) {
            resolve(normalizeUploadResult(body.data))
          } else {
            reject(body)
          }
        } catch (e) {
          reject({ message: '上传响应解析失败' })
        }
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 提交举报（标签+描述拼接为 reason，支持 evidenceUrls）
 */
function submitReport(params) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/reports',
      method: 'POST',
      header: authHeader(),
      data: {
        targetType: params.targetType,
        targetId: params.targetId,
        reason: (function() {
          var p = params
          var parts = []
          var tags = (p.reasons || []).join('、')
          if (tags) parts.push('[' + tags + ']')
          if (p.description && p.description.trim()) parts.push(p.description.trim())
          return parts.join(' ') || p.reason || ''
        })(),
        evidenceUrls: params.evidenceUrls || undefined
      },
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 获取我的举报列表
 */
function getMyReports() {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/reports/my',
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) {
          if (Array.isArray(res.data.data)) {
            resolve(res.data.data.list || res.data.data)
          } else {
            resolve(res.data.data.list)
          }
        } else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

// ========== 方法 16-18：申诉 ==========

/**
 * 获取可申诉对象列表（对接新接口）
 * @param {'evaluation'|'penalty'} appealType
 */
function getAppealTargets(appealType) {
  if (appealType === 'evaluation') {
    return getAppealableEvaluations()
  }
  return getAppealablePenalties()
}

/**
 * 获取可申诉的评价列表
 * GET /api/appeals/appealable/evaluations → List<AppealableEvaluationVO>
 * AppealableEvaluationVO: { evaluationId, projectId, projectTitle, communicationScore, taskScore, skillScore, responsibilityScore, averageScore, comment, createdAt }
 */
function getAppealableEvaluations() {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/appeals/appealable/evaluations',
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) {
          var data = res.data.data
          // 统一转为 appeal.js 期望的格式（兼容新旧字段名）
          var list = Array.isArray(data) ? data : (data && data.list ? data.list : [])
          // AppealableEvaluationVO 字段映射到旧兼容格式
          resolve(list.map(function(e) {
            return {
              id: e.evaluationId || e.id,
              evaluationId: e.evaluationId || e.id,
              comment: e.comment || '',
              communicationScore: e.communicationScore || 0,
              taskScore: e.taskScore || 0,
              skillScore: e.skillScore || 0,
              responsibilityScore: e.responsibilityScore || 0,
              createdAt: e.createdAt || '',
              projectTitle: e.projectTitle || ''
            }
          }))
        } else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 获取可申诉的处罚列表
 * GET /api/appeals/appealable/penalties → List<AppealablePenaltyVO>
 * AppealablePenaltyVO: { penaltyId, type, creditDeductValue, reason, status, createdAt }
 */
function getAppealablePenalties() {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/appeals/appealable/penalties',
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) {
          var data = res.data.data
          var list = Array.isArray(data) ? data : (data && data.list ? data.list : [])
          // AppealablePenaltyVO 字段映射到旧兼容格式
          resolve(list.map(function(p) {
            return {
              id: p.penaltyId || p.id,
              type: p.type || '',
              creditDeductValue: p.creditDeductValue || 0,
              reason: p.reason || '',
              status: p.status || '',
              createdAt: p.createdAt || ''
            }
          }))
        } else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 提交申诉
 */
function submitAppeal(params) {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/appeals',
      method: 'POST',
      header: authHeader(),
      data: {
        targetType: params.appealType,
        targetId: params.targetId,
        reason: params.reason || '',
        evidenceUrls: params.evidenceUrls || undefined
      },
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) resolve(res.data)
        else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

/**
 * 获取我的申诉列表
 */
function getMyAppeals() {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/appeals/my',
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) {
          if (Array.isArray(res.data.data)) {
            resolve(res.data.data.list || res.data.data)
          } else {
            resolve(res.data.data.list)
          }
        } else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

// ========== 方法 19-20：我的项目 / 待处理投票 ==========

/**
 * 获取当前用户已加入的项目列表
 * 策略：team-requests(已接受) + 全局项目列表(按成员过滤) 双路聚合
 */
function getMyJoinedProjects() {
  var userId = getUserId()
  return new Promise(function(resolve, reject) {
    var projectIdSet = {}
    var allDone = 0

    // 收集 projectId 并去重
    function addProjectId(pid) {
      if (pid && !projectIdSet[pid]) projectIdSet[pid] = true
    }

    // 检查是否所有异步源都完成
    function checkDone() {
      allDone++
      if (allDone < 2) return
      var pids = Object.keys(projectIdSet)
      if (pids.length === 0) { resolve([]); return }
      fetchDetails(pids)
    }

    // 用 projectIds 获取详情
    function fetchDetails(pids) {
      var promises = pids.map(function(pid) {
        return new Promise(function(dResolve) {
          wx.request({
            url: BASE_URL + '/m4/projects/' + pid,
            method: 'GET', header: authHeader(), timeout: 10000,
            success: function(res) {
              if (res.data && isSuccessCode(res.data.code)) {
                var d = res.data.data || res.data
                d.projectId = parseInt(pid)
                dResolve(d)
              } else dResolve(null)
            },
            fail: function() { dResolve(null) }
          })
        })
      })
      Promise.all(promises).then(function(details) {
        resolve(details.filter(function(d) { return d !== null }))
      }).catch(function() { resolve([]) })
    }

    // --- 来源 1：已接受的组队请求 ---
    var reqCount = 0, allReqs = []
    function onReqDone() {
      reqCount++
      if (reqCount < 2) return
      for (var i = 0; i < allReqs.length; i++) {
        if (allReqs[i].status === 'accepted') addProjectId(allReqs[i].projectId)
      }
      checkDone()
    }
    wx.request({
      url: BASE_URL + '/m4/team-requests?userId=' + userId + '&direction=received',
      method: 'GET', header: authHeader(), timeout: 10000,
      success: function(res) {
        var d = (res.data && isSuccessCode(res.data.code)) ? res.data.data : null
        if (Array.isArray(d)) allReqs = allReqs.concat(d)
        onReqDone()
      },
      fail: function() { onReqDone() }
    })
    wx.request({
      url: BASE_URL + '/m4/team-requests?userId=' + userId + '&direction=sent',
      method: 'GET', header: authHeader(), timeout: 10000,
      success: function(res) {
        var d = (res.data && isSuccessCode(res.data.code)) ? res.data.data : null
        if (Array.isArray(d)) allReqs = allReqs.concat(d)
        onReqDone()
      },
      fail: function() { onReqDone() }
    })

    // --- 来源 2：全局项目列表 → 按成员过滤 ---
    wx.request({
      url: BASE_URL + '/m4/projects?pageNum=1&pageSize=100',
      method: 'GET', header: authHeader(), timeout: 10000,
      success: function(res) {
        if (!res.data || !isSuccessCode(res.data.code)) { checkDone(); return }
        var projects = res.data.data
        var list = Array.isArray(projects) ? projects : (projects && projects.records ? projects.records : [])
        if (list.length === 0) { checkDone(); return }
        // 并行检查每个项目：用户是否为其成员
        var memberChecks = list.map(function(p) {
          var pid = p.id || p.projectId
          if (!pid) return Promise.resolve(false)
          return new Promise(function(mResolve) {
            wx.request({
              url: BASE_URL + '/m4/projects/' + pid + '/members',
              method: 'GET', header: authHeader(), timeout: 8000,
              success: function(mRes) {
                if (!mRes.data || !isSuccessCode(mRes.data.code)) { mResolve(false); return }
                var members = mRes.data.data
                var memberList = Array.isArray(members) ? members : (members && members.list ? members.list : [])
                for (var k = 0; k < memberList.length; k++) {
                  var muid = memberList[k].userId || memberList[k].id
                  if (muid == userId) { mResolve(true); return }
                }
                mResolve(false)
              },
              fail: function() { mResolve(false) }
            })
          })
        })
        Promise.all(memberChecks).then(function(results) {
          for (var j = 0; j < results.length; j++) {
            if (results[j]) addProjectId(list[j].id || list[j].projectId)
          }
          checkDone()
        })
      },
      fail: function() { checkDone() }
    })
  })
}

/**
 * 获取项目所有退出投票列表（用于投票管理页）
 * GET /api/m4/projects/{projectId}/exit/votes → List<ExitVoteVO>
 */
function getProjectExitVotes(projectId) {
  return new Promise(function(resolve) {
    wx.request({
      url: BASE_URL + '/m4/projects/' + projectId + '/exit/votes',
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) {
          var data = res.data.data
          resolve(Array.isArray(data) ? data : [])
        } else {
          resolve([])  // 接口暂未实现，返回空数组
        }
      },
      fail: function() { resolve([]) }
    })
  })
}

/**
 * 获取项目的待处理退出投票列表
 */
function getProjectPendingVotes(projectId) {
  return getProjectExitVotes(projectId).then(function(votes) {
    return votes.filter(function(v) { return v.status === 'voting' })
  })
}

// ========== 用户搜索（新接口） ==========

/**
 * 搜索用户（按昵称模糊匹配）
 * GET /api/users?keyword=xxx&page=1&size=20 → Page<ProfileDetailVO>
 */
function searchUsers(keyword, page, size) {
  return new Promise(function(resolve, reject) {
    var query = '?keyword=' + encodeURIComponent(keyword || '') +
                '&page=' + (page || 1) +
                '&size=' + (size || 20)
    wx.request({
      url: BASE_URL + '/users' + query,
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) {
          var data = res.data.data
          // Page 结构: { records/list, total, page, size }
          var list = Array.isArray(data) ? data : (data.records || data.list || [])
          resolve(list)
        } else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

// ========== 方法 21：全量项目（用于前端搜索） ==========

/**
 * 获取尽可能多的项目列表（前端按名称搜索用）
 * GET /api/m4/projects?pageSize=999
 */
function getAllProjects() {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: BASE_URL + '/m4/projects?pageNum=1&pageSize=999',
      method: 'GET',
      header: authHeader(),
      timeout: 10000,
      success: function(res) {
        if (res.data && isSuccessCode(res.data.code)) {
          var data = res.data.data
          var list = Array.isArray(data) ? data : (data && data.records ? data.records : (data && data.list ? data.list : []))
          resolve(list)
        } else reject(res.data)
      },
      fail: function(err) { reject(err) }
    })
  })
}

module.exports = {
  sendApplyRequest: sendApplyRequest,
  sendInviteRequest: sendInviteRequest,
  getRequests: getRequests,
  acceptRequest: acceptRequest,
  rejectRequest: rejectRequest,
  cancelRequest: cancelRequest,
  getMembers: getMembers,
  getProjectDetail: getProjectDetail,
  getUserProfile: getUserProfile,
  clearProfileCache: clearProfileCache,
  selfExit: selfExit,
  startProject: startProject,
  endProject: endProject,
  getExitVoteDetail: getExitVoteDetail,
  submitExitVote: submitExitVote,
  closeExitVote: closeExitVote,
  cancelExitVote: cancelExitVote,
  createExitVote: createExitVote,
  checkEvaluationEligibility: checkEvaluationEligibility,
  getEvaluatableMembers: getEvaluatableMembers,
  submitEvaluation: submitEvaluation,
  getCurrentCreditScore: getCurrentCreditScore,
  getCreditHistory: getCreditHistory,
  uploadFile: uploadFile,
  uploadAvatar: uploadAvatar,
  submitReport: submitReport,
  getMyReports: getMyReports,
  getAppealTargets: getAppealTargets,
  submitAppeal: submitAppeal,
  getMyAppeals: getMyAppeals,
  getMyJoinedProjects: getMyJoinedProjects,
  getProjectPendingVotes: getProjectPendingVotes,
  getProjectExitVotes: getProjectExitVotes,
  getAllProjects: getAllProjects,
  searchUsers: searchUsers
}
