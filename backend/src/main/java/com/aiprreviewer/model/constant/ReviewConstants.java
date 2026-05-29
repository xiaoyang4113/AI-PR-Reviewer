package com.aiprreviewer.model.constant;

/**
 * 系统常量集中管理
 */
public final class ReviewConstants {

    private ReviewConstants() {}

    // ==================== 分页 ====================
    /** 默认页码 */
    public static final int DEFAULT_PAGE = 1;
    /** 默认每页大小 */
    public static final int DEFAULT_PAGE_SIZE = 10;
    /** 最大每页大小（防止恶意请求） */
    public static final int MAX_PAGE_SIZE = 100;

    // ==================== 字段长度限制 ====================
    /** 文件路径最大长度（DB 为 VARCHAR(500)） */
    public static final int MAX_FILE_PATH_LENGTH = 500;
    /** 任务超时时间（分钟），超过此时间的 PROCESSING 任务视为失败 */
    public static final int TASK_TIMEOUT_MINUTES = 5;

    // ==================== HTTP 请求头 ====================
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String USER_AGENT_VALUE = "AI-PR-Reviewer/1.0";

    // ==================== GitHub API ====================
    /** GitHub 获取 diff 的专用 Accept 头 */
    public static final String GITHUB_DIFF_ACCEPT = "application/vnd.github.v3.diff";
    /** GitHub 仓库 URL 正则（仅允许 github.com） */
    public static final String GITHUB_URL_PATTERN = "^https?://github\\.com/([a-zA-Z0-9._-]+)/([a-zA-Z0-9._-]+)/?$";
    /** diff 文件标记（用于统计文件数） */
    public static final String DIFF_FILE_PREFIX = "diff --git";
    /** PR 标题在 API 返回中的字段名 */
    public static final String PR_TITLE_FIELD = "title";
    /** 默认 PR 标题（API 获取失败时） */
    public static final String DEFAULT_PR_TITLE = "Untitled PR";
    /** .git 后缀 */
    public static final String GIT_SUFFIX = ".git";

    // ==================== AI API 请求/响应字段 ====================
    /** API 路径 */
    public static final String AI_CHAT_PATH = "/chat/completions";
    /** 模型字段 */
    public static final String AI_MODEL_FIELD = "model";
    /** 消息列表字段 */
    public static final String AI_MESSAGES_FIELD = "messages";
    /** 角色字段 */
    public static final String AI_ROLE_FIELD = "role";
    /** 内容字段 */
    public static final String AI_CONTENT_FIELD = "content";
    /** 温度字段 */
    public static final String AI_TEMPERATURE_FIELD = "temperature";
    /** 最大token字段 */
    public static final String AI_MAX_TOKENS_FIELD = "max_tokens";
    /** system 角色 */
    public static final String AI_ROLE_SYSTEM = "system";
    /** user 角色 */
    public static final String AI_ROLE_USER = "user";
    /** choices 字段 */
    public static final String AI_CHOICES_FIELD = "choices";
    /** message 字段 */
    public static final String AI_MESSAGE_FIELD = "message";
    /** 总结字段 */
    public static final String AI_SUMMARY_FIELD = "summary";
    /** 评论列表字段 */
    public static final String AI_COMMENTS_FIELD = "comments";
    /** 文件路径字段 */
    public static final String AI_FILE_PATH_FIELD = "file_path";
    /** 行号字段 */
    public static final String AI_LINE_NUMBER_FIELD = "line_number";
    /** 风险等级字段 */
    public static final String AI_RISK_LEVEL_FIELD = "risk_level";
    /** 匹配代码字段 */
    public static final String AI_MATCH_CODE_FIELD = "match_code";
    /** 建议字段 */
    public static final String AI_SUGGESTION_FIELD = "suggestion";
    /** 优化代码字段 */
    public static final String AI_OPTIMIZED_CODE_FIELD = "optimized_code";
    /** 风险等级：严重 */
    public static final String AI_RISK_CRITICAL = "CRITICAL";
    /** 风险等级：警告 */
    public static final String AI_RISK_WARNING = "WARNING";
    /** 风险等级：建议 */
    public static final String AI_RISK_INFO = "INFO";
    /** 用户提示词前缀 */
    public static final String AI_USER_PROMPT_PREFIX = "请评审以下代码变更：\n\n";

    // ==================== CORS 跨域 ====================
    public static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};
    public static final long CORS_MAX_AGE = 3600L;

    // ==================== HTTP 超时（毫秒） ====================
    /** 建立连接超时 */
    public static final int CONNECT_TIMEOUT_MS = 10_000;
    /** 读取响应超时（AI分析可能较慢） */
    public static final int READ_TIMEOUT_MS = 120_000;

    // ==================== API 路径 ====================
    public static final String API_REVIEW_PATH = "/api/review";
    public static final String API_REVIEW_PATTERN = "/api/**";

    // ==================== 响应码 ====================
    public static final int RESPONSE_CODE_OK = 200;
    public static final int RESPONSE_CODE_BAD_REQUEST = 400;
    public static final int RESPONSE_CODE_ERROR = 500;
    public static final String RESPONSE_MSG_SUCCESS = "success";
    public static final String RESPONSE_MSG_SERVER_ERROR = "服务器内部错误，请稍后重试";
}
