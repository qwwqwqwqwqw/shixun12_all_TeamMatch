# M3 模块（用户认证与档案管理）API 文档与使用说明

## 概述

M3 模块负责 TeamMatch 平台的用户认证和个人档案管理功能。本模块提供基于微信 Mock 登录、邮箱验证、密码管理、用户名绑定以及个人档案维护等核心功能。

---

## 技术栈

- **Spring Boot 3.2.5**
- **MyBatis-Plus 3.5.5**
- **JWT (io.jsonwebtoken 0.11.5)**
- **Redis (邮箱验证码存储)**
- **BCrypt (密码加密)**
- **MySQL 8.0**

---

## 基础配置

### 服务地址
- **Base URL**: `http://localhost:8080/api`
- **Context Path**: `/api`（已在 application.yml 中配置）

### 公共请求头
```
Content-Type: application/json
Authorization: Bearer <JWT Token>  (需要认证的接口)
```

---

## 接口列表

### 一、认证模块 (`/auth`)

#### 1. 微信登录
**接口**: `POST /api/auth/login`

**说明**: 真实微信登录。前端调用 `wx.login()` 获取临时 code，后端通过微信 `code2session` 接口换取真实 `openid` 后识别用户。若用户不存在则自动创建。

**请求 Body**:
```json
{
  "code": "wx_code_from_wx_login"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "id": 1,
    "nickname": "User_xxxxx",
    "avatarUrl": null,
    "emailVerified": false,
    "formalProfileCompleted": false,
    "creditScore": 100,
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**注意**:
- 返回的是 `LoginResponse` DTO，不包含敏感字段（passwordHash、role、status等）
- `session_key` 由微信返回但后端**不持久化**（仅用于可能的解密场景，本项目目前不需要）
- `formalProfileCompleted` 表示正式档案是否完成（邮箱验证 + 昵称 + 学校）

**错误码**:
- `M3025` — 微信登录失败（code 无效/过期、微信服务器异常等）

---

#### 2. Mock 登录（开发测试用）
**接口**: `POST /api/auth/login/mock`

**说明**: 模拟微信登录，**不调微信接口**，直接把 code 当作 openid 识别用户。仅用于开发测试，生产环境不暴露。

**请求 Body**:
```json
{
  "code": "test_openid_123"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "id": 1,
    "nickname": "User_test_",
    "avatarUrl": null,
    "emailVerified": false,
    "formalProfileCompleted": false,
    "creditScore": 100,
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**注意**:
- 与微信登录返回结构相同，方便前端统一处理
- **生产环境应删除此接口**或加 IP 白名单

---

#### 3. 发送邮箱验证码
**接口**: `POST /api/auth/email/send`

**说明**: 向指定邮箱发送验证码（当前为控制台打印，实际项目需接入邮件服务）。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**请求 Body**:
```json
{
  "email": "student@example.edu.cn"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

---

#### 4. 验证邮箱验证码
**接口**: `POST /api/auth/email/verify`

**说明**: 验证邮箱验证码并绑定邮箱到当前用户。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**请求 Body**:
```json
{
  "email": "student@example.edu.cn",
  "code": "123456"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": "邮箱验证成功"
}
```

---

#### 5. 管理员创建用户密码
**接口**: `POST /api/auth/password/create`

**说明**: 为新用户设置密码，若用户不存在则自动创建。**需要管理员权限**。

**请求 Headers**:
```
Authorization: Bearer <管理员Token>
```

**请求 Body**:
```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

**业务规则**:
- **需要管理员权限**（role=admin），非管理员返回 `M3009` 错误
- 密码长度至少 6 位，否则返回 `M3007` 错误
- 若用户已设置密码，会提示使用修改接口
- 密码使用 BCrypt 加密存储

---

#### 6. 管理员修改用户密码
**接口**: `POST /api/auth/password/update`

**说明**: 修改已有用户的密码，可选择验证旧密码。**需要管理员权限**。

**请求 Headers**:
```
Authorization: Bearer <管理员Token>
```

**请求 Body**:
```json
{
  "username": "zhangsan",
  "oldPassword": "123456",
  "newPassword": "new_password_123"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

**业务规则**:
- **需要管理员权限**（role=admin），非管理员返回 `M3009` 错误
- 若提供 `oldPassword`，会先校验旧密码是否正确，错误返回 `M3008`
- 新密码长度至少 6 位，否则返回 `M3007` 错误

---

#### 7. 用户首次绑定用户名
**接口**: `POST /api/auth/username/bind`

**说明**: 微信登录用户首次绑定用户名（之前未设置过 username）。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**请求 Body**:
```json
{
  "username": "zhangsan"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

**业务规则**:
- 用户名不能为空
- 若已绑定过用户名，返回 `M3006` 错误，提示使用修改接口
- 检查用户名是否被其他用户占用，占用返回 `M3005` 错误

---

#### 8. 用户修改用户名
**接口**: `PUT /api/auth/username/update`

**说明**: 已绑定用户名的用户修改自己的用户名。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**请求 Body**:
```json
{
  "username": "new_username"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

---

#### 9. 密码登录
**接口**: `POST /api/auth/login/password`

**说明**: 使用用户名和密码进行登录。

**请求 Body**:
```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "id": 1,
    "nickname": "张三",
    "avatarUrl": null,
    "emailVerified": true,
    "formalProfileCompleted": true,
    "creditScore": 100,
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**业务规则**:
- 用户名不能为空，否则返回 `M3011` 错误
- 若用户未设置密码，返回 `M3014` 错误
- 密码错误返回 `M3013` 错误
- 登录成功后返回 `LoginResponse` DTO

---

#### 10. 用户修改密码
**接口**: `PUT /api/auth/password/change`

**说明**: 用户自主修改自己的密码，需要验证旧密码。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**请求 Body**:
```json
{
  "oldPassword": "123456",
  "newPassword": "new_password_123"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

**业务规则**:
- 必须提供 Token，验证用户身份
- 旧密码不正确返回 `M3008` 错误
- 新密码长度至少 6 位，否则返回 `M3007` 错误
- 新密码与旧密码不能相同

---

### 二、个人档案模块 (`/profile`)

#### 1. 更新个人档案
**接口**: `PUT /api/profile/update`

**说明**: 更新用户的个人信息。当邮箱已验证且填写了昵称和学校时，`formalProfileCompleted` 自动变为 `true`。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**请求 Body**:
```json
{
  "nickname": "张三",
  "avatarUrl": "https://example.com/avatar.jpg",
  "school": "清华大学",
  "major": "计算机科学与技术",
  "grade": "2023级",
  "bio": "热爱编程，寻找队友"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

---

#### 2. 绑定 GitHub 账号
**接口**: `POST /api/profile/github/bind`

**说明**: 首次绑定用户的 GitHub 用户名。已绑定请使用 `PUT /api/profile/github/update` 换绑。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**请求 Body**:
```json
{
  "githubUsername": "your-github-name"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

**业务规则**:
- 仅用于**首次绑定**，若已绑定过 GitHub 返回 `M3030` 错误
- 绑定前检查 GitHub 用户名是否存在，不存在返回 `M3029` 错误
- 绑定后自动创建/认领技术画像，异步拉取 GitHub 数据

**错误码**:
- `M3029` — GitHub 用户不存在
- `M3030` — 已绑定 GitHub 账号，请使用更新接口

---

#### 3. 更新 GitHub 账号（换绑）
**接口**: `PUT /api/profile/github/update`

**说明**: 更新已绑定的 GitHub 用户名。仅当用户已经绑定过 GitHub 账号时可修改。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**请求 Body**:
```json
{
  "githubUsername": "new-github-name"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

**业务规则**:
- 必须先调用 `POST /api/profile/github/bind` 完成首次绑定
- 若未绑定过 GitHub，返回 `M3012` 错误
- 若新 GitHub 用户名不存在，返回 `M3029` 错误
- 换绑后自动释放旧 GitHub 的技术画像认领，创建/认领新 GitHub 的技术画像
- 一个 GitHub 账号只能被一个用户认领
- 换绑后自动触发异步同步拉取新 GitHub 数据

**错误码**:
- `M3012` — GitHub 账号未绑定，请先使用绑定接口
- `M3029` — GitHub 用户不存在

---

#### 3.1 获取当前用户技术画像
**接口**: `GET /api/profile/tech-profile`

**说明**: 获取当前登录用户的技术画像（基于 GitHub 数据分析），包含技术评分和语言分布等。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "id": 1,
    "githubUsername": "zhangsan-github",
    "claimedByUserId": 2,
    "claimedByUserNickname": "张三",
    "totalStars": 245,
    "totalRepos": 12,
    "totalCommits": 1523,
    "totalPrs": 38,
    "totalContributions": 450,
    "topLanguages": "[\"Java\",\"Python\",\"TypeScript\"]",
    "techScore": 5128,
    "syncStatus": "synced",
    "bio": "Full-stack developer",
    "avatarUrl": "https://avatars.githubusercontent.com/u/zhangsan",
    "lastSyncedAt": "2025-01-15T10:30:00",
    "claimed": true
  }
}
```

**错误码**:
- `M3026` — 技术画像不存在，请先绑定 GitHub 账号

---

#### 3.2 获取指定用户技术画像
**接口**: `GET /api/profile/tech-profile/{userId}`

**说明**: 获取指定用户的技术画像，无需登录。

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |

**响应**: 同上 `TechProfileVO` 结构

**错误码**:
- `M3026` — 技术画像不存在

---

### 三、Gitee OAuth 模块 (`/profile/gitee`)

#### 1. 跳转 Gitee OAuth 授权页
**接口**: `GET /api/profile/gitee/auth`

**说明**: 跳转到 Gitee OAuth 授权页面，用户在 Gitee 侧授权后回调到 `/api/profile/gitee/callback`。推荐使用此 OAuth 流程绑定 Gitee（可获取真实 commits/PRs 数据），手动绑定接口只能验证用户名。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**响应**: 302 重定向到 `https://gitee.com/oauth/authorize?...`

---

#### 2. OAuth 回调
**接口**: `GET /api/profile/gitee/callback?code=xxx&state=userId`

**说明**: Gitee 授权成功后回调此接口，后端用 code 换取 access_token，获取用户信息，创建/认领技术画像，异步同步数据。

**请求参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| code | String | Gitee 返回的授权码 |
| state | String | 用户 ID（由 auth 接口传入） |

**响应**: 重定向到成功/失败页面

---

#### 3. 绑定成功页面
**接口**: `GET /api/profile/gitee/success`

**说明**: 返回绑定成功的 HTML 页面，通过 `window.opener.postMessage` 通知父窗口关闭。

---

#### 4. 手动绑定 Gitee（非 OAuth）
**接口**: `POST /api/profile/gitee/bind`

**说明**: 手动绑定 Gitee 用户名（通过公开 API 验证）。推荐使用 OAuth 流程，此接口仅作为备选。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**请求 Body**:
```json
{
  "giteeUsername": "your-gitee-name"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

**业务规则**:
- 仅用于**首次绑定**，若已绑定过 Gitee 返回 `M3032` 错误
- 绑定前检查 Gitee 用户名是否存在，不存在返回 `M3031` 错误
- 手动绑定无法获取真实 commits/PRs 数据（需要 OAuth 授权）

**错误码**:
- `M3031` — Gitee 用户不存在
- `M3032` — 已绑定 Gitee 账号，请使用更新接口

---

#### 5. 更新 Gitee 账号（换绑）
**接口**: `PUT /api/profile/gitee/update`

**说明**: 更新已绑定的 Gitee 用户名。仅当用户已经绑定过 Gitee 账号时可修改。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**请求 Body**:
```json
{
  "giteeUsername": "new-gitee-name"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

**错误码**:
- `M3033` — Gitee 账号未绑定，请先使用绑定接口
- `M3031` — Gitee 用户不存在

---

### 排行榜接口（板块二 冷启动）

#### 1. 获取排行榜
**接口**: `GET /api/leaderboard?page=1&size=20`

**说明**: 按技术评分（techScore）降序排列，展示所有技术画像的排名。未认领的画像也会展示（不含用户信息）。

**请求参数**:
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码（从 1 开始） |
| size | int | 20 | 每页条数（1~100） |

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": [
    {
      "rank": 1,
      "userId": 2,
      "nickname": "张三",
      "avatarUrl": "https://...",
      "school": "清华大学",
      "githubUsername": "zhangsan-github",
      "techScore": 5128,
      "totalStars": 245,
      "totalRepos": 12,
      "totalCommits": 1523,
      "totalPrs": 38,
      "totalContributions": 450,
      "source": "github",
      "topLanguages": "[\"Java\",\"Python\",\"TypeScript\"]",
      "bio": "Full-stack developer",
      "claimed": true
    },
    {
      "rank": 2,
      "userId": null,
      "nickname": null,
      "githubUsername": "torvalds",
      "techScore": 10000000,
      "totalStars": 999999,
      "claim": false
    }
  ]
}
```

**技术评分公式**:
```
techScore = totalStars × 10 + totalCommits × 2 + totalPRs × 5 + totalRepos × 3 + totalContributions × 1
```

---

#### 2. 获取排行榜总数
**接口**: `GET /api/leaderboard/count`

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": 5
}
```

---

### 三、用户中心 (`/me`)

#### 1. 获取用户列表（分页）
**接口**: `GET /api/users?page=1&size=20&keyword=张三`

**说明**: 分页获取所有用户信息，支持按昵称关键字搜索。返回 `ProfileDetailVO`，排除 `openid`、`passwordHash` 等敏感字段。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**请求参数**:
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码（从 1 开始） |
| size | int | 20 | 每页条数（1~100） |
| keyword | String | 可选 | 按昵称模糊搜索 |

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 1,
        "nickname": "张三",
        "avatarUrl": null,
        "email": null,
        "emailVerified": false,
        "school": "清华大学",
        "major": "计算机科学与技术",
        "grade": "2023级",
        "bio": "热爱编程",
        "githubUsername": null,
        "githubClaimed": false,
        "formalProfileCompleted": true,
        "creditScore": 100,
        "role": "user",
        "status": "active",
        "username": "zhangsan",
        "createdAt": "2026-01-01T00:00:00",
        "updatedAt": "2026-01-01T00:00:00"
      }
    ],
    "total": 1,
    "size": 20,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 2. 获取用户角标/待处理事项队列
