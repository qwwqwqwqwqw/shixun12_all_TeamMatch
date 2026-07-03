# TeamMatch 系统详细设计文档 V2.1

**项目名称：** TeamMatch——校园组队与互评平台
**产品形态：** 微信小程序（用户端） + Web 管理后台
**技术栈：** 微信小程序原生 + Spring Boot + MySQL 8.0 + Redis + 阿里云 ECS
**团队规模：** 6 人
**课程周期：** 16 周
**文档定位：** 系统详细设计定稿
**版本：** V2.1

---

## 0. 文档说明

### 0.1 本文档用途

本文档用于完成以下事情：

- 统一全组对产品主线和模块边界的理解
- 指导数据库、接口、前端页面、后台页面和联调实现
- 作为测试设计、Gerrit 评审、Jenkins/CI 过程证据的业务底稿
- 让第一次阅读的人快速知道：
  - 先看什么
  - 自己负责什么
  - 哪些地方不能乱改
  - 哪些地方已冻结，哪些能力移入 P1

---

### 0.2 全员先看哪几节

第一次阅读顺序：

1. 先看第 0 章：知道这份文档怎么用
2. 再看第 1 章：先把主闭环、模块关系、全局对象看明白
3. 然后看第 8 章：这里是全局冻结基线，不能各写各的
4. 最后精读自己负责的板块章节（第 2-7 章）
5. 若飞书流程图在 Markdown 导出中不可见，以本文档中的状态字段、规则说明和第 8 章全局约束为准。

---

### 0.3 已定稿事项

✅ 冷启动：P0 保留。P0 只支持 GitHub 公开仓库技术画像与离线 / 手工榜单；Gitee 双平台、批量爬取、长期画像沉淀表移入 P1。
✅ 技能标签：P0 只用预置字典，不允许用户自定义。
✅ 中文搜索：P0 采用 MySQL FULLTEXT + ngram parser，效果不佳时服务层降级 LIKE。
✅ 退出投票自动截止：P0 已落地 24h deadline_at + 懒检查关闭（见 5.5 与 8.8）。
✅ 举报对象范围：P0 只开 user / project 两类，不支持将 evaluation 作为 report.target_type。
✅ 管理员权限：P0 单级 admin，不细分层级。
✅ 结项凭证：P0 不实现，不进入 DDL、接口与信誉分计算；整体移入 P1。
✅ 核心表软删除：P0 不做，不加 is_deleted；user.status 可用于 banned；project 不提供删除能力，project.status 只表达项目生命周期。

---

### 0.4 全局冻结基线（不能随意改）

⚠️ 以下内容属于全局冻结基线。任何人不能在自己模块里偷偷改口径。

#### A. 产品主线冻结

冷启动引流 → 正式档案建立 → 发布/搜索项目 → 邀请/申请组队 → 协作执行 → 项目结项 → 互评沉淀 → 信誉变化 → 影响下一次组队

#### B. 冷启动定位冻结

- 冷启动不是附属营销动作
- 冷启动属于 P0 / MVP
- 冷启动是主流程入口的一部分
- 不能再把板块二缩成"只剩标签推荐"

#### C. 退出机制口径冻结

退出机制拆分为三层，不允许混用：

- 第一层：离队触发方式（team_member.exit_mode）
  - self_exit：成员主动申请退出（直接跑路）
  - exit_vote：队长发起移除投票

- 第二层：投票结果（exit_vote.result）- 仅 exit_vote 适用
  - pass：投票通过
  - reject：投票不通过

- 第三层：成员最终状态（team_member.status，两态）
  - active：正常在队
  - exited：已离队（self_exit 与 exit_vote-pass 均记为 exited，具体方式通过 exit_mode 区分）

💡 不再使用：有事退出 / 协商退出 / 无故退出 / remove_vote / removed 等老旧命名。
💡 "被移除的成员"识别方式：team_member.status='exited' AND exit_mode='exit_vote'。
💡 被投票人在投票期间主动 self_exit：走 self_exit 扣分链路（-10），进行中的 exit_vote 直接关闭、result 保持 NULL；不引入"被投票失效"的独立结果值。

#### D. 核心状态机冻结

- 成员离开项目：
  - team_member.exit_mode = self_exit / exit_vote
  - exit_vote.result = pass / reject
  - team_member.status = active / exited

执行规则（P0）：

