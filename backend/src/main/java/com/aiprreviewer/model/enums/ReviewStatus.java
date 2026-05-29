package com.aiprreviewer.model.enums;

/**
 * 评审任务状态
 */
public enum ReviewStatus {
    /** 待处理 */
    PENDING,
    /** 分析中 */
    PROCESSING,
    /** 已完成 */
    COMPLETED,
    /** 失败 */
    FAILED
}
