package com.aiprreviewer.service.impl;

import com.aiprreviewer.config.GitHubProperties;
import com.aiprreviewer.model.constant.ReviewConstants;
import com.aiprreviewer.model.dto.GitHubDiffResult;
import com.aiprreviewer.exception.BusinessException;
import com.aiprreviewer.service.GitHubService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub API 交互服务实现
 *
 * 核心流程：
 * 1. 解析并校验用户输入的 GitHub URL（防止 SSRF 攻击）
 * 2. 调用 GitHub REST API 获取 PR 元信息（标题）
 * 3. 调用 GitHub REST API 获取 PR 的 diff 文本
 * 4. 统计变更文件数量
 *
 * 安全措施：
 * - URL 白名单校验：仅允许 api.github.com 域名
 * - Token 不记录到日志
 * - 每次 API 调用创建独立的 HttpHeaders 实例，防止请求头交叉污染
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubServiceImpl implements GitHubService {

    private final RestTemplate restTemplate;
    private final GitHubProperties gitHubProperties;

    /** GitHub 仓库 URL 正则：提取 owner 和 repo 名称 */
    private static final Pattern GITHUB_URL_PATTERN =
            Pattern.compile(ReviewConstants.GITHUB_URL_PATTERN, Pattern.CASE_INSENSITIVE);

    /** diff 文件标记，用于统计变更文件数 */
    private static final Pattern DIFF_FILE_PATTERN =
            Pattern.compile("^" + ReviewConstants.DIFF_FILE_PREFIX, Pattern.MULTILINE);

    @Override
    public GitHubDiffResult fetchPullRequestDiff(String repoUrl, Integer prNumber) {
        // 步骤1：解析并校验 URL
        String[] ownerAndRepo = parseAndValidateRepoUrl(repoUrl);
        String owner = ownerAndRepo[0];
        String repo = ownerAndRepo[1];

        // 构建 API 请求 URL
        String prUrl = String.format("%s/repos/%s/%s/pulls/%d",
                gitHubProperties.getApiBase(), owner, repo, prNumber);

        // 步骤2-3：分别获取 PR 标题和 diff（各自独立的 headers）
        String prTitle = fetchPrTitle(prUrl);
        String diffText = fetchPrDiff(prUrl);

        // 步骤4：统计变更文件数
        int fileCount = countChangedFiles(diffText);

        log.info("GitHub PR Diff 获取成功: {}/{} PR:#{} 标题:{} 文件数:{} diff大小:{}字符",
                owner, repo, prNumber, prTitle, fileCount, diffText.length());

        return new GitHubDiffResult(diffText, prTitle, fileCount);
    }

    /**
     * 解析并校验 GitHub 仓库 URL，防止 SSRF 攻击
     */
    private String[] parseAndValidateRepoUrl(String repoUrl) {
        if (!StringUtils.hasText(repoUrl)) {
            throw new BusinessException("仓库地址不能为空");
        }

        // 去除尾部斜杠，统一格式
        String normalizedUrl = repoUrl.trim().replaceAll("/$", "");
        Matcher matcher = GITHUB_URL_PATTERN.matcher(normalizedUrl);
        if (!matcher.find()) {
            throw new BusinessException("无效的 GitHub 仓库地址格式，示例: https://github.com/owner/repo");
        }

        String owner = matcher.group(1);
        // 去除可能的 .git 后缀
        String repo = matcher.group(2).replace(ReviewConstants.GIT_SUFFIX, "");
        return new String[]{owner, repo};
    }

    /**
     * 创建 HTTP 请求头（每次调用返回新实例，防止并发问题）
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ReviewConstants.HEADER_USER_AGENT, ReviewConstants.USER_AGENT_VALUE);
        // 配置了 Token 则加上认证头
        if (StringUtils.hasText(gitHubProperties.getToken())) {
            headers.set(ReviewConstants.HEADER_AUTHORIZATION,
                    ReviewConstants.BEARER_PREFIX + gitHubProperties.getToken());
        }
        return headers;
    }

    /**
     * 获取 PR 标题（API 响应为 JSON 格式）
     */
    private String fetchPrTitle(String url) {
        try {
            HttpHeaders headers = createHeaders();
            headers.set(ReviewConstants.HEADER_ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JsonNode.class);

            if (response.getBody() != null && response.getBody().has(ReviewConstants.PR_TITLE_FIELD)) {
                return response.getBody().get(ReviewConstants.PR_TITLE_FIELD)
                        .asText(ReviewConstants.DEFAULT_PR_TITLE);
            }
            return ReviewConstants.DEFAULT_PR_TITLE;
        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException("PR 不存在或仓库为私有仓库，请检查链接是否正确");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new BusinessException("GitHub API 访问被拒绝，可能是 API 限流，建议配置 GITHUB_TOKEN");
        } catch (Exception e) {
            log.error("获取 PR 标题失败: {}", e.getMessage());
            throw new BusinessException("获取 PR 信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取 PR Diff（API 响应为纯文本格式）
     */
    private String fetchPrDiff(String url) {
        try {
            HttpHeaders headers = createHeaders();
            // 使用 GitHub 专用的 diff Accept 头，返回纯文本 diff
            headers.set(ReviewConstants.HEADER_ACCEPT, ReviewConstants.GITHUB_DIFF_ACCEPT);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            String diffText = response.getBody();
            if (!StringUtils.hasText(diffText)) {
                throw new BusinessException("该 PR 没有代码变更（可能是空提交或已合并）");
            }
            return diffText;
        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException("PR 不存在或仓库为私有仓库，请检查链接是否正确");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new BusinessException("GitHub API 访问被拒绝，可能是 API 限流，建议配置 GITHUB_TOKEN");
        } catch (BusinessException e) {
            throw e; // 业务异常直接抛出
        } catch (Exception e) {
            log.error("获取 PR Diff 失败: {}", e.getMessage());
            throw new BusinessException("获取 PR Diff 失败: " + e.getMessage());
        }
    }

    /**
     * 统计 diff 中变更的文件数量（以 "diff --git" 开头的行为标记）
     */
    private int countChangedFiles(String diffText) {
        if (!StringUtils.hasText(diffText)) {
            return 0;
        }
        Matcher matcher = DIFF_FILE_PATTERN.matcher(diffText);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
