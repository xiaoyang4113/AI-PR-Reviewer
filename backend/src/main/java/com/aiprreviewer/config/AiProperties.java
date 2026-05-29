package com.aiprreviewer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 服务配置（映射 app.ai.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** API 基础地址 */
    private String apiBase = "https://api.deepseek.com/v1";

    /** API 密钥 */
    private String apiKey;

    /** 模型名称 */
    private String model = "deepseek-v4-flash";

    /** 最大输出 Token 数 */
    private int maxTokens = 4096;

    /** 生成温度（0-2，越低越确定） */
    private double temperature = 0.2;

    /** 单次分析最大 diff 字符数 */
    private int maxDiffLength = 8000;
}
