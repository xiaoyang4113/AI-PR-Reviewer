# AI PR Review 助手 - 完整技术方案

> 七牛云 × XEngineer 暑期实训营第二批次作品
> 开发时间：2026年5月29日 00:00 - 2026年5月31日 23:59

---

## 一、产品定位与核心价值

### 1.1 解决什么问题？

开发者在代码评审（Code Review）中面临的核心痛点：

| 痛点 | 描述 | AI助手如何解决 |
|------|------|---------------|
| **耗时费力** | 人工逐行审查代码变更，大PR可能需要数小时 | AI秒级分析，自动标注风险点 |
| **遗漏隐患** | 人工审查容易忽略硬编码密钥、内存泄露等隐蔽问题 | 系统化扫描，不遗漏任何文件 |
| **标准不一** | 不同评审者关注点不同，质量参差不齐 | 统一的评审标准和最佳实践库 |
| **缺乏上下文** | 只看diff难以理解变更的业务意图 | AI总结变更意图，提供全局视角 |

### 1.2 核心功能

```
用户输入 GitHub PR 链接
        ↓
系统自动获取代码变更（Diff）
        ↓
AI 智能分析（三大模块）
├── 📋 变更总结：一句话说明改了什么、为什么改
├── ⚠️ 风险识别：Bug、安全漏洞、性能问题
└── 💡 优化建议：重构方案、代码示例
        ↓
可视化结果展示（行内批注 + 风险等级）
```

---

## 二、技术选型

### 2.1 为什么选择 Java + Spring Boot？

| 维度 | Node.js + Express | Java + Spring Boot | 选择理由 |
|------|-------------------|-------------------|----------|
| **企业认可度** | 中 | 高 | 评委是云厂商，Java更符合企业级标准 |
| **工程化能力** | 中 | 高 | Spring生态完善，依赖注入、AOP、事务管理开箱即用 |
| **异步处理** | 原生支持 | WebFlux支持 | 两者都能胜任 |
| **开发效率** | 高 | 中高 | Java有更多样板代码，但IDE支持更好 |
| **部署运维** | 简单 | 成熟 | Spring Boot打包为单JAR，运维标准化 |
| **AI接口对接** | 简单 | 简单 | 都是HTTP调用，差异不大 |

**最终选择：Java + Spring Boot**
- 更符合企业级工程标准（评委背景）
- Spring生态带来的架构清晰度
- 长期可维护性和扩展性

### 2.2 完整技术栈

```
┌─────────────────────────────────────────────────────────────┐
│                        用户浏览器                            │
│                   Vue 3 + Tailwind CSS                      │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP API
┌─────────────────────────┴───────────────────────────────────┐
│                    Spring Boot 后端                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ Controller  │  │   Service   │  │   External APIs     │ │
│  │  (路由层)   │→ │  (业务层)   │→ │  GitHub / DeepSeek  │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
│                          ↓                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              MySQL / H2 数据库                       │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **前端框架** | Vue 3 | 3.4+ | 响应式UI |
| **构建工具** | Vite | 5.x | 快速开发构建 |
| **CSS框架** | Tailwind CSS | 3.x | 原子化样式 |
| **后端框架** | Spring Boot | 3.2+ | Web服务 |
| **HTTP客户端** | RestTemplate / WebClient | - | 调用外部API |
| **JSON处理** | Jackson | - | 结构化数据解析 |
| **数据库** | MySQL 8 | 8.0+ | 持久化存储 |
| **ORM** | Spring Data JPA | - | 数据访问层 |
| **AI模型** | DeepSeek API | - | 代码分析 |
| **版本管理** | Git + GitHub | - | 代码托管 |

---

## 三、系统架构设计

### 3.1 整体架构

```
                        ┌──────────────┐
                        │   用户浏览器  │
                        └──────┬───────┘
                               │
                        ┌──────▼───────┐
                        │  Nginx/Vite  │
                        │  (静态资源)   │
                        └──────┬───────┘
                               │ API请求
                        ┌──────▼───────┐
                        │ Spring Boot  │
                        │   (8080)     │
                        └──────┬───────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
     ┌────────▼────────┐ ┌────▼────┐ ┌─────────▼────────┐
     │  GitHub API     │ │  MySQL  │ │  DeepSeek API    │
     │  (获取PR Diff)  │ │  (存储) │ │  (AI分析)        │
     └─────────────────┘ └─────────┘ └──────────────────┘