**接口**: `GET /api/me/badges`

**说明**: 聚合统计当前用户待处理的事项数量，用于前端展示未读角标。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": {
    "pendingInvites": 2,
    "pendingVotes": 1,
    "pendingEvaluations": 0,
    "total": 3
  }
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| pendingInvites | int | 待处理的组队请求（邀请/申请）数量 |
| pendingVotes | int | 待处理的退出投票数量（未投票的） |
| pendingEvaluations | int | 待完成的互评数量（应评未评的） |
| total | int | 以上三项总和 |

---

#### 4. 全量替换用户技能（推荐）
**接口**: `PUT /api/profile/skills`

**说明**: 全量替换用户的技能标签列表。先删除旧技能，再插入新技能。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**请求 Body**:
```json
{
  "skillTagIds": [1, 2, 3]
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

**业务规则**:
- 传入空数组 `[]` 表示清空所有技能
- 所有 skillTagId 必须在 `skill_tag` 表中存在且状态为 `active`
- 若技能标签不存在或已禁用，返回 `M3010` 错误

---

#### 5. 获取技能标签列表
**接口**: `GET /api/profile/skills/tags`

**说明**: 获取所有激活状态的技能标签列表，供前端展示可选技能。

**请求**: 无需 Token（公开接口）

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": [
    {
      "id": 1,
      "name": "Java",
      "category": "language",
      "status": "active"
    },
    {
      "id": 2,
      "name": "Spring Boot",
      "category": "framework",
      "status": "active"
    }
  ]
}
```

