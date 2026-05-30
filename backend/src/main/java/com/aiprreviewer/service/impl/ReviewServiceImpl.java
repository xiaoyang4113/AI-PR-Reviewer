package com.aiprreviewer.service.impl;

import com.aiprreviewer.model.constant.ReviewConstants;
import com.aiprreviewer.model.dto.*;
import com.aiprreviewer.model.dto.AiAnalysisResult.AiCommentItem;
import com.aiprreviewer.model.entity.ReviewComment;
import com.aiprreviewer.model.entity.ReviewTask;
import com.aiprreviewer.model.enums.ReviewStatus;
import com.aiprreviewer.model.enums.RiskLevel;
import com.aiprreviewer.exception.BusinessException;
import com.aiprreviewer.mapper.ReviewCommentMapper;
import com.aiprreviewer.mapper.ReviewTaskMapper;
import com.aiprreviewer.service.AiAnalysisService;
import com.aiprreviewer.service.CodePlatformService;
import com.aiprreviewer.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * 评审业务编排服务实现
 *
 * 核心流程（createReview）：
 * 1. 去重检查 → 同一 PR 不允许重复提交
 * 2. 创建任务记录 → 状态设为 PROCESSING
 * 3. 获取 GitHub PR Diff → 调用 GitHubService
 * 4. AI 分析 Diff → 调用 AiAnalysisService
 * 5. 保存评审意见 → 批量写入 review_comments 表
 * 6. 更新任务状态 → COMPLETED
 * 7. 异常处理 → 新事务写入 FAILED 状态（防止主事务回滚导致状态丢失）
 *
 * 查询流程（getReviewById / listReviews）：
 * - 单条查询：查任务 + 关联查询评论
 * - 列表查询：分页查任务 + 批量查评论（解决 N+1 问题）
 *
 * 并发控制：
 * - 去重查询防止并发重复提交
 * - 任务创建后立即提交（MyBatis-Plus insert 自动提交），确保失败时能查到记录
 * - 评论保存和状态更新包装在同一事务中保证原子性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    /** 代码平台服务列表（策略模式），当前仅支持 GitHub */
    private final List<CodePlatformService> platformServices;
    /** AI 代码分析服务，用于调用大模型进行代码评审 */
    private final AiAnalysisService aiAnalysisService;
    /** 评审任务 Mapper，操作 review_tasks 表 */
    private final ReviewTaskMapper taskMapper;
    /** 评审意见 Mapper，操作 review_comments 表 */
    private final ReviewCommentMapper commentMapper;
    /** 编程式事务模板，用于确保评论保存和状态更新原子性 */
    private final TransactionTemplate transactionTemplate;

    // ==================== 创建评审 ====================

    @Override
    public ReviewResultDTO createReview(ReviewCreateRequest request) {
        String repoUrl = normalizeRepoUrl(request.getRepoUrl());
        Integer prNumber = request.getPrNumber();

        // 步骤1：去重检查
        int pendingCount = taskMapper.countPendingByRepoAndPr(
                repoUrl, prNumber,
                ReviewStatus.PENDING.name(), ReviewStatus.PROCESSING.name(),
                ReviewConstants.TASK_TIMEOUT_MINUTES);
        if (pendingCount > 0) {
            throw new BusinessException("该 PR 已有进行中的评审任务，请等待完成后再提交");
        }

        // 步骤2：创建任务（MyBatis-Plus insert 自动提交，后续失败时新事务能查到这条记录）
        ReviewTask task = createTask(repoUrl, prNumber);

        try {
            // 步骤3：获取 GitHub PR Diff
            log.info("步骤1: 获取 PR Diff. repo={} pr={}", repoUrl, prNumber);
            GitHubDiffResult diffResult = getPlatform(repoUrl).fetchDiff(repoUrl, prNumber);
            task.setPrTitle(diffResult.getPrTitle());
            task.setDiffSize(diffResult.getDiffText().length());
            task.setFileCount(diffResult.getFileCount());

            // 步骤4：AI 分析
            log.info("步骤2: AI 分析. diff大小={}字符", diffResult.getDiffText().length());
            AiAnalysisResult analysis = aiAnalysisService.analyzeDiff(diffResult.getDiffText());
            task.setSummary(analysis.getSummary());
            task.setAiModel(aiAnalysisService.getModelName());

            // 步骤5-6：在事务中保存评论并更新状态（保证原子性）
            List<ReviewComment> comments = transactionTemplate.execute(status -> {
                List<ReviewComment> saved = saveComments(task.getId(), analysis);
                task.setStatus(ReviewStatus.COMPLETED);
                task.setUpdatedAt(LocalDateTime.now());
                taskMapper.updateById(task);
                return saved;
            });

            log.info("评审完成: taskId={} pr={}/{} 评论数={}",
                    task.getId(), repoUrl, prNumber, comments.size());
            return buildResultDTO(task, comments);

        } catch (BusinessException e) {
            markFailed(task, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("评审失败: taskId={}", task.getId(), e);
            String errMsg = "评审失败: " + e.getMessage();
            markFailed(task, errMsg);
            throw new BusinessException(errMsg, e);
        }
    }

    // ==================== 查询评审 ====================

    @Override
    public ReviewResultDTO getReviewById(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new BusinessException("任务ID无效");
        }
        ReviewTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("评审任务不存在: id=" + taskId);
        }
        List<ReviewComment> comments = commentMapper.selectByTaskId(taskId);
        return buildResultDTO(task, comments);
    }

    @Override
    public List<ReviewResultDTO> listReviews(int page, int size) {
        // 参数校验和标准化
        if (page < ReviewConstants.DEFAULT_PAGE) {
            page = ReviewConstants.DEFAULT_PAGE;
        }
        if (size < 1 || size > ReviewConstants.MAX_PAGE_SIZE) {
            size = ReviewConstants.DEFAULT_PAGE_SIZE;
        }

        int offset = (page - 1) * size;
        List<ReviewTask> tasks = taskMapper.selectTaskPage(offset, size);
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询评论，解决 N+1 问题
        List<Long> taskIds = tasks.stream().map(ReviewTask::getId).collect(toList());
        List<ReviewComment> allComments = commentMapper.selectByTaskIds(taskIds);

        // 按任务ID分组
        Map<Long, List<ReviewComment>> commentMap = allComments.stream()
                .collect(groupingBy(ReviewComment::getTaskId));

        return tasks.stream()
                .map(task -> buildResultDTO(task,
                        commentMap.getOrDefault(task.getId(), Collections.emptyList())))
                .collect(toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 根据 URL 匹配对应的代码平台（策略模式）
     * 遍历所有 CodePlatformService 实现，找到 supports() 为 true 的平台
     *
     * @param repoUrl GitHub 仓库地址
     * @return 匹配的代码平台服务实现
     * @throws BusinessException 如果没有平台支持该 URL
     */
    private CodePlatformService getPlatform(String repoUrl) {
        return platformServices.stream()
                .filter(p -> p.supports(repoUrl))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "不支持的代码托管平台，当前仅支持 GitHub: " + repoUrl));
    }

    /**
     * 创建任务记录，状态为 PROCESSING
     *
     * @param repoUrl   标准化后的仓库地址
     * @param prNumber  PR 编号
     * @return 已持久化的评审任务实体（含自增 ID）
     */
    private ReviewTask createTask(String repoUrl, Integer prNumber) {
        ReviewTask task = new ReviewTask();
        task.setRepoUrl(repoUrl);
        task.setPrNumber(prNumber);
        task.setStatus(ReviewStatus.PROCESSING);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return task;
    }

    /**
     * 批量保存评审意见
     *
     * @param taskId   关联的评审任务 ID
     * @param analysis AI 分析结果（含评论列表）
     * @return 已持久化的评论实体列表
     */
    private List<ReviewComment> saveComments(Long taskId, AiAnalysisResult analysis) {
        if (analysis.getComments() == null || analysis.getComments().isEmpty()) {
            return Collections.emptyList();
        }
        List<ReviewComment> comments = analysis.getComments().stream()
                .map(item -> buildComment(taskId, item))
                .collect(toList());
        commentMapper.batchInsert(comments);
        return comments;
    }

    /**
     * 标记任务失败
     * createReview 无事务包裹，task 已提交，此处 updateById 可直接生效
     *
     * @param task         当前评审任务实体
     * @param errorMessage 失败原因描述
     */
    private void markFailed(ReviewTask task, String errorMessage) {
        task.setStatus(ReviewStatus.FAILED);
        task.setUpdatedAt(LocalDateTime.now());
        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
        }
        taskMapper.updateById(task);
    }

    /**
     * 将 AI 返回的评论转为数据库实体
     * 同时做字段长度截断，防止数据库写入异常
     *
     * @param taskId 关联的评审任务 ID
     * @param item   AI 返回的单条评论数据
     * @return ReviewComment 实体，准备持久化
     */
    private ReviewComment buildComment(Long taskId, AiCommentItem item) {
        ReviewComment comment = new ReviewComment();
        comment.setTaskId(taskId);
        comment.setFilePath(truncate(item.getFilePath(), ReviewConstants.MAX_FILE_PATH_LENGTH));
        comment.setLineNumber(item.getLineNumber());
        comment.setRiskLevel(parseRiskLevel(item.getRiskLevel()));
        comment.setMatchCode(truncate(item.getMatchCode(), ReviewConstants.MAX_MATCH_CODE_LENGTH));
        comment.setSuggestion(truncate(item.getSuggestion(), ReviewConstants.MAX_SUGGESTION_LENGTH));
        comment.setOptimizedCode(truncate(item.getOptimizedCode(), ReviewConstants.MAX_OPTIMIZED_CODE_LENGTH));
        return comment;
    }

    /**
     * 组装返回给前端的 DTO
     * 将 ReviewTask 实体和 ReviewComment 实体列表转换为 ReviewResultDTO
     * 同时自动判断是否为 Mock 降级数据
     *
     * @param task     评审任务实体
     * @param comments 评审意见实体列表
     * @return 返回给前端的完整评审结果 DTO
     */
    private ReviewResultDTO buildResultDTO(ReviewTask task, List<ReviewComment> comments) {
        ReviewResultDTO dto = new ReviewResultDTO();
        dto.setId(task.getId());
        dto.setRepoUrl(task.getRepoUrl());
        dto.setPrNumber(task.getPrNumber());
        dto.setPrTitle(task.getPrTitle());
        dto.setStatus(task.getStatus());
        dto.setSummary(task.getSummary());
        dto.setAiModel(task.getAiModel());
        dto.setDiffSize(task.getDiffSize());
        dto.setFileCount(task.getFileCount());
        dto.setErrorMessage(task.getErrorMessage());
        dto.setMockMode(task.getSummary() != null && task.getSummary().startsWith("[演示数据]"));
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        dto.setComments(comments != null
                ? comments.stream().map(this::toCommentDTO).collect(toList())
                : Collections.emptyList());
        return dto;
    }

    /**
     * 实体转 DTO（ReviewComment -> CommentDTO）
     *
     * @param c 评审意见实体
     * @return 前端展示用的评论 DTO
     */
    private CommentDTO toCommentDTO(ReviewComment c) {
        CommentDTO dto = new CommentDTO();
        dto.setId(c.getId());
        dto.setFilePath(c.getFilePath());
        dto.setLineNumber(c.getLineNumber());
        dto.setRiskLevel(c.getRiskLevel());
        dto.setMatchCode(c.getMatchCode());
        dto.setSuggestion(c.getSuggestion());
        dto.setOptimizedCode(c.getOptimizedCode());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }

    /**
     * 将 AI 返回的风险等级字符串转为枚举
     *
     * @param level AI 返回的风险等级字符串（CRITICAL / WARNING / INFO）
     * @return 对应的 RiskLevel 枚举，无法识别时默认返回 INFO
     */
    private RiskLevel parseRiskLevel(String level) {
        if (level == null) return RiskLevel.INFO;
        if (ReviewConstants.AI_RISK_CRITICAL.equalsIgnoreCase(level)) return RiskLevel.CRITICAL;
        if (ReviewConstants.AI_RISK_WARNING.equalsIgnoreCase(level)) return RiskLevel.WARNING;
        return RiskLevel.INFO;
    }

    /**
     * 去除 URL 尾部斜杠，确保仓库地址格式统一
     *
     * @param url 原始仓库地址
     * @return 标准化后的 URL（无尾部斜杠）
     */
    private String normalizeRepoUrl(String url) {
        if (url == null) return null;
        return url.trim().replaceAll("/$", "");
    }

    /**
     * 安全截断超长字符串，防止数据库写入异常
     *
     * @param value     原始字符串
     * @param maxLength 允许的最大长度
     * @return 截断后的字符串（不超过 maxLength），null 入参返回 null
     */
    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
