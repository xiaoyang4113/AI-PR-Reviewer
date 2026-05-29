package com.aiprreviewer.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 代码风险等级
 */
public enum RiskLevel {
    /** 建议优化，低优先级 */
    INFO("INFO"),
    /** 建议修复，中优先级 */
    WARNING("WARNING"),
    /** 必须修复，高优先级 */
    CRITICAL("CRITICAL");

    /** 存入数据库的值 */
    @EnumValue
    private final String code;

    RiskLevel(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
