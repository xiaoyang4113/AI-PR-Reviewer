package com.aiprreviewer.model.enums;

/**
 * 代码风险等级
 */
public enum RiskLevel {
    /** 建议优化，低优先级 */
    INFO,
    /** 建议修复，中优先级 */
    WARNING,
    /** 必须修复，高优先级 */
    CRITICAL
}