**业务规则**:
- 只返回 `status='active'` 的技能标签
- 按 `category` 和 `name` 排序

---

#### 6. 添加技能标签（用户，旧接口）
**接口**: `POST /api/profile/skills/add`

**说明**: 为当前用户增量添加一个技能标签（保留兼容，推荐使用全量替换接口）。

**请求 Headers**:
```
Authorization: Bearer <Token>
```

**请求 Body**:
```json
{
  "skillTagId": 1
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

**业务规则**:
- 技能标签 ID 必须在 `skill_tag` 表中存在
- 避免重复添加同一技能标签
- 数据会插入到 `user_skill` 关联表

---

#### 7. 添加技能标签（管理员）
**接口**: `POST /api/profile/skills/tag/add`

**说明**: 向数据库插入新的技能标签，供后续用户关联使用。**需要管理员权限**。

**请求 Headers**:
```
Authorization: Bearer <管理员Token>
```

**请求 Body**:
```json
{
  "name": "Java",
  "category": "编程语言"
}
```

**响应**:
```json
{
  "code": "00000",
  "message": "成功",
  "data": null
}
```

**业务规则**:
- **需要管理员权限**（role=admin），非管理员返回 `M3009` 错误
- 名称不能为空
- 状态自动设为 `active`
- 插入到 `skill_tag` 表
- 若同名技能标签已存在，返回 `M3015` 错误

---
#### 8. 获取用户完整档案
**接口**: `GET /api/profile/detail`

**说明**: 获取当前用户的档案。返回 `ProfileDetailVO`（VO），排除 `openid`、`passwordHash` 等敏感字段。

**请求**: 需 Token

**响应**:
```json
{
    "code": "00000",
    "message": "成功",
    "data": {
        "id": 1,
        "nickname": "系统管理员",
        "avatarUrl": null,
        "email": "admin@teammatch.edu.cn",
        "emailVerified": true,
        "school": "TeamMatch大学",
        "major": null,
        "grade": null,
        "bio": null,
        "githubUsername": null,
        "githubClaimed": false,
        "formalProfileCompleted": true,
        "creditScore": 100,
        "role": "admin",
        "status": "active",
        "username": "admin",
        "createdAt": "2026-05-25T20:13:15",
        "updatedAt": "2026-05-25T20:13:15"
    },
    "fail": false,
    "success": true
}
```

---
#### 9. 获取指定用户的档案详情（供项目详情页显示用户名称用）
**接口**: `GET /api/profile/detail/{userId}`

**说明**: 根据 userId 获取任意用户的档案信息（不暴露 openid、passwordHash 等敏感字段）。
用于项目详情页等场景，显示队长/成员的昵称和头像。

**请求**:
```
需要 Token（任意已登录用户均可调用）
路径参数: userId - 目标用户的 ID
```

**响应**:
```json
{
    "code": "00000",
    "message": "成功",
    "data": {
        "id": 2,
        "nickname": "目标用户",
        "avatarUrl": "http://avatar.url",
        "email": null,
        "emailVerified": false,
        "school": null,
        "major": null,
        "grade": null,
        "bio": null,
        "githubUsername": null,
        "githubClaimed": false,
        "formalProfileCompleted": false,
        "creditScore": 100,
        "role": "user",
        "status": "active",
        "username": null,
        "createdAt": null,
        "updatedAt": null
    },
    "fail": false,
    "success": true
}
```

**业务规则**:
- 需要有效的登录 Token，否则返回 `M3000` 错误
- 若指定 userId 不存在，返回 `M1002` 错误
- 返回 `ProfileDetailVO`，不包含 `openid`、`passwordHash` 等敏感字段

## 数据表结构

### user 表（用户表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| openid | VARCHAR | 微信 openid（微信登录用户） |
| username | VARCHAR | 用户名（密码登录用户） |
| nickname | VARCHAR | 用户昵称 |
| avatar_url | VARCHAR | 头像 URL |
| email | VARCHAR | 学校邮箱 |
| email_verified | TINYINT | 邮箱是否已验证 |
| school | VARCHAR | 学校名称 |
| major | VARCHAR | 专业 |
| grade | VARCHAR | 年级 |
| bio | VARCHAR | 个人简介 |
| github_username | VARCHAR | GitHub 用户名 |
| github_claimed | TINYINT | GitHub 是否已认领 |
| gitee_username | VARCHAR | Gitee 用户名 |
| gitee_claimed | TINYINT | Gitee 是否已认领 |
| formal_profile_completed | TINYINT | 正式档案是否完成 |
| credit_score | INT | 信誉分 |
| role | VARCHAR | 用户角色（user/admin） |
| status | VARCHAR | 用户状态（active/banned） |
| password_hash | VARCHAR | 密码哈希值 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### skill_tag 表（技能标签表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| name | VARCHAR | 技能名称 |
| category | VARCHAR | 分类 |
| status | VARCHAR | 状态（active/inactive） |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### user_skill 表（用户技能关联表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| user_id | BIGINT | 用户 ID |
| skill_tag_id | BIGINT | 技能标签 ID |

---

## 使用流程示例

### 场景一：微信登录用户完整流程

1. **登录获取 Token（开发测试用 Mock 登录）**
   ```bash
   POST /api/auth/login/mock
   Body: { "code": "test_openid_123" }
   ```
   > 生产环境使用 `POST /api/auth/login` 走真实微信登录

2. **绑定邮箱**
   ```bash
   # 发送验证码
   POST /api/auth/email/send
   Headers: Authorization: Bearer <Token>
   Body: { "email": "student@example.edu.cn" }
   
   # 验证验证码（控制台查看验证码）
   POST /api/auth/email/verify
   Headers: Authorization: Bearer <Token>
   Body: { "email": "student@example.edu.cn", "code": "123456" }
   ```

3. **绑定用户名**
   ```bash
   POST /api/auth/username/bind
   Headers: Authorization: Bearer <Token>
   Body: { "username": "zhangsan" }
   ```

4. **完善个人档案**
   ```bash
   PUT /api/profile/update
   Headers: Authorization: Bearer <Token>
   Body: {
     "nickname": "张三",
     "school": "清华大学",
     "major": "计算机",
     "grade": "2023级",
     "bio": "寻找队友"
   }
   ```

5. **添加技能标签**
   ```bash
   # 先获取所有可用技能标签
   GET /api/profile/skills/tags
   
   # 全量替换用户技能（推荐）
   PUT /api/profile/skills
   Headers: Authorization: Bearer <Token>
   Body: { "skillTagIds": [1, 2, 3] }
   
   # 或者使用旧接口增量添加（不推荐）
   POST /api/profile/skills/add
   Headers: Authorization: Bearer <Token>
   Body: { "skillTagId": 1 }
   ```

6. **绑定 GitHub**
   ```bash
   POST /api/profile/github/bind
   Headers: Authorization: Bearer <Token>
   Body: { "githubUsername": "your-github" }
   ```

---

### 场景二：管理员创建密码登录用户

1. **创建用户密码**
   ```bash
   POST /api/auth/password/create
   Body: { "username": "admin", "password": "admin123" }
   ```

2. **修改密码（可选）**
   ```bash
   POST /api/auth/password/update
   Body: { 
     "username": "admin", 
     "oldPassword": "admin123",
     "newPassword": "new_admin123" 
   }
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