- self_exit（主动跑路）：不经过投票，直接生效。重罚扣 10 分，status → exited，exit_mode → self_exit（堵死规避差评漏洞）。
- exit_vote（队长踢人）：队长发起时选择档位【negotiated(-5 分，协商退出) / malicious(-10 分，恶意失联)】。
- 赞成票 > 反对票 (pass)：按队长所选档位扣分，status → exited，exit_mode → exit_vote。
- 其余情况 (reject)：扣 0 分，status 保持 active，不写 credit_change。
- 被投票人在投票期间 self_exit：走 self_exit 扣分链路（-10）；进行中的 exit_vote 直接关闭，result 留 NULL，不产生额外扣分。
- P1 预留：被移除后，其余组员可对其进行贡献评价，在此基础分上浮动增加/减少。

阶段约束：

1. recruiting 招募中
   - 队长可直接同意 / 拒绝入队申请。
   - 不保留"队长直接移除"能力，移除统一走 exit_vote。
2. in_progress 进行中
   - 成员离开仅允许：self_exit / exit_vote。
3. ended / eval_closed
   - 不允许成员变更。

#### E. 互评核心规则冻结

- 仅同项目正式成员可互评
- 同一项目内同一对象只能评价一次
- 互评窗口固定为项目结束后 7 日
- 1 分 / 2 分必须附负向标签和说明
- 5 分必须附正向标签
- P0 异常检测先只保留基础规则
- 单项目信誉分影响封顶

#### F. 过程要求冻结

- 不是只拼最终功能
- 必须同时准备：开发过程、质量保证、评审证据、测试证据、Gerrit/Jenkins 流程

---

### 0.5 本文档使用原则

✅ 你可以改什么：

- 你自己板块内部的字段补充
- 接口细化
- 页面细化
- 测试用例细化
- 文案、展示、交互细节补充

🚫 你不要乱改什么：

- 不要改主流程
- 不要改全局状态机
- 不要改退出机制名称
- 不要改互评窗口时长
- 不要绕开 credit_change 直接改信誉分
- 不要在不同章节写两套冲突说法

---

## 1. 系统总体设计

### 1.1 项目核心目标

TeamMatch 面向校园组队场景，解决四类核心问题：

- 组队效率低
- 选人盲目
- 协作过程缺少可信记录
- 项目经历难沉淀为可信档案

---

### 1.2 核心业务闭环

#### 1.2.1 冷启动入口闭环

```
排行榜引流 → 临时技术画像 → 学校邮箱认证 → 档案认领 → 进入正式榜单/搜索 → 发布或加入项目
```

这条链路的作用：

- 缓解平台初期空城感
- 给用户第一次进入平台的理由
- 先给收益，再要求填写成本
- 把围观用户转成正式用户

#### 1.2.2 正式业务主闭环

```
正式档案建立 → 发布/搜索项目 → 邀请/申请组队 → 协作执行 → 项目结项 → 互评沉淀 → 信誉变化 → 影响下一次组队
```

---

### 1.3 模块划分与职责视图

六大板块：

- 板块一：用户与认证 / 档案基础
- 板块二：冷启动 / 技术画像 / 档案认领
- 板块三：项目管理
- 板块四：组队 / 成员管理 / 退出机制
- 板块五：互评 / 信誉分 / 举报申诉联动
- 板块六：后台治理 / 审核 / 流程支撑

---

### 1.4 数据库总览（当前冻结核心表）

#### 用户与认证相关
- user、skill_tag、user_skill、team_request、team_member、exit_vote、exit_vote_record

#### 互评与信誉相关
- evaluation、evaluation_tag、skill_endorse、credit_change

#### 项目与推荐相关
- project、project_skill

#### 平台治理相关
- board、report、appeal、penalty

---

### 1.5 全局接口约定

**Base URL**

- 用户端：`https://{domain}/api`
- 管理端：`https://{domain}/api/admin`

**鉴权方式**

- 用户端：`Authorization: Bearer {token}`
- 管理端：`Authorization: Bearer {adminToken}`
- 小程序用户 Token 有效期：7 天
- 管理后台 Token 有效期：2 小时
- P0 不实现 refresh token，P0 不实现 Token 撤销 / 黑名单机制

**统一响应格式**

```json
// 成功
{ "code": 200, "message": "success", "data": {} }

// 分页
{ "code": 200, "message": "success", "data": { "list": [], "total": 0, "page": 1, "pageSize": 20 } }

// 失败
{ "code": 400, "message": "参数错误", "data": null }
```

**统一错误码**

