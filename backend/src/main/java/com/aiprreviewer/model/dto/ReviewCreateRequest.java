package com.aiprreviewer.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建评审任务请求参数
 */
@Data
public class ReviewCreateRequest {

    /** GitHub 仓库地址，如 https://github.com/owner/repo */
    @NotBlank(message = "GitHub 仓库地址不能为空")
    @Pattern(regexp = "^https?://github\\.com/[a-zA-Z0-9._-]+/[a-zA-Z0-9._-]+/?$",
             message = "GitHub 仓库地址格式无效，示例: https://github.com/owner/repo")
    private String repoUrl;

    /** PR 编号，不能为空且必须大于 0 */
    @NotNull(message = "PR 编号不能为空")
    @Min(value = 1, message = "PR 编号必须大于 0")
    private Integer prNumber;
}
