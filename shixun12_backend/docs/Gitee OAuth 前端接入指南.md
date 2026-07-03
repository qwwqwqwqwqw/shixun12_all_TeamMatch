# Gitee OAuth 授权前端接入文档

## 为什么要接 OAuth

目前 Gitee 绑定方式是手动输入用户名（`POST /profile/gitee/bind`），后端用公开 API 只能拿到 stars / repos / languages，**commits 和 PRs 是估算值**。

OAuth 授权后能拿到真实数据：

| 指标 | 手动绑定 | OAuth 授权 |
|------|----------|------------|
| Stars / Repos / Languages | ✅ 真实 | ✅ 真实 |
| Commits 数 | ❌ 公式估算 | ✅ 真实 |
| PRs 数 | ❌ 公式估算 | ✅ 真实 |
| 访问私有仓库 | ❌ | ✅ |

---

## 整体流程

分为 3 步，小程序端 + 浏览器 + 后端配合完成：

**第 1 步：获取授权链接（小程序 → 后端）**
- 用户点击「OAuth 授权」
- 前端调 `GET /profile/gitee/auth-url`，拿到 `{ authUrl }`
这里如果能用小程序直接打开authUrl最好
（不行的话就用 `wx.setClipboardData` 将 `authUrl` 复制到剪贴板，然后，弹窗提示用户去手机浏览器中粘贴打开）

**第 2 步：授权**
用户在打开的网址里进行授权
能在小程序内部实现最好，就不需要跳转到浏览器里面操作了
- 用户在浏览器中粘贴链接，打开 Gitee 授权页
- 登录 Gitee → 点击授权
- Gitee 回调后端 `/profile/gitee/callback?code=xxx`
- 后端用 code 换 access_token → 拉取用户仓库数据 → 创建技术画像 → 跳转到 `/mp-success`
- 浏览器显示"✅ 授权成功，请返回小程序"

**第 3 步：检查结果（小程序 → 后端）**
- 用户切回小程序，点击「检查授权状态」
- 前端调 `GET /profile/detail`，看 `giteeClaimed` 是否为 `true`（这个是关键）
- 为 `true` → 显示绑定成功，刷新页面
- 为 `false` → 提示"尚未完成，请再试"

---

## 后端接口说明

### 获取授权链接

```
GET /api/profile/gitee/auth-url
Authorization: Bearer <token>
```

**响应：**

```json
{
  "code": "00000",
  "data": {
    "authUrl": "https://gitee.com/oauth/authorize?client_id=xxx&redirect_uri=...&state=3",
    "state": "3"
  }
}
```

### 检查绑定状态

```
GET /api/profile/detail
Authorization: Bearer <token>
```

**响应中相关字段：**

```json
{
  "giteeUsername": "xxx",
  "giteeClaimed": true
}
```

`giteeClaimed` 为 `true` 即表示 OAuth 授权已完成。

### 其他接口

手动绑定和换绑的接口不变，与 OAuth 互不冲突：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/profile/gitee/bind` | POST | 手动输入用户名绑定 |
| `/profile/gitee/update` | PUT | 换绑 |

### 错误码

| 错误码 | 含义 |
|--------|------|
| `M3031` | Gitee 用户不存在 |
| `M3032` | 已绑定过 Gitee，请用更新接口 |
| `M3033` | Gitee 未绑定 |

---

## 小程序端接入步骤

### 1. API 层

在 API 服务中新增两个方法：

- `getGiteeAuthUrl()` — 调 `GET /profile/gitee/auth-url`，返回 `authUrl`
- `getProfileDetail()` — 调 `GET /profile/detail`，检查 `giteeClaimed`

### 2. 绑定入口

加一个gitee认证

- **手动输入**：走现有 `POST /profile/gitee/bind` 逻辑
- **OAuth 授权**：走第 3 步流程

### 3. OAuth 授权交互

```
点「OAuth 授权」→ 调 getGiteeAuthUrl()
  → 拿到 authUrl
  → wx.setClipboardData({ data: authUrl })
  → 弹窗提示："授权链接已复制到剪贴板，请在手机浏览器中粘贴并打开，登录 Gitee 完成授权后返回"
```


> 关于 WebView 的具体实现方式由前端团队自行决定，上述方案已验证可行。

### 4. 检查授权状态

用户从浏览器返回小程序后，提供两种检查方式：

**方式 A：手动检查（推荐）**

在认证区域显示一个按钮：

```
[🔄 检查授权状态]
```

按钮逻辑：调 `GET /profile/detail`，判断 `giteeClaimed`：
- `true` → 显示"授权成功"，刷新页面
- `false` → 提示"尚未完成授权，请在浏览器中完成后再试"

**方式 B：自动轮询（可选）**

用户点 OAuth 后，每 3~5 秒自动检查一次，60 秒超时：

```
点 OAuth → 显示"等待授权..."
         → 每 5 秒调一次 /profile/detail
         → giteeClaimed=true → 提示成功，刷新页面
         → 60 秒超时 → 提示"请手动检查"
```

### 5. 授权后页面

用户在浏览器完成授权后，Gitee 会回调后端，后端处理后跳转到 `/api/profile/gitee/mp-success`，显示：

```
┌──────────────────────────┐
│          ✅              │
│    Gitee 授权成功        │
│                          │
│  数据正在同步中，请返回   │
│  TeamMatch 小程序        │
│  进入个人档案页查看      │
└──────────────────────────┘
```

该页面由后端渲染，小程序端不需要额外处理。

---

## 差异对比：手动绑定 vs OAuth 授权

| | 手动绑定 | OAuth 授权 |
|--|----------|------------|
| 用户操作 | 输入用户名 | 复制链接 → 浏览器打开 → 授权 |
| 交互步骤 | 1 步 | 3 步（稍多但一次性的） |
| 实现复杂度 | 简单 | 中等 |
| Stars / Repos | ✅ | ✅ |
| Commits 数 | ❌ 估算值 | ✅ 真实数据 |
| PRs 数 | ❌ 估算值 | ✅ 真实数据 |
| 私有仓库 | ❌ | ✅ |

两种方式可以并存，用户自己选择。