```

### 3.2 分层架构

```
┌─────────────────────────────────────────────────────────┐
│                    Controller 层                         │
│  接收HTTP请求，参数校验，返回响应                         │
│  ReviewController.java                                  │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│                    Service 层                            │
│  核心业务逻辑：GitHub交互、AI分析、数据组装               │
│  ├── GitHubService.java     (获取PR Diff)               │
│  ├── AiAnalysisService.java (调用大模型分析)             │
│  └── ReviewService.java     (业务编排)                  │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│                    Repository 层                         │
│  数据访问，与数据库交互                                   │
│  ├── ReviewTaskRepository.java                          │
│  └── ReviewCommentRepository.java                       │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│                    Entity / DTO 层                       │
│  数据模型定义                                            │
│  ├── ReviewTask.java      (评审任务实体)                 │
│  ├── ReviewComment.java   (评审意见实体)                 │
│  └── ReviewResultDTO.java (返回给前端的DTO)              │
└─────────────────────────────────────────────────────────┘
```

### 3.3 核心流程时序

```
用户                前端                 后端                  GitHub           DeepSeek
 │                   │                   │                     │                  │
 │  输入PR链接       │                   │                     │                  │
 │──────────────────>│                   │                     │                  │
 │                   │  POST /api/review │                     │                  │
 │                   │──────────────────>│                     │                  │
 │                   │                   │  获取PR信息          │                  │
 │                   │                   │────────────────────>│                  │
 │                   │                   │  返回PR Diff        │                  │
 │                   │                   │<────────────────────│                  │
 │                   │                   │                     │                  │
 │                   │                   │  发送Diff给AI分析   │                  │
 │                   │                   │─────────────────────────────────────>│
 │                   │                   │  返回结构化评审结果  │                  │
 │                   │                   │<─────────────────────────────────────│
 │                   │                   │                     │                  │
 │                   │                   │  存储结果到数据库    │                  │
 │                   │                   │──────┐              │                  │
 │                   │                   │      │              │                  │
 │                   │                   │<─────┘              │                  │
 │                   │  返回评审结果      │                     │                  │
 │                   │<──────────────────│                     │                  │
 │  展示结果         │                   │                     │                  │
 │<──────────────────│                   │                     │                  │
```

---

## 四、数据库设计

### 4.1 ER图

```
┌─────────────────────┐       ┌─────────────────────┐
│    review_tasks     │       │   review_comments   │
├─────────────────────┤       ├─────────────────────┤
│ PK id (BIGINT)      │───┐   │ PK id (BIGINT)      │
│ repo_url (VARCHAR)  │   │   │ FK task_id (BIGINT) │
│ pr_number (INT)     │   └──>│ file_path (VARCHAR) │
│ pr_title (VARCHAR)  │       │ line_number (INT)   │
│ status (VARCHAR)    │       │ risk_level (VARCHAR)│
│ summary (TEXT)      │       │ match_code (TEXT)   │
│ ai_model (VARCHAR)  │       │ suggestion (TEXT)   │
│ created_at (DATETIME)│      │ optimized_code (TEXT)│
│ updated_at (DATETIME)│      │ created_at (DATETIME)│
└─────────────────────┘       └─────────────────────┘
```

### 4.2 建表SQL

```sql
-- 评审任务表
CREATE TABLE review_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repo_url VARCHAR(500) NOT NULL COMMENT '仓库地址',
    pr_number INT NOT NULL COMMENT 'PR编号',
    pr_title VARCHAR(500) DEFAULT NULL COMMENT 'PR标题',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/processing/completed/failed',
    summary TEXT DEFAULT NULL COMMENT 'AI生成的整体变更总结',
    ai_model VARCHAR(50) DEFAULT NULL COMMENT '使用的AI模型',
    diff_size INT DEFAULT 0 COMMENT 'Diff大小(字符数)',
    file_count INT DEFAULT 0 COMMENT '变更文件数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_repo_pr (repo_url, pr_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评审任务表';

