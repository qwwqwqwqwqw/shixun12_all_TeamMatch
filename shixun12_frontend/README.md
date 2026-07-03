# TeamMatch 前端（shixun12_frontend）

TeamMatch 校园组队与互评平台 - 微信小程序前端项目。

> **仓库结构**
> - `miniprogram/` — 微信小程序（用户端）
> - `admin-web/` — Vue3 + ElementPlus Web 管理后台（待搭建）

> **⚠️ 当前状态（W10）**
> 以下页面均为**静态 Mock 页面**，仅完成了 UI 布局和 Mock 数据的展示逻辑。
> **尚未实现的功能**：页面间跳转、真实接口联调、表单校验的完整覆盖、错误处理、页面美化等。
> 这些内容将在 W10-W13 的联调和优化阶段逐步补全。

---

## 技术栈

- 微信小程序原生框架（未使用第三方框架）
- Mock 数据内联在各页面的 `Page()` 方法中，联调时将替换为 `wx.request` 调用

---

## 项目结构

\`\`\`
├── miniprogram/                 # 微信小程序用户端
│   ├── pages/
│   │   ├── login/               # 登录页
│   │   ├── index/               # 首页-微信登录入口
│   │   ├── function-hub/        # 功能选择界面
│   │   ├── team-requests/       # 组队请求管理
│   │   ├── project-members/     # 项目成员列表
│   │   ├── exit-project/        # 成员主动退出
│   │   ├── exit-vote-detail/    # 退出投票详情
│   │   ├── evaluation/          # 互评页
│   │   ├── credit-history/      # 信誉分变化记录
│   │   ├── report/              # 举报入口
│   │   ├── appeal/              # 申诉入口
│   │   ├── my-reports/          # 我的举报列表
│   │   ├── my-appeals/          # 我的申诉列表
│   │   ├── profile/             # 个人档案
│   │   ├── email-verify/        # 邮箱认证
│   │   ├── projects/            # 项目相关
│   │   └── logs/                # 占位页
│   ├── utils/
│   │   ├── util.js              # 通用工具
│   │   └── team-service.js      # 组队/互评/治理服务（18个方法）
│   ├── docs/
│   │   └── alignment-T003-token.md
│   ├── app.js / app.json / app.wxss
│   └── project.config.json
├── admin-web/                   # Web 管理后台（待搭建）
│   └── .gitkeep
├── .gitignore
└── README.md
\`\`\`


---

## M1 已完成页面清单（均为静态 Mock 版本）

| 任务ID | 页面名称 | 文件路径 | 说明 |
|:---|:---|:---|:---|
| T-002 | 登录引导页 | `pages/login/login` | 欢迎页，点击跳转首页进行微信登录 |
| T-006 | 邮箱认证页 | `pages/email-verify/email-verify` | 邮箱输入、验证码发送/校验（Mock: 输入 654321 成功） |
| T-008 | 个人档案编辑页 | `pages/profile/edit/edit` | 昵称、学校、专业、年级、简介表单 |
| T-010 | 技能标签选择页 | `pages/profile/skills/skills` | 按分类展示预置标签，多选后全量替换保存 |
| T-012 | 项目列表页 | `pages/projects/list/list` | 项目卡片列表，包含状态、人数、技能标签 |
| T-014 | 项目详情页 | `pages/projects/detail/detail` | 项目完整信息、成员列表、技能需求 |
| T-016 | 发布项目表单 | `pages/projects/create/create` | 标题、描述、板块、人数、技能、截止日期 |

---

## M2 已完成页面清单（Mock 模式，后端未就绪）

| 任务ID | 页面名称 | 文件路径 | 说明 |
|:---|:---|:---|:---|
| T-031 | 组队请求页 | `pages/team-requests/team-requests` | 收到/发出请求列表，状态标签，操作按钮 |
| T-032 | 项目成员列表 | `pages/project-members/project-members` | 角色标签、ACTIVE/EXITED、Trust 分、退出原因 |
| T-033 | 请求列表联调准备 | `pages/team-requests/team-requests` | Service 层封装，加载/错误/空状态 |
| T-034 | 接受/拒绝/取消联调 | `pages/team-requests/team-requests` | 防重复点击，操作 loading 状态 |
| T-035 | 成员列表联调 | `pages/project-members/project-members` | Service 层接入，加载/错误/空状态 |
| T-036 | 状态字段对齐 | — | 与 M4 对齐记录文档 |
| T-037 | 成员主动退出页 | `pages/exit-project/exit-project` | 退出原因、确认弹窗、扣分提示 |
| T-038 | 退出投票详情页 | `pages/exit-vote-detail/exit-vote-detail` | 倒计时、票数统计、投票按钮 ⚠️ timeout 问题待修复 |
| T-039 | 退出投票联调 | `pages/exit-vote-detail/exit-vote-detail` | Service 层真实接口预留 |
| T-040 | 互评页 | `pages/evaluation/evaluation` | 四维评分、标签选择、技能点赞 |
| T-041 | 互评资格/提交联调 | `pages/evaluation/evaluation` | 资格校验、错误码处理 |
| T-042 | 信誉分变化记录页 | `pages/credit-history/credit-history` | 当前分数总览、变化流水列表 |
| T-043 | 举报入口页 | `pages/report/report` | 类型选择、对象搜索、原因标签 |
| T-044 | 申诉入口页 | `pages/appeal/appeal` | 评价/处罚申诉、对象选择 |
| T-045 | 举报申诉联调 | `pages/report/report`、`pages/appeal/appeal` | Service 层完善，My List 接口预留 |

### M2 Service 层封装（`utils/team-service.js`）

所有接口使用 `USE_MOCK = true` 切换 Mock/真实模式：

| 方法 | 用途 |
|------|------|
| `getRequests(type)` | 获取组队请求列表 |
| `acceptRequest(id)` | 接受请求 |
| `rejectRequest(id)` | 拒绝请求 |
| `cancelRequest(id)` | 取消请求 |
| `getMembers(projectId)` | 获取项目成员 |
| `selfExit(projectId, reason)` | 主动退出 |
| `getExitVoteDetail(voteId)` | 获取退出投票详情 |
| `submitExitVote(voteId, choice)` | 提交投票 |
| `closeExitVote(voteId)` | 关闭投票 |
| `checkEvaluationEligibility(projectId, targetUserId)` | 检查互评资格 |
| `submitEvaluation(params)` | 提交互评 |
| `getCurrentCreditScore()` | 获取当前信誉分 |
| `getCreditHistory()` | 获取信誉变化记录 |
| `submitReport(params)` | 提交举报 |
| `getMyReports()` | 获取我的举报列表 |
| `getAppealTargets(type)` | 获取可申诉对象 |
| `submitAppeal(params)` | 提交申诉 |
| `getMyAppeals()` | 获取我的申诉列表 |

---

## 待完成内容

### M1 待完成
- [ ] **页面间跳转**：如首页 → 档案页、项目列表 → 项目详情 → 申请加入等
- [ ] **真实接口联调**（T-005/T-007/T-009/T-011/T-013/T-015/T-017）
- [ ] **请求封装**：创建 `utils/request.js`，统一处理 Token 携带、401 拦截
- [ ] **错误处理**：reasonCode 统一提示（T-024）
- [ ] **页面美化**：与 M2 统一 UI 风格，计划引入 WeUI 组件库
- [ ] **搜索/筛选**（T-023）
- [ ] **冷启动榜单/画像页面**（T-018/T-019/T-020）

### M2 待完成
- [ ] 修复 `exit-vote-detail` 页面 timeout 问题
- [ ] T-046 ~ T-051 Vue3 Web 管理后台页面
- [ ] T-052 ~ T-060 联调 / 测试 / 证据 / 答辩

---

## 本地开发指南

### 环境要求

- [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)（最新版）
- 一个微信小程序 AppID（测试号即可，不影响开发）

### 使用步骤

1. 克隆本项目到本地：
   \`\`\`bash
   git clone "https://你的学号@gerrit.lilingkun.com/a/shixun12_frontend"
   \`\`\`

2. 打开**微信开发者工具**，点击 **“导入项目”**。

3. 填写项目信息：
   | 字段 | 内容 |
   |:---|:---|
   | 项目名称 | 任意，如 `TeamMatch 前端` |
   | 目录 | 选择克隆后的 `shixun12_frontend/miniprogram` 文件夹 |
   | AppID | 使用你自己的测试号，或从 `project.config.json` 获取 |

4. 点击 **“确定”**，工具会自动加载项目并编译。

5. 如果编译后没有看到期望的页面，可以在顶部工具栏**“编译模式”**中选择要预览的页面路径，例如：
   - `pages/login/login`
   - `pages/projects/list/list`
   - `pages/profile/skills/skills`

6. 开始开发和调试。

> **注意**：v2.0 起项目结构已拆分，小程序文件全部位于 `miniprogram/` 目录下。
> 微信开发者工具导入时需选择 `shixun12_frontend/miniprogram` 而不是仓库根目录。

---

## Git 提交规范

所有代码需通过 Gerrit 评审后合并：

\`\`\`bash
git add .
git commit -m "feat: 功能描述"
git push origin HEAD:refs/for/master
\`\`\`

提交说明需写清影响范围。

---

## 已知问题

| 问题 | 影响范围 | 状态 |
|------|----------|------|
| `exit-vote-detail` 出现 `Error: timeout` | 投票详情页无法正常加载 | 待修复 |
| 所有页面使用 Mock 数据 | 全平台 | 待后端接口就绪后切换 |