### M3 模块专用错误码
| 错误码 | 说明 | 触发场景 |
|--------|------|----------|
| M3001 | 需要先完成正式档案 | 访问需要正式档案的接口 |
| M3002 | 该邮箱已被其他用户绑定 | 邮箱验证时邮箱已被占用 |
| M3003 | 验证码错误或已过期 | 邮箱验证码不正确 |
| M3004 | 发送验证码过于频繁 | 短时间内多次发送验证码 |
| M3005 | 该用户名已被占用 | 绑定/修改用户名时重复 |
| M3006 | 用户名已绑定，请使用修改接口 | 重复调用 bind 接口 |
| M3007 | 密码长度不能少于6位 | 密码太短 |
| M3008 | 旧密码不正确 | 修改密码时旧密码错误 |
| M3009 | 需要管理员权限 | 非管理员调用管理接口 |
| M3010 | 技能标签不存在或已禁用 | 使用无效的技能ID |
| M3011 | 用户名不能为空 | 密码登录时未提供用户名 |
| M3012 | GitHub 账号未绑定 | 尝试更新未绑定的 GitHub |
| M3013 | 用户名或密码错误 | 密码登录时凭证错误 |
| M3014 | 该用户未设置密码 | 未设置密码的用户尝试密码登录 |
| M3015 | 技能标签已存在 | 添加同名技能标签时重复 |
| M3025 | 微信登录失败，请重试 | code 无效/过期、微信服务器异常等 |
| M3026 | 技术画像不存在，请先绑定 GitHub 账号 | 用户未认领技术画像或未绑定 GitHub |
| M3027 | 该 GitHub 账号已被其他用户认领 | 尝试认领已被他人认领的 GitHub 账号 |
| M3031 | Gitee 用户不存在 | 绑定时 Gitee API 找不到该用户 |
| M3032 | 已绑定 Gitee 账号，请使用更新接口 | 重复调用 Gitee 绑定接口 |
| M3033 | Gitee 账号未绑定，请先使用绑定接口 | 尝试更新未绑定的 Gitee 账号 |

