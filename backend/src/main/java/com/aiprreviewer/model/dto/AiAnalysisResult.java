package com.aiprreviewer.model.dto;

import lombok.Data;
import java.util.List;

/**
 * AI 分析结果，对应 AI API 返回的 JSON 结构
 */
@Data
public class AiAnalysisResult {

    /** 变更总结 */
    private String summary;

    /** 评审意见列表 */
    private List<AiCommentItem> comments;

    /**
     * AI 返回的单条评论（内部类，仅用于 JSON 反序列化）
     */
    @Data
    public static class AiCommentItem {
        /** 变更文件路径 */
        private String filePath;
        /** 问题行号 */
        private Integer lineNumber;
        /** 风险等级（JSON字符串，后续转为枚举） */
        private String riskLevel;
        /** 触发问题的原始代码 */
        private String matchCode;
        /** AI修改建议 */
        private String suggestion;
        /** AI提供的优化代码 */
        private String optimizedCode;
    }
}
