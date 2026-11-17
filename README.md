# 学而刷题 · 大学生在线刷题系统

**基于 Spring Boot 与 Vue3 的前后端分离刷题系统**

面向在校大学生的在线练习与模拟考试平台，支持分类练习、即时判分、
错题本、收藏夹、模拟考试、学习统计与后台管理。

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
| --- | --- | --- |
| Spring Boot | 3.3.5 | 应用框架 |
| Spring MVC | 6.x | RESTful 接口 |
| Spring Data JPA | 3.x | 数据持久化 |
| Spring Security | 6.x | BCrypt 密码加密 |
| Hibernate | 6.x | ORM |
| MySQL | 8.0.28 | 生产数据库 |
| H2 | 2.x | 本地开发数据库（免安装） |
| Jakarta Validation | 3.x | 参数校验 |
| Java | 17 | 运行环境 |

### 前端

| 技术 | 版本 | 用途 |
| --- | --- | --- |
| Vue | 3.5 | 前端框架（Composition API） |
| Vue Router | 4.x | 路由与导航守卫 |
| Pinia | 2.x | 状态管理 |
| Axios | 1.x | HTTP 请求 |
| Vite | 6.x | 构建与开发服务器 |
| KaTeX | 0.16 | 数学公式渲染 |

---

## 目录结构

```
quiz-springboot/
├── backend/                        # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/huyuhui/quiz/
│       │   ├── QuizApplication.java        # 启动类
│       │   ├── bootstrap/
│       │   │   ├── AdminSeeder.java        # 默认管理员初始化
│       │   │   └── QuestionSeeder.java     # 题库初始化
│       │   ├── common/                     # 统一响应、异常处理
│       │   ├── config/WebConfig.java       # CORS、拦截器、密码编码器
│       │   ├── controller/
│       │   │   ├── AdminController.java    # 后台管理接口
│       │   │   ├── AuthController.java     # 认证接口
│       │   │   ├── PracticeController.java # 练习接口
│       │   │   └── QuestionController.java # 题库接口
│       │   ├── entity/                     # 实体层（6 张表）
│       │   ├── repository/                 # 数据访问层
│       │   ├── security/
│       │   │   ├── AdminGuard.java         # 管理员权限校验
│       │   │   └── AuthInterceptor.java    # Token 鉴权
│       │   └── service/
│       │       ├── AdminService.java       # 后台业务
│       │       ├── AuthService.java        # 认证业务
│       │       ├── PracticeService.java    # 练习业务
│       │       └── QuestionService.java    # 题库与判分
│       └── resources/
│           ├── application.yml
│           └── data/questions.json  ← 题库权威副本
│
├── frontend/                       # Vue3 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── api/
│       │   ├── index.js            # 业务接口
│       │   └── admin.js            # 后台接口
│       ├── router/index.js
│       ├── stores/auth.js
│       ├── styles/main.css
│       ├── components/
│       │   ├── AppHeader.vue
│       │   ├── MathText.vue        # LaTeX 公式渲染
│       │   └── QuestionCard.vue
│       └── views/
│           ├── AdminView.vue       # 后台管理
│           ├── ExamView.vue        # 模拟考试
│           ├── FavoriteView.vue    # 收藏夹
│           ├── HomeView.vue        # 首页
│           ├── LoginView.vue       # 登录注册
│           ├── PracticeView.vue    # 分类练习
│           ├── StatisticsView.vue  # 学习统计
│           └── WrongView.vue       # 错题本
│
├── docs/
│   ├── sql/
│   │   ├── schema.sql              # MySQL 建表脚本
│   │   └── data-questions.sql      # 100 道题库数据
│   ├── MySQL使用说明.md             # MySQL 启动、连接、排错
│   ├── 系统设计说明.md              # 论文配套材料
│   └── screenshots/                # 界面截图
│
├── start-mysql.cmd                 # 一键启动 MySQL（免安装版）
│
└── tools/
    ├── export-questions.js         # 导出题库
    ├── gen-sql.js                  # 生成 SQL
    ├── verify-api.js               # 接口自动化测试
    ├── verify-admin.js             # 后台接口测试
    ├── verify-browser.js           # 浏览器端到端测试
    └── verify-admin-ui.js          # 后台界面测试
```