---

## Gitee OAuth 授权完整流程

### 流程概览

```
┌─ 小程序端 ──────────────────────────────────────────┐
│                                                       │
│  个人档案页 → 点击「🔐 Gitee OAuth 授权（推荐）」     │
│       │                                               │
│       ▼                                               │
│  GET /api/profile/gitee/auth-url                     │
│       │                                               │
│       ▼ 返回 { authUrl, state }                       │
│  复制授权链接到剪贴板                                  │
│       │                                               │
│       ▼ 用户在手机浏览器中粘贴打开                     │
│  ┌──────────────┐                                     │
│  │  Gitee 登录   │  ← 浏览器中操作                    │
│  │  Gitee 授权   │                                     │
│  └──────┬───────┘                                     │
│         │ Gitee 回调                                   │
│         ▼                                              │
│  GET /api/profile/gitee/callback?code=xxx&state=userId │
│         │                                              │
│         ▼ 后端处理                                      │
│  ① 用 code 换 access_token                             │
│  ② 用 token 拉取 Gitee 用户信息 + 仓库数据             │
│  ③ 创建/认领 tech_profile（source=gitee）              │
│  ④ 更新 user 表 gitee_username / gitee_claimed         │
│  ⑤ 异步同步 stars/repos/languages/commits/PRs         │
│         │                                              │
│         ▼ 302 → /mp-success（✅ 授权成功页面）         │
│                                                       │
│  用户切回小程序 → 个人档案页                            │
│  → 点击「🔄 检查授权状态」                              │
│  → GET /api/profile/detail → giteeClaimed=true → 完成  │
│                                                       │
└───────────────────────────────────────────────────────┘
```

