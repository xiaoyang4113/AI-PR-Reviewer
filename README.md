# AI PR Review 助手

> 七牛云 XEngineer 暑期实训营 · 第二批次作品 · 题目三  
> 开发周期：2026 年 5 月 29 日 — 5 月 31 日（72 小时）

AI 驱动的 GitHub Pull Request 代码评审工具，自动获取 PR 变更、智能分析代码问题、生成结构化评审报告。

---

## 目录（CTRL+单击跳转）

- [核心功能](#核心功能)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [数据库设计](#数据库设计)
- [快速开始](#快速开始)
- [API 文档](#api-文档)
- [设计思路说明](#设计思路说明)
  - [模型选择](#模型选择)
  - [上下文获取方式](#上下文获取方式)
  - [误报与漏报控制](#误报与漏报控制)
  - [未来扩展方向](#未来扩展方向)
- [项目结构](#项目结构)
- [Demo 视频和Github项目地址](#Demo 视频和Github项目地址)

---

## 核心功能

| 功能 | 说明 |
|------|------|
| **PR 变更总结** | AI 一句话概括本次 PR 的修改意图、影响范围和涉及模块 |
| **风险代码识别** | 自动扫描安全漏洞、性能隐患、健壮性问题，按严重度标注 CRITICAL / WARNING / INFO |
| **Review 建议生成** | 对每个问题给出详细的修改建议和优化代码示例 |
| **历史记录** | 自动保存评审历史，支持回看和对比 |

### 评审维度

```
1. 安全性  → 硬编码密钥、SQL注入、XSS、CSRF、命令注入、路径遍历
2. 性能    → 内存泄露、死循环、N+1 查询、未分页查询
3. 健壮性  → 空指针、未处理异常、资源未关闭、边界条件缺失
4. 规范性  → 命名不规范、魔法数字、重复代码、过长函数
5. 最佳实践 → 设计模式滥用、耦合过紧、SOLID 原则违反
```

---

## 技术栈

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.4 | 渐进式 JavaScript 框架，Composition API + `<script setup>` |
| Vite | 5.2 | 前端构建工具，HMR 热更新，开发体验极快 |
| Tailwind CSS | 3.4 | 原子化 CSS 框架，暗色主题，响应式布局 |
| PostCSS + Autoprefixer | — | CSS 后处理，自动添加浏览器前缀 |

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | LTS 版本，稳定且生态成熟 |
| Spring Boot | 3.2.5 | 企业级 Web 框架，依赖注入、自动配置 |
| Spring MVC | — | REST 接口，参数校验（Jakarta Validation） |
| MyBatis-Plus | 3.5.7 | ORM 框架，BaseMapper 基础 CRUD + XML 复杂查询 |
| MySQL | 8.0 | 关系型数据库，InnoDB 引擎，utf8mb4 字符集 |
| HikariCP | — | Spring Boot 默认连接池，高性能 |
| Lombok | — | 减少样板代码（@Data, @RequiredArgsConstructor） |
| Jackson | — | JSON 序列化/反序列化 |
| Maven | 3.8+ | 项目构建与依赖管理 |

### AI 与外部集成

| 技术 | 用途 |
|------|------|
| DeepSeek V4 Flash API | AI 代码分析，OpenAI 兼容接口，JSON Mode 结构化输出 |
| GitHub REST API v3 | 获取 PR 元信息（标题）和 Diff（纯文本格式） |
| RestTemplate | Spring 同步 HTTP 客户端，调用外部 API |

### 选型理由

| 决策 | 选择 | 原因 |
|------|------|------|
| 后端语言 | Java | 企业认可度高，评委背景为云厂商 |
| ORM | MyBatis-Plus | 比 JPA 更灵活，SQL 完全可控，XML 映射清晰 |
| 数据库 | MySQL | 事务支持好，索引能力强，MySQL 8.0 性能优秀 |
| AI 模型 | DeepSeek V4 Flash | 1M 上下文、响应快、成本低、JSON Mode |
| 前端 | Vue 3 | 比 React 上手快，Composition API 代码更简洁 |
| CSS | Tailwind | 无需手写 CSS 文件，暗色主题开箱即用 |

---

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                     用户浏览器                              │
│                 Vue 3 + Tailwind CSS                        │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP (JSON)
                          │ /api/review/*
┌─────────────────────────┴───────────────────────────────────┐
│                   Spring Boot 后端                           │
│                                                             │
│  Controller  ──→  Service/Impl  ──→  Mapper  ──→  MySQL    │
│  (接口层)        (业务编排层)        (数据层)                │
│                     │                                       │
│                     ├── GitHubService  ──→ GitHub API       │
│                     └── AiAnalysisService ──→ DeepSeek API  │
└─────────────────────────────────────────────────────────────┘
```

### 后端分层

```
controller/       REST 接口，参数校验（@Valid + @NotNull + @Min），直接返回 ApiResponse<T>
service/          业务接口（ReviewService、GitHubService、AiAnalysisService）
service/impl/     业务实现：GitHub API 调用、AI 分析、流程编排、事务管理
mapper/           MyBatis-Plus 接口 + XML 映射文件
model/entity/     数据库实体（ReviewTask、ReviewComment）
model/dto/        数据传输对象（请求/响应/AI 结果）
model/enums/      枚举（ReviewStatus、RiskLevel）
model/constant/   系统常量（ReviewConstants）
config/           CORS、RestTemplate、MyBatis-Plus 分页、配置属性
exception/        业务异常 + 全局异常处理器
```

---

## 数据库设计

### review_tasks（评审任务表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| repo_url | VARCHAR(500) | GitHub 仓库地址 |
| pr_number | INT | PR 编号 |
| pr_title | TEXT | PR 标题（从 GitHub API 获取） |
| status | VARCHAR(20) | PENDING / PROCESSING / COMPLETED / FAILED |
| summary | TEXT | AI 变更总结 |
| ai_model | VARCHAR(50) | 使用的 AI 模型名称 |
| diff_size | INT | Diff 字符数 |
| file_count | INT | 变更文件数 |
| error_message | TEXT | 失败时的错误信息 |
| created_at / updated_at | DATETIME | 时间戳 |

**索引：** PRIMARY KEY(id), INDEX(repo_url(100), pr_number), INDEX(status), INDEX(created_at)

### review_comments（评审意见表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| task_id | BIGINT FK | 关联 review_tasks.id（CASCADE 删除） |
| file_path | VARCHAR(500) | 变更文件路径 |
| line_number | INT | 问题行号 |
| risk_level | VARCHAR(20) | INFO / WARNING / CRITICAL |
| match_code | TEXT | 触发问题的原始代码 |
| suggestion | TEXT | AI 修改建议 |
| optimized_code | TEXT | 优化代码示例 |
| created_at | DATETIME | 创建时间 |

**关系：** review_tasks 1 : N review_comments

---

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 1. 初始化数据库

```bash
mysql -u root -p < backend/sql/schema.sql
```

### 2. 配置密钥

编辑 `backend/src/main/resources/application.yml`，修改数据库密码和 AI API Key：

```yaml
spring:
  datasource:
    password: 你的数据库密码             # 改这里

app:
  ai:
    api-key: 你的DeepSeek_API_Key        # 改这里
```

### 3. 启动后端

```bash
cd backend
./mvnw spring-boot:run
# 服务运行在 http://localhost:8080
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
# 页面运行在 http://localhost:5173
```

### 5. 使用

输入 GitHub 公开仓库的 PR 链接 → 点击「开始智能评审」→ 等待 AI 分析 → 查看评审结果。

---

## API 文档

### 创建评审任务

```
POST /api/review/create
Content-Type: application/json

{
  "repoUrl": "https://github.com/owner/repo",
  "prNumber": 42
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "repoUrl": "https://github.com/owner/repo",
    "prNumber": 42,
    "prTitle": "feat: add user authentication",
    "status": "COMPLETED",
    "summary": "本次 PR 实现了用户认证功能，涉及 3 个文件，新增 JWT Token 生成与验证中间件。",
    "aiModel": "deepseek-v4-flash",
    "fileCount": 3,
    "diffSize": 6873,
    "comments": [
      {
        "id": 1,
        "filePath": "src/auth/jwt.js",
        "lineNumber": 45,
        "riskLevel": "CRITICAL",
        "matchCode": "const secret = require('./config').jwtSecret; // 从配置文件读取",
        "suggestion": "密钥不应硬编码在代码中，建议从环境变量读取以避免凭证泄露。",
        "optimizedCode": "const secret = process.env.JWT_SECRET;"
      }
    ],
    "createdAt": "2026-05-29T22:30:00"
  }
}
```

### 其他接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/review/{id}` | 查询评审详情 |
| GET | `/api/review/list?page=1&size=10` | 分页查询评审历史 |

---

## 设计思路说明

### 模型选择

**选择 DeepSeek V4 Flash**，理由如下：

| 考量维度 | DeepSeek V4 Flash | DeepSeek V4 Pro |
|---------|-------------------|-----------------|
| 上下文窗口 | 1M tokens | 1M tokens |
| 响应速度 | 快（适合交互式场景） | 较慢 |
| 价格 | ￥**0.02元**/百万输入 tokens | ￥**0.025元**/百万输入 tokens |
| 代码分析质量 | 满足 Code Review 需求 | 更高，但性价比低 |

**结论：** Flash 模型在代码审查场景下质量足够、响应快、成本低。追求更高精度时可一键切换为 Pro。

**模型迁移策略：** 通过 `application-dev.yml` 的 `app.ai.model` 配置即可切换，无需改代码。DeepSeek 弃用旧模型名（`deepseek-chat`）的截止日期为 2026 年 7 月，届时需确认新模型名。

### 上下文获取方式

本系统通过以下策略确保 AI 获得充分的代码上下文：

1. **完整 Diff 获取：** 调用 GitHub API 的 `Accept: application/vnd.github.v3.diff` 头获取完整 diff 文本，包含文件路径、行号、变更内容。

2. **智能截断：** 对于超大 PR（diff > 8000 字符），按字符数截断并标注 `[...diff 内容过长，已截断...]`，防止超出模型 token 限制。

3. **结构化 Prompt：** System Prompt 明确定义 5 个评审维度、3 级风险标准、JSON 输出格式。要求 AI 只报告"实际存在"的问题，减少臆测。

4. **降级兜底：** AI API 不可用时自动返回 Mock 演示数据，确保系统在任何情况下都能展示完整流程。

**关于按文件/Chunk 拆分：** 当前版本采用统一截断策略。后续可优化为按文件拆分 + 并发分析 + 结果汇总，进一步提升大 PR 的分析质量。

### 误报与漏报控制

| 策略 | 具体措施 |
|------|---------|
| **Prompt 约束** | 明确要求"只报告实际存在的问题，不要臆想或推测" |
| **三级风险分类** | CRITICAL（必须修复）、WARNING（建议修复）、INFO（建议优化），降低对低优先级建议的过度关注 |
| **Mock 对比验证** | 开发过程中使用 Mock 数据验证输出格式，确保 AI 输出可控 |
| **空评论处理** | 代码质量良好时允许返回空数组，避免强行找问题增加误报 |

### 未来扩展方向

**1. 多平台支持（策略模式）**

```java
public interface CodePlatformService {
    DiffResult fetchDiff(String repoUrl, Integer prNumber);
}
// 已实现：GitHubService
// 可扩展：GitLabService、GiteeService、BitbucketService
```

**2. 多模型切换**

通过配置 `app.ai.model` 动态切换模型（DeepSeek / OpenAI / 通义千问 / 文心一言），无需改代码。

**3. 异步 + WebSocket 实时推送**

当前为同步阻塞模式，后续可改为异步架构：

```
前端提交 PR → 后端立即返回 taskId → @Async 后台分析
                                          ↓
                               WebSocket 实时推送进度
                               ├── "正在获取 PR Diff..."
                               ├── "正在 AI 分析 src/UserService.java (2/5)..."
                               └── "分析完成" → 前端自动渲染结果
```

技术方案：
- 后端 `@Async` + `ThreadPoolTaskExecutor` 异步执行评审任务
- Spring WebSocket + STOMP 协议推送分析进度到前端
- 前端 `SockJS` + `stomp.js` 订阅任务频道，实时更新进度条
- 即使中途断网，任务继续在后端执行，重连后可查询结果

优势：用户不用等待 30 秒，提交后即可关闭页面；大 PR 分析进度一目了然。

**4. 按文件拆分分析**

大 PR 按文件拆分 → 多线程并发调用 AI → 汇总结果。提升大 PR 分析完整性。

**5. Review 规则自定义**

支持团队自定义评审规则集（如某团队关注安全 > 性能，可调整 Prompt 权重）。

**6. IDE 插件**

开发 VS Code / IntelliJ IDEA 插件，评审结果直接在 IDE 内展示。

---

## 项目结构

```
ai-pr-reviewer/
├── backend/
│   ├── pom.xml
│   ├── sql/schema.sql                         # 数据库初始化脚本
│   └── src/main/
│       ├── java/com/aiprreviewer/
│       │   ├── AiPrReviewerApplication.java   # 启动入口
│       │   ├── config/                         # Spring 配置
│       │   │   ├── WebConfig.java              # CORS 跨域
│       │   │   ├── RestTemplateConfig.java     # HTTP 客户端超时
│       │   │   ├── MyBatisPlusConfig.java      # 分页插件 + 事务模板
│       │   │   ├── GitHubProperties.java       # GitHub 配置属性
│       │   │   └── AiProperties.java           # AI 配置属性
│       │   ├── controller/
│       │   │   └── ReviewController.java       # REST 接口
│       │   ├── service/
│       │   │   ├── ReviewService.java          # 评审编排接口
│       │   │   ├── GitHubService.java          # GitHub 接口
│       │   │   ├── AiAnalysisService.java      # AI 分析接口
│       │   │   └── impl/
│       │   │       ├── ReviewServiceImpl.java   # 核心业务流程
│       │   │       ├── GitHubServiceImpl.java   # GitHub API 交互
│       │   │       └── AiAnalysisServiceImpl.java # AI 调用与解析
│       │   ├── mapper/                         # MyBatis 数据访问
│       │   ├── model/
│       │   │   ├── entity/                     # 数据库实体
│       │   │   ├── dto/                        # 数据传输对象
│       │   │   ├── enums/                      # 枚举
│       │   │   └── constant/                   # 系统常量
│       │   └── exception/                      # 异常处理
│       └── resources/
│           ├── application.yml                 # 公共配置（可提交 Git）
│           ├── application-dev.yml             # 开发配置（密钥，不入 Git）
│           └── mapper/                         # MyBatis XML
└── frontend/
    ├── package.json
    ├── vite.config.js                          # Vite + API 代理
    ├── index.html
    └── src/
        ├── main.js
        ├── App.vue                             # 主页面
        ├── style.css                           # 全局样式
        └── components/
            ├── ReviewForm.vue                  # 输入表单
            ├── SummaryCard.vue                 # 总结卡片
            └── CommentCard.vue                 # 评论卡片
```

---

## Demo 视频和Github项目地址

> 哔哩哔哩视频链接：https://www.bilibili.com/video/BV1UbV56dEY9
>
> Github项目地址：[xiaoyang4113/AI-PR-Reviewer](https://github.com/xiaoyang4113/AI-PR-Reviewer)
>
> 注：Ctrl+鼠标左键单击跳转

---

## 许可证

本作品为七牛云 XEngineer 暑期实训营参赛作品。

---

> **提交人：** xiaoyang4113  
> **开发时间：** 2026.05.29 — 2026.05.31
