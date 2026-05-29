package com.aiprreviewer.service;

import com.aiprreviewer.model.dto.AiAnalysisResult;

/**
 * AI 代码分析服务接口
 */
public interface AiAnalysisService {

    /**
     * 分析代码 Diff，返回结构化评审结果
     *
     * @param diffText git diff 文本
     * @return 总结 + 评审意见列表
     */
    AiAnalysisResult analyzeDiff(String diffText);

    /**
     * 获取当前使用的 AI 模型名称
     */
    String getModelName();
}
