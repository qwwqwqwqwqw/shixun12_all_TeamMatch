# T-003 Token 存储规则对齐记录

**对齐双方**：M1（前端一）, M3（后端一）
**对齐依据**：详细设计文档 V2.1、M3 API 文档、M2 首页实现代码

---

## 对齐结论

### 1. Token 存储
- **存储位置**：微信小程序 `localStorage`
- **存储键名**：`token`
- **存储方法**：`wx.setStorageSync('token', tokenValue)`

### 2. Token 携带
- **请求头名称**：`Authorization`
- **携带格式**：`Bearer <token>`（注意 Bearer 后有一个空格）

### 3. Token 有效期
- **有效期**：7 天
- **过期策略**：P0 不实现 refresh token，过期后需重新登录

### 4. 401 处理
- 当前端收到 HTTP 状态码 `401` 时：
    1. 清除本地 `token` 和用户信息缓存
    2. 跳转至登录/欢迎页（`pages/login/login`）

### 5. 退出登录
- 调用 `wx.removeStorageSync('token')` 清除 Token
- 同时清除 `nickname`、`profile` 等用户缓存
- 跳转至登录/欢迎页

---

## 产出物状态
✅ 已完成，可作为 T-003 任务证据。