package com.aiprreviewer.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.aiprreviewer.model.enums.ReviewStatus;

/**
 * 评审任务实体，映射 review_tasks 表
 */
@Data
@TableName("review_tasks")
public class ReviewTask {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** GitHub 仓库地址 */
    private String repoUrl;

    /** Pull Request 编号 */
    private Integer prNumber;

    /** PR 标题（从 GitHub API 获取） */
    private String prTitle;

    /** 任务状态 */
    private ReviewStatus status;

    /** AI 生成的变更总结 */
    private String summary;

    /** 使用的 AI 模型名称 */
    private String aiModel;

    /** diff 原始字符数 */
    private Integer diffSize;

    /** 变更文件数量 */
    private Integer fileCount;

    /** 失败时的错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
