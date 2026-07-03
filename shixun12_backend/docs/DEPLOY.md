# TeamMatch 后端部署指南（阿里云轻量服务器）

> 课程要求 **CI**（Jenkins `mvn test`）即可；**部署为手动 CD**，与 Jenkins 解耦。  
> 联调 Base URL 示例：`http://<公网IP>:8080/api`

---

## 1. 服务器准备

1. 购买 [阿里云轻量应用服务器](https://www.aliyun.com/product/swas)（2C2G 即可），系统选 **Ubuntu 22.04** 或 **Alibaba Cloud Linux 3**。
2. 控制台 **防火墙 / 安全组** 放行：
   - `22`（SSH）
   - `8080`（后端 API，context-path 已是 `/api`）
   - **不要**对公网开放 `3306`（MySQL）、`6379`（Redis）。
3. SSH 登录：

```bash
ssh root@<你的公网IP>
```

---

## 2. 安装依赖（Ubuntu 示例）

```bash
apt update
apt install -y openjdk-17-jdk mysql-server redis-server

java -version   # 应为 17
systemctl enable mysql redis-server
systemctl start mysql redis-server
```

---

## 3. 初始化数据库

```bash
# 将仓库 sql 目录上传到服务器，或在服务器 git clone 项目
mysql -u root -p < sql/rebuild_database.sql
```

脚本会重建 `teammatch` 库并插入示例数据。默认管理员：

| 字段 | 值 |
|------|-----|
| 用户名 | `admin` |
| 密码 | `admin123` |

登录验证：

```bash
curl -s -X POST http://127.0.0.1:8080/api/auth/login/password \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

---

## 4. 构建并上传 JAR

**在本地或 Jenkins 构建机：**

```bash
mvn clean package -DskipTests
# 产物：target/teammatch-backend-1.0.0-SNAPSHOT.jar
```

**上传到服务器：**

```bash
scp target/teammatch-backend-1.0.0-SNAPSHOT.jar root@<公网IP>:/opt/teammatch/
scp scripts/deploy.sh root@<公网IP>:/opt/teammatch/
```

---

## 5. 启动服务

在服务器 `/opt/teammatch` 下：

```bash
chmod +x deploy.sh

export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=teammatch
export DB_USERNAME=root
export DB_PASSWORD='你的MySQL密码'
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=prod

./deploy.sh start
```

查看日志：

```bash
tail -f /opt/teammatch/logs/app.log
```

健康检查：

```bash
curl http://127.0.0.1:8080/api/health
```

外网访问（本机浏览器 / Apifox）：

```text
http://<公网IP>:8080/api
```

---

## 6. 常用运维命令

```bash
./deploy.sh status   # 是否在跑
./deploy.sh stop     # 停止
./deploy.sh restart  # 重启（更新 jar 后执行）
```

更新版本流程：`mvn package` → `scp` 新 jar → `./deploy.sh restart`

---

## 7. 前端 / Apifox 配置

| 端 | 配置 |
|----|------|
| M2 管理端 | API Base = `http://<公网IP>:8080/api` |
| Apifox | 同上；管理员 Token：`POST /auth/login/password` |
| M6 文档 | `M6_API_Documentation.md` 治理闭环 §7 |

小程序真机需 **HTTPS + 备案域名**；开发阶段可在开发者工具勾选「不校验合法域名」用 IP 联调。

---

## 8. 与 Jenkins 的关系

| 环节 | 做法 |
|------|------|
| **CI（课程要求）** | Jenkins：`mvn clean test`（可选 `verify` 产出 JaCoCo 报告） |
| **部署（联调）** | 本机 `package` + `scp` + `deploy.sh`，**不必**接入 Jenkins CD |
