# M4 模块（项目管理与组队协作）API 文档与使用说明

## 概述

M4 模块负责 TeamMatch 平台的项目管理与组队协作核心业务。本模块提供项目发布与状态流转、组队邀请与申请处理、成员管理、成员退出机制（主动退出与投票退出）以及信誉分计算规则服务等功能。

---

## 技术栈

- **Spring Boot 3.2.5**
- **MyBatis-Plus 3.5.5**
- **JUnit 5 + Mockito**（Spring Boot 默认）
- **MySQL 8.0**

---

## 基础配置

### 服务地址
- **Base URL**: `http://localhost:8080/api`
- **Context Path**: `/api`（已在 application.yml 中配置）

### 公共请求头
```
Content-Type: application/json
Authorization: Bearer <JWT Token>  (需要鉴权的接口，M5 互评/信誉接口必须携带)
```

---

## 接口速查表

| # | 方法 | 路径 | 说明 | 鉴权 |
|---|------|------|------|------|
| 1 | POST | `/api/m4/projects` | 创建项目 | 否 |
| 2 | GET | `/api/m4/projects` | 项目列表（分页/状态筛选） | 否 |
| 3 | GET | `/api/m4/projects/{id}` | 项目详情 | 否 |
| 4 | PUT | `/api/m4/projects/{id}` | 更新项目信息 | 否 |
| 5 | POST | `/api/m4/projects/{id}/start` | 开始项目 | 否 |
| 6 | POST | `/api/m4/projects/{id}/end` | 结束项目 | 否 |
| 7 | GET | `/api/m4/projects/{id}/eval-status` | 互评窗口懒检查 | 否 |
| 8 | GET | `/api/m4/projects/{id}/members` | 项目成员列表 | 否 |
| 9 | POST | `/api/m4/team-requests/invite` | 队长发送邀请 | 否 |
| 10 | POST | `/api/m4/team-requests/apply` | 用户申请加入 | 否 |
| 11 | POST | `/api/m4/team-requests/{id}/accept` | 接受请求 | 否 |
| 12 | POST | `/api/m4/team-requests/{id}/reject` | 拒绝请求 | 否 |
| 13 | POST | `/api/m4/team-requests/{id}/cancel` | 取消请求 | 否 |
| 14 | GET | `/api/m4/team-requests` | 请求列表（收到/发出） | 否 |
| 15 | POST | `/api/m4/projects/{id}/exit/self` | 成员主动退出 | 否 |
| 16 | POST | `/api/m4/projects/{id}/exit/votes` | 队长发起退出投票 | 否 |
| 17 | GET | `/api/m4/projects/{id}/exit/votes` | 退出投票列表 | 否 |
| 18 | GET | `/api/m4/projects/{id}/exit/votes/{voteId}` | 退出投票详情 | 否 |
| 19 | POST | `/api/m4/projects/{id}/exit/votes/{voteId}/submit` | 提交投票 | 否 |
| 20 | POST | `/api/m4/projects/{id}/exit/votes/{voteId}/close` | 关闭投票并执行结果 | 否 |
| 21 | GET | `/api/m5/projects/{id}/evaluation-eligibility` | 检查项目级互评资格 | 是 |
| 22 | GET | `/api/m5/projects/{id}/members/{targetId}/evaluation-eligibility` | 检查目标级互评资格 | 是 |
| 23 | GET | `/api/m5/projects/{id}/evaluatable-members` | 可评价成员列表 | 是 |
| 24 | POST | `/api/m5/evaluations` | 提交互评 | 是 |
| 25 | GET | `/api/m5/evaluations/received` | 查看收到的互评 | 是 |
| 26 | GET | `/api/m5/credit/score` | 查看我的信誉分 | 是 |
| 27 | GET | `/api/m5/credit/changes` | 查看信誉流水（分页） | 是 |

---

## 接口列表

### 一、项目管理模块 (`/m4/projects`)

> Controller：`com.teammatch.m4.controller.ProjectController`
> 任务覆盖：T-113 创建项目、T-114 项目列表、T-115 项目详情、T-116 更新项目、T-117 开始项目、T-118 结束项目、T-119 互评窗口懒检查、T-132 成员列表

---

#### 1. 创建项目（T-113）

**接口**: `POST /api/m4/projects`

**说明**: 发布一个新项目，发起人自动成为队长，初始状态为 `recruiting`（招募中）。

