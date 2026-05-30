package com.aiprreviewer.service;

import com.aiprreviewer.model.dto.GitHubDiffResult;

/**
 * 代码托管平台抽象接口（策略模式）
 *
 * 新增平台只需实现此接口，无需改动业务编排层。
 * 已实现：GitHub
 * 可扩展：Gitee、GitLab、Bitbucket、Coding
 */
public interface CodePlatformService {

    /**
     * 判断是否支持该 URL
     */
    boolean supports(String repoUrl);

    /**
     * 获取 PR Diff 和元信息
     */
    GitHubDiffResult fetchDiff(String repoUrl, Integer prNumber);
}