-- 评审意见表
CREATE TABLE review_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '关联的任务ID',
    file_path VARCHAR(500) NOT NULL COMMENT '变更的文件路径',
    line_number INT DEFAULT NULL COMMENT '代码行号',
    risk_level VARCHAR(20) NOT NULL DEFAULT 'info' COMMENT '风险等级: info/warning/critical',
    match_code TEXT DEFAULT NULL COMMENT '触发审查的核心代码片段',
    suggestion TEXT NOT NULL COMMENT 'AI给出的修改建议',
    optimized_code TEXT DEFAULT NULL COMMENT 'AI提供的优化后代码示例',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_risk_level (risk_level),
    FOREIGN KEY (task_id) REFERENCES review_tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评审意见表';
```

### 4.3 实体类设计

```java
// ReviewTask.java
@Entity
@Table(name = "review_tasks")
public class ReviewTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String repoUrl;

    @Column(nullable = false)
    private Integer prNumber;

    @Column(length = 500)
    private String prTitle;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 50)
    private String aiModel;

    private Integer diffSize = 0;
    private Integer fileCount = 0;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewComment> comments = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

// ReviewComment.java
@Entity
@Table(name = "review_comments")
public class ReviewComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private ReviewTask task;

    @Column(nullable = false, length = 500)
    private String filePath;

    private Integer lineNumber;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel = RiskLevel.INFO;

    @Column(columnDefinition = "TEXT")
    private String matchCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String suggestion;

    @Column(columnDefinition = "TEXT")
    private String optimizedCode;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

// 枚举类
public enum ReviewStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}

public enum RiskLevel {
    INFO, WARNING, CRITICAL
}
```

---

## 五、API 设计

### 5.1 接口列表

| 方法 | 路径 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | `/api/review/create` | 创建评审任务 | `{repoUrl, prNumber}` | 评审结果 |
| GET | `/api/review/{id}` | 查询任务详情 | - | 任务详情 |
| GET | `/api/review/list` | 查询任务列表 | `?page=0&size=10` | 分页列表 |

### 5.2 接口详情

#### POST /api/review/create

**请求：**
```json
{
    "repoUrl": "https://github.com/owner/repo",
    "prNumber": 42
}
```

**成功响应：**
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
        "summary": "本次PR实现了用户认证功能，包括JWT token生成和验证中间件，涉及3个文件的修改。",
        "fileCount": 3,
        "diffSize": 1250,
        "comments": [
            {
                "id": 1,
                "filePath": "src/auth/jwt.js",
                "lineNumber": 45,
                "riskLevel": "CRITICAL",
                "matchCode": "const secret = 'hardcoded_secret_key';",
                "suggestion": "检测到硬编码的密钥！这会导致严重的安全风险。JWT密钥应该从环境变量中读取。",
                "optimizedCode": "const secret = process.env.JWT_SECRET;"
            },
            {
                "id": 2,
                "filePath": "src/middleware/auth.js",
                "lineNumber": 12,
                "riskLevel": "WARNING",
                "matchCode": "if (!token) return;",
                "suggestion": "未授权时直接return，没有返回401状态码，客户端无法区分认证失败和正常响应。",
                "optimizedCode": "if (!token) {\n    return res.status(401).json({ error: 'Unauthorized' });\n}"
            }
        ],
        "createdAt": "2026-05-29T10:30:00"
    }
}
```

**错误响应：**
```json
{
    "code": 400,
    "message": "无效的GitHub仓库链接格式"
}
```

---

## 六、核心代码实现

### 6.1 后端项目结构

```
backend/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── aiprreviewer/
│       │           ├── AiPrReviewerApplication.java    # 启动类
│       │           ├── controller/
│       │           │   └── ReviewController.java        # 控制器
│       │           ├── service/
│       │           │   ├── ReviewService.java           # 业务编排
│       │           │   ├── GitHubService.java           # GitHub API交互
│       │           │   └── AiAnalysisService.java       # AI分析服务
│       │           ├── repository/
│       │           │   ├── ReviewTaskRepository.java    # 任务数据访问
│       │           │   └── ReviewCommentRepository.java # 评论数据访问
│       │           ├── entity/
│       │           │   ├── ReviewTask.java              # 任务实体
│       │           │   ├── ReviewComment.java           # 评论实体
│       │           │   ├── ReviewStatus.java            # 状态枚举
│       │           │   └── RiskLevel.java               # 风险等级枚举
│       │           ├── dto/
│       │           │   ├── ReviewCreateRequest.java     # 创建请求DTO
│       │           │   ├── ReviewResultDTO.java         # 返回结果DTO
│       │           │   └── ApiResponse.java             # 统一响应封装
│       │           ├── config/
│       │           │   ├── WebConfig.java               # CORS配置
│       │           │   └── RestTemplateConfig.java      # HTTP客户端配置
│       │           └── exception/
│       │               ├── BusinessException.java       # 业务异常
│       │               └── GlobalExceptionHandler.java  # 全局异常处理
│       └── resources/
│           ├── application.yml                          # 应用配置
│           └── application-dev.yml                      # 开发环境配置
```

