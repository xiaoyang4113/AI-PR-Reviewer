package com.aiprreviewer.service;

import com.aiprreviewer.model.dto.GitHubDiffResult;

/**
 * GitHub API 交互服务接口
 */
public interface GitHubService {

    /**
     * 获取 Pull Request 的 Diff 数据和元信息
     *
     * @param repoUrl  GitHub 仓库地址
     * @param prNumber PR 编号
     * @return diff 文本 + PR 标题 + 文件数
     */
    GitHubDiffResult fetchPullRequestDiff(String repoUrl, Integer prNumber);
}