---

## 快速开始

### 环境要求

- JDK 17 或更高版本
- Node.js 18 或更高版本
- MySQL 8.0（本项目已内置免安装版，可选）

### 方式一：H2 数据库（最简单，无需 MySQL）

**1. 启动后端**

```bash
cd backend
mvn spring-boot:run
```

启动后自动创建 H2 数据库（`backend/data/quizdb.mv.db`）并导入 100 道题。

后端地址：<http://localhost:8080>

**2. 启动前端**

```bash
cd frontend
npm install
npm run dev
```

前端地址：<http://localhost:5173>

**3. 登录**

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `admin123` |
| 学生 | 自行注册 | — |

---

### 方式二：MySQL 数据库（推荐用于答辩演示）

**1. 启动 MySQL**

双击项目根目录的 **`start-mysql.cmd`**（保持窗口开启）。

**2. 导入数据库**

```cmd
mysql -u root -proot --default-character-set=utf8mb4 -e "source docs/sql/schema.sql"
mysql -u root -proot --default-character-set=utf8mb4 -e "source docs/sql/data-questions.sql"
```

> ⚠️ **不要用 `Get-Content | mysql` 这类管道导入**。
> PowerShell 会把 UTF-8 中文转成乱码，导致 SQL 语法错误。
> 必须用 `source` 让 mysql 客户端自己读文件。

**3. 用 mysql profile 启动**

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

或打包后运行：

```bash
mvn clean package -DskipTests
java -jar target/quiz-server-1.0.0.jar --spring.profiles.active=mysql
```

> MySQL 详细说明（启动/停止、账号、排错、重装）见
> [`docs/MySQL使用说明.md`](docs/MySQL使用说明.md)。

---

## 功能说明

| 模块 | 功能 |
| --- | --- |
| **用户认证** | 注册、登录、登出；BCrypt 密码加密；连续失败 5 次锁定 10 分钟 |
| **分类练习** | 按学科 / 章节 / 题型 / 难度筛选；即时判分；显示正确答案与解析 |
| **模拟考试** | 随机抽题、限时倒计时、答题卡、自动交卷、成绩单与逐题解析 |
| **错题本** | 答错自动收录；答对后标记「已订正」；可移除 |
| **收藏夹** | 练习中一键收藏；集中复习 |
| **学习统计** | 累计答题、正确率、学科维度掌握度、考试历史 |
| **后台管理** | 管理员专属：题目增删改查、用户与角色管理 |
| **公式渲染** | 题干、选项、解析均支持 LaTeX（KaTeX 渲染） |

### 后台管理

管理员登录后，导航栏出现橙色「后台管理」入口（学生账号看不到）。

**默认管理员账号**（首次启动自动创建）：`admin` / `admin123`

> 可用环境变量 `QUIZ_ADMIN_USERNAME` / `QUIZ_ADMIN_PASSWORD` 覆盖。
> **正式部署请务必修改默认密码。**

| 标签页 | 功能 |
| --- | --- |
| 总览 | 用户数、题目数、考试记录、近 7 天新增；题库按题型与难度分布 |
| 题目管理 | 分页列表、关键词搜索、按学科筛选、新增、编辑、删除 |
| 用户管理 | 用户列表、搜索、设为管理员 / 降为学生、重置密码、删除用户 |

**安全设计**

- 所有管理接口都在后端校验 `role == ADMIN`，学生访问返回 **403**
- 前端隐藏入口只是体验优化，**不作为安全边界**
- 管理员不能降级或删除自己的账号（防止把自己锁在系统外）
- 重置密码后该用户的所有会话立即失效，需重新登录

### 题型支持