| 状态码 | 含义 |
|---|---|
| 400 | 参数错误 |
| 401 | 未登录 / token 无效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 状态冲突 / 重复操作 |
| 422 | 业务规则不满足 |
| 500 | 服务异常 |

---

### 1.6 命名与类型约定

- 表名：小写下划线、单数形式
- 字段名：小写下划线
- 主键：统一 `id BIGINT AUTO_INCREMENT`
- 状态字段：统一英文枚举值，不存数字
- 时间字段：统一 DATETIME
- 字符集：utf8mb4
- P0 不做软删除，不加 is_deleted 字段

---

## 2. 板块一：用户与认证 / 档案基础

### 2.1 板块目标

为系统提供统一身份入口和正式档案底座，后续所有业务都建立在本板块之上。

### 2.2 功能范围

- 微信登录
- 管理员登录
- 学校邮箱验证码发送
- 学校邮箱验证
- 获取我的档案
- 更新我的档案
- 查看他人公开档案
- 技能标签列表
- 更新我的技能标签

### 2.3 关键规则

- 普通用户统一通过微信登录
- 管理员统一使用 username + password_hash
- 管理员账号由后端预置，不开放注册
- 被封禁用户不能登录
- 邮箱验证码：60 秒防刷、10 分钟有效
- 邮箱不能被多人重复绑定
- 技能标签默认复用预置字典
- 技能更新采用全量替换
- 正式档案建立前，核心操作权限受限制
- 用户角色只能由后端控制，前端不能自行决定

### 2.4 涉及的数据对象

**user** 关键字段：openid、nickname、avatar_url、email、email_verified、school、major、grade、bio、credit_score、role、status、username、password_hash

**skill_tag** 预置技能标签字典，分类：language、framework、tool、soft_skill

**user_skill** 用户与技能标签的多对多关联表

### 2.5 与其他板块的依赖关系

- 为板块二提供：邮箱认证、用户主体、正式档案前置能力
- 为板块三提供：项目创建者与技能标签基础
- 为板块四提供：邀请 / 申请 / 成员主体
- 为板块五提供：评价者、被评价者、信誉归属主体
- 为板块六提供：举报 / 申诉 / 处罚的用户主体

### 2.6 P1 预留

- P1 可评估更细的管理员权限分层
- P1 可评估更丰富的档案可信材料能力
- P0 已定稿：技能标签只使用预置字典；管理员权限为单级 admin

---

## 3. 板块二：冷启动 / 技术画像 / 档案认领

### 3.1 板块目标

提供 P0 / MVP 阶段必须成立的冷启动路径，把"围观用户"逐步转成"正式档案用户"和"组队用户"。

### 3.2 功能范围

- 首页排行榜展示
- 技术分榜展示
- GitHub 用户名输入
- 临时技术画像生成
- 技术画像结果展示
- 邮箱认证引导
- GitHub 画像认领确认（非组队硬前置）
- 正式档案认领
- 正式榜单准入控制
- 标签匹配推荐

### 3.3 核心流程

**标签匹配推荐流程：**

1. 读取 user_skill
2. 读取 project_skill
3. 计算交并集相似度（Jaccard）
4. 返回匹配项目列表
5. 支持返回匹配标签名称与匹配度

### 3.4 关键规则

- 首页冷启动阶段优先展示技术榜单
- 临时技术画像遵循"先给结果，再要求认证"
- 正式榜单准入依赖正式档案建立
- 被搜索到依赖正式档案建立
- 接收邀请依赖正式档案建立
- 主动申请加入项目依赖正式档案建立
- 冷启动模块不是独立营销动作，而是主流程入口
- 技术画像只分析公开仓库信息
- 标签匹配推荐采用 Jaccard 相似度，相似度为 0 的项目不返回
- 技术画像定位：轻量引流入口，不落核心主库

### 3.5 涉及的数据对象

复用对象：user、skill_tag、user_skill、project_skill

P0 不新增冷启动核心表。排行榜数据由离线 JSON / 缓存承载。临时画像由 GitHub 实时查询结果承载。

### 3.6 P1 预留

- Gitee 双平台支持
- 批量爬取与自动更新
- 冷启动画像长期沉淀表
- 更复杂的画像可视化
- P0 已定稿：只支持 GitHub；技术画像不落核心主库；正式技术榜单只展示完成邮箱认证、正式档案建立、GitHub 画像认领的用户

---

## 4. 板块三：项目管理

### 4.1 板块目标

