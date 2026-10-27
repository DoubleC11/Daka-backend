# 打卡生活后端

打卡生活是一个面向个人日常记录与情侣协作的 Web 应用。本仓库提供后端 API，基于 Spring Boot 4、MyBatis-Plus 和 MySQL，实现习惯打卡、心情日记、目标管理、待办、记账统计、情侣空间及 CSV 数据导出等能力。

前端项目：[DoubleC11/daka-frontend](https://github.com/DoubleC11/daka-frontend)

## 功能

- 用户注册、登录与 JWT 鉴权，密码使用 BCrypt 加密存储
- 习惯管理、每日打卡、补打卡、连续天数和日历统计
- 心情日记、关键词检索、心情分布与月度回顾
- 目标进度、待办事项及完成状态管理
- 收支记账、自定义分类、分类占比和余额趋势
- 首页聚合统计、近 N 天趋势与近 N 月收支对比
- 情侣邀请码绑定、共同日历和伴侣空间只读视图
- 账单、待办、日记、习惯记录和目标的 CSV 导出
- 统一 JSON 响应格式、参数校验、全局异常处理

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 运行时 | Java 21 |
| 框架 | Spring Boot 4.1.1 |
| 数据访问 | MyBatis-Plus 3.5.17 |
| 数据库 | MySQL 8.0+ |
| 鉴权 | JJWT 0.12.6 |
| 密码加密 | Spring Security Crypto / BCrypt |
| 构建 | Maven 3.9+ |
| 容器 | Docker |

## 项目结构

```text
.
├── src/main/java/com/daka/
│   ├── common/       # 鉴权、统一响应、异常处理和 Web 配置
│   ├── controller/   # REST API 控制器
│   ├── dto/          # 请求参数
│   ├── entity/       # 数据库实体
│   ├── mapper/       # MyBatis-Plus Mapper
│   ├── service/      # 业务逻辑
│   └── vo/           # 统计与响应视图
├── src/main/resources/application.yml
├── sql/schema.sql    # 建库建表及预置分类
├── Dockerfile
└── pom.xml
```

## 本地运行

### 1. 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8.0+

### 2. 初始化数据库

```bash
mysql -u root -p < sql/schema.sql
```

建库脚本会创建 `daka` 数据库、业务表以及系统预置收支分类。

如果尚未创建应用数据库账号，可执行：

```sql
CREATE USER IF NOT EXISTS 'daka'@'localhost' IDENTIFIED BY 'your-database-password';
GRANT ALL PRIVILEGES ON daka.* TO 'daka'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 配置环境变量

本地开发可以修改 `src/main/resources/application.yml`，也可以通过环境变量覆盖配置：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` | `127.0.0.1` | MySQL 地址 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `daka` | 数据库名 |
| `DB_USER` | `daka` | 数据库用户名 |
| `DB_PASSWORD` | `daka@2026` | 数据库密码 |
| `JWT_SECRET` | 内置开发密钥 | JWT 签名密钥 |
| `JWT_EXPIRE_HOURS` | `168` | Token 有效时长，单位小时 |

示例：

```bash
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=daka
export DB_USER=daka
export DB_PASSWORD='your-database-password'
export JWT_SECRET='replace-with-a-long-random-secret'
```

### 4. 启动服务

开发模式：

```bash
mvn spring-boot:run
```

服务默认运行在 `http://127.0.0.1:8080`。

生产构建：

```bash
mvn clean package
java -jar target/daka-backend-1.0.0.jar
```

如需将前端构建产物内嵌到同一个 JAR，请先构建同级目录下的 `../frontend/dist`，Maven 会自动激活 `embed-frontend` profile。

## Docker

构建镜像：

```bash
docker build -t daka-backend:1.0.0 .
```

连接宿主机 MySQL 启动：

```bash
docker run -d \
  --name daka-backend \
  -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_NAME=daka \
  -e DB_USER=daka \
  -e DB_PASSWORD='your-database-password' \
  -e JWT_SECRET='replace-with-a-long-random-secret' \
  daka-backend:1.0.0
```

## API 约定

接口统一以 `/api` 作为前缀。除注册和登录外，请求需要携带：

```http
Authorization: Bearer <token>
```

普通响应格式：

```json
{
  "code": 0,
  "msg": "ok",
  "data": {}
}
```

`code` 为 `0` 表示成功，业务错误会返回非零错误码和可读错误信息。

### 主要接口

| 模块 | 接口前缀 | 说明 |
| --- | --- | --- |
| 认证 | `/api/auth` | 注册、登录、当前用户 |
| 习惯 | `/api/habits` | 习惯 CRUD、打卡、日历和统计 |
| 日记 | `/api/diaries` | 日记 CRUD、心情分布 |
| 目标 | `/api/goals` | 目标 CRUD |
| 待办 | `/api/todos` | 待办 CRUD 与完成切换 |
| 分类 | `/api/categories` | 收支分类查询与管理 |
| 记账 | `/api/bills` | 账单 CRUD、汇总和分类统计 |
| 综合统计 | `/api/stats` | 首页概览、趋势和月度统计 |
| 情侣空间 | `/api/couple` | 邀请码、绑定和共同日历 |
| 数据导出 | `/api/export` | CSV 文件下载 |

已绑定情侣关系的用户可在 GET 请求中传入 `viewUserId`，以只读方式查看伴侣的习惯、日记、目标、待办、账单和统计。

## 部署提示

- 生产环境必须替换 `JWT_SECRET`，并避免使用 `application.yml` 中的开发默认值。
- 数据库账号建议按最小权限原则分配，不要在生产环境使用默认密码。
- 当前 CORS 配置面向本地开发和生产反向代理场景；正式开放公网前，建议将允许来源限制为实际前端域名。
- CSV 导出接口直接返回文件流，不套用统一 JSON 响应结构。

## 相关项目

- 前端：[DoubleC11/daka-frontend](https://github.com/DoubleC11/daka-frontend)
- 后端：[DoubleC11/daka-](https://github.com/DoubleC11/daka-)
