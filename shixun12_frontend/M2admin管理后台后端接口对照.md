
以下是完整的 M2 管理后台接口对照清单。

---

# M2 管理后台接口对照清单

## 统一响应格式

所有接口返回 `Result<T>` 结构：

```json
{
  "code": "00000",     // 成功为 "00000"，失败为错误码
  "message": "成功",   // 提示信息
  "data": T            // 业务数据
}
```

> ⚠️ **成功码是 `"00000"`（字符串），不是 `200`**

---

## 全局确认项

| 项目 | 统一规范 |
|------|---------|
| **全局前缀** | 所有接口以 `/api` 开头 |
| **认证方式** | `Authorization: Bearer {token}`（所有管理接口都是必填） |
| **权限校验** | 所有管理接口调用 `authUtil.requireAdmin(token)`，role 必须是 `"admin"` |
| **Admin Token 获取** | `POST /api/auth/login/password`（管理员用户名+密码登录） |
| **字段命名** | 驼峰 camelCase |
| **时间格式** | `yyyy-MM-ddTHH:mm:ss`（ISO 8601） |
| **无分页** | 所有管理列表接口均返回全量列表（`List<T>`），不带分页参数 |

---

## 一、板块管理 — BoardController.java

### 1. 获取板块列表（管理端）

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `GET` |
| **完整路径** | `/api/admin/boards` |
| **路径参数** | 无 |
| **Query 参数** | 无 |
| **Body 参数** | 无 |
| **成功响应 data** | `Board[]`（数组） |

**Board 实体字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 板块ID |
| `name` | String | 板块名称 |
| `description` | String | 板块描述 |
| `status` | String | 状态：`active`/`inactive` |
| `sortOrder` | Integer | 排序值 |
| `createdAt` | String | 创建时间 |
| `updatedAt` | String | 更新时间 |

**错误码**：`M3000`(未授权)、`M3009`(非管理员)

---

### 2. 获取板块详情

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `GET` |
| **完整路径** | `/api/admin/boards/{id}` |
| **路径参数** | `id`: Long，必填 |
| **Query 参数** | 无 |
| **Body 参数** | 无 |
| **成功响应 data** | `Board` |
| **错误码** | `M3009`、`M1002`(NOT_FOUND) |

---

### 3. 创建板块

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `POST` |
| **完整路径** | `/api/admin/boards` |
| **路径参数** | 无 |
| **Query 参数** | 无 |
| **Body 参数** | `BoardCreateDTO` |

**BoardCreateDTO 字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | ✅ | 板块名称，长度 1-64 |
| `description` | String | ❌ | 板块描述，长度 0-255 |
| `sortOrder` | Integer | ❌ | 排序值，默认 0 |

| 成功响应 data | `Board` |
| **错误码** | `M3009`、`M1001`(PARAM_ERROR) |

---

### 4. 更新板块

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `PUT` |
| **完整路径** | `/api/admin/boards/{id}` |
| **路径参数** | `id`: Long，必填 |
| **Query 参数** | 无 |
| **Body 参数** | `BoardUpdateDTO` |

**BoardUpdateDTO 字段（所有字段可选）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | ❌ | 板块名称，长度 ≤64 |
| `description` | String | ❌ | 板块描述，长度 ≤255 |
| `status` | String | ❌ | `active` 或 `inactive` |
| `sortOrder` | Integer | ❌ | 排序值 |

| 成功响应 data | `Board` |
| **错误码** | `M3009`、`M1002`、`M1001` |

---

### 5. 删除板块

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `DELETE` |
| **完整路径** | `/api/admin/boards/{id}` |
| **路径参数** | `id`: Long，必填 |
| **Query 参数** | 无 |
| **Body 参数** | 无 |
| **成功响应 data** | `null`（空） |
| **错误码** | `M3009`、`M1002`、`M1003`(STATUS_CONFLICT) |

---

## 二、举报处理 — ReportAdminController.java

### 6. 获取举报列表

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `GET` |
| **完整路径** | `/api/admin/reports` |
| **路径参数** | 无 |
| **Query 参数** | `status`: String，可选（`pending`/`resolved`/`dismissed`） |
| **Body 参数** | 无 |
| **成功响应 data** | `Report[]`（数组） |

**Report 实体字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 举报ID |
| `reporterId` | Long | 举报人ID |
| `targetType` | String | 举报对象类型：`user`/`project` |
| `targetId` | Long | 举报对象ID |
| `reason` | String | 举报原因 |
| `status` | String | 状态：`pending`/`resolved`/`dismissed` |
| `handlerId` | Long | 处理人ID（管理员） |
| `handleResult` | String | 处理结果说明 |
| `handledAt` | String | 处理时间 |
| `createdAt` | String | 创建时间 |
| `updatedAt` | String | 更新时间 |

**错误码**：`M3009`

---

