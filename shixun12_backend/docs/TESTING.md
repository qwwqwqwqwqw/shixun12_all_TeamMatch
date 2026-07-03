# TeamMatch 测试策略

## 三层测试（推荐）

| 层级 | 做法 | 工具 | CI 是否默认跑 |
|------|------|------|----------------|
| **单元测试** | Service / 规则计算 | JUnit5 + Mockito | ✅ `mvn test` |
| **接口测试** | Controller 鉴权、错误码、参数校验 | `@WebMvcTest` + MockMvc | ✅ `mvn test` |
| **集成测试** | 真实 MySQL + Redis，Spring 全上下文 | Testcontainers | ⚠️ 需 Docker，见下 |

**不建议**用 H2 替代 MySQL 做全库集成：本项目 DDL 为 MySQL 方言（`ENGINE=InnoDB`、`ON UPDATE CURRENT_TIMESTAMP`、保留字 `user` 等），H2 兼容成本高、易与生产不一致。

---

## 日常开发 / Jenkins CI

```bash
mvn clean test
```

生成覆盖率报告：

```bash
mvn clean verify
# 报告：target/site/jacoco/index.html
```

可选门禁（本地或 Jenkins 加 `-Pcoverage-check`）：

```bash
mvn clean verify -Pcoverage-check
```

阈值见 `pom.xml` 中 `jacoco.line.coverage.minimum` / `jacoco.branch.coverage.minimum`（默认偏低，避免历史代码拉低导致 CI 失败；可逐步调高）。

---

## 集成测试（Testcontainers + MySQL）

**前提**：本机或 CI 节点已安装 **Docker**。

```bash
# Windows PowerShell
$env:RUN_INTEGRATION_TESTS="true"
mvn clean verify -Pintegration

# Linux/macOS
RUN_INTEGRATION_TESTS=true mvn clean verify -Pintegration
```

实现类：`src/test/java/com/teammatch/integration/ApplicationIntegrationIT.java`  
使用 MySQL 8 容器加载 `integration-schema.sql`，并启动 Redis 容器；请求路径为 `/health`（勿写 `/api/health`，context-path 已含 `/api`）。

老师 Jenkins **若无 Docker**，保持默认 `mvn test` 即可，集成测试不会执行。

---

## 业务闭环（手工 / Apifox）

治理链路见 `M6_API_Documentation.md` §7，在**已部署**环境用 Apifox 跑通并留截图，作为 MVP 过程证据。
