package com.aiprreviewer.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 评审任务状态
 */
public enum ReviewStatus {
    /** 待处理 */
    PENDING("PENDING"),
    /** 分析中 */
    PROCESSING("PROCESSING"),
    /** 已完成 */
    COMPLETED("COMPLETED"),
    /** 失败 */
    FAILED("FAILED");

    /** 存入数据库的值 */
    @EnumValue
    private final String code;

    ReviewStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