提供项目从发布到互评关闭的完整生命周期支撑，是组队、退出、互评的统一时序基础。

### 4.2 功能范围

- 发布项目
- 项目列表
- 项目详情
- 更新项目
- 开始项目
- 结束项目
- 搜索
- 标签匹配推荐入口承接
- 板块列表（公开）

### 4.3 核心流程

**项目生命周期状态机：**

```
recruiting → in_progress → ended → eval_closed
```

- recruiting：招募中
- in_progress：进行中
- ended：已结束，互评窗口开放
- eval_closed：互评关闭，只读

**状态迁移副作用：**

- 项目开始后，剩余 pending 请求全部失效（→ expired）
- 项目结束时写入 ended_at
- 项目结束时写入 eval_deadline = ended_at + 7天
- ended → eval_closed：懒检查触发（不依赖定时任务）

### 4.4 关键规则

- 只有队长可以切换项目状态
- 只有 recruiting 状态允许邀请和申请
- 项目开始后不允许继续加入
- 项目结束后开启 7 日互评窗口
- 互评关闭采用懒检查，P0 不依赖定时任务
- 创建项目时，创建者自动成为队长
- P0 不实现结项凭证

### 4.5 涉及的数据对象

**project** 关键字段：creator_id、board_id、title、description、max_members、status、deadline、eval_deadline、ended_at

**project_skill** 项目与技能需求的多对多关系表

**board** 项目所属板块 / 分类

### 4.6 P1 预留项

- 结项凭证整体移入 P1
- P1 可评估更复杂的项目排序与推荐策略
- P0 已定稿：中文搜索采用 MySQL FULLTEXT + ngram parser，效果不佳时服务层降级 LIKE

---

## 5. 板块四：组队 / 成员管理 / 退出机制

### 5.1 板块目标

本板块负责组队请求、正式成员关系、成员主动退出与队长发起移除投票。P0 退出机制统一采用三层结构：

- 触发方式：team_member.exit_mode = self_exit / exit_vote
- 投票结果：exit_vote.result = pass / reject / NULL
- 成员状态：team_member.status = active / exited

被移除成员通过 `status='exited' AND exit_mode='exit_vote'` 识别。全文不再使用 remove_vote、removed 等旧命名。

### 5.2 功能范围

- 发送邀请
- 申请加入
- 接受组队请求
- 拒绝组队请求
- 取消组队请求
- 查看收到的请求
- 查看发出的请求
- 项目成员列表
- 成员主动退出
- 队长发起移除投票
- 投票
- 队长手动关闭投票
- 查看投票详情

### 5.3 退出结果及扣分定义

| 场景 | 扣分 | 副作用 |
|---|---|---|
| self_exit | -10 | 直接生效，status → exited，exit_mode → self_exit |
| exit_vote + pass + negotiated | -5 | status → exited，exit_mode → exit_vote |
| exit_vote + pass + malicious | -10 | status → exited，exit_mode → exit_vote |
| exit_vote + reject | 0 | 成员保持 active，不写 credit_change |

### 5.4 投票判定规则（P0 固定多数）

- 赞成票数 > 反对票数 → pass（通过移除）
- 其余情况（含票数相等）→ reject（否决保留）

### 5.5 关键规则

- 邀请和申请统一走 team_request
- 同一请求组合在 pending 状态下只允许一条
- 只有队长能邀请、接受申请、拒绝申请、发起 exit_vote、手动关闭 exit_vote
- 成员 self_exit 直接生效；队长 P0 不能 self_exit
- 队长只能在 in_progress 阶段发起 exit_vote
- P0 不支持队长移除已加入成员（recruiting 阶段）
- 成员如需退出 recruiting 项目，按轻量 self_exit 处理，不写信誉分流水
- 正式惩罚性 self_exit / exit_vote 仅在 in_progress 阶段生效
- 同一目标成员同一时间只能有一个进行中的 exit_vote 流程
- P0 已实现 24h deadline_at + 懒检查关闭

### 5.6 涉及的数据对象

**team_request** 关键字段：project_id、from_user_id、to_user_id、request_type（invite/apply）、status

**team_member** 关键字段：project_id、user_id、role、status、exit_mode、joined_at、left_at

**exit_vote** 关键字段：
- id、project_id、target_user_id、initiator_id
- status（voting / closed）
- penalty_level（negotiated[-5] / malicious[-10]，发起时队长必选）
- result（pass / reject，关闭时写入；被投票人中途 self_exit 时留 NULL）
- total_voters（= 当前 active 成员数 - 1，被投票人不计入）
- agree_count / disagree_count（默认 0）
- deadline_at（= created_at + 24h，超时由懒检查自动关闭）
- created_at / closed_at