### 绑定方式对比

| | 手动绑定 | OAuth 授权 |
|---|---|---|
| **接口** | `POST /profile/gitee/bind` | `GET /profile/gitee/auth-url` → 浏览器授权 → callback |
| **验证方式** | Gitee 公开 API 查用户名 | Gitee OAuth 2.0 标准流程 |
| **Stars/Repos/Languages** | ✅ 公开 API | ✅ 含私有仓库 |
| **真实 commits** | ❌ 估算值 (stars×20+repos×5) | ✅ 通过 token 获取 |
| **真实 PRs** | ❌ 估算值 (repos×2) | ✅ 通过 token 获取 |
| **小程序适配** | ✅ 直接输入用户名 | ✅ 复制链接到浏览器（绕过 web-view 限制） |

### 数据同步流程

```
OAuth 回调拿到 access_token
  │
  ├─ fetchGiteeUser(token)
  │   └─ GET /api/v5/user?access_token=xxx
  │       → avatar_url, bio, login
  │
  └─ fetchGiteeReposWithToken(token)
      └─ GET /api/v5/user/repos?access_token=xxx&type=all
          │
          ├─ 遍历仓库（跳过 fork）
          │   ├─ stargazers_count → totalStars
          │   ├─ language → topLanguages
          │   ├─ commits count → totalCommits
          │   └─ PRs count → totalPrs
          │
          └─ computeTechScore()
              = stars×10 + commits×2 + prs×5 + repos×3 + contributions×1
```