**请求 Body**:
```json
{
  "creatorId": 10,
  "boardId": 1,
  "title": "后端架构实训项目",
  "description": "基于 Spring Boot 的后端开发实训",
  "maxMembers": 5,
  "deadline": "2026-07-01T23:59:59",
  "skillTagIds": [1, 3, 7]
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "id": 123,
    "creatorId": 10,
    "boardId": 1,
    "title": "后端架构实训项目",
    "status": "recruiting",
    "maxMembers": 5,
    "deadline": "2026-07-01T23:59:59",
    "createdAt": "2026-06-01T10:00:00"
  }
}
```

**业务规则**:
- `creatorId` 为发起人 userId，生产环境建议从 Token 提取
- `boardId` 必须对应存在且状态为 active 的板块，否则返回 `M1001` 错误
- `skillTagIds` 为可选，传入的技能标签需在 skill_tag 表中存在

---

#### 2. 项目列表（T-114）

**接口**: `GET /api/m4/projects?pageNum={pageNum}&pageSize={pageSize}&status={status}`

**说明**: 分页获取项目列表，支持按状态筛选，按创建时间倒序排列。

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 123,
        "title": "后端架构实训项目",
        "status": "recruiting",
        "maxMembers": 5,
        "createdAt": "2026-06-01T10:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

**业务规则**:
- `pageNum` 默认 1，`pageSize` 默认 10
- `status` 可选值：`recruiting` / `in_progress` / `ended` / `eval_closed`，不传则返回全部

---

#### 3. 项目详情（T-115）

**接口**: `GET /api/m4/projects/{id}`

**说明**: 根据项目 ID 获取项目详情，包含技能标签、成员摘要和状态信息。

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "id": 123,
    "creatorId": 10,
    "boardId": 1,
    "title": "后端架构实训项目",
    "status": "in_progress",
    "maxMembers": 5,
    "deadline": "2026-07-01T23:59:59",
    "evalDeadline": null,
    "endedAt": null,
    "createdAt": "2026-06-01T10:00:00"
  }
}
```

**业务规则**:
- 项目 ID 不存在时返回 `M4001` 错误

---

#### 4. 更新项目信息（T-116）

**接口**: `PUT /api/m4/projects/{id}`

**说明**: 队长更新 `recruiting` 状态项目的基本信息，只填需要修改的字段。

**请求 Body**:
```json
{
  "title": "新项目标题",
  "description": "更新后的描述",
  "maxMembers": 6
}
```

**响应**:
```json
{ "code": "00000", "message": "成功", "data": null }
```

**业务规则**:
- 项目不存在返回 `M4001` 错误

---

#### 5. 开始项目（T-117）

**接口**: `POST /api/m4/projects/{id}/start?operatorId={operatorId}`

**说明**: 队长将项目从 `recruiting` 切换到 `in_progress`，同时批量将所有 pending 状态的组队请求标记为 `expired`。

**响应**:
```json
{ "code": "00000", "message": "成功", "data": null }
```

**业务规则**:
- 操作人必须是该项目队长，否则操作会被拒绝
- 当前状态不是 `recruiting` 时返回 `M4003` 错误

---

#### 6. 结束项目（T-118）

**接口**: `POST /api/m4/projects/{id}/end?operatorId={operatorId}`

**说明**: 队长将项目从 `in_progress` 切换到 `ended`，系统写入 `ended_at` 和 `eval_deadline`（互评截止时间 = ended_at + 7 天）。

**响应**:
```json
{ "code": "00000", "message": "成功", "data": null }
```

**业务规则**:
- 操作人必须是该项目队长
- 当前状态不是 `in_progress` 时返回 `M4003` 错误

---

#### 7. 互评窗口懒检查（T-119）

**接口**: `GET /api/m4/projects/{id}/eval-status`

**说明**: 返回项目当前互评窗口状态。若 `eval_deadline` 已过期，自动切换状态为 `eval_closed` 并返回新状态（懒检查，按需触发，无需定时任务）。

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": "eval_open"
}
```

**业务规则**:
- `data` 枚举值：`eval_open`（互评进行中）/ `eval_closed`（互评已关闭）
- 项目 ID 不存在返回 `M4001` 错误

---

#### 8. 项目成员列表（T-132）

**接口**: `GET /api/m4/projects/{id}/members`

