# TeamMatch Backend

TeamMatch 校园组队与互评平台后端服务

## 技术栈

- Java 17
- Spring Boot 3.2.5
- MyBatis-Plus 3.5.5
- MySQL 8.0
- Redis 7.x
- Maven

## 项目结构

```
backend/
├── src/main/java/com/teammatch/
│   ├── TeamMatchApplication.java    # 启动类
│   ├── common/                      # 通用工具、常量、异常
│   ├── config/                      # 配置类
│   ├── controller/                  # 控制器层
│   ├── service/                     # 业务逻辑层
│   ├── mapper/                      # 数据访问层
│   ├── entity/                      # 实体类
│   └── dto/                         # 数据传输对象
├── src/main/resources/
│   ├── application.yml              # 主配置文件
│   ├── application-local.yml.example # 本地配置示例
│   └── mapper/                      # MyBatis XML 映射文件
└── src/test/                        # 测试代码
```

## 快速开始

### 1. 环境准备

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 7.x+

### 2. 配置数据库

```sql
CREATE DATABASE teammatch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

执行 `sql/schema.sql` 初始化数据库表结构（含示例数据可用 `sql/rebuild_database.sql`）。

### 3. 配置本地环境

复制配置文件模板：

**Linux/macOS:**
```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

**Windows (PowerShell):**
```powershell
Copy-Item src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

**Windows (CMD):**
```cmd
copy src\main\resources\application-local.yml.example src\main\resources\application-local.yml
```

编辑 `application-local.yml`，填入真实配置：
- 数据库连接信息
- Redis 连接信息

### 4. 启动应用

```bash
# 使用 Maven
mvn spring-boot:run

# 或打包后运行
mvn clean package
java -jar target/teammatch-backend-1.0.0-SNAPSHOT.jar
```

应用将在 `http://localhost:8080/api` 启动。

### 5. 验证启动

访问健康检查接口：
```bash
curl http://localhost:8080/api/health
```

## 开发规范

### 分工说明

- **M3**: 板块一（用户认证/档案）+ 板块二（冷启动）
- **M4**: 板块三（项目管理）+ 板块四（组队/退出）
- **M5**: 板块五（互评/信誉/申诉）
- **M6**: 板块六（治理/举报/处罚）

### 代码规范

1. 遵循阿里巴巴 Java 开发手册
2. 使用 Lombok 简化代码
3. 统一使用 MyBatis-Plus 进行数据访问
4. 关键业务逻辑必须添加事务注解 `@Transactional`
5. 并发保护使用 `SELECT ... FOR UPDATE`

### 测试要求

- 单元测试：信誉分计算、退出结果计算、项目级封顶
- 接口测试：状态流转、资格校验、错误码、幂等性
- 集成测试：完整业务闭环

## 接口文档

- Base URL: `http://localhost:8080/api`
- 管理端: `http://localhost:8080/api/admin`
- 鉴权方式: `Authorization: Bearer {token}`

详细接口文档参见根目录 `M3_API_Documentation.md`、`M6_API_Documentation.md`等；系统设计见 `docs/design/system-design-v2.1.md`。

## 注意事项

⚠️ **开发前必读**

1. 先阅读 `docs/design/system-design-v2.1.md`
2. 了解全局冻结基线（第 8 章）
3. 涉及状态机、退出机制、互评规则、信誉流水的改动必须全组对齐
4. 不要绕开 `credit_change` 直接修改信誉分
5. P0 不做软删除，不加 `is_deleted` 字段
6. 提交前必须自测相关链路

## 提交规范

使用 Gerrit 提交代码：

```bash
git add .
git commit -m "feat: 功能描述"
git push origin HEAD:refs/for/master
```

提交说明必须写清影响范围，涉及接口、数据库、状态机、事务的改动需单独标注。