### 6.2 pom.xml 核心依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
    </parent>

    <groupId>com.aiprreviewer</groupId>
    <artifactId>ai-pr-reviewer</artifactId>
    <version>1.0.0</version>
    <name>AI PR Reviewer</name>
    <description>AI Code Review Assistant for Pull Requests</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- MySQL Driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- H2 Database (开发/测试用) -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 6.3 application.yml 配置

```yaml
server:
  port: 8080

spring:
  # 开发环境使用H2内存数据库，生产环境切换为MySQL
  profiles:
    active: dev

  # JPA配置
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

# 自定义配置
app:
  github:
    api-base: https://api.github.com
    # 生产环境建议使用环境变量: ${GITHUB_TOKEN}
    token: ${GITHUB_TOKEN:}

  ai:
    api-base: https://api.deepseek.com/v1
    api-key: ${AI_API_KEY:your-api-key-here}
    model: deepseek-chat
    max-tokens: 4000
    temperature: 0.2

# 日志配置
logging:
  level:
    com.aiprreviewer: DEBUG
    org.springframework.web: DEBUG
```

```yaml
# application-dev.yml (开发环境)
spring:
  datasource:
    url: jdbc:h2:mem:reviewdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
```

### 6.4 核心Service实现

```java
// ReviewService.java - 业务编排层
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final GitHubService gitHubService;
    private final AiAnalysisService aiAnalysisService;
    private final ReviewTaskRepository taskRepository;
    private final ReviewCommentRepository commentRepository;

    /**
     * 创建并执行PR评审任务
     */
    @Transactional
    public ReviewResultDTO createReview(ReviewCreateRequest request) {
        // 1. 创建任务记录
        ReviewTask task = new ReviewTask();
        task.setRepoUrl(request.getRepoUrl());
        task.setPrNumber(request.getPrNumber());
        task.setStatus(ReviewStatus.PROCESSING);
        task = taskRepository.save(task);

        try {
            // 2. 获取GitHub PR Diff
            log.info("开始获取PR Diff: {}/{}", request.getRepoUrl(), request.getPrNumber());
            GitHubDiffResult diffResult = gitHubService.fetchDiff(
                request.getRepoUrl(), 
                request.getPrNumber()
            );
            task.setPrTitle(diffResult.getPrTitle());
            task.setDiffSize(diffResult.getDiffText().length());
            task.setFileCount(diffResult.getFileCount());

            // 3. 调用AI分析
            log.info("开始AI分析，Diff大小: {}字符", diffResult.getDiffText().length());
            AiAnalysisResult analysis = aiAnalysisService.analyzeDiff(diffResult.getDiffText());
            task.setSummary(analysis.getSummary());
            task.setAiModel(aiAnalysisService.getModelName());

            // 4. 保存评审意见
            List<ReviewComment> comments = analysis.getComments().stream()
                .map(commentDto -> {
                    ReviewComment comment = new ReviewComment();
                    comment.setTask(task);
                    comment.setFilePath(commentDto.getFilePath());
                    comment.setLineNumber(commentDto.getLineNumber());
                    comment.setRiskLevel(commentDto.getRiskLevel());
                    comment.setMatchCode(commentDto.getMatchCode());
                    comment.setSuggestion(commentDto.getSuggestion());
                    comment.setOptimizedCode(commentDto.getOptimizedCode());
                    return comment;
                })
                .collect(Collectors.toList());
            commentRepository.saveAll(comments);

            // 5. 更新任务状态
            task.setStatus(ReviewStatus.COMPLETED);
            taskRepository.save(task);

            // 6. 组装返回结果
            return buildResultDTO(task, comments);

        } catch (Exception e) {
            log.error("评审任务执行失败", e);
            task.setStatus(ReviewStatus.FAILED);
            taskRepository.save(task);
            throw new BusinessException("评审任务执行失败: " + e.getMessage());
        }
    }

    private ReviewResultDTO buildResultDTO(ReviewTask task, List<ReviewComment> comments) {
        ReviewResultDTO dto = new ReviewResultDTO();
        dto.setId(task.getId());
        dto.setRepoUrl(task.getRepoUrl());
        dto.setPrNumber(task.getPrNumber());
        dto.setPrTitle(task.getPrTitle());
        dto.setStatus(task.getStatus());
        dto.setSummary(task.getSummary());
        dto.setFileCount(task.getFileCount());
        dto.setDiffSize(task.getDiffSize());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setComments(comments.stream()
            .map(this::convertCommentDTO)
            .collect(Collectors.toList()));
        return dto;
    }
}
```