💡 不设 credit_deduct 字段：扣分完全由 penalty_level + result 推导
💡 不设 abstain_count：P0 只有"赞成 / 反对"两值
💡 关闭触发条件（任一满足即关闭）：① 全员投完；② 队长手动关；③ NOW() > deadline_at

**exit_vote_record** 关键字段：id、vote_id、voter_id、choice（agree/disagree）、created_at

💡 唯一约束 (vote_id, voter_id) 保证一人一票

### 5.7 P1 预留项

- 队长转让能力
- 投票移除后的贡献评价浮动机制
- P0 已定稿：队长不能主动退出；P0 不支持队长转让

---

## 6. 板块五：互评 / 信誉分 / 举报申诉联动

### 6.1 板块目标

在项目结束后，把协作反馈沉淀为互评记录和信誉变化，并与治理链联动，保障评价的可参考性与可纠错性。

### 6.2 功能范围

- 检查互评资格
- 提交互评
- 某项目互评列表
- 我收到的互评
- 我的信誉分
- 信誉分变化记录
- 技能点赞
- 异常评价挂起
- 申诉恢复联动
- 处罚联动

### 6.3 互评资格规则

- 当前用户必须是项目正式成员
- 目标用户必须是项目正式成员
- 项目必须处于 ended
- 必须在互评窗口内
- 不能重复评价
- 不能评价自己

### 6.4 四维互评结构

每条评价至少包含四个维度：沟通协作、任务完成、技术能力、责任心

系统先保存四维明细分，再基于四维均值形成本条评价汇总分，用于信誉分计算。

### 6.5 信誉分计算规则

**初始分：** 100

**单条评价汇总分到信誉变化的映射：**

| 均分区间 | change_value |
|---|---|
| [1.0, 1.5) | -5 |
| [1.5, 2.0) | -3 |
| [2.0, 2.5) | -2 |
| [2.5, 3.0) | -1 |
| [3.0, 3.5) | 0 |
| [3.5, 4.0) | +2 |
| [4.0, 4.5) | +3 |
| [4.5, 5.0] | +5 |

**单项目封顶：** 同一用户在同一项目中的互评分变化总和不超过 ±10

**退出结果扣分：**

| 场景 | 扣分 |
|---|---|
| self_exit | -10 |
| exit_vote + pass + negotiated | -5 |
| exit_vote + pass + malicious | -10 |
| exit_vote + reject | 0，不写流水 |

### 6.6 异常检测（P0 冻结口径）

P0 保留两条基础规则（跨被评价者整体判断，检测时机：评价者覆盖全部队友后同步触发）：

- **规则 A（全低分）：** 评价者对同项目所有被评价者的 4 维度均分 ≤ 2 → 进入 pending_review
- **规则 B（全满分）：** 评价者对同项目所有被评价者的 4 维度均分 = 5 → 进入 pending_review

满足任一条件，将该评价者在该项目中的全部评价批量标记为 pending_review。

P1 预留：明显偏离团队均值、双向互刷高分、理由与打分不匹配增强版

### 6.7 申诉恢复机制

申诉通过后：

- 不篡改原 credit_change.effective（原始流水是已发生事实）
- 新增一条 change_type='appeal_restore' 正向流水（change_value = 原流水相反数）
- 回写 user.credit_score
- 幂等：同一 (source_type, source_id) 已存在 appeal_restore 流水时跳过
- 当原流水 effective=0（如关联评价是 pending_review），跳过写入，无恢复动作

**function_limit 特殊分支：** 若申诉对象是 function_limit 类型处罚，不走信誉分恢复，按"剩余未撤销 function_limit 处罚计数"决定 user.status：>0 保持 banned，=0 设为 active

### 6.8 关键规则

- 互评唯一性由 (project_id, evaluator_id, target_id) 保证
- 任一维度为 1 分 / 2 分时，必须附负向标签和说明（≥ 20 字）
- 任一维度为 5 分时，必须附正向标签
- 互评窗口固定为项目结束后 7 日
- effective=false 的信誉分流水也要展示，但需标注"已挂起"
- voided 的评价不展示给被评价者
- 互评前台匿名展示，后台可追溯
- P0 不等待全员提交
- 用户侧 evaluation 申诉入口仅对 status=normal 的评价开放
- pending_review 评价可展示为"平台复核中"，不计入信誉分，P0 暂不开放申诉入口
- voided 评价不展示，不开放申诉入口
- kept_no_credit 评价可展示但不计信誉分，P0 不开放申诉入口