**说明**: 返回项目所有成员，包含在队（active）和已退出（exited）成员。

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": [
    {
      "id": 1,
      "projectId": 123,
      "userId": 10,
      "role": "leader",
      "status": "active",
      "joinedAt": "2026-06-01T10:00:00"
    },
    {
      "id": 2,
      "projectId": 123,
      "userId": 20,
      "role": "member",
      "status": "active",
      "joinedAt": "2026-06-02T09:00:00"
    }
  ]
}
```

**业务规则**:
- `role`：`leader`（队长）/ `member`（成员）
- `status`：`active`（在队）/ `exited`（已退出）

---

### 二、组队请求模块 (`/m4/team-requests`)

> Controller：`com.teammatch.m4.controller.TeamRequestController`
> 任务覆盖：T-122 队长邀请、T-123 申请加入、T-124 接受、T-129 拒绝、T-130 取消、T-131 请求列表

---

#### 1. 队长发送邀请（T-122）

**接口**: `POST /api/m4/team-requests/invite`

**说明**: 队长邀请指定用户加入项目，项目必须处于 `recruiting` 状态。

**请求 Body**:
```json
{
  "projectId": 123,
  "fromUserId": 10,
  "toUserId": 20,
  "message": "欢迎加入我们的团队！"
}
```

**响应**:
```json
{ "code": "00000", "message": "成功", "data": null }
```

**业务规则**:
- 项目非 `recruiting` 状态时返回 `M4002`（PROJECT_NOT_RECRUITING）错误
- 已存在对同一用户的 pending 邀请时返回 `M4004`（DUPLICATE_PENDING_REQUEST）错误
- 被邀请人已是项目成员时返回 `M1003` 错误

---

#### 2. 用户申请加入项目（T-123）

**接口**: `POST /api/m4/team-requests/apply`

**说明**: 用户主动申请加入项目，项目须处于 `recruiting` 状态且未满员。

**请求 Body**:
```json
{
  "projectId": 123,
  "fromUserId": 50,
  "toUserId": 10,
  "message": "我擅长 Vue，希望加入"
}
```

**响应**:
```json
{ "code": "00000", "message": "成功", "data": null }
```

**业务规则**:
- `toUserId` 为队长 userId
- 项目已满员（active 成员数 ≥ maxMembers）时返回 `M4003`（PROJECT_FULL）错误
- 已存在 pending 申请时返回 `M4004`（DUPLICATE_PENDING_REQUEST）错误

---

#### 3. 接受请求（T-124）

**接口**: `POST /api/m4/team-requests/{id}/accept?operatorId={operatorId}`

**说明**: 操作人接受一条 pending 状态的请求，自动将对方写入 `team_member` 表（role=member, status=active）。

**响应**:
```json
{ "code": "00000", "message": "成功", "data": null }
```

**业务规则**:
- 请求不存在时返回 `M4005` 错误
- 请求不是 pending 状态时返回 `M1003` 错误
- 项目已满员时返回 `M4003` 错误
- 接受后，同一目标用户的其他 pending 请求自动失效，防止重复加入

---

#### 4. 拒绝请求（T-129）

**接口**: `POST /api/m4/team-requests/{id}/reject?operatorId={operatorId}`

**说明**: 操作人拒绝一条 pending 状态的请求，状态改为 `rejected`。

**响应**:
```json
{ "code": "00000", "message": "成功", "data": null }
```

**业务规则**:
- 请求不存在时返回 `M4005` 错误
- 请求不是 pending 状态，或操作人非接收方时返回 `M1003` 错误

---

#### 5. 取消请求（T-130）

**接口**: `POST /api/m4/team-requests/{id}/cancel?operatorId={operatorId}`

**说明**: 发送方取消自己发出的 pending 请求，状态改为 `cancelled`。

**响应**:
```json
{ "code": "00000", "message": "成功", "data": null }
```

**业务规则**:
- 操作人必须是请求的发送方
- 请求不存在或非 pending 状态时返回 `M4005` 错误

---

#### 6. 收到/发出的请求列表（T-131）

**接口**: `GET /api/m4/team-requests?userId={userId}&direction={direction}`

**说明**: 查询用户收到或发出的所有组队请求列表。

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": [
    {
      "id": 1,
      "projectId": 123,
      "fromUserId": 10,
      "toUserId": 20,
      "requestType": "invite",
      "status": "pending",
      "message": "欢迎加入！",
      "createdAt": "2026-06-01T10:00:00"
    }
  ]
}
```

**业务规则**:
- `direction`：`received`（收到的，默认）/ `sent`（发出的）
- `requestType`：`invite`（邀请）/ `apply`（申请）
- `status`：`pending` / `accepted` / `rejected` / `cancelled` / `expired`

---

### 三、退出流程模块 (`/m4/projects/{projectId}/exit`)