### 7. 获取举报详情

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `GET` |
| **完整路径** | `/api/admin/reports/{id}` |
| **路径参数** | `id`: Long，必填 |
| **Query 参数** | 无 |
| **Body 参数** | 无 |
| **成功响应 data** | `Report` |
| **错误码** | `M3009`、`M1002` |

---

### 8. 处理举报（驳回/解决）

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `PUT` |
| **完整路径** | `/api/admin/reports/{id}/handle` |
| **路径参数** | `id`: Long，必填 |
| **Query 参数** | 无 |
| **Body 参数** | `ReportHandleDTO` |

**ReportHandleDTO 字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `status` | String | ✅ | `resolved`（已处理）/ `dismissed`（已驳回） |
| `handleResult` | String | ❌ | 处理说明，长度 ≤500 |
| `createPenalty` | Boolean | ❌ | 是否创建处罚，默认 false |
| `penaltyType` | String | ❌ | 处罚类型：`credit_deduct`/`function_limit`（createPenalty=true时必填） |
| `creditDeductValue` | Integer | ❌ | 扣分值（penaltyType=credit_deduct时必填） |
| `penaltyReason` | String | ❌ | 处罚原因（默认取举报原因） |

| 成功响应 data | `Report`（处理后的举报对象） |
| **错误码** | `M3009`、`M1002`、`M1003`、`M1001`、`M9999` |

---

## 三、申诉处理 — AppealAdminController.java

### 9. 获取申诉列表

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `GET` |
| **完整路径** | `/api/admin/appeals` |
| **路径参数** | 无 |
| **Query 参数** | `status`: String，可选（`pending`/`approved`/`rejected`）；`targetType`: String，可选（`evaluation`/`penalty`） |
| **Body 参数** | 无 |
| **成功响应 data** | `Appeal[]`（数组） |

**Appeal 实体字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 申诉ID |
| `userId` | Long | 申诉用户ID |
| `targetType` | String | 申诉对象类型：`evaluation`/`penalty` |
| `targetId` | Long | 申诉对象ID |
| `reason` | String | 申诉原因 |
| `status` | String | 状态：`pending`/`approved`/`rejected` |
| `handlerId` | Long | 处理人ID |
| `handleResult` | String | 处理结果说明 |
| `handledAt` | String | 处理时间 |
| `createdAt` | String | 创建时间 |
| `updatedAt` | String | 更新时间 |

**错误码**：`M3009`

---

### 10. 获取申诉详情

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `GET` |
| **完整路径** | `/api/admin/appeals/{id}` |
| **路径参数** | `id`: Long，必填 |
| **Query 参数** | 无 |
| **Body 参数** | 无 |
| **成功响应 data** | `Appeal` |
| **错误码** | `M3009`、`M1002` |

---

### 11. 处理申诉（驳回/通过）

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `PUT` |
| **完整路径** | `/api/admin/appeals/{id}/handle` |
| **路径参数** | `id`: Long，必填 |
| **Query 参数** | 无 |
| **Body 参数** | `AppealHandleDTO` |

**AppealHandleDTO 字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `status` | String | ✅ | `approved`（通过）/ `rejected`（驳回） |
| `handleResult` | String | ❌ | 处理说明 |

| 成功响应 data | `Appeal`（处理后的申诉对象） |
| **错误码** | `M3009`、`M1001`、`M1003`、`M9999` |

---

## 四、评价复核 — EvaluationAdminController.java

### 12. 获取待复核评价列表

| 项目 | 内容 |
|------|------|
| **所属模块** | M5 |
| **请求方式** | `GET` |
| **完整路径** | `/api/admin/evaluations/pending` |
| **路径参数** | 无 |
| **Query 参数** | `projectId`: Long，可选（按项目筛选） |
| **Body 参数** | 无 |
| **成功响应 data** | `PendingEvaluationVO[]`（数组） |

**PendingEvaluationVO 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `evaluationId` | Long | 评价ID |
| `projectId` | Long | 项目ID |
| `evaluatorId` | Long | 评价人ID（管理端可见） |
| `targetId` | Long | 被评价人ID |
| `communicationScore` | Integer | 沟通分 (1-5) |
| `taskScore` | Integer | 任务完成分 (1-5) |
| `skillScore` | Integer | 技能水平分 (1-5) |
| `responsibilityScore` | Integer | 责任分 (1-5) |
| `averageScore` | BigDecimal | 平均分 |
| `comment` | String | 评价内容 |
| `status` | String | 状态（固定为 `pending_review`） |
| `createdAt` | String | 创建时间 |

**错误码**：`M3009`

---

### 13. 执行评价复核

| 项目 | 内容 |
|------|------|
| **所属模块** | M5 |
| **请求方式** | `POST` |
| **完整路径** | `/api/admin/evaluations/{id}/review` |
| **路径参数** | `id`: Long，必填（评价ID） |
| **Query 参数** | 无 |
| **Body 参数** | `EvaluationReviewRequest` |