### 6.9 涉及的数据对象

**evaluation** 关键字段：project_id、evaluator_id、target_id、communication_score、task_score、skill_score、responsibility_score、comment、status

**evaluation_tag** 评价原因标签关联表

**skill_endorse** 技能点赞明细表（P0 仅展示，不影响信誉分）

**credit_change** 信誉变化流水表，关键字段：
- id、user_id
- change_type（evaluation / exit_vote / self_exit / penalty / penalty_restore / appeal_restore）
- change_value（正数=加分，负数=扣分）
- effective（1=生效，0=挂起或不计分）
- source_id、source_type
- description、created_at

💡 信誉分当前值 = 100 + SUM(change_value WHERE effective=1)，user.credit_score 为冗余缓存
💡 追加为主；仅 effective 字段可被异常检测 / 复核流程修改

**P0 已定稿标签：**

- 正向标签：沟通积极、按时交付、技术可靠、责任心强
- 负向标签：沟通差、延期、质量低、失联

### 6.10 P1 预留

- 评价者历史稳定度权重
- 更强异常检测规则
- 双向互刷识别

---

## 7. 板块六：后台治理 / 审核 / 流程支撑

### 7.1 板块目标

为平台提供治理、审核、复核和处理能力，保障平台内容质量、评价纠错能力与用户权益。

### 7.2 功能范围

- 创建板块
- 更新板块
- 删除板块（仅允许无项目引用时）
- 板块列表（管理端）
- 提交举报
- 举报列表
- 处理举报
- 提交申诉
- 我的申诉列表
- 申诉列表（管理端）
- 处理申诉
- 待复核评价列表
- 复核评价
- 违规处罚
- 信誉分变动审计

### 7.3 关键规则

**举报 report：**

- target_type = user
- target_type = project
- P0 不支持将 evaluation 作为 report.target_type

**申诉 appeal：**

- target_type = evaluation：对收到的互评不满
- target_type = penalty：对管理员处罚不满

**评价争议：**

- 用户侧仅能对 status=normal 的评价提交 appeal(target_type='evaluation')
- status=pending_review 表示平台已主动介入复核，P0 不再开放用户申诉入口，避免同一评价同时走"平台复核"和"用户申诉"双入口
- status=voided 不展示且不可申诉；status=kept_no_credit 可展示但不计分，P0 不开放申诉入口
- 管理员侧走 evaluation review
- 不走 report

**退出投票结果：** P0 不支持对 exit_vote 民主结果本身申诉

**其他治理规则：**

- 管理端接口必须管理员鉴权
- 同一事件申诉有唯一性约束
- 板块名称唯一
- 举报、申诉、复核、处罚都要留痕

### 7.4 评价复核流程

- approve：恢复为 normal，评价重新计分，用户侧恢复申诉入口
- void：作废，不展示，不计分，不开放申诉入口
- keep_no_credit：保留展示但不计分，不开放申诉入口

### 7.5 处罚流程

- credit_deduct：扣信誉分
- function_limit：限制功能 / 封禁

### 7.6 涉及的数据对象

**board** 板块管理表

**report** 举报表

**appeal** 申诉表

**penalty** 违规处罚记录表，关键字段：

| 字段 | 说明 |
|---|---|
| id | 主键 |
| user_id | 被处罚用户 |
| type | credit_deduct / function_limit |
| credit_deduct_value | INT，仅 credit_deduct 类型必填，正整数 |
| reason | TEXT，处罚理由 |
| admin_id | 执行处罚的管理员 ID（必填，用于审计追溯） |
| related_report_id | 关联举报记录，可为 NULL |
| created_at | 创建时间 |

💡 credit_deduct 副作用（同事务）：向 credit_change 写扣分流水 + 更新 user.credit_score
💡 credit_deduct 撤销 / penalty 类申诉通过后的恢复副作用（同事务）：向 credit_change 写 change_type='penalty_restore' 正向流水 + 回写 user.credit_score
💡 function_limit 副作用（同事务）：不写 credit_change，设 user.status='banned'
💡 admin_id 必填：P0 所有处罚必须可追溯到具体管理员账号

### 7.7 P1 预留