> Controller：`com.teammatch.m4.controller.ExitVoteController`
> 任务覆盖：T-136 成员主动退出、T-137 队长发起退出投票、投票列表、T-138 投票详情、T-139 提交投票、T-140/T-141 关闭投票（事务+幂等+并发保护）

---

#### 1. 成员主动退出（T-136）

**接口**: `POST /api/m4/projects/{projectId}/exit/self?userId={userId}`

**说明**: 活跃成员主动退出进行中的项目，触发信誉扣分（写入 credit_change -10 流水）。使用 CAS 保证并发安全，多个退出操作同时发生时只有一个生效。

**响应**:
```json
{ "code": "00000", "message": "成功", "data": null }
```

**业务规则**:
- 项目不存在时返回 `M4001` 错误
- 操作人是当前队长时返回 `M4009` 错误（队长不能直接退出，须先转让队长身份）
- 用户不是该项目活跃成员时返回 `M4006` 错误
- 并发退出操作冲突（CAS 失败）时返回 `TEAM_VOTE_CONFLICT` 错误，客户端可重试

---

#### 2. 队长发起退出投票（T-137）

**接口**: `POST /api/m4/projects/{projectId}/exit/votes`

**说明**: 队长对某活跃成员发起投票，项目必须处于 `in_progress`，同一成员同时只能有一个进行中的退出投票。投票截止时间为创建时 +24h，超时后自动失效。

**请求 Body**:
```json
{
  "initiatorId": 10,
  "targetUserId": 30,
  "reason": "长期失联，严重影响项目进度",
  "penaltyLevel": "malicious"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "id": 5,
    "projectId": 123,
    "targetUserId": 30,
    "initiatorId": 10,
    "status": "voting",
    "penaltyLevel": "malicious",
    "result": null,
    "reason": "长期失联，严重影响项目进度",
    "totalVoters": 3,
    "agreeCount": 0,
    "disagreeCount": 0,
    "deadlineAt": "2026-06-02T10:00:00",
    "createdAt": "2026-06-01T10:00:00"
  }
}
```

**业务规则**:
- `penaltyLevel` 必须为 `negotiated`（协商退出）或 `malicious`（恶意退出），缺失或非法返回 `INVALID_PENALTY_LEVEL` 错误
- 发起人不是队长时返回 `M4002` 错误
- 项目不是 `in_progress` 状态时返回 `M4003` 错误
- 目标成员不是项目活跃成员时返回 `M4006` 错误
- 该成员已有进行中的退出投票时，数据库唯一键 `uk_exit_vote_active_target` 拦截，返回 `M4008` 错误
- 不能对队长本人发起退出投票，返回 `M1003` 错误

---

#### 3. 退出投票列表

**接口**: `GET /api/m4/projects/{projectId}/exit/votes`

**说明**: 查询指定项目下所有退出投票列表，按创建时间倒序排列。

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": [
    {
      "id": 5,
      "projectId": 123,
      "targetUserId": 30,
      "initiatorId": 10,
      "status": "voting",
      "penaltyLevel": "malicious",
      "result": null,
      "totalVoters": 3,
      "agreeCount": 1,
      "disagreeCount": 0,
      "deadlineAt": "2026-06-02T10:00:00",
      "createdAt": "2026-06-01T10:00:00"
    }
  ]
}
```

**业务规则**:
- 空列表（无投票记录）也正常返回，不报错

---

#### 4. 退出投票详情（T-138）

**接口**: `GET /api/m4/projects/{projectId}/exit/votes/{voteId}`

**说明**: 查询退出投票详情，含目标成员、原因、当前投票状态和统计信息。

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "id": 5,
    "projectId": 123,
    "targetUserId": 30,
    "initiatorId": 10,
    "status": "voting",
    "penaltyLevel": "malicious",
    "result": null,
    "totalVoters": 3,
    "agreeCount": 1,
    "disagreeCount": 0,
    "deadlineAt": "2026-06-02T10:00:00"
  }
}
```

**业务规则**:
- voteId 不存在时返回 `M4005` 错误

---

#### 5. 提交投票（T-139）

**接口**: `POST /api/m4/projects/{projectId}/exit/votes/{voteId}/submit`

**说明**: 活跃成员对进行中的退出投票表态。目标成员不能参与自己的投票，同一成员只能投票一次（数据库唯一键保证）。

**请求 Body**:
```json
{
  "voterId": 20,
  "choice": "agree"
}
```

**响应**:
```json
{ "code": "00000", "message": "成功", "data": null }
```

