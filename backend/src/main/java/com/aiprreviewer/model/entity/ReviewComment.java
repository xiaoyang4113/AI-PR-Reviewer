package com.aiprreviewer.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.aiprreviewer.model.enums.RiskLevel;

/**
 * 评审意见实体，映射 review_comments 表
 */
@Data
@TableName("review_comments")
public class ReviewComment {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的评审任务ID */
    private Long taskId;

    /** 发生变更的文件相对路径 */
    private String filePath;

    /** 问题所在行号 */
    private Integer lineNumber;

    /** 风险等级 */
    private RiskLevel riskLevel;

    /** 触发审查的原始代码片段 */
    private String matchCode;

    /** AI 给出的修改建议 */
    private String suggestion;

    /** AI 提供的优化代码示例 */
    private String optimizedCode;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