- 更细粒度管理员权限
- 更完善的治理工作台
- P0 已定稿：举报范围只开 user / project 两类；管理员权限为单级 admin

---

## 8. 全局统一设计约束

### 8.1 同步执行链（事务边界）【全员必读 / 后端重点】

以下链路必须在同一事务中完成：

- 提交互评
- 关闭退出投票
- 复核评价
- 通过申诉
- 执行处罚
- 成员主动退出

### 8.2 P0 明确不引入的机制【全员必读】

为降低复杂度，P0 明确不做：

- 定时任务
- 消息队列
- 分布式锁
- 数据库 CHECK 约束
- 软删除
- 独立审计日志表
- 高级信誉分权重与熟人衰减正式落地

**通知降级：**

- P0 不做微信主动推送 / 完整消息系统
- P0 不建 notification 消息表
- 替代方案：通过极简"未读角标聚合接口"（GET /api/me/badges），实时 count 待处理的邀请 / 投票 / 互评

**冷启动降级：**

- P0 不做：Gitee 支持、系统内批量爬取、定时自动更新榜单、临时画像落核心主库、GitHub 绑定作为组队/搜索/邀请的硬前置条件
- 排行榜允许使用离线脚本或手工维护数据

### 8.3 并发保护策略【后端重点】

- 关键链路用事务包裹
- 需要时使用 SELECT ... FOR UPDATE
- 用数据库唯一约束兜底幂等性和重复提交

### 8.4 reasonCode 统一枚举【联调重点】

| reasonCode | 含义 | 适用场景 |
|---|---|---|
| NOT_PROJECT_MEMBER | 当前用户不是该项目成员 | 互评 |
| TARGET_NOT_PROJECT_MEMBER | 目标用户不是该项目成员 | 互评 |
| PROJECT_NOT_ENDED | 项目尚未进入互评阶段 | 互评 |
| EVAL_WINDOW_CLOSED | 互评窗口已关闭 | 互评 |
| ALREADY_EVALUATED | 已评价过该成员 | 互评 |
| SELF_EVALUATION | 不能评价自己 | 互评 |
| PROJECT_NOT_RECRUITING | 项目不在招募中 | 组队 |
| PROJECT_FULL | 项目已满员 | 组队 |
| ALREADY_MEMBER | 已是项目成员 | 组队 |
| DUPLICATE_PENDING_REQUEST | 已有待处理的同类请求 | 组队 |
| NOT_LEADER | 当前用户不是队长 | 组队/投票 |
| PROJECT_NOT_IN_PROGRESS | 项目不在进行中 | 退出投票 |
| TARGET_NOT_ACTIVE_MEMBER | 目标不是活跃成员 | 退出投票 |
| SELF_VOTE | 不能对自己发起退出投票 | 退出投票 |
| DUPLICATE_VOTING | 该成员已有进行中的投票 | 退出投票 |
| VOTE_ALREADY_CLOSED | 投票已关闭 | 投票 |
| VOTE_DEADLINE_PASSED | 投票已超过截止时间 | 投票 |
| ALREADY_VOTED | 已投过票 | 投票 |
| VOTER_IS_TARGET | 被投票人不能参与投票 | 投票 |
| INVALID_PENALTY_LEVEL | penaltyLevel 参数缺失或不合法 | 发起退出投票 |
| LEADER_CANNOT_EXIT | 队长不能主动退出 | 主动退出 |
| DUPLICATE_APPEAL | 已有待处理的同类申诉 | 申诉 |
| EVALUATION_ALREADY_INVALIDATED | 评价已被判定无效 | 申诉 |
| FORMAL_PROFILE_REQUIRED | 未建立正式档案，不能被搜索/被邀请/申请加入项目 | 档案前置 |
| TEAM_VOTE_CONFLICT | 组队/投票状态冲突（成员状态已变化、投票已关闭、目标不在项目内） | 退出投票 |

💡 前端收到 TEAM_VOTE_CONFLICT 必须明确提示（如"项目成员状态已更新，请刷新后重试"），严禁统一回退为"系统异常"。

### 8.5 可测试性要求【全员必读 / 测试重点】

- JUnit 单元测试：信誉分、项目级封顶截断、退出结果计算
- 接口测试：状态流转、资格校验、错误码、幂等性
- 端到端测试：完整业务闭环

至少覆盖：登录/认证、项目发布/状态流转、邀请/申请/加入、退出认定、互评、举报/申诉/处罚闭环