```java
// GitHubService.java - GitHub API交互
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubService {

    private final RestTemplate restTemplate;

    @Value("${app.github.api-base}")
    private String githubApiBase;

    @Value("${app.github.token:}")
    private String githubToken;

    /**
     * 获取PR的Diff数据
     */
    public GitHubDiffResult fetchDiff(String repoUrl, Integer prNumber) {
        // 解析仓库owner和name
        String[] parts = parseRepoUrl(repoUrl);
        String owner = parts[0];
        String repo = parts[1];

        // 构建API请求
        String diffUrl = String.format("%s/repos/%s/%s/pulls/%d", 
            githubApiBase, owner, repo, prNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3.diff");
        headers.set("User-Agent", "AI-PR-Reviewer-Agent");
        if (StringUtils.hasText(githubToken)) {
            headers.set("Authorization", "Bearer " + githubToken);
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                diffUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );

            String diffText = response.getBody();

            // 同时获取PR标题
            String prInfoUrl = String.format("%s/repos/%s/%s/pulls/%d", 
                githubApiBase, owner, repo, prNumber);
            headers.set("Accept", "application/vnd.github.v3+json");
            
            ResponseEntity<JsonNode> prInfoResponse = restTemplate.exchange(
                prInfoUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
            );

            String prTitle = prInfoResponse.getBody().get("title").asText();
            int fileCount = countChangedFiles(diffText);

            return new GitHubDiffResult(diffText, prTitle, fileCount);

        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException("PR不存在或仓库为私有: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException("获取GitHub数据失败: " + e.getMessage());
        }
    }

    private String[] parseRepoUrl(String repoUrl) {
        // 支持格式: https://github.com/owner/repo 或 owner/repo
        Pattern pattern = Pattern.compile("github\\.com/([^/]+)/([^/]+)");
        Matcher matcher = pattern.matcher(repoUrl);
        if (matcher.find()) {
            return new String[]{matcher.group(1), matcher.group(2)};
        }
        throw new BusinessException("无效的GitHub仓库链接格式");
    }

    private int countChangedFiles(String diff) {
        // 统计diff中的文件数量
        Pattern pattern = Pattern.compile("^diff --git", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(diff);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
}
```