**EvaluationReviewRequest 字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | String | ✅ | `approve`（通过）/ `void`（作废）/ `kept_no_credit`（保留不计分） |
| `reviewNote` | String | ❌ | 复核备注 |

| 成功响应 data | `EvaluationReviewResult` |

**EvaluationReviewResult 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `evaluationId` | Long | 评价ID |
| `oldStatus` | String | 旧状态 |
| `newStatus` | String | 新状态 (`normal`/`voided`/`kept_no_credit`) |
| `targetId` | Long | 被评价人ID |
| `creditDelta` | Integer | 信誉分变化（正/负/0） |
| `creditEffectiveChanged` | Boolean | 信誉分是否生效变化 |

| **错误码** | `M3009`、`M5005`、`M5007`(INVALID_REVIEW_ACTION)、`M5008`(REVIEW_NOTE_TOO_LONG) |

---

## 五、处罚管理 — PenaltyAdminController.java

### 14. 创建处罚

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `POST` |
| **完整路径** | `/api/admin/penalties` |
| **路径参数** | 无 |
| **Query 参数** | 无 |
| **Body 参数** | `PenaltyCreateDTO` |

**PenaltyCreateDTO 字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | Long | ✅ | 被处罚用户ID |
| `type` | String | ✅ | `credit_deduct`（扣分）/ `function_limit`（功能限制） |
| `creditDeductValue` | Integer | ❌ | 扣分值，type=credit_deduct时需正数 |
| `reason` | String | ✅ | 处罚理由，长度 1-500 |
| `relatedReportId` | Long | ❌ | 关联举报ID |

| 成功响应 data | `Penalty` |
| **错误码** | `M3009`、`M1001` |

---

### 15. 获取处罚列表

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `GET` |
| **完整路径** | `/api/admin/penalties` |
| **路径参数** | 无 |
| **Query 参数** | `status`: String，可选（`active`/`revoked`）；`type`: String，可选（`credit_deduct`/`function_limit`） |
| **Body 参数** | 无 |
| **成功响应 data** | `Penalty[]`（数组） |

**Penalty 实体字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 处罚ID |
| `userId` | Long | 被处罚用户ID |
| `type` | String | 处罚类型：`credit_deduct`/`function_limit` |
| `creditDeductValue` | Integer | 扣分值 |
| `reason` | String | 处罚理由 |
| `adminId` | Long | 执行管理员ID |
| `relatedReportId` | Long | 关联举报ID |
| `status` | String | 状态：`active`/`revoked` |
| `revokedAt` | String | 撤销时间 |
| `createdAt` | String | 创建时间 |
| `updatedAt` | String | 更新时间 |

**错误码**：`M3009`

---

### 16. 获取处罚详情

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `GET` |
| **完整路径** | `/api/admin/penalties/{id}` |
| **路径参数** | `id`: Long，必填 |
| **Query 参数** | 无 |
| **Body 参数** | 无 |
| **成功响应 data** | `Penalty` |
| **错误码** | `M3009`、`M1002` |

---

### 17. 撤销处罚

| 项目 | 内容 |
|------|------|
| **所属模块** | M6 |
| **请求方式** | `PUT` |
| **完整路径** | `/api/admin/penalties/{id}/revoke` |
| **路径参数** | `id`: Long，必填 |
| **Query 参数** | 无 |
| **Body 参数** | `PenaltyRevokeDTO` |

**PenaltyRevokeDTO 字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `reason` | String | ✅ | 撤销原因，长度 1-500 |

| 成功响应 data | `Penalty`（撤销后的处罚对象） |
| **错误码** | `M3009`、`M1002`、`M1003` |

---

## 六、管理员登录 —— 获取 Admin Token

### `POST /api/auth/login/password`

| 项目 | 内容 |
|------|------|
| **请求方式** | `POST` |
| **完整路径** | `/api/auth/login/password` |
| **Body** | `{ "username": "admin用户名", "password": "admin密码" }` |
| **成功响应 data** | `LoginResponse`（含 `token` 字段） |

> 注：只有 `role=admin` 的用户可以通过密码登录，普通用户使用 `POST /api/auth/login`（Mock 微信登录）

---

## 七、重要注意事项

1. **无独立 `/api/admin` 前缀路由**：所有管理接口直接以 `/admin/xxx` 挂在全局 `/api` 下
2. **成功码不是 200**：`Result.success()` 返回 `code: "00000"`，前端判断 `code === "00000"`
3. **列表无分页**：`getReportList`、`getAppealList`、`getPenaltyList`、`getPendingEvaluations` 全部返回全量数组，无 `page/pageSize` 参数
4. **板块列表特殊**：`listBoards()` 和 `getBoardList()` 无 status 筛选参数
5. **所有管理接口需 `@RequestHeader("Authorization")`**
6. **时间字段为 ISO 8601 字符串**（Java LocalDateTime 序列化格式）