### 排行榜展示

Gitee 和 GitHub 技术画像**混合排名**，按 `tech_score DESC` 统一排序：

```json
{
  "rank": 8,
  "nickname": "李四",
  "githubUsername": "xiaocaozi666",
  "source": "gitee",        // ← 区分来源
  "techScore": 537,
  "totalStars": 6,
  "totalRepos": 7,
  "topLanguages": "[Java, C++]",
  "claimed": true
}
```

---

## 常见问题

### 1. 接口返回 404
- 检查路径是否包含前导 `/`
- 确认 Apifox 开发环境是否配置了前置路径 `/api`
- 重启服务确保路由已加载

### 2. Token 无效
- 确认 Token 格式：`Bearer <token>`（Bearer 后有空格）
- Token 有效期为 7 天，过期需重新登录

### 3. 技能标签添加失败
- 确保 `skill_tag` 表中存在对应的 ID
- 技能标签状态必须为 `active`
- 使用管理员接口先插入技能标签

### 4. 邮箱验证失败
- 检查验证码是否正确（当前在控制台打印）
- 确认邮箱未被其他用户占用

---

## 开发与测试

### 运行测试
```bash
cd backend/shixun12_backend
mvn clean test
```

### 启动服务
```bash
cd backend/shixun12_backend
mvn spring-boot:run
```

### 配置文件
- `application.yml`: 主配置文件
- `application-local.yml`: 本地环境配置（数据库密码等）

---

## 注意事项

1. **Lombok 已启用**：实体类和 DTO 使用 `@Data` 注解自动生成 Getter/Setter
2. **密码加密**：所有密码使用 BCrypt 加密存储，不可逆
3. **JWT 密钥**：当前使用固定密钥，生产环境建议改为环境变量
4. **Redis 依赖**：邮箱验证码存储在 Redis 中，确保 Redis 服务已启动
5. **数据库编码**：JDBC URL 使用 `utf8` 编码，避免 utf8mb4 兼容性问题
6. **管理员权限**：密码管理接口需要 role=admin 的 Token，否则返回 M3009 错误
7. **登录响应**：登录接口返回 `LoginResponse` DTO，不包含敏感字段（passwordHash、role等）
8. **技能标签**：推荐使用全量替换接口 `PUT /api/profile/skills`，旧接口保留兼容

---

## 联系方式

如有问题，请联系 M3 模块负责人。
