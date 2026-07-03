const LOGIN_PAGE_URL = '/pages/index/index'
const TEAM_REQUESTS_PAGE_URL = '/pages/team-requests/team-requests'
const PROJECT_MEMBERS_PAGE_URL = '/pages/project-members/project-members'
const EXIT_PROJECT_PAGE_URL = '/pages/exit-project/exit-project'
const EXIT_VOTE_DETAIL_PAGE_URL = '/pages/exit-vote-detail/exit-vote-detail'
const EVALUATION_PAGE_URL = '/pages/evaluation/evaluation'
const CREDIT_HISTORY_PAGE_URL = '/pages/credit-history/credit-history'
const REPORT_PAGE_URL = '/pages/report/report'
const APPEAL_PAGE_URL = '/pages/appeal/appeal'
const MY_REPORTS_PAGE_URL = '/pages/my-reports/my-reports'
const MY_APPEALS_PAGE_URL = '/pages/my-appeals/my-appeals'
const PROFILE_EDIT_PAGE_URL = '/pages/profile/edit/edit'
const SKILLS_PAGE_URL = '/pages/profile/skills/skills'
const EMAIL_VERIFY_PAGE_URL = '/pages/email-verify/email-verify'
const PROJECT_LIST_PAGE_URL = '/pages/projects/list/list'
const PROJECT_DETAIL_PAGE_URL = '/pages/projects/detail/detail'
const PROJECT_CREATE_PAGE_URL = '/pages/projects/create/create'
const LEADERBOARD_PAGE_URL = '/pages/leaderboard/leaderboard'
const PROFILE_CLAIM_PAGE_URL = '/pages/profile-claim/profile-claim'
const MY_CENTER_PAGE_URL = '/pages/my-center/my-center'
const TOKEN_KEYS = ['token', 'accessToken', 'userToken', 'sessionToken']

Page({
  data: {
    isAuthenticated: false,
    isRedirectingToLogin: false
  },

  onShow() {
    this.ensureLogin()
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
  },

  goTeamRequests() {
    wx.navigateTo({
      url: TEAM_REQUESTS_PAGE_URL
    })
  },

  goProjectMembers() {
    wx.navigateTo({
      url: PROJECT_MEMBERS_PAGE_URL
    })
  },

  goExitProject() {
    wx.navigateTo({
      url: EXIT_PROJECT_PAGE_URL
    })
  },

  goExitVoteDetail() {
    wx.navigateTo({
      url: EXIT_VOTE_DETAIL_PAGE_URL
    })
  },

  goEvaluation() {
    wx.navigateTo({
      url: EVALUATION_PAGE_URL
    })
  },

  goCreditHistory() {
    wx.navigateTo({
      url: CREDIT_HISTORY_PAGE_URL
    })
  },

  goReport() {
    wx.navigateTo({
      url: REPORT_PAGE_URL
    })
  },

  goAppeal() {
    wx.navigateTo({
      url: APPEAL_PAGE_URL
    })
  },

  goMyReports() {
    wx.navigateTo({
      url: MY_REPORTS_PAGE_URL
    })
  },

  goMyAppeals() {
    wx.navigateTo({
      url: MY_APPEALS_PAGE_URL
    })
  },

  goProfileEdit() {
    wx.navigateTo({
      url: PROFILE_EDIT_PAGE_URL
    })
  },

  goSkills() {
    wx.navigateTo({
      url: SKILLS_PAGE_URL
    })
  },

  goEmailVerify() {
    wx.navigateTo({
      url: EMAIL_VERIFY_PAGE_URL
    })
  },

  goProjectList() {
    wx.navigateTo({
      url: PROJECT_LIST_PAGE_URL
    })
  },

  goProjectDetail() {
    wx.navigateTo({
      url: PROJECT_DETAIL_PAGE_URL
    })
  },

  goProjectCreate() {
    wx.navigateTo({
      url: PROJECT_CREATE_PAGE_URL
    })
  },

  goLeaderboard() {
    wx.navigateTo({
      url: LEADERBOARD_PAGE_URL
    })
  },

  goProfileClaim() {
    wx.navigateTo({
      url: PROFILE_CLAIM_PAGE_URL
    })
  },

  goMyCenter() {
    wx.navigateTo({
      url: MY_CENTER_PAGE_URL
    })
  }
})