### 8.6 DDL 建表顺序【后端重点】

建议顺序（按外键依赖关系）：

1. user
2. skill_tag
3. user_skill
4. board
5. project
6. project_skill
7. team_request
8. team_member
9. exit_vote
10. exit_vote_record
11. evaluation
12. evaluation_tag
13. skill_endorse
14. credit_change
15. report
16. appeal
17. penalty

### 8.7 并发与级联协议

P0 单实例部署场景下的关键并发保护，联调阶段严格遵守：

**退出投票三闭合入口原子性**

关闭入口：提交投票时若全员投完 / 队长手动关闭 / 懒检查发现超时

单事务协议：

1. SELECT ... FOR UPDATE 锁定 exit_vote 行
2. UPDATE exit_vote SET status='closed' WHERE id=? AND status='voting'
3. 仅 rowsAffected=1 的事务执行副作用（team_member + credit_change + user.credit_score）
4. rowsAffected=0 时按入口返回：提交投票命中超时 → VOTE_DEADLINE_PASSED；其余 → VOTE_ALREADY_CLOSED

**D-11 / D-13 并发级联**

P0 通过入口约束避免 evaluation 类申诉与评价复核双入口并发：status=pending_review 的评价不开放用户申诉入口，因为平台已经主动进入复核流程。管理员复核完成后仅 approve → normal 的评价重新开放申诉入口；voided 不展示且不可申诉；kept_no_credit 可展示但不计分，P0 不开放申诉入口。

通过 evaluation 类申诉时，事务内 SELECT FOR UPDATE 锁定 evaluation 行。若处理时评价已是 voided，返回 EVALUATION_ALREADY_INVALIDATED；若评价为 kept_no_credit 或原 credit_change.effective=0，则不写 appeal_restore，避免双倍恢复或状态撕裂。

**懒检查触发点：**

- project.status ended → eval_closed：项目详情、互评资格检查、提交互评等入口
- exit_vote.status voting → closed（超时）：提交投票、手动关闭、查询详情等入口

**同步事务边界：** 以下操作链在同一 @Transactional 中完成，任一步失败全部回滚：提交互评、手动关闭/懒检查关闭投票、通过申诉、复核评价、违规处罚、成员主动退出

---

## 9. 分工建议 / 阶段计划

### 9.1 负责人映射

| 成员 | 职责 |
|---|---|
| M1 | 小程序前端一：认证 / 档案 / 冷启动 / 项目浏览（7 个小程序页面） |
| M2 | 小程序前端二 + 简化后台：组队 / 退出 / 互评 / 信誉展示 / 管理端基础页面（6 个小程序页面 + 5 个后台页面） |
| M3 | 后端一：用户认证 / Token / 邮箱验证码 / 档案 / 技能标签 / 权限基础 + 单元测试 |
| M4 | 后端二：项目 / 组队请求 / team_member / exit_vote / 并发关闭 + 并发测试 |
| M5 | 后端三：evaluation / credit_change / appeal / 评价复核 + 测试套件 |
| M6 | 治理与质量部署：report / penalty / 管理端接口 + 接口测试 + Jenkins 配置 + 部署脚本 |

### 9.2 阶段节奏

| 阶段 | 周次 | 目标 |
|---|---|---|
| Phase 1 | W5-W6 | 基础搭建、认证、档案、项目发布、基础后台 |
| Phase 2 | W7-W9 | 组队、互评、信誉分、搜索、平台治理 |
| Phase 3 | W10-W13 | 联调、补齐后台、测试、MVP 验收 |
| Phase 4 | W14-W16 | P1 功能、优化、部署、答辩 |

### 9.3 MVP 验收标准

**功能层：**

- 主闭环能完整跑通
- 后台治理可用
- 冷启动入口可用
- 组队 / 退出 / 互评 / 信誉 / 治理链路能串起来

**质量层：**

- 无阻塞性 Bug
- 关键状态流转一致
- 无明显数据库口径冲突

**过程层：**

- Gerrit 提交与 Review 记录完整
- 至少有最小可展示的 Jenkins / CI 流程
- 有接口测试 / 单元测试 / 业务链路测试证据

### 9.4 协作规则

- 全局基线只维护一份（`docs/design/system-design-v2.1.md`）
- 各自改自己板块时，只能在本板块边界内加细节
- 遇到以下内容必须先在组内对齐后再改：公共字段、状态机、退出规则、互评规则、信誉分流水、申诉恢复逻辑、冷启动前置条件
