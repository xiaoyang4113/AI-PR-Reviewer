package com.aiprreviewer.service.impl;

import com.aiprreviewer.config.AiProperties;
import com.aiprreviewer.model.constant.ReviewConstants;
import com.aiprreviewer.model.dto.AiAnalysisResult;
import com.aiprreviewer.model.dto.AiAnalysisResult.AiCommentItem;
import com.aiprreviewer.service.AiAnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 代码分析服务实现
 * <p>
 * 核心流程：
 * 1. 检查 API Key 是否配置（未配置直接返回 Mock 数据）
 * 2. 截断过长的 diff（防止超出 AI 模型 token 限制）
 * 3. 调用 AI API 发送结构化 System Prompt + diff 文本
 * 4. 解析 AI 返回的 JSON 为结构化结果
 * 5. 如果任何环节失败，降级返回 Mock 数据（保证演示可用）
 * <p>
 * 安全措施：
 * - API Key 仅通过配置注入，不硬编码、不记录日志
 * - AI 响应解析失败不影响系统运行
 * <p>
 * 容错设计：
 * - API 调用失败 → Mock 降级
 * - JSON 解析失败 → Mock 降级
 * - 两层 try-catch 区分网络异常和格式异常，便于排查
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    /**
     * AI 系统提示词：定义评审维度、输出格式、风险等级标准
     */
    private static final String SYSTEM_PROMPT = """
            你是一位精通 JavaScript、TypeScript、Go、Python、Java 的资深架构师与代码安全专家。
            请对以下 git diff 文本进行严格代码评审。

            ## 评审维度
            1. **安全性**：硬编码密钥/密码/Token、SQL 注入、XSS、CSRF、命令注入、路径遍历
            2. **性能**：内存泄露、死循环、N+1 查询、未分页的大数据量查询
            3. **健壮性**：空指针/NPE、未处理异常、资源未关闭、边界条件缺失
            4. **规范性**：命名不规范、魔法数字、重复代码、过长函数
            5. **最佳实践**：设计模式滥用、耦合过紧、SOLID 原则违反

            ## 评审要求
            - 只报告**实际存在**的问题，不要臆想或推测
            - 如果代码质量良好，comments 可以为空数组
            - risk_level: "CRITICAL"(必须修复) / "WARNING"(建议修复) / "INFO"(建议优化)
            - line_number: 新文件中的行号，无法确定则填 null
            - 每条 suggestion 需说明**为什么**这是问题以及**如何**修复

            ## 输出格式（严格 JSON，禁止 Markdown 包装）
            {
              "summary": "一句话总结本次 PR 的修改意图和影响范围",
              "comments": [
                {
                  "file_path": "变更文件路径",
                  "line_number": 42,
                  "risk_level": "CRITICAL",
                  "match_code": "问题代码片段",
                  "suggestion": "详细的问题说明和修复建议",
                  "optimized_code": "推荐的修复代码，无则为 null"
                }
              ]
            }""";

    @Override
    public AiAnalysisResult analyzeDiff(String diffText) {
        // API Key 未配置时直接降级
        String apiKey = aiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI API Key 未配置，返回 Mock 演示数据");
            return buildMockResult();
        }

        // 截断过长 diff，防止 token 超限
        String truncatedDiff = truncateDiff(diffText);

        try {
            // 第一层：调用 AI API（网络/认证/超时异常在此捕获）
            String responseJson = callAiApi(truncatedDiff);
            try {
                // 第二层：解析响应 JSON（格式异常在此捕获）
                return parseAiResponse(responseJson);
            } catch (Exception e) {
                log.error("AI 响应 JSON 解析失败（API 调用成功但返回格式异常）: {}", e.getMessage());
                return buildMockResult();
            }
        } catch (Exception e) {
            log.error("AI API 调用失败，降级为 Mock 数据: {}", e.getMessage());
            return buildMockResult();
        }
    }

    @Override
    public String getModelName() {
        return aiProperties.getModel();
    }

    /**
     * 调用 AI API，发送 System Prompt + diff 文本，返回原始 JSON 字符串
     * 使用 OpenAI 兼容的聊天补全接口，通过 RestTemplate 发送 POST 请求
     *
     * @param diffText 待分析的 diff 文本（已截断）
     * @return AI 返回的原始 JSON 字符串（message.content 内容）
     * @throws RuntimeException API 调用失败、返回空数据或格式异常时抛出
     */
    private String callAiApi(String diffText) {
        String model = aiProperties.getModel();
        String url = aiProperties.getApiBase() + ReviewConstants.AI_CHAT_PATH;
        log.info("正在调用 AI API: model={} url={} 请求大小={}字符", model, url, diffText.length());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(ReviewConstants.HEADER_AUTHORIZATION,
                ReviewConstants.BEARER_PREFIX + aiProperties.getApiKey());

        // 构建请求体：model + messages + 参数
        Map<String, Object> requestBody = Map.of(
                ReviewConstants.AI_MODEL_FIELD, model,
                ReviewConstants.AI_MESSAGES_FIELD, List.of(
                        Map.of(ReviewConstants.AI_ROLE_FIELD, ReviewConstants.AI_ROLE_SYSTEM,
                                ReviewConstants.AI_CONTENT_FIELD, SYSTEM_PROMPT),
                        Map.of(ReviewConstants.AI_ROLE_FIELD, ReviewConstants.AI_ROLE_USER,
                                ReviewConstants.AI_CONTENT_FIELD,
                                ReviewConstants.AI_USER_PROMPT_PREFIX + diffText)
                ),
                ReviewConstants.AI_TEMPERATURE_FIELD, aiProperties.getTemperature(),
                ReviewConstants.AI_MAX_TOKENS_FIELD, aiProperties.getMaxTokens()
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        long startTime = System.currentTimeMillis();
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, JsonNode.class);
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("AI API 响应成功，耗时: {}ms", elapsed);

        // 校验响应结构
        JsonNode body = response.getBody();
        if (body == null || !body.has(ReviewConstants.AI_CHOICES_FIELD)
                || body.get(ReviewConstants.AI_CHOICES_FIELD).isEmpty()) {
            throw new RuntimeException("AI API 返回数据为空或格式异常");
        }

        // 提取 message.content（path() 不会返回 null，避免 NPE）
        return body.path(ReviewConstants.AI_CHOICES_FIELD).path(0)
                .path(ReviewConstants.AI_MESSAGE_FIELD)
                .path(ReviewConstants.AI_CONTENT_FIELD).asText();
    }

    /**
     * 截断 diff 文本，确保不超过配置的最大长度
     * 超出部分直接裁剪，并在末尾添加截断标记，提示用户 diff 不完整
     *
     * @param diffText 原始 diff 文本
     * @return 截断后的 diff 文本，末尾含截断说明
     */
    private String truncateDiff(String diffText) {
        if (diffText == null || diffText.isEmpty()) {
            return "";
        }
        int maxLen = aiProperties.getMaxDiffLength();
        if (diffText.length() <= maxLen) {
            return diffText;
        }
        log.info("Diff 过大 ({} 字符)，截断至 {} 字符", diffText.length(), maxLen);
        return diffText.substring(0, maxLen)
                + "\n\n[... diff 内容过长，已截断，以上为前 " + maxLen + " 字符 ...]";
    }

    /**
     * 解析 AI 返回的 JSON，转成 AiAnalysisResult 对象
     * 兼容 AI 可能返回 Markdown 代码块包裹的 JSON（```json ... ```）
     *
     * @param jsonContent AI 返回的 JSON 字符串（可能含 Markdown 包装）
     * @return 结构化的 AI 分析结果
     * @throws Exception JSON 格式异常时抛出，由调用方降级处理
     */
    private AiAnalysisResult parseAiResponse(String jsonContent) throws Exception {
        // AI 可能返回带 ```json ... ``` 包裹的内容，先清理
        String cleanedJson = jsonContent.trim();
        if (cleanedJson.startsWith("```")) {
            cleanedJson = cleanedJson.replaceAll("^```(?:json)?\\s*", "")
                    .replaceAll("\\s*```$", "");
        }

        JsonNode root = objectMapper.readTree(cleanedJson);

        AiAnalysisResult result = new AiAnalysisResult();
        result.setSummary(root.has(ReviewConstants.AI_SUMMARY_FIELD)
                ? root.get(ReviewConstants.AI_SUMMARY_FIELD).asText("") : "");

        // 解析评论列表
        List<AiCommentItem> comments = new ArrayList<>();
        if (root.has(ReviewConstants.AI_COMMENTS_FIELD)
                && root.get(ReviewConstants.AI_COMMENTS_FIELD).isArray()) {
            for (JsonNode node : root.get(ReviewConstants.AI_COMMENTS_FIELD)) {
                comments.add(parseCommentItem(node));
            }
        }
        result.setComments(comments);
        return result;
    }

    /**
     * 解析单条评论的 JSON 节点
     * 提取 file_path、line_number、risk_level、match_code、suggestion、optimized_code 字段
     * line_number 和 optimized_code 可能为 null，需特殊处理
     *
     * @param node 单条评论的 JSON 节点
     * @return 解析后的 AiCommentItem 对象
     */
    private AiCommentItem parseCommentItem(JsonNode node) {
        AiCommentItem item = new AiCommentItem();
        item.setFilePath(getJsonText(node, ReviewConstants.AI_FILE_PATH_FIELD));
        item.setLineNumber(node.has(ReviewConstants.AI_LINE_NUMBER_FIELD)
                && !node.get(ReviewConstants.AI_LINE_NUMBER_FIELD).isNull()
                ? node.get(ReviewConstants.AI_LINE_NUMBER_FIELD).asInt() : null);
        item.setRiskLevel(getJsonTextOrDefault(node, ReviewConstants.AI_RISK_LEVEL_FIELD,
                ReviewConstants.AI_RISK_INFO));
        item.setMatchCode(getJsonText(node, ReviewConstants.AI_MATCH_CODE_FIELD));
        item.setSuggestion(getJsonText(node, ReviewConstants.AI_SUGGESTION_FIELD));
        item.setOptimizedCode(node.has(ReviewConstants.AI_OPTIMIZED_CODE_FIELD)
                && !node.get(ReviewConstants.AI_OPTIMIZED_CODE_FIELD).isNull()
                ? node.get(ReviewConstants.AI_OPTIMIZED_CODE_FIELD).asText("") : null);
        return item;
    }

    /**
     * 安全获取 JSON 文本字段
     * 字段不存在时返回空字符串，避免 NPE
     *
     * @param node  JSON 节点
     * @param field 字段名
     * @return 字段值，不存在时返回 ""
     */
    private String getJsonText(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText("") : "";
    }

    /**
     * 带默认值的 JSON 文本字段获取
     * 字段不存在时返回指定的 default value，避免 NPE
     *
     * @param node         JSON 节点
     * @param field        字段名
     * @param defaultValue 字段不存在时的默认值
     * @return 字段值，不存在时返回 defaultValue
     */
    private String getJsonTextOrDefault(JsonNode node, String field, String defaultValue) {
        return node.has(field) ? node.get(field).asText(defaultValue) : defaultValue;
    }

    // ==================== Mock 数据（演示兜底） ====================

    /**
     * 构建 Mock 评审结果
     * 当 AI API 不可用时返回此数据，确保演示流程完整
     */
    private AiAnalysisResult buildMockResult() {
        AiAnalysisResult result = new AiAnalysisResult();
        result.setSummary("[演示数据] 本次 PR 优化了用户认证模块，新增 JWT 令牌验证逻辑，"
                + "修复了数据库连接泄露问题。涉及 3 个文件，共 45 行新增、12 行删除。");

        List<AiCommentItem> comments = new ArrayList<>();

        // Mock 评审意见 #1：CRITICAL 硬编码密钥
        AiCommentItem c1 = new AiCommentItem();
        c1.setFilePath("src/main/java/com/example/service/UserService.java");
        c1.setLineNumber(45);
        c1.setRiskLevel(ReviewConstants.AI_RISK_CRITICAL);
        c1.setMatchCode("String secretKey = \"my-secret-key-2026\";");
        c1.setSuggestion("检测到硬编码密钥！密钥明文写入代码会导致泄露风险，"
                + "攻击者可通过反编译或查看源码获取密钥，伪造 JWT Token。"
                + "密钥应从环境变量或配置中心获取。");
        c1.setOptimizedCode("String secretKey = System.getenv(\"JWT_SECRET_KEY\");");
        comments.add(c1);

        // Mock 评审意见 #2：WARNING 未分页查询
        AiCommentItem c2 = new AiCommentItem();
        c2.setFilePath("src/main/java/com/example/controller/UserController.java");
        c2.setLineNumber(78);
        c2.setRiskLevel(ReviewConstants.AI_RISK_WARNING);
        c2.setMatchCode("return userMapper.selectList(null);");
        c2.setSuggestion("未做分页处理直接查询所有用户，当数据量大时会导致内存溢出和响应超时。"
                + "建议使用分页查询并限制每页大小。");
        c2.setOptimizedCode("Page<User> page = new Page<>(pageNum, pageSize);\n"
                + "return userMapper.selectPage(page, null);");
        comments.add(c2);

        // Mock 评审意见 #3：WARNING 连接超时过长
        AiCommentItem c3 = new AiCommentItem();
        c3.setFilePath("src/main/java/com/example/config/DatabaseConfig.java");
        c3.setLineNumber(23);
        c3.setRiskLevel(ReviewConstants.AI_RISK_WARNING);
        c3.setMatchCode("dataSource.setConnectionTimeout(60000);");
        c3.setSuggestion("数据库连接超时设置为 60 秒过长。在微服务架构中，"
                + "长时间等待连接可能导致线程池耗尽。建议设置为 5-10 秒并配合快速失败机制。");
        c3.setOptimizedCode("dataSource.setConnectionTimeout(5000);");
        comments.add(c3);

        // Mock 评审意见 #4：INFO 空指针风险
        AiCommentItem c4 = new AiCommentItem();
        c4.setFilePath("src/main/java/com/example/service/UserService.java");
        c4.setLineNumber(67);
        c4.setRiskLevel(ReviewConstants.AI_RISK_INFO);
        c4.setMatchCode("public User getUser(Long id) {\n    return userMapper.selectById(id);\n}");
        c4.setSuggestion("方法未处理 id 为 null 的情况，可能导致 NullPointerException。"
                + "建议添加 @NotNull 参数校验或在方法内做空值判断。");
        c4.setOptimizedCode("public User getUser(@NotNull Long id) {\n"
                + "    if (id == null) throw new IllegalArgumentException(\"id 不能为 null\");\n"
                + "    return userMapper.selectById(id);\n}");
        comments.add(c4);

        result.setComments(comments);
        return result;
    }
}