| 题型 | 说明 | 答案存储 |
| --- | --- | --- |
| 单选题 | 四选一 | 选项下标数组，如 `[1]` |
| 多选题 | 多选 | 多个下标，如 `[0,2]` |
| 判断题 | 正确 / 错误 | 下标，`[0]` 正确 / `[1]` 错误 |
| 填空题 | 文本输入 | 答案文本，忽略首尾空白与大小写 |

---

## 接口文档

统一响应格式：

```json
{ "code": 0, "message": "ok", "data": { } }
```

`code = 0` 表示成功。鉴权通过请求头 `Authorization: Bearer <token>` 传递。

### 认证接口

| 方法 | 路径 | 说明 | 需登录 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 注册 | 否 |
| POST | `/api/auth/login` | 登录 | 否 |
| POST | `/api/auth/logout` | 登出 | 是 |
| GET | `/api/auth/me` | 当前用户信息 | 是 |

**注册 / 登录请求体**

```json
{ "username": "student01", "password": "123456", "nickname": "张三" }
```

**响应**

```json
{
  "code": 0,
  "data": {
    "token": "64 位十六进制字符串",
    "expiresAt": "2026-11-24T12:00:00",
    "user": { "id": 1, "username": "student01", "nickname": "张三", "role": "STUDENT" }
  }
}
```

### 题库接口（无需登录）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/meta/subjects` | 学科列表 |
| GET | `/api/meta/chapters?subject=高等数学` | 章节列表 |
| GET | `/api/meta/questions` | 分页查询题目 |
| GET | `/api/meta/questions/by-subject?subject=高等数学` | 按学科取全部题目 |
| GET | `/api/meta/questions/random?limit=10` | 随机取题 |
| POST | `/api/meta/questions/batch` | 按 id 批量取题 |

分页参数：`subject`、`chapter`、`type`、`difficulty`、`keyword`、`page`（从 0 开始）、`size`

### 练习接口（需登录）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/practice/submit` | 提交答案并判分 |
| POST | `/api/practice/favorite/{questionId}` | 收藏 / 取消收藏 |
| GET | `/api/practice/wrong` | 错题本 |
| GET | `/api/practice/favorites` | 收藏夹 |
| DELETE | `/api/practice/mark/{questionId}?type=WRONG` | 移除错题或收藏 |
| GET | `/api/practice/statistics` | 学习统计 |
| POST | `/api/practice/exam` | 保存考试成绩 |
| GET | `/api/practice/exam/history` | 考试历史 |

**提交答案请求体**

```json
{ "questionId": 1, "answer": [1] }
```

填空题传字符串：`{ "questionId": 5, "answer": "1" }`

**判分响应**

```json
{
  "code": 0,
  "data": {
    "correct": true,
    "correctAnswer": "[1]",
    "analysis": "这是第一个重要极限……",
    "attempts": 1,
    "correctCount": 1
  }
}
```

### 后台管理接口（需 ADMIN 角色）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/dashboard` | 总览数据 |
| GET | `/api/admin/questions` | 题目分页列表 |
| GET | `/api/admin/questions/stats` | 题库构成统计 |
| POST | `/api/admin/questions` | 新增题目 |
| PUT | `/api/admin/questions/{id}` | 修改题目 |
| DELETE | `/api/admin/questions/{id}` | 删除题目 |
| POST | `/api/admin/questions/batch-delete` | 批量删除 |
| GET | `/api/admin/users` | 用户列表 |
| PUT | `/api/admin/users/{id}/role` | 修改角色 |
| POST | `/api/admin/users/{id}/unlock` | 解锁账号 |
| POST | `/api/admin/users/{id}/reset-password` | 重置密码 |
| DELETE | `/api/admin/users/{id}` | 删除用户 |

**新增题目请求体示例**

```json
{
  "subject": "高等数学",
  "chapter": "极限与连续",
  "type": "single",
  "difficulty": 1,
  "stem": "极限 $\\lim\\limits_{x \\to 0} \\dfrac{\\sin x}{x}$ 的值为：",
  "options": ["$0$", "$1$", "$\\infty$", "不存在"],
  "answer": [1],
  "analysis": "这是第一个重要极限。",
  "knowledge": ["重要极限"]
}
```

