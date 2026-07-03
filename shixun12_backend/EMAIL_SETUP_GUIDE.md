# 邮箱验证码发送功能配置指南

## 📧 功能说明

已实现真正的邮箱验证码发送功能，使用 Spring Boot Mail 通过 SMTP 协议发送邮件。

## ⚙️ 配置步骤

### 1. 配置邮件服务器参数

在 `application-local.yml` 或环境变量中配置以下参数：

```yaml
spring:
  mail:
    host: smtp.qq.com          # SMTP 服务器地址
    port: 587                   # SMTP 端口（TLS）
    username: your_email@qq.com # 发件人邮箱
    password: your_auth_code    # 授权码（不是邮箱密码！）
```

### 2. 获取邮箱授权码

#### QQ 邮箱
1. 登录 QQ 邮箱网页版
2. 进入「设置」→「账户」
3. 找到「POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务」
4. 开启「IMAP/SMTP服务」
5. 点击「生成授权码」，按提示操作
6. 复制生成的授权码，填入配置中的 `password` 字段

#### 163 邮箱
1. 登录 163 邮箱网页版
2. 进入「设置」→「POP3/SMTP/IMAP」
3. 开启「IMAP/SMTP服务」
4. 点击「客户端授权密码」，设置授权码
5. 使用授权码作为配置中的 `password`

#### Gmail
1. 登录 Google 账户
2. 进入「安全性」→「两步验证」
3. 开启两步验证后，进入「应用专用密码」
4. 生成应用专用密码
5. 使用该密码作为配置中的 `password`
6. SMTP 配置：
   - host: smtp.gmail.com
   - port: 587

### 3. 常用邮箱 SMTP 配置参考

| 邮箱服务商 | SMTP 服务器 | 端口 | 加密方式 |
|-----------|------------|------|---------|
| QQ 邮箱 | smtp.qq.com | 587 | STARTTLS |
| 163 邮箱 | smtp.163.com | 587 | STARTTLS |
| Gmail | smtp.gmail.com | 587 | STARTTLS |
| Outlook | smtp-mail.outlook.com | 587 | STARTTLS |

## 🧪 测试方法

### 1. 启动服务

```bash
cd d:\shixun12\backend\shixun12_backend
mvn spring-boot:run
```

### 2. 调用发送验证码接口

使用 Postman 或其他 API 测试工具：

**请求：**
```
POST http://localhost:8080/api/auth/email/send
Headers:
  Authorization: Bearer <your_jwt_token>
  Content-Type: application/json

Body:
{
  "email": "test@example.com"
}
```

### 3. 验证结果

- ✅ 成功：收到格式精美的 HTML 邮件，包含 6 位验证码
- ❌ 失败：检查日志中的错误信息，常见错误：
  - `Authentication failed`：授权码错误或未开启 SMTP 服务
  - `Connection timed out`：网络问题或防火墙阻止
  - `Mail server connection failed`：SMTP 配置错误

## 📝 注意事项

1. **授权码 ≠ 邮箱密码**：必须使用邮箱服务商提供的授权码
2. **安全存储**：不要将授权码提交到 Git，使用环境变量或配置文件管理
3. **频率限制**：邮箱服务商通常有发送频率限制，建议添加限流机制
4. **垃圾邮件**：频繁发送可能被标记为垃圾邮件，注意控制发送频率
5. **开发环境**：开发时可以使用 MailHog 等本地邮件测试工具

## 🔧 开发环境替代方案

如果暂时没有可用的 SMTP 服务器，可以：

1. **使用 MailHog（推荐）**
   ```bash
   docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
   ```
   配置：
   ```yaml
   spring:
     mail:
       host: localhost
       port: 1025
       username: ""
       password: ""
   ```
   访问 http://localhost:8025 查看邮件

2. **临时使用控制台输出**
   修改 `EmailServiceImpl.java`，在 catch 块中添加：
   ```java
   log.warn("邮件发送失败，验证码: {}", code);
   ```

## 📊 邮件内容预览

发送的邮件包含：
- 🎨 精美的 HTML 格式
- 📱 响应式设计，支持移动端
- 🔢 醒目的 6 位验证码显示
- ⏰ 有效期提示（10 分钟）
- ⚠️ 安全警告信息

## 🚀 下一步优化建议

1. 添加邮件发送失败重试机制
2. 实现邮件模板管理（Thymeleaf）
3. 添加发送频率限制（防止滥用）
4. 记录邮件发送日志
5. 支持自定义邮件主题和内容