**业务规则**:
- `choice`：`agree`（赞成踢出）/ `disagree`（反对）
- voteId 不存在时返回 `M4005` 错误
- 投票已关闭时返回 `M4007` 错误
- 该成员已投过票时，数据库唯一键 `uk_exit_vote_record_voter` 拦截，返回 `M4008` 错误
- 投票人不是项目活跃成员时返回 `M4006` 错误
- 目标成员不能参与自己的退出投票，返回 `M1003` 错误

---

#### 6. 关闭投票并执行结果（T-140/T-141）

**接口**: `POST /api/m4/projects/{projectId}/exit/votes/{voteId}/close?operatorId={operatorId}`

**说明**: 只有发起人（队长）可关闭投票。采用 `@Transactional` + 幂等校验 + team_member CAS 三重保护：统计赞成票超过半数则 result=pass，移除目标成员并写入信誉扣分流水（-10）；否则 result=reject，成员继续留队。

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "id": 5,
    "projectId": 123,
    "targetUserId": 30,
    "status": "closed",
    "result": "pass",
    "agreeCount": 2,
    "disagreeCount": 1,
    "closedAt": "2026-06-02T08:00:00"
  }
}
```

**业务规则**:
- voteId 不存在时返回 `M4005` 错误
- 操作人不是发起人时返回 `M4002` 错误
- 投票已关闭时（幂等）直接返回当前状态，不重复执行
- pass 时并发保护：使用 CAS 更新 team_member，`memberRows != 1` 则不写 credit_change

---

### 四、互评接口 (`/m5/...`)

> Controller：`com.teammatch.controller.EvaluationController`
> 鉴权：所有接口必须携带 `Authorization: Bearer <token>`
> 任务覆盖：T-086 提交互评、T-090 资格校验、T-095 可评价成员列表/收到的互评

---

#### 1. 检查项目级互评资格（T-090）

**接口**: `GET /api/m5/projects/{projectId}/evaluation-eligibility`

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**说明**: 检查当前登录用户是否有资格进入该项目的互评页面（项目已 ended、互评窗口内、用户是项目成员）。

**响应**:
```json
{ "code": "00000", "message": "成功", "data": true }
```

**业务规则**:
- Token 缺失或无效返回 `M3000` 错误
- 用户不是项目成员时 data 为 false 或返回 `NOT_PROJECT_MEMBER`
- 项目非 ended 状态时返回 `PROJECT_NOT_ENDED`
- 互评窗口已过期时返回 `EVAL_WINDOW_CLOSED`

---

#### 2. 检查目标级互评资格（T-090）

**接口**: `GET /api/m5/projects/{projectId}/members/{targetId}/evaluation-eligibility`

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**说明**: 检查当前用户是否可以评价某个具体成员（不能自评、目标是成员、未重复评价）。

**响应**:
```json
{ "code": "00000", "message": "成功", "data": true }
```

**业务规则**:
- 评价人与目标相同时返回 `SELF_EVALUATION` 错误
- 已存在评价记录时返回 `ALREADY_EVALUATED` 错误
- 目标用户不是项目成员时返回 `TARGET_NOT_PROJECT_MEMBER` 错误

---

#### 3. 可评价成员列表（T-095）

**接口**: `GET /api/m5/projects/{projectId}/evaluatable-members`

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**说明**: 查询当前用户在该项目中可以评价的成员列表，不含自己，并标记是否已评价。

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": [
    { "userId": 20, "nickname": "张三", "role": "member", "hasEvaluated": false },
    { "userId": 30, "nickname": "李四", "role": "member", "hasEvaluated": true }
  ]
}
```

---

#### 4. 提交互评（T-086）

**接口**: `POST /api/m5/evaluations`

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**说明**: 提交对项目成员的四维互评（沟通/任务/技能/责任各 1-5 分）。评价人身份从 Token 提取，不接受请求体传入 evaluatorId。

**请求 Body**:
```json
{
  "projectId": 123,
  "targetId": 20,
  "communicationScore": 4,
  "taskScore": 5,
  "skillScore": 4,
  "responsibilityScore": 5,
  "comment": "团队合作积极，按时完成所有任务",
  "positiveTags": ["按时交付", "主动沟通"],
  "negativeTags": []
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "evaluationId": 88,
    "creditDelta": 3,
    "status": "normal"
  }
}
```

**业务规则**:
- `status`：`normal`（正常计分）/ `pending_review`（疑似异常，挂起待复核）
- 5 分时至少提供一个正向标签，否则返回 `M5204` 错误
- 1/2 分时至少提供一个负向标签，否则返回 `M5205` 错误
- 低分评价 comment 长度不足时返回 `M5206` 错误
- 不能自评，返回 `SELF_EVALUATION` 错误；重复评价返回 `ALREADY_EVALUATED` 错误