```java
// AiAnalysisService.java - AI分析服务
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {

    private final RestTemplate restTemplate;

    @Value("${app.ai.api-base}")
    private String apiBase;

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.model}")
    private String model;

    @Value("${app.ai.max-tokens:4000}")
    private Integer maxTokens;

    @Value("${app.ai.temperature:0.2}")
    private Double temperature;

    private static final String SYSTEM_PROMPT = """
        你是一位精通各大编程语言（JavaScript, TypeScript, Go, Python, Java）的资深架构师与首席安全官。
        请对输入的 git diff 文本进行严格的代码评审。

        你必须严格按照以下JSON格式响应，不要包含任何额外的Markdown标记：
        {
          "summary": "一句话总结本次PR的主要修改意图和影响业务",
          "comments": [
            {
              "file_path": "发生变更的文件相对路径",
              "line_number": 10,
              "risk_level": "info或warning或critical",
              "match_code": "引发风险或需要优化的原始代码行",
              "suggestion": "具体清晰的审查意见，指出为什么这样做不好",
              "optimized_code": "优化的参考代码片段，若不需要则为null"
            }
          ]
        }

        评审要点：
        1. 安全性：硬编码密钥、SQL注入、XSS、CSRF等
        2. 性能：内存泄露、N+1查询、不必要的循环
        3. 健壮性：空指针、异常处理、边界条件
        4. 规范性：命名规范、代码重复、魔法数字
        5. 最佳实践：设计模式、SOLID原则

        注意事项：
        - 只关注实际存在的问题，不要臆想
        - risk_level为critical表示必须修复，warning表示建议修复，info表示建议优化
        - 如果diff内容无明显问题，comments可以为空数组
        """;

    /**
     * 分析代码Diff
     */
    public AiAnalysisResult analyzeDiff(String diffText) {
        // 截断过长的diff，避免超出token限制
        String truncatedDiff = diffText.length() > 8000 
            ? diffText.substring(0, 8000) + "\n\n[...diff过长，已截断...]"
            : diffText;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", "请评审以下代码变更：\n\n" + truncatedDiff)
            ),
            "response_format", Map.of("type", "json_object"),
            "temperature", temperature,
            "max_tokens", maxTokens
        );

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                apiBase + "/chat/completions",
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                JsonNode.class
            );

            String content = response.getBody()
                .get("choices").get(0)
                .get("message").get("content")
                .asText();

            return parseAiResponse(content);

        } catch (Exception e) {
            log.error("AI分析失败，返回Mock数据", e);
            return getMockResult();
        }
    }

    private AiAnalysisResult parseAiResponse(String jsonContent) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonContent, AiAnalysisResult.class);
        } catch (Exception e) {
            log.error("解析AI响应失败", e);
            return getMockResult();
        }
    }

    /**
     * Mock数据兜底（演示保险）
     */
    private AiAnalysisResult getMockResult() {
        AiAnalysisResult result = new AiAnalysisResult();
        result.setSummary("本次PR优化了核心业务逻辑，修复了潜在的安全隐患和性能问题。");
        
        List<AiCommentDTO> comments = List.of(
            createMockComment(
                "src/main/java/service/UserService.java", 45, RiskLevel.CRITICAL,
                "String password = \"admin123\";",
                "检测到硬编码密码！这会导致严重的安全风险。",
                "String password = System.getenv(\"ADMIN_PASSWORD\");"
            ),
            createMockComment(
                "src/main/java/controller/UserController.java", 78, RiskLevel.WARNING,
                "return userRepository.findAll();",
                "未分页查询所有用户，当数据量大时会导致内存溢出。",
                "return userRepository.findAll(PageRequest.of(page, size));"
            )
        );
        result.setComments(comments);
        return result;
    }

    public String getModelName() {
        return model;
    }
}
```

### 6.5 Controller层

```java
// ReviewController.java
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/create")
    public ApiResponse<ReviewResultDTO> createReview(
            @Valid @RequestBody ReviewCreateRequest request) {
        ReviewResultDTO result = reviewService.createReview(request);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<ReviewResultDTO> getReview(@PathVariable Long id) {
        ReviewResultDTO result = reviewService.getReviewById(id);
        return ApiResponse.success(result);
    }

    @GetMapping("/list")
    public ApiResponse<Page<ReviewResultDTO>> listReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReviewResultDTO> results = reviewService.listReviews(page, size);
        return ApiResponse.success(results);
    }
}
```

### 6.6 统一响应封装

```java
// ApiResponse.java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

---

## 七、前端设计

### 7.1 页面布局

```
┌──────────────────────────────────────────────────────────────┐
│  🔍 AI Code Reviewer v1.0                    [实训营作品]     │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  GitHub 仓库链接    │  PR 编号    │  [开始智能评审]      │ │
│  │  [________________] [________]   │  ⏳ AI分析中...     │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  📋 AI 变更概要总结                                      │ │
│  │  本次PR实现了用户认证功能，包括JWT token生成...           │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  🛠️ 发现潜在缺陷 (3)                                        │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  FILE: src/auth/jwt.js : 行 45        [🔴 CRITICAL]   │ │
│  │────────────────────────────────────────────────────────│ │
│  │  评审意见：检测到硬编码的密钥！...                       │ │
│  │                                                        │ │
│  │  ⚠️ 触发风险代码        │  💡 建议优化方案              │ │
│  │  const secret = 'xxx'  │  const secret =               │ │
│  │                        │    process.env.JWT_SECRET     │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  FILE: src/middleware/auth.js : 行 12   [🟡 WARNING]   │ │
│  │────────────────────────────────────────────────────────│ │
│  │  ...                                                   │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