> `options` / `answer` / `knowledge` 也可传 JSON 字符串，后端都兼容。
> `code` 字段可省略，后端会自动生成（如 `q-1`）。

---

## 数据库设计

### E-R 关系

```
sys_user (用户)
   │ 1
   ├──────< sys_session (会话)
   ├──────< quiz_record (答题记录) >────── 1 quiz_question (题目)
   ├──────< quiz_mark   (错题/收藏) >────── 1 quiz_question
   └──────< quiz_exam   (考试记录)
```

### 表说明

| 表名 | 说明 | 关键索引 |
| --- | --- | --- |
| `sys_user` | 用户 | `username` 唯一 |
| `sys_session` | 登录会话 | `token` 唯一，`user_id` 关联 |
| `quiz_question` | 题目 | `code` 唯一；`subject`/`chapter`/`type` 索引 |
| `quiz_record` | 答题记录 | `(user_id, question_id)` 唯一 |
| `quiz_mark` | 错题本与收藏 | `(user_id, question_id, type)` 唯一 |
| `quiz_exam` | 考试记录 | `user_id` 索引 |

> 完整建表语句（含字段注释、索引、外键）见 `docs/sql/schema.sql`。

---

## 测试

### 接口自动化测试

先启动后端，再执行：

```bash
node tools/verify-api.js          # 35 项：题库、认证、鉴权、判分、错题、统计、考试
node tools/verify-admin.js        # 36 项：后台总览、题目增删改、用户管理、权限隔离
```

### 浏览器端到端测试

先启动前后端，再执行：

```bash
node tools/verify-browser.js      # 28 项：页面渲染、注册、练习、公式、考试
node tools/verify-admin-ui.js     # 22 项：后台界面、权限隐藏、无 JS 异常
```

> 浏览器测试依赖 `ws` 模块（仅测试用），通过 `npm i ws` 安装。

**测试总计 121 项断言，全部通过。**

---

## 安全设计

| 措施 | 实现 |
| --- | --- |
| 密码存储 | BCrypt 加盐哈希，不存明文 |
| 会话管理 | 64 位随机 token（SecureRandom），服务端校验有效期 |
| 防爆破 | 同一账号连续失败 5 次锁定 10 分钟 |
| 越权防护 | 所有数据操作强制带 `userId` 条件，无法访问他人数据 |
| 后台权限 | 后端校验 `role == ADMIN`，学生访问返回 403 |
| 参数校验 | Jakarta Validation 注解 + 业务层完整性校验 |
| SQL 注入 | JPA 参数化查询，不拼接 SQL |
| XSS 防护 | 公式渲染设置 `trust: false`，禁用 `\href` 等危险命令 |
| 跨域控制 | CORS 白名单限制来源 |

---

## 常见问题

**Q：启动报端口被占用？**

修改 `application.yml` 中 `server.port`，或启动时加 `--server.port=8081`。

**Q：前端页面空白 / 请求 404？**

确认后端已启动在 8080 端口。开发环境由 Vite 代理 `/api` 到后端，
若改过后端端口，需同步修改 `vite.config.js` 中的 `target`。

**Q：H2 数据库文件在哪？**

`backend/data/quizdb.mv.db`。删除该文件即可重置数据（题目会重新导入）。

**Q：看不到「后台管理」入口？**

只有 `role = ADMIN` 的账号才显示。用默认账号 `admin` / `admin123` 登录，
或让管理员把你的账号设为管理员。

**Q：如何清空做题记录？**

MySQL：
```sql
TRUNCATE TABLE quiz_record;
TRUNCATE TABLE quiz_mark;
TRUNCATE TABLE quiz_exam;
```
H2：删除 `backend/data/` 目录后重启。

**Q：MathText 组件为什么不用 markdown-it？**

题库数据是受控的（自己编写），只需要公式定界符解析。
自己实现约 80 行可以精确控制转义与 `trust` 设置，避免引入依赖与 XSS 风险。

---

## 许可

本项目为毕业设计作品，仅供学习参考。