---

#### 5. 查看我收到的互评（T-095）

**接口**: `GET /api/m5/evaluations/received?projectId={projectId}`

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**说明**: 查看当前用户收到的互评，评价人匿名展示（不暴露 evaluatorId）。已作废（voided）的评价不展示。

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": [
    {
      "evaluationId": 88,
      "projectId": 123,
      "communicationScore": 4,
      "taskScore": 5,
      "skillScore": 4,
      "responsibilityScore": 5,
      "averageScore": 4.5,
      "comment": "团队合作非常积极",
      "status": "normal",
      "createdAt": "2026-06-05T12:00:00"
    }
  ]
}
```

**业务规则**:
- `projectId` 为可选，不传则返回该用户全部项目的互评
- `pending_review` 和 `kept_no_credit` 状态的评价正常展示，`voided` 不展示

---

### 五、信誉分查询接口 (`/m5/credit/...`)

> Controller：`com.teammatch.controller.CreditController`
> 鉴权：所有接口必须携带 `Authorization: Bearer <token>`
> 任务覆盖：T-093 我的信誉分、T-094 信誉流水列表

---

#### 1. 查看我的信誉分（T-093）

**接口**: `GET /api/m5/credit/score`

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**说明**: 返回当前登录用户的信誉分缓存值（由 credit_change 有效流水累加推导）。

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "userId": 10,
    "creditScore": 95
  }
}
```

**业务规则**:
- Token 无效返回 `M3000` 错误
- 用户不存在返回 `M1002` 错误

---

#### 2. 查看我的信誉流水（T-094）