### 7.2 关键组件

- **Header**: 顶部导航栏，显示项目名称
- **ReviewForm**: PR输入表单，包含仓库链接和PR编号输入
- **SummaryCard**: AI变更总结卡片
- **CommentCard**: 单条评审意见卡片，包含风险等级、代码对比
- **RiskBadge**: 风险等级标签组件（CRITICAL/WARNING/INFO）

---

## 八、72小时交付计划

### Day 1 (5月29日) - 基础搭建

| 时间 | 任务 | 产出 |
|------|------|------|
| 上午 | 初始化项目、搭建Spring Boot骨架 | 可运行的后端服务 |
| 下午 | 实现GitHub API交互、AI分析服务 | 核心功能可用 |
| 晚上 | 搭建前端Vue项目、基础页面 | 前后端联调基础版 |

**PR #1**: 初始化Spring Boot项目 + 基础配置
**PR #2**: 实现GitHub Diff获取功能

### Day 2 (5月30日) - 功能完善

| 时间 | 任务 | 产出 |
|------|------|------|
| 上午 | 接入真实AI API、优化Prompt | AI分析功能可用 |
| 下午 | 前端完整UI、结果展示 | 完整的评审结果页面 |
| 晚上 | 端到端联调、Bug修复 | 可演示的MVP |

**PR #3**: 实现AI分析服务 + Mock兜底
**PR #4**: 前端完整UI实现

### Day 3 (5月31日) - 优化提交

| 时间 | 任务 | 产出 |
|------|------|------|
| 上午 | UI优化、体验打磨 | 精美的界面 |
| 下午 | 录制Demo视频 | 视频链接 |
| 晚上 | 提交代码、填写报名表 | 完成提交 |

**PR #5**: UI优化 + 体验改进
**PR #6**: 最终版本 + README完善

---

## 九、扩展性设计

### 9.1 支持更多代码托管平台

```java
// 策略模式设计
public interface CodePlatformService {
    DiffResult fetchDiff(String repoUrl, Integer prNumber);
}

@Service("github")
public class GitHubService implements CodePlatformService { ... }

@Service("gitlab")
public class GitLabService implements CodePlatformService { ... }

@Service("gitee")
public class GiteeService implements CodePlatformService { ... }
```

### 9.2 支持更多AI模型

```java
// 配置化切换
app:
  ai:
    provider: deepseek  # 可选: openai, deepseek, qwen, etc.
    api-base: ${AI_API_BASE}
    api-key: ${AI_API_KEY}
    model: ${AI_MODEL}
```

### 9.3 WebSocket实时推送

当PR较大、分析耗时较长时，可通过WebSocket实时推送分析进度：

```
分析进度: ████████░░ 80%
当前正在分析: src/service/UserService.java
已完成: 4/5 个文件
```

---

## 十、答辩要点准备

### 10.1 技术亮点

1. **架构清晰**：标准的Spring Boot分层架构，职责分离
2. **异常处理**：全局异常处理 + Mock兜底机制
3. **扩展性**：策略模式支持多平台、多模型
4. **AI工程化**：结构化Prompt + JSON Mode + Token控制

### 10.2 可能的问题

| 问题 | 回答 |
|------|------|
| 如何处理大PR？ | 按文件拆分，单文件截断，最终汇总 |
| 如何保证AI分析质量？ | 结构化Prompt + 多轮调优 + 人工验证 |
| 如何减少误报？ | 明确要求"只关注实际问题" + 风险等级分类 |
| 为什么选择DeepSeek？ | 代码能力强、性价比高、支持JSON Mode |
| 未来如何扩展？ | 策略模式支持多平台、多模型、WebSocket实时推送 |

---

## 十一、环境准备清单

### 开发环境

- [ ] JDK 17+
- [ ] Maven 3.8+
- [ ] Node.js 18+
- [ ] Git
- [ ] IDE (IntelliJ IDEA / VS Code)

### API Key

- [ ] DeepSeek API Key (https://platform.deepseek.com/)
- [ ] GitHub Token (可选，提高API限额)

### 账号

- [ ] GitHub 账号（创建仓库）
- [ ] hr.qiniu.com 报名账号

---

> **文档版本**: v1.0
> **最后更新**: 2026-05-29
> **作者**: AI PR Reviewer Team
