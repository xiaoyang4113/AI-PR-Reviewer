package com.aiprreviewer.model.dto;

import com.aiprreviewer.model.enums.RiskLevel;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 单条评审意见，返回给前端展示
 */
@Data
public class CommentDTO {

    /** 评论ID */
    private Long id;

    /** 变更文件路径 */
    private String filePath;

    /** 问题行号 */
    private Integer lineNumber;

    /** 风险等级 */
    private RiskLevel riskLevel;

    /** 触发问题的原始代码 */
    private String matchCode;

    /** AI修改建议 */
    private String suggestion;

    /** AI提供的优化代码示例 */
    private String optimizedCode;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