**接口**: `GET /api/m5/credit/changes?projectId={projectId}&changeType={changeType}&page={page}&pageSize={pageSize}`

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**说明**: 分页查询当前用户的信誉分变化记录，支持按项目和变化类型筛选。

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "total": 5,
    "page": 1,
    "pageSize": 20,
    "records": [
      {
        "id": 1,
        "userId": 10,
        "projectId": 123,
        "changeType": "evaluation",
        "changeValue": 3,
        "effective": true,
        "sourceType": "evaluation",
        "sourceId": 88,
        "description": "项目 123 互评加分",
        "createdAt": "2026-06-05T12:00:00"
      }
    ]
  }
}
```

**业务规则**:
- `page` 默认 1，`pageSize` 默认 20，最大 100
- `changeType` 合法值：`evaluation` / `exit_vote` / `self_exit` / `penalty` / `penalty_restore` / `appeal_restore`，非法值返回 `M1001` 错误
- `effective`：`true` 已生效计入信誉分，`false` 挂起或不计入

---

## 信誉分计算规则（CreditRuleService，内部服务）

> Service：`com.teammatch.m4.service.CreditRuleService`
> 非 REST 接口，为 M5 互评流程调用的内部计算规则服务。任务覆盖：T-088/T-089

| 方法 | 说明 |
|------|------|
| `mapAverageScoreToDelta(double avg)` | T-088：四维均分按八区间映射到 -5~+5 的 delta 值 |
| `calculateCappedDelta(Long userId, Long projectId, int proposedDelta)` | T-089：同项目累计 evaluation 变化不超过 ±10，返回截断后有效 delta |
| `calculateCappedDeltaFromExisting(int existingDelta, int proposedDelta)` | 纯函数封顶截断（不查 DB），供单测 |

**评分区间映射规则（T-088，V2.1 八区间）：**

| 平均分区间 | delta |
|-----------|-------|
| [4.5, 5.0] | +5 |
| [4.0, 4.5) | +4 |
| [3.5, 4.0) | +3 |
| [3.0, 3.5) | +2 |
| [2.5, 3.0) | +1 |
| [2.0, 2.5) | -1 |
| [1.5, 2.0) | -3 |
| [1.0, 1.5) | -5 |

**单项目封顶规则（T-089）：** 同一用户在同一项目内，evaluation 类型的信誉变化累计不超过 ±10，超出截断。

---

## 数据表结构

### project 表（项目主表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| creator_id | BIGINT | 创建人（队长）userId |
| board_id | BIGINT | 所属板块 ID |
| title | VARCHAR | 项目标题 |
| description | TEXT | 项目描述 |
| max_members | INT | 最大成员数 |
| status | VARCHAR | 状态：recruiting / in_progress / ended / eval_closed |
| deadline | DATETIME | 招募截止时间 |
| eval_deadline | DATETIME | 互评截止时间（ended_at + 7天） |
| ended_at | DATETIME | 项目结束时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### team_member 表（项目成员关系表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| project_id | BIGINT | 项目 ID |
| user_id | BIGINT | 用户 ID |
| role | VARCHAR | 角色：leader / member |
| status | VARCHAR | 状态：active / exited |
| exit_mode | VARCHAR | 退出方式：self_exit / exit_vote；在队时为 NULL |
| joined_at | DATETIME | 加入时间 |
| left_at | DATETIME | 退出时间 |

### team_request 表（组队请求表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| project_id | BIGINT | 项目 ID |
| from_user_id | BIGINT | 发起人 userId |
| to_user_id | BIGINT | 接收人 userId |
| request_type | VARCHAR | 类型：invite / apply |
| status | VARCHAR | 状态：pending / accepted / rejected / cancelled / expired |
| message | VARCHAR | 附言 |
| handled_at | DATETIME | 处理时间 |

### exit_vote 表（退出投票主表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| project_id | BIGINT | 项目 ID |
| target_user_id | BIGINT | 被踢目标成员 userId |
| initiator_id | BIGINT | 发起人（队长）userId |
| status | VARCHAR | 状态：voting / closed |
| penalty_level | VARCHAR | 处罚级别：negotiated / malicious |
| result | VARCHAR | 结果：pass / reject / NULL（进行中） |
| reason | TEXT | 发起原因 |
| total_voters | INT | 有效投票人数（不含目标成员） |
| agree_count | INT | 赞成票数 |
| disagree_count | INT | 反对票数 |
| deadline_at | DATETIME | 投票截止时间（创建时 +24h） |
| closed_at | DATETIME | 关闭时间 |

### exit_vote_record 表（投票记录表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| vote_id | BIGINT | 退出投票 ID |
| voter_id | BIGINT | 投票人 userId |
| choice | VARCHAR | 选择：agree / disagree |
| created_at | DATETIME | 投票时间 |

---

## 使用流程示例

### 场景一：项目完整生命周期

1. **创建项目**
   ```bash
   POST /api/m4/projects
   Body: { "creatorId": 10, "boardId": 1, "title": "后端实训项目", "maxMembers": 5 }
   ```

2. **队长邀请成员**
   ```bash
   POST /api/m4/team-requests/invite
   Body: { "projectId": 123, "fromUserId": 10, "toUserId": 20, "message": "欢迎加入" }
   ```

3. **受邀成员接受邀请**
   ```bash
   POST /api/m4/team-requests/{requestId}/accept?operatorId=20
   ```

4. **开始项目**
   ```bash
   POST /api/m4/projects/123/start?operatorId=10
   ```

5. **结束项目**
   ```bash
   POST /api/m4/projects/123/end?operatorId=10
   ```

6. **查看互评窗口状态**
   ```bash
   GET /api/m4/projects/123/eval-status
   ```

---

### 场景二：投票退出全流程

1. **队长发起退出投票**
   ```bash
   POST /api/m4/projects/123/exit/votes
   Body: { "initiatorId": 10, "targetUserId": 30, "reason": "长期失联", "penaltyLevel": "malicious" }
   ```

2. **其他成员提交投票**
   ```bash
   POST /api/m4/projects/123/exit/votes/5/submit
   Body: { "voterId": 20, "choice": "agree" }
   ```

3. **队长关闭投票**
   ```bash
   POST /api/m4/projects/123/exit/votes/5/close?operatorId=10
   ```
   > result=pass 时：目标成员 status 改为 exited，写入 credit_change -10 流水

---

### 场景三：互评与信誉查询

1. **提交互评**
   ```bash
   POST /api/m5/evaluations
   Headers: Authorization: Bearer <Token>
   Body: { "projectId": 123, "targetId": 20, "communicationScore": 4, "taskScore": 5, "skillScore": 4, "responsibilityScore": 5, "comment": "..." }
   ```

2. **查看收到的互评**
   ```bash
   GET /api/m5/evaluations/received?projectId=123
   Headers: Authorization: Bearer <Token>
   ```

3. **查看信誉分**
   ```bash
   GET /api/m5/credit/score
   Headers: Authorization: Bearer <Token>
   ```

4. **查看信誉流水**
   ```bash
   GET /api/m5/credit/changes?changeType=evaluation&page=1&pageSize=20
   Headers: Authorization: Bearer <Token>
   ```

---

## 错误码说明

### 通用错误码
| 错误码 | 说明 |
|--------|------|
| 00000 | 成功 |
| M1001 | 参数错误 |
| M1002 | 资源不存在 |
| M1003 | 状态冲突 |
| M9999 | 未知错误 |

### M4 模块专用错误码
| 错误码 | 说明 | 触发场景 |
|--------|------|----------|
| M4001 | 项目不存在 | 项目 ID 无效 |
| M4002 | 无队长权限 | 操作人不是队长 |
| M4003 | 项目状态不符合要求 | 状态流转条件不满足 |
| M4004 | 该用户不是项目的活跃成员 | 成员已退出或不在队 |
| M4005 | 请求或投票不存在 | team_request / exit_vote ID 无效 |
| M4006 | 重复操作 | 已存在相同 pending 请求或进行中投票 |
| M4007 | 投票已结束 | 对已关闭投票操作 |
| M4008 | 重复操作 | 该成员已投过票 |
| M4009 | 队长不能退出，请先转让队长身份 | 队长调用 self_exit |
| INVALID_PENALTY_LEVEL | 处罚级别缺失或不合法 | penaltyLevel 非 negotiated/malicious |
| TEAM_VOTE_CONFLICT | 退出操作并发冲突，请重试 | CAS 失败 |

### M3/M5 相关错误码
| 错误码 | 说明 |
|--------|------|
| M3000 | 未授权，请重新登录 |
| NOT_PROJECT_MEMBER | 当前用户不是该项目成员 |
| ALREADY_EVALUATED | 已评价过该成员 |
| SELF_EVALUATION | 不能评价自己 |
| PROJECT_NOT_ENDED | 项目尚未进入互评阶段 |
| EVAL_WINDOW_CLOSED | 互评窗口已关闭 |
| M5204~M5206 | 评分内容校验错误 |

---

## 常见问题

### 1. 接口返回 404
- 确认路径是否包含 `/api` 前缀（context-path 已配置为 `/api`）
- 确认 Apifox/Postman 的开发环境 Base URL 已设置为 `http://localhost:8080/api`

### 2. 开始/结束项目返回状态错误
- 检查当前项目状态是否满足流转条件（recruiting → in_progress → ended）
- 确认操作人（operatorId）是该项目的队长

### 3. 投票关闭后信誉未扣分
- 确认 result=pass（同意票数 > total_voters / 2）
- 检查是否出现 `TEAM_VOTE_CONFLICT`，说明并发 CAS 失败，team_member 状态已被其他操作修改

### 4. 互评提交返回 pending_review
- 属于正常状态，表示系统检测到疑似异常评价（全低分/全满分）
- 评价仍会保留，待管理员复核后决定是否生效计分

### 5. 信誉分未变化
- 检查 credit_change 记录中 `effective` 是否为 `true`
- `pending_review` 状态的互评对应流水 `effective=false`，不计入当前信誉分

---

## 开发与测试

### 运行 M4 相关测试
```bash
cd shixun12_backend
mvn -Dtest=ProjectServiceImplTest,TeamRequestServiceImplTest,ExitVoteServiceTest,CreditRuleServiceImplTest,ProjectControllerTest,TeamRequestControllerTest,ExitVoteControllerTest test
```

### 运行全量测试
```bash
mvn -DforkCount=0 test
```

### 启动服务
```bash
mvn spring-boot:run
```

### 配置文件
- `src/main/resources/application.yml`：主配置文件（context-path、数据库、Redis）
- `src/main/resources/sql/schema.sql`：建表语句
- `src/main/resources/sql/m4_demo_data.sql`：M4 演示数据

---

## 注意事项

1. **operatorId 来源**：当前接口通过 Query 参数传入 operatorId，生产环境应从 JWT Token 解析，M5 互评接口已采用 Token 方式
2. **演示数据**：运行 `m4_demo_data.sql` 前需确保 user 表中已预置 userId=10/20/30/40/50/60 等用户（由 M3 负责提供）
3. **并发安全**：selfExit 和 closeVote 均使用 team_member CAS 保证幂等，客户端遇到 `TEAM_VOTE_CONFLICT` 可重试一次
4. **credit_change 写入**：退出扣分流水由 M4 直接写入（T-146 联调后），互评加分流水由 M5 负责写入
5. **Lombok 已启用**：实体类和 DTO 使用 `@Data` 注解自动生成 Getter/Setter
6. **互评接口 evaluatorId**：评价人身份严格从 Token 提取，请求体中不接受 evaluatorId 字段

---

## 联系方式

如有问题，请联系 M4 模块负责人（后端二）。